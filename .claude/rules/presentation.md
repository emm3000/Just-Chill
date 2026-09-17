---
paths:
  - "presentation/**"
---

# `:presentation` — compose-free by review, not by compiler

- No Compose dependency here, ever. `androidx.lifecycle` is the only androidx artifact this module
  holds. ADR 011 Decision 4 made that a reviewed convention: a Compose import compiles and the gate
  stays green, so the check is a grep and it returns nothing today:
  `rg -e 'androidx\.compose' -e 'BuildConfig' -e '\bR\.' -e 'stringResource|painterResource|Font\(R\.' -e '@Preview|tooling\.preview' -e 'LocalConfiguration' -e 'koin\.androidx' presentation/src/main`
- A model carries a semantic id (`iconId`, `colorId`), never an `ImageVector` or a `Color`;
  `:ui-android` resolves it at render time (`CategoryResolve.kt`).
- A ViewModel takes `:domain` interfaces, never a SQLDelight type or a `Default*` implementation.
  The module depends on `:data` for one reason: the Koin modules in `hh/di/` bind interface to
  implementation in one place.
- A Koin binding is registered exactly once: a feature module in `hh/di/`, listed in `appModules()`;
  a platform binding in `:androidApp`'s `androidPlatformModule`. Every new ViewModel goes into
  `AppGraphKoinTest`'s `EXPECTED_VIEW_MODELS`; a binding whose only consumer is a direct
  `koinInject`/`koin.get` outside the graph owes its own test.
- `:ui-android` shares these packages on purpose. A same-package symbol that crosses the module
  boundary is imported explicitly.
- State classes are immutable (`val` plus immutable collections). `:ui-android` declares them stable
  in `compose_stability.conf`, and a `var` or a `MutableMap` turns that declaration into a lie no
  compiler catches.
