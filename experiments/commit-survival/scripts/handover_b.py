#!/usr/bin/env python3
"""Render the final review handover with all measurements loaded from results."""
import json
from pathlib import Path
from dev_common import EXP
from survival import GROUPS, read
from events import mdtable,ORDER,LABEL


def load(name): return json.loads((EXP/'results'/name).read_text())


def main():
    checks=load('consistency-summary.json'); setup=load('setup.json'); builds=load('build-summary.json')
    integrity=load('final-integrity.json'); coverage=load('coverage-stage.json'); continuation=load('completion-timing.json')
    summary={g:load(g+'-survival-summary.json') for g in GROUPS}
    differences=checks['tag_differing_cells']+checks['same_tree_differing_cells']
    first='Consistency differences: none.' if not differences else f"Consistency differences: {checks['tag_differing_cells']} unique cells at tagged points; {checks['same_tree_differing_cells']} unique cells at same-tree points."
    lines=[first,'','# Commit-survival final handover','',
           'Branch: `'+setup['branch']+'`. Base: `'+setup['base_commit']+'`.','',
           f"Built later commits: {builds['later_built']}; failed: {builds['later_failed']}; baseline copied. Failed points: "+(', '.join(r['point'] for r in builds['failed']) or 'none')+'.',
           '', '## Survival by event','',(EXP/'results/table1-survival-by-event.md').read_text(),
           '## Largest breaking commits','',(EXP/'results/table2-breaking-commits.md').read_text(),
           '## Baseline coverage','',(EXP/'results/table3-coverage-t.md').read_text(),
           'Coverage is authoritative for the denominator-only executions. Inner and anonymous class counters are summed into their outer CUT; these totals can differ from the previous source-file counter totals even with identical execution.',
           f"Coverage execution status differences from the passing denominator: {len(coverage['discrepancies'])}. Per-CUT Dev/pooled coverage differences from the previous report: {coverage['previous_differences']}.",
           '', '[Coverage comparison](../results/coverage-t-vs-prev.md). [Coverage execution discrepancies](../results/coverage-t-discrepancies.csv).','',
           '## Baseline funnel','',(EXP/'results/t-funnel.md').read_text(),
           '## First breaks by status','']
    statuses=['compile-fail','absent','fail','error','timeout','not-run','not-evaluated','survived']
    lines.append(mdtable(['Group',*statuses],[[LABEL[g],*[summary[g]['first_break_counts'].get(s,0) for s in statuses]] for g in ORDER]))
    lines.extend(['## Transient breaks','',
                  'Each method is counted once, using its earliest evaluated non-pass and its first later evaluated pass. Recovery does not restore cumulative survival. Up to three available examples per group follow.','',
                  mdtable(['Group','Transient methods'],[[LABEL[g],summary[g]['transient_methods']] for g in ORDER])])
    for g in ORDER:
        examples=read(g+'-transient.csv')[:3]
        lines.extend(['### '+LABEL[g],''])
        if examples:
            rows=[]
            for r in examples:
                name=(r.get('class','')+'/'+r.get('technique','')+'/' if g!='dev' else '')+r['test_class']+'#'+r['method']
                rows.append([name,r['i'],r['status_i'],r['j']])
            lines.append(mdtable(['Method','First non-pass index','Status','First later pass index'],rows))
        else: lines.extend(['None.',''])
    lines.extend(['## Flaky lists',''])
    for track in ['dev','llm']:
        flaky=read(track+'-flaky.csv')
        lines.extend(['### '+track,''])
        if flaky:
            fields=list(flaky[0]); lines.append(mdtable(fields,[[r[k] for k in fields] for r in flaky]))
        else: lines.extend(['None.',''])
    lines.extend(['## Consistency and missing methods','',
                  '[Full consistency counts and examples](../results/consistency.md). Group overlap is counted once in the first-line totals.','',
                  mdtable(['Track','Completed points','Tracked methods','Not-run cells','Flaky events'],
                          [[t,*[load(t+'-points-summary.json')[k] for k in ['points_completed','tracked_methods','not_run_cells','flaky_events']]] for t in ['dev','llm']]),
                  '', 'Listing differences include methods outside the fixed denominators; these are retained separately in the track listing-difference JSON files.','',
                  '## Stage wall time',''])
    stages=[('Commit enumeration',load('commits-summary.json')['seconds']),('Build',builds['seconds']),
            ('Developer track',load('dev-points-summary.json')['seconds']),('LLM track',load('llm-points-summary.json')['seconds']),
            ('Coverage',coverage['seconds']),('Survival and consistency',load('analysis-stage.json')['seconds']),
            ('Event tables',load('events-stage.json')['seconds']),('Figure',load('figure-stage.json')['seconds']),
            ('Final integrity audit',integrity['seconds']),('Authorized continuation through final verification',continuation['seconds'])]
    if (EXP/'results/handover-stage.json').exists():
        stages.append(('Handover initial rendering',load('handover-stage.json')['seconds']))
    lines.append(mdtable(['Stage','Wall seconds'],stages))
    lines.extend(['## Integrity and deviations','',
                  f"Verified {integrity['frozen_files_checked']} frozen files and {integrity['copied_results_checked']} copied result files; frozen changes: {len(integrity['changed_frozen_files'])}. Checked {integrity['checkout_source_files_checked']} archived source/POM files across all checkouts against their Git blob hashes.",
                  f"Figure dimensions: {integrity['figure_width']} × {integrity['figure_height']}. Coverage rows: {integrity['coverage_rows']}.",
                  '', *load('deviations.json')['items'],'',
                  'No LLM generation or API calls were used. No source or generated test was edited. Changes are committed locally; no push was performed.','',
                  '## Open questions',''])
    if differences:
        lines.append('Review the reported consistency differences before interpreting them as historical behavior changes. The per-method evidence and limited examples are in the consistency report.')
    else:
        lines.append('No unresolved consistency questions. Review the event-level losses, transient recoveries, and denominator-only coverage for interpretation.')
    lines.extend(['', 'Stopped at the requested final review point.','',
                  'All numeric values in this handover are rendered from files under results/. Raw execution, compilation, and coverage artifacts remain in their ignored per-point directories.'])
    (EXP/'docs/handover-b.md').write_text('\n'.join(lines)+'\n')
    print(EXP/'docs/handover-b.md')


if __name__=='__main__': main()
