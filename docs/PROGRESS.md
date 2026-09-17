# JustChill — Progreso

> Lee esto primero, y luego el `CLAUDE.md` del módulo que vayas a tocar. Acá no se anota
> hash de commit, conteo de commits ni número de línea: el commit que los escribe ya los
> deja viejos. Acá tampoco se anota trabajo: si hay compromiso, es un issue de GitHub.

## Dónde estamos ahora

- App Android de finanzas personales, local-first, en Play Store alpha cerrada.
- Toda migración preserva la data y se prueba con `:data:connectedAndroidDeviceTest` antes de
  `trunk`. El porqué — la instalación del autor tiene data real — lo dice el header de `CLAUDE.md`.
- El trabajo comprometido no se lista acá: vive en GitHub Issues. El índice es
  `gh issue list --label ready-for-agent`; las ideas sin compromiso son issues con `needs-triage`.

## Cómo se verifica el estado

```bash
git rev-list --count origin/trunk..trunk            # commits sin pushear
git rev-list --count --merges origin/trunk..trunk   # debe dar 0: la historia es lineal
gh api repos/emm3000/Just-Chill/branches/trunk/protection \
  --jq '{checks: .required_status_checks.contexts, admins: .enforce_admins.enabled,
         linear: .required_linear_history.enabled, force: .allow_force_pushes.enabled}'
gh issue list --label ready-for-agent               # el trabajo con ticket
git tag --list 'v[0-9]*'                            # los tags de release; no se copian acá
```

GitHub ya impone la historia lineal en `trunk` y le cerró el force-push, así que el merge commit
y la reescritura de historia ya pusheada no dependen de que te acuerdes. Lo que sigue dependiendo
de ti: `enforce_admins: false` es deliberado — pusheas directo a `trunk`, y en ese camino
`quality-gate` **no corre**. La única red ahí es el hook de pre-push, que corre
`qualityGate` local. Si alguna vez quieres que el CI sea la red, hay que prender `enforce_admins`,
y desde ese momento cada cambio necesita branch y PR. Toda la protección se cambia con un `PUT`
del objeto completo, nunca un PATCH.
