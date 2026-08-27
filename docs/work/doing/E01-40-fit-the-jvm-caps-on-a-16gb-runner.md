# E01-40 — Fit the build's JVM caps on a 16 GB runner

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)
**Follows:** E01-35, which raised the caps against local memory only.

## Done when

- [ ] Every hard cap a `qualityGate` run can hold at once on `ubuntu-latest` (4 vCPU / 16 GB) is
      enumerated in the commit body with its number, and the total is ≤ 11 GB.
- [ ] Each cap that moves is justified by a live-set peak measured with `jcmd <pid> GC.run` followed
      by `jcmd <pid> GC.heap_info`, sampled across a full run — not by an unforced `used` reading.
- [ ] The Gradle daemon cap keeps ≥ 25% headroom over its measured post-GC live peak.
- [ ] `build-logic`'s Kotlin daemon cap is measured, not copied from the root build's.
- [ ] Five consecutive `./gradlew qualityGate --rerun-tasks` are green with no daemon killed and no
      `build/` deleted between them, and one `./gradlew assembleProdRelease` is green.
- [ ] No `org.gradle.parallel`, no raised `--max-workers`, no retry added.

## Context

detekt 2.0.0-alpha.6 analyses inside the Gradle daemon, so `org.gradle.jvmargs` is its only heap
(E01-35). Two Kotlin daemons exist and cannot collapse: `build-logic` compiles with Gradle 9.7.1's
embedded Kotlin, the root build with the catalog's — daemon identity is the compiler classpath.
