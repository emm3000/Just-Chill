#!/usr/bin/env bash
#
# Proves the `backups` bucket's RLS isolates tenants — through the Storage REST API, the same edge
# `SupabaseBackupObjectStore` talks to, not through the policy text in
# supabase/migrations/20260814200043_backup_storage_bucket.sql.
#
# Run it by hand:
#
#     supabase start                          # the probe never boots or stops the stack itself
#     supabase/tests/backups-rls-probe.sh
#
# No secrets: every key comes from `supabase status`, which prints the local stack's well-known
# development keys. It signs up two throwaway users and uploads objects, so it must never be aimed
# at the hosted project.
#
# Every refusal it asserts is paired with the identical request issued by the owner, which
# succeeds. An empty listing or a 400 proves nothing on its own — the pair does.

set -euo pipefail

readonly BUCKET='backups'
# Six characters is the local stack's minimum (`auth.minimum_password_length`, supabase/config.toml).
readonly PASSWORD='rls-probe-password'

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
readonly REPO_ROOT="${script_dir%/supabase/tests}"
RUN_ID="$(date +%s)-$$"
readonly RUN_ID

WORK_DIR="$(mktemp -d)"
readonly WORK_DIR
# One response file, because bash cannot return a status and a body from the same call.
readonly RESPONSE_BODY="$WORK_DIR/response"
# Cleanup lists are files, not arrays: this has to run under the bash 3.2 that ships with macOS.
readonly PROBE_OBJECTS="$WORK_DIR/objects"
readonly PROBE_USERS="$WORK_DIR/users"

trap cleanup EXIT

assertions=0
failures=0

die() {
  printf 'ERROR: %s\n' "$*" >&2
  exit 1
}

response_excerpt() {
  local body
  body="$(< "$RESPONSE_BODY")"
  printf '%.200s' "${body//$'\n'/ }"
}

pass() {
  assertions=$((assertions + 1))
  printf '  ok    %s\n' "$1"
}

fail() {
  assertions=$((assertions + 1))
  failures=$((failures + 1))
  printf '  FAIL  %s\n' "$1"
  printf '        body: %s\n' "$(response_excerpt)"
}

assert_status() {
  local expected=$1 actual=$2 description=$3
  if [ "$expected" = "$actual" ]; then
    pass "$description"
  else
    fail "$description — expected HTTP $expected, got HTTP $actual"
  fi
}

assert_refused() {
  local actual=$1 description=$2
  if [ "$actual" = '200' ]; then
    fail "$description — the request SUCCEEDED"
  else
    pass "$description (HTTP $actual)"
  fi
}

assert_body_holds() {
  local needle=$1 description=$2 body
  body="$(< "$RESPONSE_BODY")"
  if [ "${body#*"$needle"}" != "$body" ]; then
    pass "$description"
  else
    fail "$description — the payload did not come back"
  fi
}

assert_body_lacks() {
  local needle=$1 description=$2 body
  body="$(< "$RESPONSE_BODY")"
  if [ "${body#*"$needle"}" != "$body" ]; then
    fail "$description — the payload came back"
  else
    pass "$description"
  fi
}

listing_holds() {
  jq -e --arg name "$1" 'any(.[]; .name == $name)' "$RESPONSE_BODY" >/dev/null 2>&1
}

assert_listing_holds() {
  if listing_holds "$1"; then
    pass "$2"
  else
    fail "$2 — the object is not in the listing"
  fi
}

assert_listing_lacks() {
  if listing_holds "$1"; then
    fail "$2 — the object is in the listing"
  else
    pass "$2"
  fi
}

assert_listing_empty() {
  if jq -e 'length == 0' "$RESPONSE_BODY" >/dev/null 2>&1; then
    pass "$1"
  else
    fail "$1 — the listing is not empty"
  fi
}

# Storage calls carry the anon key as `apikey` and the caller's JWT as the bearer. Passing the anon
# key as both is how this probe speaks as an unauthenticated client.
storage_upload() {
  local token=$1 key=$2 payload=$3
  # Bare `application/json`, no charset: the bucket's allowed_mime_types is a verbatim match.
  curl -sS -o "$RESPONSE_BODY" -w '%{http_code}' -X POST "$API_URL/storage/v1/object/$BUCKET/$key" \
    -H "apikey: $ANON_KEY" -H "Authorization: Bearer $token" \
    -H 'Content-Type: application/json' -H 'x-upsert: false' \
    --data-binary "$payload"
}

