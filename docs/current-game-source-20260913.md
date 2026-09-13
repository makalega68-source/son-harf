# Current game source checkpoint — 2026-09-13

The score/ownership preview built from `main` at `6be985f` omitted 28 newer
changes on PR #369 (`feat/ui-polish-auth-home-20260913`, head `4d8df34`).
The user reported that this delivered the old game. A successful build of
`main` alone is therefore not sufficient evidence of the current product.

PR #370 now integrates that entire development head with the green/red cube
ownership and separate word/territory/total score UI. Preserve the premium
home, four destinations, simplified profile and competition, adaptive launcher,
per-game TR/EN selection and master dictionary v5. Scoring remains permanent
word points plus two points for each currently owned cube.

Before the next APK, check current development PRs and release provenance as
well as `main`, preserve subsequent changes, and verify the build contains the
latest accepted development state. The two September 13 development lines
must not be replaced by a standalone build of the older main branch.

Integration repairs keep theme selection reachable through Profile → Settings,
show weekly RP independently of rating and align existing regression checks
with the current screens and canonical v5 dictionary. Database migrations and
live database state are not changed by these repairs.
