#!/usr/bin/env python3
"""Execute the later-release developer track sequentially, with exact reruns."""
from collections import Counter, defaultdict
import csv
import json
import subprocess
import sys
import time
from dev_common import BASELINE, EXP, classpath, write_json


def write_csv(path, rows, fields):
    with path.open('w',newline='') as f:
        writer=csv.DictWriter(f,fieldnames=fields,lineterminator='\n')
        writer.writeheader()
        writer.writerows(rows)


def invoke_script(script,args,log):
    with log.open('w') as stream:
        code=subprocess.run([sys.executable,str(EXP/'scripts'/script),*args],cwd=EXP,
                            stdout=stream,stderr=subprocess.STDOUT).returncode
    if code:
        write_json(EXP/'results/dev-later-failure.json',dict(script=script,args=args,
            exit_status=code,log=str(log.relative_to(EXP)),tail='\n'.join(log.read_text().splitlines()[-30:])))
        raise SystemExit('STOP: '+script+' failed; see results/dev-later-failure.json')


def main():
    releases=[r for r in csv.DictReader((EXP/'results/releases.csv').open()) if int(r['index'])>0]
    baseline=list(csv.DictReader((EXP/'results/dev-denominator.csv').open()))
    denominator=[(r['test_class'],r['method']) for r in baseline if r['in_denominator']=='1']
    assert len(denominator)==len(set(denominator))==json.loads((EXP/'results/dev-t-summary.json').read_text())['pass_in_all_three']
    baseline_methods=defaultdict(list)
    for row in baseline:
        baseline_methods[row['test_class']].append(row['method'])
    started=time.monotonic()
    finals=[]
    for release in releases:
        tag=release['tag']
        root=EXP/'dev'/tag
        root.mkdir(parents=True,exist_ok=True)
        if (root/'final-summary.json').exists():
            final=json.loads((root/'final-summary.json').read_text())
        else:
            start=time.monotonic()
            invoke_script('dev_compile.py',['--release',tag],root/'compile.log')
            invoke_script('dev_run.py',['--release',tag,'--run','1'],root/'run.log')
            compiled=json.loads((root/'compile-summary.json').read_text())
            run=json.loads((root/'run1/run-summary.json').read_text())
            final_methods=[]
            flaky=[]
            reruns=[]
            unexpected=[]
            for file in compiled['files']:
                name=file['test_class']
                if file['exit_status']:
                    statuses={m:'compile-fail' for m in baseline_methods[name]}
                    if name in json.loads((EXP/'results/dev-run-policy.json').read_text())['class_level_ignored']:
                        statuses={m:'ignored' for m in baseline_methods[name]}
                else:
                    summary=json.loads((root/'run1'/f'{name}.summary.json').read_text())
                    initial={r['method']:r['status'] for r in summary['methods']}
                    statuses={m:initial.get(m,'not-run') for m in baseline_methods[name]}
                    missing=set(baseline_methods[name])-set(initial)
                    extra=set(initial)-set(baseline_methods[name])
                    if missing or extra:
                        unexpected.append(dict(test_class=name,missing=sorted(missing),extra=sorted(extra)))
                    include=[m for m,s in initial.items() if s in {'fail','error'}]
                    if include:
                        output=root/'rerun'/f'{name}.json'
                        output.parent.mkdir(exist_ok=True)
                        command=[sys.executable,str(EXP/'scripts/run_class.py'),name,
                                 '--classpath',classpath(tag,name),'--output',str(output),
                                 '--timeout-ms','30000','--cwd',str(EXP/'checkouts'/tag),
                                 '--include',','.join(include)]
                        p=subprocess.run(command,cwd=EXP,capture_output=True,text=True)
                        output.with_suffix('.wrapper.log').write_text(p.stdout+p.stderr)
                        if p.returncode:
                            raise SystemExit('STOP: rerun wrapper failed: '+tag+' '+name)
                        rerun=json.loads(output.with_suffix('.summary.json').read_text())
                        measured={r['method']:r['status'] for r in rerun['methods']}
                        assert set(measured)==set(include)
                        for method in include:
                            reruns.append(dict(test_class=name,method=method,initial=initial[method],rerun=measured[method]))
                            if measured[method]=='pass':
                                statuses[method]='pass'
                                flaky.append(dict(tag=tag,test_class=name,method=method))
                final_methods.extend(dict(test_class=name,method=m,status=s) for m,s in statuses.items())
            final=dict(tag=tag,helpers_failed=compiled['helpers_failed'],files_compiled=compiled['files_compiled'],
                       files_compile_fail=compiled['files_compile_fail'],methods_run=run['methods_run'],
                       seconds=round(time.monotonic()-start,3),compile_seconds=compiled['seconds'],
                       run_seconds=run['seconds'],methods=final_methods,flaky=flaky,reruns=reruns,
                       unexpected_listings=unexpected,cross_test_references=compiled['cross_test_references'])
            write_json(root/'final-summary.json',final)
        finals.append(final)
        maps={f['tag']:{(r['test_class'],r['method']):r['status'] for r in f['methods']} for f in finals}
        tags=[f['tag'] for f in finals]
        matrix=[dict(test_class=c,method=m,**{tag:maps[tag][(c,m)] for tag in tags}) for c,m in denominator]
        write_csv(EXP/'results/dev-matrix.csv',matrix,['test_class','method',*tags])
        write_csv(EXP/'results/dev-flaky.csv',[r for f in finals for r in f['flaky']],['tag','test_class','method'])
        logs=[dict(tag=f['tag'],helpers_failed='|'.join(f['helpers_failed']),files_compiled=f['files_compiled'],
                   files_compile_fail=f['files_compile_fail'],methods_run=f['methods_run'],seconds=f['seconds']) for f in finals]
        write_csv(EXP/'results/dev-releases-log.csv',logs,['tag','helpers_failed','files_compiled','files_compile_fail','methods_run','seconds'])
        counts=Counter(maps[tag][key] for key in denominator)
        print(json.dumps(dict(tag=tag,counts=dict(counts),rerun_methods=len(final['reruns']),
                              flaky=len(final['flaky']),seconds=final['seconds'])),flush=True)
        if final['unexpected_listings']:
            write_json(EXP/'results/dev-unexpected-listings.json',dict(tag=tag,listings=final['unexpected_listings']))
            raise SystemExit('STOP: unexpected method listing differences; see results/dev-unexpected-listings.json')
    write_json(EXP/'results/dev-later-summary.json',dict(releases_completed=len(finals),denominator=len(denominator),
        seconds=round(time.monotonic()-started,3),compile_seconds=sum(f['compile_seconds'] for f in finals),
        run_seconds=sum(f['run_seconds'] for f in finals),
        rerun_methods=sum(len(f['reruns']) for f in finals),flaky_events=sum(len(f['flaky']) for f in finals),
        unexpected_listings=[r for f in finals for r in f['unexpected_listings']]))


if __name__=='__main__':
    main()