storage_list() {
  local token=$1 prefix=$2
  curl -sS -o "$RESPONSE_BODY" -w '%{http_code}' -X POST "$API_URL/storage/v1/object/list/$BUCKET" \
    -H "apikey: $ANON_KEY" -H "Authorization: Bearer $token" \
    -H 'Content-Type: application/json' \
    --data-binary "$(jq -n --arg prefix "$prefix" '{prefix: $prefix, limit: 100}')"
}

storage_download() {
  local token=$1 key=$2
  curl -sS -o "$RESPONSE_BODY" -w '%{http_code}' "$API_URL/storage/v1/object/$BUCKET/$key" \
    -H "apikey: $ANON_KEY" -H "Authorization: Bearer $token"
}

storage_delete() {
  local token=$1 key=$2
  # storage.protect_delete() refuses `delete from storage.objects`, so this is the only way an
  # object ever leaves the bucket.
  curl -sS -o "$RESPONSE_BODY" -w '%{http_code}' -X DELETE "$API_URL/storage/v1/object/$BUCKET/$key" \
    -H "apikey: $ANON_KEY" -H "Authorization: Bearer $token"
}

# The service role bypasses RLS. It is used here and in require_bucket only, both outside the
# assertions — an assertion holding this key would prove nothing.
delete_probe_user() {
  curl -sS -o /dev/null -X DELETE "$API_URL/auth/v1/admin/users/$1" \
    -H "apikey: $SERVICE_ROLE_KEY" -H "Authorization: Bearer $SERVICE_ROLE_KEY" || true
}

cleanup() {
  local token key uid
  if [ -f "$PROBE_OBJECTS" ]; then
    while read -r token key; do
      storage_delete "$token" "$key" >/dev/null 2>&1 || true
    done < "$PROBE_OBJECTS"
  fi
  if [ -f "$PROBE_USERS" ]; then
    while read -r uid; do
      delete_probe_user "$uid"
    done < "$PROBE_USERS"
  fi
  rm -rf "$WORK_DIR"
}

read_stack_config() {
  local status_json
  status_json="$(supabase status --workdir "$REPO_ROOT" -o json 2>/dev/null)" ||
    die "no local Supabase stack answered. Run 'supabase start' first."
  API_URL="$(jq -r '.API_URL' <<< "$status_json")"
  ANON_KEY="$(jq -r '.ANON_KEY' <<< "$status_json")"
  SERVICE_ROLE_KEY="$(jq -r '.SERVICE_ROLE_KEY' <<< "$status_json")"
  readonly API_URL ANON_KEY SERVICE_ROLE_KEY
  [ -n "$API_URL" ] && [ "$API_URL" != 'null' ] || die "'supabase status' returned no API_URL."
}

require_bucket() {
  local status
  status="$(curl -sS -o "$RESPONSE_BODY" -w '%{http_code}' "$API_URL/storage/v1/bucket/$BUCKET" \
    -H "apikey: $SERVICE_ROLE_KEY" -H "Authorization: Bearer $SERVICE_ROLE_KEY")"
  [ "$status" = '200' ] ||
    die "bucket '$BUCKET' is not on this stack (HTTP $status). Run 'supabase db reset' to apply supabase/migrations."
  printf '\nthe bucket itself\n'
  if jq -e '.public == false' "$RESPONSE_BODY" >/dev/null 2>&1; then
    pass "'$BUCKET' is private"
  else
    fail "'$BUCKET' is public — a public bucket serves a snapshot to anyone holding its name"
  fi
}

# Signup returns a session because `auth.email.enable_confirmations` is false on the local stack.
sign_up() {
  local label=$1 email status
  email="justchill-rls-probe-$label-$RUN_ID@example.com"
  status="$(curl -sS -o "$RESPONSE_BODY" -w '%{http_code}' "$API_URL/auth/v1/signup" \
    -H "apikey: $ANON_KEY" -H 'Content-Type: application/json' \
    --data-binary "$(jq -n --arg email "$email" --arg password "$PASSWORD" \
      '{email: $email, password: $password}')")"
  [ "$status" = '200' ] || die "could not sign up $email (HTTP $status): $(response_excerpt)"
  jq -r '.user.id' "$RESPONSE_BODY" >> "$PROBE_USERS"
  jq -r '"\(.user.id) \(.access_token)"' "$RESPONSE_BODY"
}

