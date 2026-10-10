# Which commits break tests? jsoup at commit granularity, 2017 to 2020

## Question

Last week we measured, release by release, how many tests written at jsoup 1.11.2 still pass unmodified at later releases. This week we run the same tests at every commit on `master` from 1.11.2 to 1.13.1, so that each loss is attributed to one commit, and we measure the coverage of each test set at the starting release.

## Setup

- **Tests.** Exactly last week's tests, frozen at 1.11.2 (release *t*): the developer suite (594 methods passing at *t*) and the LLM tests generated with the five prompts of Ouédraogo et al. (EMSE 31:103, 2026), one generation per class and prompt with `gpt-oss-120b` (1,862 methods passing at *t* after de-duplication; 2,243 listed, 1,961 passing in three runs, 99 duplicates removed). No test was regenerated or edited.
- **Points.** The 185 consecutive commits on the first-parent line of `master` after *t*, up to 1.13.1 (2020-02-29), none skipped. A merge commit is one point, compared with the previous `master` state. Every commit built with JDK 11. The four release tags in this range (1.11.3, 1.12.1, 1.12.2, 1.13.1) are among the 185.
- **Survival.** Cumulative over commits: a test survives at commit *c* if it passed at every commit from the first to *c*. The denominator is fixed at *t* and survival only decreases; the row for *t* is 100% by definition. A test that stopped surviving is classified by its state at the first commit where it failed: compile failure (whole file), assertion failure, runtime exception, timeout, or class removed.
- **Coverage.** JaCoCo line and branch coverage of the 52 classes under test at *t*, executing only the methods in each group's denominator.

## Results

**Table 1. Surviving tests at the commits that broke five or more tests, and at the release tags** (cell: count and percent of the fixed denominator in the header; the full 185-row series is in the repository).

| # | Commit | Days | Change | Dev (594) | ZSL (342) | FSL (381) | CoT (427) | ToT (345) | GToT (465) | LLM all (1,862) |
|---:|---|---:|---|---:|---:|---:|---:|---:|---:|---:|
| 0 | 1.11.2 (*t*) | 0 | | 594 (100%) | 342 (100%) | 381 (100%) | 427 (100%) | 345 (100%) | 465 (100%) | 1,862 (100%) |
| 14 | 2c4e79b | 146 | DataUtil throws IOException on failed reads | 593 (99.8%) | 342 (100%) | 378 (99.2%) | 427 (100%) | 339 (98.3%) | 459 (98.7%) | 1,847 (99.2%) |
| 23 | **1.11.3** | 147 | | 593 (99.8%) | 342 (100%) | 377 (99.0%) | 427 (100%) | 339 (98.3%) | 459 (98.7%) | 1,846 (99.1%) |
| 30 | f1b885d | 161 | "Spring cleaning" (10 files) | 592 (99.7%) | 331 (96.8%) | 371 (97.4%) | 427 (100%) | 324 (93.9%) | 458 (98.5%) | 1,813 (97.4%) |
| 39 | c8c0569 | 171 | StringBuilder pool in `StringUtil` (13 files) | 591 (99.5%) | 294 (86.0%) | 338 (88.7%) | 427 (100%) | 292 (84.6%) | 413 (88.8%) | 1,686 (90.5%) |
| 41 | ae7d216 | 173 | `StringUtil` moved to `internal` package (merge, 21 files) | 449 (75.6%) | 294 (86.0%) | 338 (88.7%) | 422 (98.8%) | 292 (84.6%) | 413 (88.8%) | 1,682 (90.3%) |
| 68 | **1.12.1** | 539 | | 449 (75.6%) | 294 (86.0%) | 338 (88.7%) | 420 (98.4%) | 292 (84.6%) | 412 (88.6%) | 1,679 (90.2%) |
| 136 | e020854 | 811 | `W3CDom` handles doctypes | 441 (74.2%) | 289 (84.5%) | 338 (88.7%) | 419 (98.1%) | 291 (84.3%) | 409 (88.0%) | 1,669 (89.6%) |
| 142 | **1.12.2** | 811 | | 441 (74.2%) | 289 (84.5%) | 338 (88.7%) | 419 (98.1%) | 291 (84.3%) | 409 (88.0%) | 1,669 (89.6%) |
| 144 | a7b0c2d | 812 | Deprecated classes and methods removed (13 files) | 347 (58.4%) | 236 (69.0%) | 241 (63.3%) | 354 (82.9%) | 224 (64.9%) | 319 (68.6%) | 1,308 (70.2%) |
| 179 | 696461b | 826 | Tokeniser comment handling (3 files) | 347 (58.4%) | 236 (69.0%) | 239 (62.7%) | 331 (77.5%) | 224 (64.9%) | 306 (65.8%) | 1,270 (68.2%) |
| 185 | **1.13.1** | 832 | | **345 (58.1%)** | **236 (69.0%)** | **239 (62.7%)** | **331 (77.5%)** | **224 (64.9%)** | **306 (65.8%)** | **1,270 (68.2%)** |

