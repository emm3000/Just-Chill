# Advertising ID: the correct Play Console answer is "No"

JustChill does not use the advertising ID. It did: `v2.0.0` and `v2.1.0` shipped
`firebase-analytics`, and their bundles declare the permission for that reason. `50ff8d4d` dropped
the dependency and every bundle from `v2.2.0` on is clean. This file holds the evidence and the
commands that produce it, so the question does not have to be re-investigated the next time Play
asks — it will ask again.

> **Current state (2026-08-20): the declaration says "No", and Play will not accept it.**
> Every pre-submission check fails with *"Incomplete advertising ID declaration"* even though
> App content lists the declaration as actioned and `Need attention` is empty. Turning managed
> publishing off did not clear it. A support case is open. See
> [If Play blocks the "No" answer](#if-play-blocks-the-no-answer).

## Quick path — re-verify any build in about two minutes

```bash
# 1. Grab the AAB the release workflow actually published
gh run download <run-id> -n prod-release-aab-<tag> -D /tmp/aab
unzip -o -q /tmp/aab/*.aab -d /tmp/aab/x

# 2. Search every module manifest. AAB manifests are protobuf, so permission
#    names are plain UTF-8 and ripgrep finds them without bundletool or aapt2.
rg -a -l 'AD_ID' /tmp/aab/x

# 3. CONTROL TEST — this step is not optional
for p in INTERNET USE_BIOMETRIC AD_ID advertising; do
  printf '%-16s ' "$p"
  rg -aoc "$p" /tmp/aab/x/base/manifest/AndroidManifest.xml || echo 0
done
```

Expected: the known permissions return `1`, `AD_ID` and `advertising` return `0`.

**Why the control test matters.** "No match" and "my search is broken" look identical. A zero
result only means something once you have proved the same command finds something you know is
there. Skipping this step is how a search bug gets reported as a clean bill of health.

## Evidence — valid for `v2.5.0` (commit `b5d48191`), nothing newer

All three were run against that bundle and agree. They say nothing about the bundle you are about
to upload: `git rev-list --count v2.5.0..trunk` says how far `trunk` has moved past that tag.
**Re-run the quick path against the AAB that will actually be published before answering the
question again** — this table is a record, not a standing clearance.

| Check | Command | Result |
|---|---|---|
| Published AAB manifest | `rg -a 'AD_ID'` over the unzipped bundle | absent (control test passed: `INTERNET` and `USE_BIOMETRIC` both found) |
| Manifest merger report | `rg -i 'AD_ID\|advertising' androidApp/build/outputs/logs/manifest-merger-prod-release-report.txt` | 0 mentions |
| Runtime classpath | `./gradlew :androidApp:dependencies --configuration prodReleaseRuntimeClasspath` | no `play-services-measurement`, no `play-services-ads-identifier`, no `firebase-analytics` |

The published bundle declares no `com.google.android.gms.permission.*` node of any kind. `v2.4.0`
was checked the same way and was equally clean — and Play still refused its Edit for declaring
`AD_ID`. Play's message describes a permission that is not in the binary.

The merger report is the strongest of the three: it records every manifest node contributed by
every dependency, including nodes that are later removed. Zero mentions means no library ever
asked for the permission — not that something asked and was overridden.

`v2.3.0` was checked the same way and is also clean, and it uploaded to Play without complaint.
Two bundles that are identical in this respect got different answers from Play, which is why the
conflict is understood to be on the Play Console side rather than in the binary.

## The one dependency that looks alarming and is not

`com.google.firebase:firebase-measurement-connector:20.0.1` is on the runtime classpath, pulled in
by `firebase-crashlytics`.

It is an interface, not the analytics SDK. It lets Crashlytics hand breadcrumbs to Firebase
Analytics *when Analytics is present*. Analytics is not present today. The connector declares no
permissions and contains no advertising-ID code. It is an empty socket.

Analytics **was** present, in `v2.0.0` and `v2.1.0`, as `implementation(libs.firebase.analytics)`
in what was then `app/build.gradle.kts`. That is where `AD_ID`,
`ACCESS_ADSERVICES_AD_ID`, `ACCESS_ADSERVICES_ATTRIBUTION` and
`BIND_GET_INSTALL_REFERRER_SERVICE` in those bundles come from — legitimately.

**The search that missed it is the lesson.** A version catalog accessor spells the dependency with
dots, not the hyphen the catalog declares, so `git log -S'firebase-analytics'` finds the `.toml`
entry and not one single call site. It reported an orphan that was in fact wired into the app.
Search both spellings, always:

```bash
git log --oneline --all -S'firebase-analytics'   # the catalog declaration
git log --oneline --all -S'firebase.analytics'   # every actual use
```

The module was called `app/`, not `androidApp/`, before the KMP migration — a path-scoped search
over the current name finds nothing in the old tags.

## Why "Yes" is the wrong answer

Declaring that the app uses the advertising ID is not a harmless way to make a red check go green:

- **It makes Data Safety inconsistent.** Declaring the advertising ID obliges the Data Safety form
  to disclose collection of a persistent advertising identifier. Leaving that form unchanged means
  two official declarations that contradict each other, which is its own policy problem.
- **Users see it.** The Play listing tells users the app collects an advertising ID. For a personal
  finance app with no ads, that is a trust cost bought with nothing.
- **It contradicts the privacy policy**, which promises a telemetry-free build.

The practical risk is that a "Yes" set to unblock one release is never revisited, because nobody
reopens something that is already green.

## If Play blocks the "No" answer

Do not switch back to "Yes". Open a Play Console support case with the evidence above — the three
checks are concrete and reproducible, which is what a support case needs.

"Yes" is not even an escape. It produces its own error — *"your declaration says your app uses
advertising ID, a manifest in one of your active artifacts doesn't include the permission"* — and
the only way past that is the `Release without permission` button, which ships the release with a
false declaration permanently attached to the listing.

Play evaluates declarations against artifacts, not only against the bundle being uploaded, and
**an uploaded bundle can never be deleted** — Play Console offers no way to remove one from the
library. The artifacts that declare `AD_ID` are `v2.0.0` (versionCode 553) and `v2.1.0`. Both show
`0 releases`, so if they are still being counted, only Play can stop counting them: ask in the
support case rather than looking for a button.

There is no urgency to trade the correct answer for a shipped build. The app has no third-party
users, and reaching the author's own device does not need Play — `assembleProdRelease` does.

## Checklist before answering the question again

- [ ] Ran the quick path against the AAB that will actually be published
- [ ] Control test passed (known permissions found, `AD_ID` not found)
- [ ] Manifest merger report still reports 0 mentions
- [ ] No `play-services-measurement` or `play-services-ads-identifier` on the release classpath
- [ ] Answered **No**

## Next step

`v2.5.0` uploaded and **committed its Edit** — the step that refused `v2.4.0` — leaving a draft on
the alpha track. The declaration was then set to "No", which is where it stands and where it stays.

What blocks the release now is not the declaration's content but its state: Play's pre-submission
check calls it incomplete while App content lists it as actioned. Nothing in this repo can move
that. The next step belongs to the support case; when it clears, publish the draft and re-run the
quick path on the release after it.