assert_prefix_is_owner_only() {
  local owner=$1 owner_token=$2 owner_uid=$3 intruder=$4 intruder_token=$5
  local snapshot_name="backup-v3-$RUN_ID.json"
  local planted_name="planted-by-$intruder-$RUN_ID.json"
  local snapshot="$owner_uid/$snapshot_name"
  local pinned="$owner_uid/pinned/$snapshot_name"
  local planted="$owner_uid/$planted_name"
  local marker="owned-by-$owner_uid"
  local payload="{\"marker\":\"$marker\"}"

  printf '\n%s owns %s/ — %s must not reach into it\n' "$owner" "$owner_uid" "$intruder"

  printf '%s %s\n%s %s\n' "$owner_token" "$snapshot" "$owner_token" "$pinned" >> "$PROBE_OBJECTS"
  # The planted key can only exist if the bucket is broken; tracking it keeps a failed run from
  # leaving residue behind.
  printf '%s %s\n' "$intruder_token" "$planted" >> "$PROBE_OBJECTS"
  assert_status 200 "$(storage_upload "$owner_token" "$snapshot" "$payload")" \
    "$owner uploads a snapshot to its own prefix"
  # pinned/ is the bucket's second key shape; the policy reads path segment 1 so one predicate
  # covers both.
  assert_status 200 "$(storage_upload "$owner_token" "$pinned" "$payload")" \
    "$owner uploads under its own pinned/ subfolder"

  storage_list "$owner_token" "$owner_uid/" >/dev/null
  assert_listing_holds "$snapshot_name" "$owner lists its own prefix and sees the snapshot"
  storage_download "$owner_token" "$snapshot" >/dev/null
  assert_body_holds "$marker" "$owner downloads its own snapshot"

  storage_list "$intruder_token" "$owner_uid/" >/dev/null
  assert_listing_empty "$intruder lists the same prefix and sees nothing"
  assert_refused "$(storage_download "$intruder_token" "$snapshot")" \
    "$intruder cannot download the snapshot"
  assert_body_lacks "$marker" "$intruder received no snapshot bytes"
  assert_refused "$(storage_download "$intruder_token" "$pinned")" \
    "$intruder cannot download the pinned snapshot"
  assert_refused "$(storage_delete "$intruder_token" "$snapshot")" \
    "$intruder cannot delete the snapshot"
  assert_refused "$(storage_upload "$intruder_token" "$planted" '{"marker":"planted"}')" \
    "$intruder cannot upload into the prefix"

  storage_list "$ANON_KEY" "$owner_uid/" >/dev/null
  assert_listing_empty "an unauthenticated client lists the prefix and sees nothing"
  assert_refused "$(storage_download "$ANON_KEY" "$snapshot")" \
    "an unauthenticated client cannot download the snapshot"

  # The refusals above are worth something only if the object survived them intact.
  storage_download "$owner_token" "$snapshot" >/dev/null
  assert_body_holds "$marker" "$owner still downloads its snapshot afterwards"
  storage_list "$owner_token" "$owner_uid/" >/dev/null
  assert_listing_lacks "$planted_name" "$owner's prefix holds nothing $intruder planted"

  assert_status 200 "$(storage_delete "$owner_token" "$snapshot")" \
    "$owner deletes its own snapshot"
  assert_status 200 "$(storage_delete "$owner_token" "$pinned")" \
    "$owner deletes its own pinned snapshot"
}

main() {
  local dependency
  for dependency in curl jq supabase; do
    command -v "$dependency" >/dev/null 2>&1 || die "$dependency is required and not on PATH."
  done

  read_stack_config
  require_bucket

  local a_session b_session a_uid a_token b_uid b_token
  a_session="$(sign_up a)"
  b_session="$(sign_up b)"
  read -r a_uid a_token <<< "$a_session"
  read -r b_uid b_token <<< "$b_session"

  assert_prefix_is_owner_only 'A' "$a_token" "$a_uid" 'B' "$b_token"
  assert_prefix_is_owner_only 'B' "$b_token" "$b_uid" 'A' "$a_token"

  printf '\n'
  if [ "$failures" -eq 0 ]; then
    printf '%s assertions passed — the backups bucket isolates its tenants.\n' "$assertions"
    return 0
  fi
  printf '%s of %s assertions FAILED — the backups bucket does NOT isolate its tenants.\n' \
    "$failures" "$assertions"
  return 1
}

main "$@"
