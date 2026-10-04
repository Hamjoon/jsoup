#!/usr/bin/env python3
"""Fix the denominator to methods that pass all three complete baseline runs."""
from collections import Counter
import csv
import json
from dev_common import BASELINE, EXP, fqcn, test_classes, write_json


def main():
    root=EXP/'dev'/BASELINE
    compile_summary=json.loads((root/'compile-summary.json').read_text())
    runs=[json.loads((root/f'run{k}'/'run-summary.json').read_text()) for k in [1,2,3]]
    assert all(r['complete'] for r in runs)
    rows=[]
    outcomes=[]
    for file in test_classes():
        name=fqcn(file)
        results=[]
        for k in [1,2,3]:
            summary=json.loads((root/f'run{k}'/f'{name}.summary.json').read_text())
            assert not summary['list_error']
            measured={r['method']:r['status'] for r in summary['methods']}
            assert len(measured)==len(summary['methods'])
            assert set(measured)==set(summary['listed_methods'])
            results.append(measured)
        assert set(results[0])==set(results[1])==set(results[2]), 'Baseline listings differ: '+name
        for method in sorted(results[0]):
            statuses=[r[method] for r in results]
            row=dict(test_class=name,method=method,run1=statuses[0],run2=statuses[1],run3=statuses[2],
                     in_denominator=int(all(s=='pass' for s in statuses)),
                     flaky_at_t=int('pass' in statuses and any(s!='pass' for s in statuses)))
            rows.append(row)
            if not row['in_denominator']:
                outcomes.append(row)
    with (EXP/'results/dev-denominator.csv').open('w',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=['test_class','method','run1','run2','run3','in_denominator','flaky_at_t'],lineterminator='\n')
        writer.writeheader()
        writer.writerows(rows)
    listed=len(rows)
    denominator=sum(r['in_denominator'] for r in rows)
    summary=dict(listed_methods=listed,ignored=sum(all(r[f'run{k}']=='ignored' for k in [1,2,3]) for r in rows),
        pass_in_all_three=denominator,flaky=sum(r['flaky_at_t'] for r in rows),
        fail_error_timeout_in_all_three=sum(all(r[f'run{k}'] in {'fail','error','timeout'} for k in [1,2,3]) for r in rows),
        consistent_failures_by_status={s:sum(all(r[f'run{k}']==s for k in [1,2,3]) for r in rows) for s in ['fail','error','timeout']},
        compile_failures_at_t=compile_summary['files_compile_fail'],
        denominator_rate=round(denominator/listed,6),
        status_counts_by_run={str(k):dict(Counter(r[f'run{k}'] for r in rows)) for k in [1,2,3]},
        not_run_methods=sum(any(r[f'run{k}']=='not-run' for k in [1,2,3]) for r in rows),
        non_denominator_methods=outcomes,
        stage_seconds=dict(compile=compile_summary['seconds'],**{f'run{k}':runs[k-1]['seconds'] for k in [1,2,3]}))
    errors=[]
    if abs(listed-599)>30:
        errors.append('Listed methods are not close to the baseline annotation count')
    if summary['ignored']!=4:
        errors.append('Ignored methods differ from the expected count')
    if denominator/listed<0.9 or summary['compile_failures_at_t']:
        errors.append('Baseline passing threshold or compilation requirement failed')
    if summary['flaky']>5 or summary['not_run_methods']:
        errors.append('Unexpected flaky or not-run methods at baseline')
    summary['errors']=errors
    write_json(EXP/'results/dev-t-summary.json',summary)
    print(json.dumps(summary,indent=2))
    if errors:
        raise SystemExit('STOP: baseline results outside expectations; see results/dev-t-summary.json')


if __name__=='__main__':
    main()
