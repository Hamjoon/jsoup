#!/usr/bin/env python3
"""Write the final developer-track report from tracked results only."""
from collections import defaultdict
import csv
import json
import time
from dev_common import EXP, write_json
from handover_a import table

RESULTS=EXP/'results'


def read_csv(name):
    return list(csv.DictReader((RESULTS/name).open()))


def read_json(name):
    return json.loads((RESULTS/name).read_text())


def main():
    start=time.monotonic()
    survival=read_csv('dev-survival.csv')
    minors=[dict(release=r['tag'],date=r['date'],days=r['days_since_t'],
                 survived_denominator=r['survived']+' / '+r['denominator'],rate=r['survival_rate'])
            for r in survival if r['last_of_minor']=='1']
    minor_table=table(minors,[('release','release'),('date','date'),('days','days'),
                            ('survived / denominator','survived_denominator'),('rate','rate')])
    (RESULTS/'dev-survival-by-minor.md').write_text(minor_table)
    baseline=read_json('dev-t-summary.json')
    later=read_json('dev-later-summary.json')
    summary=read_json('dev-survival-summary.json')
    integrity=read_json('dev-integrity-check.json')
    build=read_json('build-summary.json')
    status=read_csv('dev-first-break-summary.csv')
    byrelease={r['tag']:dict(tag=r['tag'],**{s:0 for s in ['fail','error','timeout','compile-fail','not-run']}) for r in survival}
    for row in status:
        if row['scope']=='release':
            byrelease[row['first_break_tag']][row['first_break_status']]=int(row['count'])
    timing=[dict(stage='Release builds and frozen dependency download',seconds=build['seconds']),
            dict(stage='Baseline compilation',seconds=baseline['stage_seconds']['compile'])]
    timing.extend(dict(stage='Baseline run '+str(k),seconds=baseline['stage_seconds'][f'run{k}']) for k in [1,2,3])
    timing.extend([dict(stage='Later releases (compile, run, rerun, orchestration)',seconds=later['seconds']),
                   dict(stage='Later compilation component',seconds=later['compile_seconds']),
                   dict(stage='Later initial-run component',seconds=later['run_seconds']),
                   dict(stage='Survival and first-break analysis',seconds=summary['seconds']),
                   dict(stage='Source and protocol verification',seconds=integrity['seconds'])])
    deviations=[
        'Gary clarified that compile-fail is a file-level dev_compile.py outcome; the copied wrapper adds only not-run.',
        'Gary corrected the inventory expectations to the observed counts; integration includes its servlets subpackage.',
        'Gary authorized dev_run.py to inventory the class-level ignored methods of ConstrainableInputStreamTest and record ignored without executing them at every release; the expected ignored count was corrected.',
        'The interrupted baseline attempt was preserved separately and excluded; all measured baseline runs were restarted in full after the ignore correction.',
        'The production-class map records observed top-level Java-file counts (excluding package-info, including examples), as permitted by the instruction to record what is found.',
        'The quiet Maven build logs did not contain obsolete/deprecated source-level warnings; warning flags reflect recorded output.',
        'Unrecovered reruns retain their initial fail/error status; raw rerun outcomes are preserved. Flaky markers identify only fail/error methods that passed the one rerun.',
        'Compile-error counts count distinct javac diagnostic lines from each file at its first compile-fail break, rather than weighting them by the number of methods in that file.',
        'Wall time for initial branch/tag setup, Docker image construction, tool copying, and review pauses was not recorded; no retrospective estimates are supplied.'
    ]
    write_json(RESULTS/'dev-part1-summary.json',dict(baseline_denominator=baseline['pass_in_all_three'],
        later_releases=later['releases_completed'],final_release=survival[-1]['tag'],
        final_survived=summary['final_survived'],final_survival_rate=summary['final_survival_rate'],
        final_point_pass=summary['final_point_pass'],flaky_events=later['flaky_events'],
        first_break_counts=summary['first_break_counts'],stage_times=timing,deviations=deviations,
        open_questions=['Part 1 is complete and awaits review; the LLM track is reserved for Part 2.'],
        branch_pushed=False,source_and_tools_unchanged=True))
    part=read_json('dev-part1-summary.json')
    text='# Developer test survival: final handover\n\n'
    text+=f"The developer track is complete at `{part['final_release']}`. The fixed denominator is {part['baseline_denominator']}; {part['final_survived']} methods survive cumulatively (rate {part['final_survival_rate']}), and {part['final_point_pass']} pass at the final release alone. Every accepted later release was evaluated.\n\n"
    inventory=read_json('dev-inventory-summary.json')['observed']
    text+=f"The accepted baseline has {baseline['listed_methods']} listed methods, {baseline['ignored']} ignored, and {baseline['pass_in_all_three']} passing in every complete baseline run. Its corrected inventory is {inventory['test']} test files, {inventory['helper']} helpers, and {inventory['integration']} integration files, {inventory['files']} total. Baseline flaky methods: {baseline['flaky']}; baseline compile failures: {baseline['compile_failures_at_t']}.\n\n"
    text+='## Survival by minor line\n\n'+minor_table
    text+='\n## Full survival table\n\n'+table(survival,[('Release','tag'),('Date','date'),('Days','days_since_t'),
        ('Denominator','denominator'),('Point pass','point_pass'),('Survived','survived'),('Rate','survival_rate'),
        ('Fail','fail'),('Error','error'),('Timeout','timeout'),('Compile fail','compile_fail'),('Not run','not_run')])
    text+='\n## First break by status\n\n'+table([r for r in status if r['scope']=='status'],[('Status','first_break_status'),('Methods','count')])
    text+='\n## First break by release\n\n'+table(list(byrelease.values()),[('Release','tag'),('Fail','fail'),('Error','error'),('Timeout','timeout'),('Compile fail','compile-fail'),('Not run','not-run')])
    text+='\n## Compile-error categories\n\n'+table(read_csv('dev-compile-error-summary.csv'),[('Category','category'),('Diagnostic lines','count')])
    text+='\n## Most common javac messages\n\n'
    text+='Message frequencies include javac’s symbol detail where present, to distinguish missing APIs sharing the same generic error header. Examples below are verbatim.\n\n'
    for row in read_csv('dev-javac-common.csv'):
        text+=f"This message occurs {row['count']} times in the first-break diagnostics; first appears at `{row['first_tag']}` in `{row['test_class']}`. Verbatim diagnostic example:\n\n```text\n{row['diagnostic']}\n```\n\n"
    flaky=read_csv('dev-flaky.csv')
    text+=f"## Flaky rerun recoveries\n\nRecorded recoveries: {part['flaky_events']}.\n\n"
    text+=table(flaky,[('Release','tag'),('Class','test_class'),('Method','method')])
    text+='\n## Execution log\n\n'+table(read_csv('dev-releases-log.csv'),[('Release','tag'),('Helpers omitted','helpers_failed'),('Compiled files','files_compiled'),('Compile-fail files','files_compile_fail'),('Methods listed/measured','methods_run'),('Seconds','seconds')])
    text+='\n## Wall times\n\n'+table(part['stage_times'],[('Stage or component','stage'),('Seconds','seconds')])
    text+='\nComponent timings are contained within the later-release total and should not be added to it. Review pauses and the archived interrupted attempt are excluded.\n'
    text+='\n## Verification\n\nThe fixed-denominator coverage, point-status totals, cumulative monotonicity, first-break totals, and per-class survivor totals passed their assertions. All archived source and resource files match their release tags. Copied tools and frozen test libraries match their original hashes. Classpaths use frozen baseline resources and release main classes; all method execution used the network-disabled service. Rerun selections exactly match the initial fail/error sets; class-level ignored methods did not launch a JVM.\n'
    text+='\n## Deviations and clarifications\n\n'+''.join('- '+d+'\n' for d in part['deviations'])
    text+='\n## Open questions and stop\n\n'+''.join('- '+q+'\n' for q in part['open_questions'])
    text+='\nNo experiment branch push was performed. The API key remains in the ignored environment file and was not read or copied into results.\n'
    (EXP/'docs/handover-part1-b.md').write_text(text)
    write_json(RESULTS/'dev-report-summary.json',dict(minor_rows=len(minors),full_rows=len(survival),
        seconds=round(time.monotonic()-start,3),handover='docs/handover-part1-b.md'))
    # Include measured report-generation time after its first complete rendering.
    report=read_json('dev-report-summary.json')
    part['stage_times'].append(dict(stage='Initial final-report generation',seconds=report['seconds']))
    write_json(RESULTS/'dev-part1-summary.json',part)
    text+='\nInitial report-generation wall time (recorded in results/dev-report-summary.json): '+str(report['seconds'])+' seconds.\n'
    (EXP/'docs/handover-part1-b.md').write_text(text)
    print(json.dumps({k:v for k,v in part.items() if k not in ['stage_times','deviations','open_questions']},indent=2))


if __name__=='__main__':
    main()
