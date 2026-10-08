#!/usr/bin/env python3
"""Execute all points, isolated by directory; assemble fixed-denominator matrices."""
import argparse
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor, wait, FIRST_COMPLETED
import csv
import json
import subprocess
import sys
import time
from dev_common import EXP, PREV, classpath as dev_cp, write_json
from csv_utils import write_csv
from llm_common import files, fqcn
from llm_run import run_file
from run_class import run_class

STATUSES={'pass','fail','error','timeout','compile-fail','absent','not-run','not-evaluated'}


def read(name):
    return list(csv.DictReader((EXP/'results'/name).open()))


def invoke(script,args,log):
    with log.open('w') as out:
        code=subprocess.run([sys.executable,str(EXP/'scripts'/script),*args],cwd=EXP,stdout=out,stderr=subprocess.STDOUT).returncode
    if code:
        raise RuntimeError(f'{script} failed; {log}: '+ '\n'.join(log.read_text().splitlines()[-30:]))


def process(commit,track,baseline,eligible,builds):
    start=time.monotonic(); point=commit['point']; root=EXP/track/point
    if root.exists():
        raise RuntimeError('Refusing cached/repeated point: '+str(root))
    root.mkdir(parents=True)
    ids=['test_class','method'] if track=='dev' else ['class','technique','test_class','method']
    result=dict(index=int(commit['index']),point=point,helpers_failed=[],files_compiled=0,files_compile_fail=0,
                files_absent=0,methods_run=0,compile_seconds=0,run_seconds=0,rerun_seconds=0,
                methods=[],flaky=[],reruns=[],listing_differences=[])
    if builds[point]['build_fail']=='1':
        result['methods']=[dict(**{k:r[k] for k in ids},status='not-evaluated') for r in eligible]
    else:
        invoke(track+'_compile.py',['--point',point],root/'compile.log')
        compiled=json.loads((root/'compile-summary.json').read_text())
        invoke(track+'_run.py',['--point',point,'--run','1'],root/'run.log')
        run=json.loads((root/'run1/run-summary.json').read_text())
        result.update(compile_seconds=compiled['seconds'],run_seconds=run['seconds'])
        if track=='dev':
            result.update(helpers_failed=compiled['helpers_failed'],files_compiled=compiled['files_compiled'],
                          files_compile_fail=compiled['files_compile_fail'],methods_run=run['methods_run'])
            ledger=compiled['files']; grouped=defaultdict(list)
            for r in baseline: grouped[r['test_class']].append(r)
            extracted={}
        else:
            result.update(files_compiled=compiled['compiled'],files_compile_fail=compiled['compile_fail'],
                          files_absent=compiled['absent'],methods_run=run['methods_measured'])
            ledger=compiled['files_ledger']; grouped=defaultdict(list)
            for r in baseline: grouped[(r['class'],r['technique'])].append(r)
            extracted={(r['class'],r['technique']):r for r in files()}
        eligible_keys={tuple(r[k] for k in ids) for r in eligible}
        rerun_start=time.monotonic()
        for file in ledger:
            key=file['test_class'] if track=='dev' else (file['class'],file['technique'])
            stem=key if track=='dev' else '.'.join(key)
            file_status=file['status']
            members=grouped[key]
            if file_status!='compiled':
                statuses={r['method']:file_status for r in members}
            else:
                summary=json.loads((root/'run1'/(stem+'.summary.json')).read_text())
                initial={m['method']:m['status'] for m in summary['methods']}
                statuses={r['method']:initial.get(r['method'],'not-run') for r in members}
                expected={r['method'] for r in members}
                listed=set(summary['all_listed_methods']) if not summary['list_error'] else set()
                if expected!=listed:
                    result['listing_differences'].append(dict(point=point,file=stem,missing=sorted(expected-listed),
                                                             extra=sorted(listed-expected),list_error=summary['list_error']))
                include=[m for m,s in initial.items() if s in {'fail','error'}]
                if include:
                    output=root/'rerun'/(stem+'.json')
                    if track=='dev':
                        repeated=run_class(key,dev_cp(point,key),output,timeout_ms=30000,
                                           working_directory=EXP/'checkouts'/point,include_methods=include)
                        name=key
                    else:
                        repeated,_=run_file(point,extracted[key],output,include)
                        name=fqcn(extracted[key])
                    measured={r['method']:r['status'] for r in repeated['methods']}
                    assert set(measured)==set(include)
                    for method in include:
                        identity=dict(test_class=name,method=method)
                        if track=='llm': identity.update({'class':key[0],'technique':key[1]})
                        result['reruns'].append(dict(**identity,initial=initial[method],rerun=measured[method]))
                        if measured[method]=='pass':
                            if method in statuses: statuses[method]='pass'
                            result['flaky'].append(dict(point=point,**identity))
            for r in members:
                if tuple(r[k] for k in ids) in eligible_keys:
                    result['methods'].append(dict(**{k:r[k] for k in ids},status=statuses[r['method']]))
        result['rerun_seconds']=round(time.monotonic()-rerun_start,3)
    assert len(result['methods'])==len(eligible)==len({tuple(r[k] for k in ids) for r in result['methods']})
    assert {r['status'] for r in result['methods']}<=STATUSES
    result['seconds']=round(time.monotonic()-start,3)
    write_json(root/'final-summary.json',result)
    return result


