# E04-05 — Ship an adaptive icon

**Epic:** [E04 — release and compliance](../epics/E04-release-and-compliance.md)

## Done when

- [ ] an adaptive icon exists with foreground and background layers authored to the 66dp safe
  zone
- [ ] the manifest points at it
- [ ] the legacy rasters remain for pre-26 and are not deleted by accident

## Context

`androidApp/src/main/AndroidManifest.xml` points `android:icon` at a legacy raster set; no
`mipmap-anydpi-v26/` exists anywhere in the repo, so the launcher ignores the OEM mask on Android
8+. Blocked on art: the source file is not in the repo and re-authoring it is not code work.
