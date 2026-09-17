# Code Quality — What We Enforce and Who Enforces It

Two halves, different owners.

- **Machine half** — what detekt measures, with a baseline so the gate never reddens on code that
  predates a rule. Owner: `config/detekt/detekt.yml` plus `qualityGate`.
- **Reviewer half** — what no tool sees: whether a module boundary is cut in the right place, whether
  duplication is repeated knowledge or coincidence, whether an abstraction answers a real need. Owner
  fixed by [ADR 007](adr/007-one-way-of-working-writer-reviewer-and-model-tiers.md): reviewer always
  Opus, fresh context, never the writer.

**Admission rule: a rule that falls into neither half does not enter**, and a reviewer-half rule
enters only with a test a diff can fail. No third bucket for taste.

## The machine half

The thresholds are `config/detekt/detekt.yml`; read the file, a copy here goes stale.

### Where detekt goes blind — three kinds, not interchangeable

- **`LongMethod` — blind by config** (`ignoreAnnotated: ['Composable', 'Preview']`), revertible in
  one edit; nothing about the rule is Compose-specific.
- **`LargeClass` — blind structurally.** It counts lines in a *class*; a Compose screen is a
  top-level function, so there is no class to measure. No config change fixes that.
- **Type-resolution rules — blind wherever detekt cannot resolve a symbol.** Unresolvable code is
  downgraded to a warning and the task passes, so a rule that needs type resolution can stay silent
  and still leave the gate green. What a module owes is the `compiler errors found during analysis`
  line its `detekt<Variant>` task prints. What keeps that line off four of the five modules is
  `DetektConventionPlugin.putOwnClassesOnAnalysisClasspath`: AGP 9's built-in Kotlin empties the
  `classesDirs` half of detekt's own `classpath` convention, so anything `excludeGeneratedSources`
  drops from `source` resolves to nothing until the compile task's output is wired back — and
  deleting that wiring deletes analysis silently rather than reddening the gate. `:androidApp` still
  owes 10–13, over a Java-generated `BuildConfig` no Kotlin `destinationDirectory` carries
  (E07-06, issue #91).

`CyclomaticComplexMethod`, `ComplexCondition`, `NestedBlockDepth` and `LongParameterList` have no
annotation escape and **do** fire on Composables — `config/detekt/baseline-ui-android-debug.xml`
carries three `LongParameterList` entries against them. `TooManyFunctions` per file is the rule the
repo *leans on* for Compose, not the only one that reaches it.

### One baseline file per analysis task

detekt 2.0 registers an analysis task per variant, and each one derives its OWN baseline file from
the extension stem `config/detekt/baseline-<module>.xml` — set once in
`build-logic/.../DetektConventionPlugin.kt`, which all five modules apply through `justchill.detekt`.
The suffix is the variant, and the gate's two aggregates fan out into them: `detektMain` runs
`detektDebug` + `detektRelease` over `src/main`, so one violation there lands in
`baseline-<module>-debug.xml` AND `-release.xml`; `detektTest` runs `detektDebugUnitTest` +
`detektDebugAndroidTest`. `:androidApp` fans out further, over its flavors —
`baseline-androidApp-{devDebug,devRelease,prodDebug,prodRelease}.xml`. Because each task derives its
own path, two of them for the same module never overwrite each other.

The stem files themselves (`baseline-<module>.xml`, no suffix) belong to the plain `detekt` task,
which is not on the gate. **Leave them alone.**

To grandfather pre-existing issues, run the matching baseline task and commit what it writes — the
file it writes is the file the analysis task reads, so a wrong guess is self-correcting
(`./gradlew :data:detektBaselineMain`). **NEVER baseline to dodge a NEW violation** the current
change introduced; a baseline only ever grandfathers what predates the rule.

## Three gotchas, all found by measurement

**1. A baseline entry for a file-level rule is permanent amnesty.** The ID carries no count —
`TooManyFunctions:AccountsScreen.kt:com.emm.justchill.hh.account.AccountsScreen.kt` nowhere says
"11 functions" — so the file is exempt at *any* size, forever. The baseline stops new bleeding and
creates **zero** pressure on old code, and nothing will ever ask for the burn-down. Read the current
holders out of the baseline file, never out of this one — that file is the only list there is.

**2. `ignoreAnnotatedFunctions` takes simple annotation names, not fully-qualified ones.** detekt
matches the name as written in the source: `androidx.compose.ui.tooling.preview.Preview` silently
matches nothing, `Preview` works. Getting it wrong drops screens into permanent amnesty with no
error, no warning and no failing task.

**3. detekt lints test sources with the production thresholds.** `detektDevDebugUnitTest` analyses
`androidApp/src/test`, and `LongMethod` (60 lines) has no test exclusion — a long test method is a
red gate, not a warning. So is `MultiLineIfElse`, in a test too.

The StateFlow test idiom and the `CancellationException` arm every catch-all owes:
`presentation/CLAUDE.md` `## Testing`.

## Arbitration and suppression

- Passing the gate is *necessary, never sufficient* — a reviewer may require a change detekt is
  perfectly happy with. In the other direction, a detekt failure becomes a baseline entry **only
  together with a GitHub issue naming it**; a silent addition is not allowed.
- **Removing an entry, or changing detekt config:** regenerate into a scratch file and diff against
  the committed one. Whoever touches the file next does it, at minimum — and a config that silently
  matches nothing (gotcha 2) shows up as "no change" and as nothing else.
- **Prefer a baseline entry over an inline `@Suppress`:** a baseline entry is inventoried in a file
  someone can count; an inline `@Suppress` is invisible and permanent. One carries a written
  justification at the same place, **re-checked whenever the thing it suppresses changes** — delete
  one method and the suppression goes inert while its comment still claims a vanished overage.

## Comments

**Default is zero comments. A comment exists only to state a constraint the code cannot show — the
why, never the what.**

- **The test (a diff can fail it):** delete the comment mentally. If only a restatement of the code
  is lost, delete it for real. If the *what* becomes unclear, rename or extract instead.
- Survivors: product decisions invisible in code (the exclusive `>` on `BACKUP_STALE_AFTER_DAYS`),
  warnings about non-obvious consequences, invariants no test pins, deliberate duplication markers,
  `@Suppress` justifications. Each is 1–3 lines of present-tense fact. **Zero history:** a sentence
  about what the code used to be, what replaced what, or what a review said is deleted, not rephrased.
- **No KDoc by default**, on any declaration. **This bullet decides
  EXISTENCE, never form** — read alone it has been misread as "KDoc never". What survives picks its
  syntax by POSITION: KDoc on a declaration something else calls (the only form the IDE shows at the
  call site, and the only one whose `[Symbol]` a rename keeps honest); `//` on a statement inside a
  body, which KDoc cannot attach to anyway. **A `@Test` takes `//`** — nothing calls it, and its name
  is its documentation.
- **A comment describing behaviour enforced elsewhere is a copy, and copies diverge.** A constraint
  lives once, at the declaration it constrains — not at a call site, not on a consuming type, not
  further down the same file (`MAX_CATCH_UP_MONTHS` carries its product limit at the `const val` in
  `RecurringDueRules.kt` and nowhere else). Move it to the declaration; never keep both. Diagrams the
  same: **one source, maximum.**
- **A true constraint is not sufficient to survive.** True *and* not already at the declaration.
  Blocks failing the second half get deleted whole — shortening a duplicate leaves a duplicate, and
  converting KDoc to `//` decides nothing. A comment that promises more than the code delivers is
  worse than none.

### The ladder — "reduced it" is a failure signal

**Delete** (it says what the code does) → **rename** (it says what a name should have said:
`BackupHealth.canUploadToDestination`) → **redesign** (it warns about a misuse the design permits — a
read-once warning on `nowMillis()` is a signature that should take the instant) → **comment**, only
when none of the three can carry it. **If you are shortening a comment, you are almost certainly on
the wrong step:** reduction is what deleting looks like when someone flinched.

**Enforcement is the reviewer, not detekt.** A `ForbiddenComment` pattern sees only the word, so the
way to satisfy it is to rephrase — the one move this policy forbids — and enabling it would force the
whole repo green in one sweep, turning an incremental policy into a mass migration of delegated
judgment calls nobody can review. The policy applies at two moments instead, either able to fail a diff:

1. **New code ships with zero comments** unless a line passes the survivor test. A new comment
   explaining *what* the code does is a naming or decomposition defect; the fix is the rename or the
   extraction, not the sentence.
2. **Code touched in passing gets stripped** — the file already in the diff, same commit; not the
   whole module, not a cleanup ticket. The reviewer's second half already reads that file, so the
   cost is bounded and the surface converges as the code moves.

No repo-wide sweep: files converge under rule 2 or they do not, and that is the accepted trade — a
comment nobody has touched in a year is not the one misleading anybody.

## The acronyms — verdict table

Each **in** row carries the test that makes a finding falsifiable:

| Item | Verdict | Reason and test |
|---|---|---|
| **SOLID S** | **in** | SRP decides where modules cut. **Test:** an SRP finding must name the *two unrelated reasons* the unit would change. If the reviewer cannot name two, it is not a finding. |
| **SOLID D** | **in** | DIP is why `:domain` does not know `:data`. **Test:** mechanical — a DIP finding must point at a forbidden import line or a module dependency that should not exist. No import, no finding. |
| **SOLID O** | weak | Open/Closed is the usual excuse for premature abstraction. |
| **SOLID L** | weak | Almost no inheritance hierarchies here to violate it. |
| **SOLID I** | weak | The `:domain` `*Repository` interfaces are wide, most consumers use one or two methods, and there are no role clusters to split along. Strictly ISP is violated; splitting buys no decoupling. **Test:** an ISP finding must name a consumer a split would actually decouple — never a method count. |
| **YAGNI** | **in, and it bites** | Solo developer, no third-party users. Every abstraction built for a future is pure cost. |
| **DRY** | **in — about knowledge, not text** | Two blocks that change for different reasons must not be unified. *Duplication is far cheaper than the wrong abstraction.* |
| **KISS** | **out** | Unfalsifiable. Nobody ever chose the complex option on purpose, so nothing can be reviewed against it. |
| **Design patterns** | not a checklist | A pattern is the name of a solution you arrived at, not a target to hit. |

`:presentation` and `:ui-android` share package names **on purpose**: "deduplicating" across that
boundary collapses it. **The open tension, unresolved:** YAGNI and Clean Architecture pull against
each other — Clean Architecture is speculative generality by design, and this repo already paid that
bill in the sync layer.

## Compose sizing

Function length is a weak signal in Compose: a flat 200-line layout reads fine; a 60-line one with
remembered state and nested conditionals does not. What correlates with pain is one file carrying
state, layout *and* N sub-components — so the repo measures **decomposition, not length**, and both
routes to it count equally: a component package (`core/ui/atoms/`, `hh/report/components/`,
`hh/transaction/sheets/`) or sibling files in the feature package (`hh/profile/`, `hh/recurring/`,
`hh/seetransactions/`). Neither is preferred; a screen file holding its own sub-components is what
fails review.

`core/ui/atoms/` is the design system — `DESIGN_SYSTEM.md` holds its criteria, not a component index;
`com/emm/justchill/components/` holds legacy `Emm*` widgets that are *not* the design system; and
`hh/shared/` is the cross-feature package, not a feature package (`ui-android/CLAUDE.md`).

A parameter carrying a default does not count toward `LongParameterList`. The number that measures
coupling is the count of parameters every caller must supply, not the count declared — a defaulted
parameter is opt-in surface, which is Compose's own idiom (`androidx.compose.material3.Button` takes
ten parameters, eight of them defaulted). A composable with two required parameters and a dozen
defaulted ones is not the same defect as one with eight required and none, and a rule counting
declared parameters cannot tell them apart. The flag does not launder a real overage: a caller reads
only the arguments it passes, and `LongMethod` and this section's decomposition rule already own the
size of what a composable builds out of what it receives.

