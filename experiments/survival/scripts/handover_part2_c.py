#!/usr/bin/env python3
"""Assemble the final review handover exclusively from results artifacts."""
import json
from dev_common import EXP, write_json
from dev_later import write_csv
from handover_a import table
from survival_common import GROUPS, read_csv


def load(name):
    return json.loads((EXP/'results'/name).read_text())


def main():
    later=load('llm-later-summary.json')
    summaries={g:load(f'{g}-survival-summary.json') for g in GROUPS}
    final=[dict(group=g,**{k:v for k,v in summaries[g].items() if k in ['denominator','final_survived','final_survival_rate']},
        lenient_survived=int(read_csv(f'{g}-survival-lenient.csv')[-1]['survived'])) for g in GROUPS]
    write_csv(EXP/'results/part2-final-summary.csv',final,list(final[0]))
    verification=load('llm-parallel-verification.json')
    baseline=load('part2-baseline-summary.json')
    stages=dict(baseline['stage_seconds'])
    stages.update(later_release_llm_sweep=later['seconds'],later_release_compile=later['compile_seconds'],
        later_release_run=later['run_seconds'],later_release_rerun=later['rerun_seconds'],
        parallel_sequential_verification=verification['seconds'],
        strict_analysis=round(sum(summaries[g]['seconds'] for g in GROUPS),3),
        compile_attribution=load('compile-attribution-timing.json')['seconds'],comparison_tables=load('comparison-summary.json')['seconds'])
    listing=later['listing_differences']
    baseline_files={(r['class'],r['technique']) for r in read_csv('llm-methods.csv')}
    classified=[dict(**r,baseline_file_compiled=(r['class'],r['technique']) in baseline_files) for r in listing]
    write_json(EXP/'results/llm-listing-differences-classified.json',classified)
    notes=[*baseline['deviations'],
        'Gary accepted the revised pooled and per-technique denominators and authorized the final release sweep.',
        'Per-file javac and per-class runner invocations use four workers within a release. Compilation finishes before execution; releases advance in order. Reruns also use at most four independent class workers. Output ledgers use fixed input order.',
        'Every completed release was also checked with isolated sequential compilation/execution. Exact compiler exits/stderr and class-file bytes, JUnit listings, method statuses and exception types were compared. Execution durations and process-specific message identity hashes are excluded from equality; these supplementary checks do not alter measured statuses or denominators.',
        'First-break compile-fail methods receive own/sibling attribution from their frozen source body ranges. The lenient view rechecks attribution at every later compile-fail cell, then applies cumulative survival; it does not permanently pardon a sibling first break.',
        'The developer removed-CUT sub-count matches whole-word CUT names in javac error headers and symbol/location details at each method’s first-break file. Source-code excerpts are excluded. All affected methods retain compile-fail; CSV rows may name multiple symbols, while the summary counts distinct methods.',
        'New listings from files that failed compilation at t are reported separately from missing baseline methods and never enlarge the fixed denominator.',
        'The initial multi-file analysis patch was rejected before edits because it attempted two operations on one path; the patch was split and applied. No measured data changed.'
    ]
    report=dict(final=final,stage_seconds=stages,removed_cut_compile_fail_methods=summaries['dev']['compile_fail_cut_removed_methods'],
        listing_difference_records=len(listing),missing_baseline_listing_records=sum(bool(r['missing']) for r in classified if r['baseline_file_compiled']),
        parallel_verification_releases=len(verification['releases']),all_parallel_checks_identical=verification['all_identical'],
        llm_flaky_events=later['flaky_events'],deviations=notes,branch_pushed=False,steps_17_18_19_complete=True)
    write_json(EXP/'results/part2-final-report.json',report)
    text='# jsoup survival comparison: final handover\n\n'
    text+='Steps 17, 18 and 19 are complete. The accepted denominators remain fixed; all later releases were evaluated in order. This is the final review stop.\n\n'
    text+='## Final strict and lenient survivors\n\n'+table(final,[('Group','group'),('Denominator','denominator'),('Strict survivors','final_survived'),('Strict rate','final_survival_rate'),('Lenient survivors','lenient_survived')])
    for heading,filename in [('Table 1: strict survival by minor','table1-survival-by-minor.md'),
        ('Table 2: first break and compile-fail attribution','table2-first-break.md'),('Lenient survival by minor','table1-lenient-by-minor.md')]:
        text+='\n## '+heading+'\n\n'+(EXP/'results'/filename).read_text()
    text+='\n## Developer compile-fail methods naming disappeared CUTs\n\n'
    text+=f"Distinct first-break compile-fail methods naming one of the accepted disappeared CUTs: {report['removed_cut_compile_fail_methods']}. This is a repository-only sub-count, not a status, and overlaps the compile-fail column in Table 2. Details: `results/dev-compile-fail-cut-removed.csv`; count line: scope `cut-removed-subcount` in `results/dev-first-break-summary.csv`.\n"
    text+='\n## Matched-size pooled LLM check\n\n'+table(read_csv('llm-matched-594.csv'),[('Release','tag'),('Days','days_since_t'),('Size','denominator'),('Samples','samples'),('Seed','seed'),('Mean survivors','mean_survived'),('Min','min_survived'),('Max','max_survived'),('Mean rate','mean_survival_rate')])
    text+='\n## Accepted baseline funnel\n\n'+table(read_csv('llm-t-funnel.csv'),[('Technique/group','group'),('Generated','files_generated'),('Extracted','files_extracted'),('Compiled','files_compiled'),('Listed','methods_listed'),('Pass all runs','pass_in_all_three'),('Duplicates removed','duplicates_removed'),('Denominator','denominator')])
    text+='\n## Common first-break javac symbols\n'
    for group in GROUPS:
        text+='\n### '+group+'\n\n'+table(read_csv(f'{group}-javac-common.csv'),[('Symbol / diagnostic','symbol'),('Error-line count','count'),('First release','first_tag'),('Example test class','test_class')])
    text+='\n## Coverage at t\n\n```json\n'+json.dumps(load('coverage-t-summary.json'),indent=2)+'\n```\n'
    text+='\nCoverage is supplementary full-class coverage; per-CUT values are in `results/coverage-t.csv`. Per-CUT pooled survivors and same-name developer figures are in `results/llm-by-class.md`.\n'
    text+='\n## Flaky records\n\nDeveloper later-release records:\n\n'+table(read_csv('dev-flaky.csv'),[('Release','tag'),('Test class','test_class'),('Method','method')])
    text+='\nLLM later-release records:\n\n'+table(read_csv('llm-flaky.csv'),[('Release','tag'),('CUT','class'),('Technique','technique'),('Test class','test_class'),('Method','method'),('Initial','initial'),('Rerun','rerun')])
    text+='\nBaseline LLM flaky records:\n\n'+table(read_csv('llm-flaky-t.csv'),[('CUT','class'),('Technique','technique'),('Test class','test_class'),('Method','method'),('Run one','run1'),('Run two','run2'),('Run three','run3')])
    listing_table=[dict(tag=r['tag'],**{'class':r['class']},technique=r['technique'],baseline_file_compiled=r['baseline_file_compiled'],
        missing=len(r['missing']),extra=len(r['extra'])) for r in classified]
    text+='\n## Listing differences\n\n'+table(listing_table,[('Release','tag'),('CUT','class'),('Technique','technique'),('Compiled at t','baseline_file_compiled'),('Missing methods','missing'),('Extra methods','extra')])
    text+='\nFull method identities and listing messages: `results/llm-listing-differences-classified.json`.\n'
    text+='\n## Worker counts and release wall times\n\n'+table(read_csv('llm-releases-log.csv'),[('Release','tag'),('Workers','workers'),('Compiled','files_compiled'),('Compile fail','files_compile_fail'),('Absent files','files_absent'),('Methods run','methods_run'),('Seconds','seconds')])
    text+='\n## Stage wall times\n\n'+table([dict(stage=k,seconds=round(v,3)) for k,v in stages.items()],[('Stage','stage'),('Seconds','seconds')])
    text+='\nCompile/run/rerun times are components of the release sweep and are not additive to its wall time. Sequential validation ran alongside the sweep and continued after it; its reported wall time is a separate supplementary check.\n'
    text+='\n## Integrity and parallel-result verification\n\n```json\n'+json.dumps(load('llm-survival-integrity.json'),indent=2)+'\n```\n'
    text+='\nSequential comparison details per release: `results/llm-parallel-verification.csv` and `.json`. Source and resource hashes for all frozen releases: `results/dev-integrity-check.json`. Baseline preservation audit: `results/llm-baseline-integrity.json`.\n'
    text+='\n## Deviations and interpretation notes\n\n'+''.join('- '+n+'\n' for n in notes)
    text+='\n## Open questions and stop\n\nNo unresolved implementation questions remain within the requested scope. The primary file-level tables and the secondary lenient tables are both retained for review. No generated Java, jsoup source, copied runner, or copied library was edited. No additional generation calls were made after the authorized single reissue. The API key remains ignored. The branch was not pushed.\n'
    (EXP/'docs/handover-part2-c.md').write_text(text)
    print(json.dumps(report,indent=2))


if __name__=='__main__':
    main()
