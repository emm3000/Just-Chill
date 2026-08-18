# E01-27 — Drop the orphaned isDebug capability

**Epic:** [E01 — snapshot backup](../epics/E01-snapshot-backup.md)

## Done when

- [ ] `PlatformHostActions.isDebug` (`ui-android/.../hh/shared/PlatformHostActions.kt:41`) and its Android implementation at `:123` are deleted, along with whatever supplies `debuggable` if that becomes its only reader
- [ ] `rg -n 'isDebug' --type kotlin` returns nothing and `assembleDevDebug` passes

## Context

E01-05 deleted the Perfil "Debug" section, whose "Sincronizar ahora" row was the capability's only
consumer. It is a public interface member, so detekt cannot see that it is now dead.