**Table 2. Commits that stopped the most tests** (Dev lost 249 tests at 13 commits; LLM lost 592 at 15 commits).

| # | Commit | Change | Files in `src/main` | Dev lost | LLM lost | LLM failure kind |
|---:|---|---|---:|---:|---:|---|
| 144 | a7b0c2d | Deprecated classes and methods removed (`BooleanAttribute` deleted, node constructors lose `baseUri`) | 13 | 94 | 361 | compile failure 352, class removed 9 |
| 41 | ae7d216 | `StringUtil` moved from `helper` to `internal` | 21 | 142 | 4 | compile failure 4 |
| 39 | c8c0569 | `StringUtil` StringBuilder pool (`stringBuilder()` replaced) | 13 | 0 | 127 | compile failure 127 |
| 179 | 696461b | Tokeniser comment handling (`Token`, `Tokeniser`) | 3 | 0 | 35 | compile failure 35 |
| 30 | f1b885d | "Spring cleaning" (`DataUtil`, `PseudoTextElement`) | 10 | 0 | 32 | compile failure 32 |
| 14 | 2c4e79b | `DataUtil` throws IOException (`UncheckedIOException`) | 3 | 0 | 15 | compile failure 15 |
| | 12 other commits | | | 13 | 18 | assertion failure 18 |

**Table 3. Coverage of the 52 classes under test at 1.11.2** (only the methods in each denominator were executed).

| Group | Methods | Line coverage | Branch coverage | Classes reached (of 52) |
|---|---:|---:|---:|---:|
| Dev | 594 | 5,674 / 7,172 (79.1%) | 2,715 / 3,628 (74.8%) | 48 |
| ZSL | 342 | 3,446 / 7,172 (48.0%) | 1,441 / 3,628 (39.7%) | 47 |
| FSL | 381 | 3,633 / 7,172 (50.7%) | 1,502 / 3,628 (41.4%) | 50 |
| CoT | 427 | 3,226 / 7,172 (45.0%) | 1,248 / 3,628 (34.4%) | 49 |
| ToT | 345 | 3,404 / 7,172 (47.5%) | 1,394 / 3,628 (38.4%) | 48 |
| GToT | 465 | 3,628 / 7,172 (50.6%) | 1,479 / 3,628 (40.8%) | 50 |
| LLM all | 1,862 | 4,519 / 7,172 (63.0%) | 2,002 / 3,628 (55.2%) | 52 |

## Observations

1. **Two commits account for most losses in both groups, and the remaining losses differ in kind.** Commits 41 and 144 stop 236 of the 249 developer tests lost (95%) and commits 144 and 39 stop 488 of the 592 LLM tests lost (82%); outside these, developer tests fail mostly on assertions (12 of 13: output formatting, `Tag` semantics, `W3CDom` output) while LLM tests fail mostly on compilation (86 of 104: changes to `DataUtil`, `Tokeniser`, `Token`). Two developer tests failed at an intermediate commit and passed again before the next release (`TagTest#defaultSemantics` at 118, `W3CDomTest#simpleConversion` at 128); under the cumulative definition they count as lost.
2. **The same class, `StringUtil`, killed developer tests and LLM tests at different commits for different reasons.** The 127 LLM tests of `StringUtil` stopped compiling at commit 39, when its `stringBuilder()` method was replaced, two commits before the class was moved; the 142 developer tests stopped at commit 41, when the move changed the import path, and none of them was affected by commit 39. At release granularity both appeared as one event at 1.12.1.
3. **Three times as many LLM tests cover less code than the developer suite.** Line coverage of the 52 classes is 63.0% for all 1,862 LLM tests against 79.1% for 594 developer tests; a single prompt covers 45.0 to 50.7%, so the five prompts reach different code and combine to 63.0%.

## Limitations

- The range ends at 1.13.1 (185 commits, 832 days); the later and larger losses seen at release granularity (1.15 to 1.23) are not covered.
- One generation per class and prompt with one model, as last week.
- One LLM test (`EntitiesTest`, GToT) failed when executed for coverage with only denominator methods, although it passed in three full runs at *t*; it depends on another test in its file. Survival counts are unaffected.

## Repository

`Hamjoon/jsoup`, branch `experiment/2026-10-week2-jsoup-commit-survival`, directory `experiments/commit-survival/`. Commits: `results/commits.csv`. Per-group series: `results/<group>-survival.csv` (185 rows), `<group>-first-break.csv`, `<group>-transient.csv`. Tables: `results/table1-survival-by-event.md` (all 28 event rows), `table2-breaking-commits.md`, `table3-coverage-t.md`. Coverage: `results/coverage-t.csv`. Checks: `results/consistency.md`. Figure: `results/figure1-survival-by-commit.png`.