## Use cases

**A use case exists where there is domain logic. A pure read may go from ViewModel to repository.**
ViewModels in `:presentation` inject `:domain` repositories directly wherever the read is pure.
**Test:** a use case whose whole body is one `repository.x(...)` call on a READ is a rename, not a
layer. Writes earn one far more often than reads, because a write is where the invariants are.

**A repository write taking a fully-built aggregate makes skipping its use case type-legal.**
`LoanRepository.create`/`update` take a whole `Loan` whose `totalDue` the domain already computed via
`LoanMath`, and `AddEditLoanViewModel` injects that same repository for its reads, so nothing
compiles against the rule: **loan writes go through `CreateLoanUseCase`/`UpdateLoanUseCase`.**
**Test:** a ViewModel calling `LoanRepository.create` or `update` fails review, however well-formed
the `Loan` it hands over looks.

The leak stops at `:presentation`: `:ui-android` and `:androidApp` production code import no
`:domain` repository (only `androidApp/src/test`, which mocks them).

## Dates

**Whatever asks "what day is it" takes an injected `Clock` AND an injected `TimeZone`, and neither
parameter carries a default.** A Kotlin default never blocks an explicit argument, so a test that
passes a fake clock still compiles and passes against a constructor that defaults to the ambient
one — the default hides the defect instead of failing. `hh/di/SharedModule.kt` is the only place a
clock or a zone enters the graph. `AppGraphKoinTest` asserts by identity (`assertSame`, because
every `Clock.System` reference is `==`) that every graph-built `com.emm.` class holds the bound
instances.