def assemble(track,finals,eligible):
    finals=sorted(finals,key=lambda r:r['index']); points=[r['point'] for r in finals]
    ids=['test_class','method'] if track=='dev' else ['class','technique','test_class','method']
    fields=ids if track=='dev' else [*ids,'body_hash','in_denominator_pooled','in_denominator_technique']
    maps={f['point']:{tuple(r[k] for k in ids):r['status'] for r in f['methods']} for f in finals}
    matrix=[dict(**{k:r[k] for k in fields},**{p:maps[p][tuple(r[k] for k in ids)] for p in points}) for r in eligible]
    write_csv(EXP/'results'/(track+'-matrix.csv'),matrix,[*fields,*points])
    write_csv(EXP/'results'/(track+'-flaky.csv'),[r for f in finals for r in f['flaky']],['point',*ids])
    logfields=['index','point',*(['helpers_failed'] if track=='dev' else []),'files_compiled','files_compile_fail',
               *(['files_absent'] if track=='llm' else []),'methods_run','seconds']
    logs=[{k:'|'.join(f[k]) if k=='helpers_failed' else f[k] for k in logfields} for f in finals]
    write_csv(EXP/'results'/(track+'-points-log.csv'),logs,logfields)
    write_json(EXP/'results'/(track+'-listing-differences.json'),[r for f in finals for r in f['listing_differences']])
    missing=[dict(point=f['point'],**r) for f in finals for r in f['methods'] if r['status']=='not-run']
    write_csv(EXP/'results'/(track+'-not-run.csv'),missing,['point',*ids,'status'])


def main():
    parser=argparse.ArgumentParser(); parser.add_argument('--track',choices=['dev','llm'],required=True)
    parser.add_argument('--workers',type=int,choices=range(1,5),default=4); args=parser.parse_args()
    start=time.monotonic(); track=args.track
    assert (EXP/'results'/('prev-'+track+'-matrix.csv')).is_file()
    commits=[r for r in read('commits.csv') if int(r['index'])>0]
    builds={r['point']:r for r in read('builds.csv')}
    baseline=read('dev-denominator.csv' if track=='dev' else 'llm-methods.csv')
    eligible=[r for r in baseline if r['in_denominator']=='1'] if track=='dev' else [r for r in baseline if r['in_denominator_pooled']=='1' or r['in_denominator_technique']=='1']
    if track=='llm':
        cuts=[r for r in read('cut-classes.csv') if not r['excluded']]
        presence=[dict(**{'class':c['class']},**{r['point']:int((EXP/'checkouts'/r['point']/c['path']).is_file()) for r in commits}) for c in cuts]
        write_csv(EXP/'results/cut-presence.csv',presence,['class',*[r['point'] for r in commits]])
    finals=[]; pending=iter(commits)
    with ThreadPoolExecutor(max_workers=args.workers) as pool:
        running={pool.submit(process,next(pending),track,baseline,eligible,builds) for _ in range(args.workers)}
        while running:
            done,running=wait(running,return_when=FIRST_COMPLETED)
            for future in done:
                final=future.result(); finals.append(final)
                assemble(track,finals,eligible)
                print(json.dumps(dict(track=track,index=final['index'],completed=len(finals),point=final['point'],
                                      counts=dict(Counter(r['status'] for r in final['methods'])),
                                      flaky=len(final['flaky']),seconds=final['seconds'])),flush=True)
                commit=next(pending,None)
                if commit is not None: running.add(pool.submit(process,commit,track,baseline,eligible,builds))
    summary=dict(points_completed=len(finals),tracked_methods=len(eligible),workers=args.workers,
                 seconds=round(time.monotonic()-start,3),flaky_events=sum(len(f['flaky']) for f in finals),
                 rerun_methods=sum(len(f['reruns']) for f in finals),not_run_cells=sum(r['status']=='not-run' for f in finals for r in f['methods']),
                 compile_seconds=sum(f['compile_seconds'] for f in finals),run_seconds=sum(f['run_seconds'] for f in finals))
    write_json(EXP/'results'/(track+'-points-summary.json'),summary)
    print(json.dumps(summary),flush=True)


if __name__=='__main__': main()
