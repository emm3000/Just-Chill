# Advertising ID: the correct Play Console answer is "No"

JustChill does not use the advertising ID. No release ever has. This file holds the evidence and
the commands that produce it, so the question does not have to be re-investigated the next time
Play asks — it will ask again.

> **Current state (2026-08-10): the declaration says "Yes", which is false.**
> It was set during the `v2.4.0` upload to clear a rejection. It should go back to "No". See
> [If Play blocks the "No" answer](#if-play-blocks-the-no-answer) before flipping it.

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

## Evidence — valid for `v2.4.0` (commit `dcc176a`), nothing newer

All three were run against that bundle and agree. They say nothing about the bundle you are about
to upload: `git rev-list --count v2.4.0..trunk` says how far `trunk` has moved past that tag.
**Re-run the quick path against the AAB that will actually be published before answering the
question again** — this table is a record, not a standing clearance.

| Check | Command | Result |
|---|---|---|
| Published AAB manifest | `rg -a 'AD_ID'` over the unzipped bundle | absent (control test passed) |
| Manifest merger report | `rg -i 'AD_ID\|advertising' androidApp/build/outputs/logs/manifest-merger-prod-release-report.txt` | 0 mentions |
| Runtime classpath | `./gradlew :androidApp:dependencies --configuration prodReleaseRuntimeClasspath` | no `play-services-measurement`, no `play-services-ads-identifier` |

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
Analytics *when Analytics is present*. Analytics is not present: `firebase-analytics` has only
ever existed as an orphan entry in `gradle/libs.versions.toml` and was never added to any module's
dependencies (`git log -S'firebase-analytics'` confirms this). The connector declares no
permissions and contains no advertising-ID code. It is an empty socket.

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

Note that Play evaluates declarations against releases that are still active in a track, not only
against the bundle being uploaded. If an old active release is the source of the conflict, letting
it be superseded may resolve it on its own.

## Checklist before answering the question again

- [ ] Ran the quick path against the AAB that will actually be published
- [ ] Control test passed (known permissions found, `AD_ID` not found)
- [ ] Manifest merger report still reports 0 mentions
- [ ] No `play-services-measurement` or `play-services-ads-identifier` on the release classpath
- [ ] Answered **No**

## Next step

Flip the Play Console declaration back to "No" once `v2.4.0` has shipped, then re-run the checklist
on the following release to confirm nothing regressed.