This command regenerates the list of production code that still reads the machine, and its output
**is** the list. Skim the output; never paste its count anywhere.

```
rg -n --type kotlin \
  -e 'LocalDate\.now\(\)' -e 'LocalDateTime\.now\(\)' -e 'Instant\.now\(\)' \
  -e 'System\.currentTimeMillis' -e 'Clock\.System' -e 'currentSystemDefault\(\)' \
  -g '!build/' -g '!**/src/*[Tt]est*/**' -g '!androidApp/src/dev/**' \
  | rg -v ':[0-9]+: *(//|\*)'
```

The second `rg` drops lines that start with a comment marker, which is what keeps KDoc *about*
ambient reads out of the output; it is a heuristic, not a parser, so a trailing comment on a code
line still shows. Test sources and the dev-flavor `androidApp/src/dev/.../experiences/` playground
are outside the rule and read the clock freely.

Everything the command reports beyond the two `SharedModule` factories is deliberate, lives in
`:ui-android`, and holds no value that outlives the screen: `DatePickerSheet` (today, and the zone
it dims future days in), the `SeeTransactionsScreen` `@Preview` helper, and
`PlatformHostActions.suggestedExportFilename` — where the date the user is standing in **is** the
correct answer, and the value never leaves the SAF picker. That filename is an independent read from
the `exportedAt` inside the payload, which comes from the injected clock; only the payload's is
pinned by a test.
