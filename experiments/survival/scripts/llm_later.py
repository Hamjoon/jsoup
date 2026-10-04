#!/usr/bin/env python3
"""Execute releases in order, with parallel independent files and exact reruns."""
from collections import Counter, defaultdict
from concurrent.futures import ThreadPoolExecutor
import csv
import json
import time
from dev_common import EXP, write_json
from dev_later import invoke_script as dev_invoke, write_csv
from llm_common import files
from llm_run import run_file

WORKERS=4


def invoke(script,args,log):
    # Keep the developer failure ledger separate from this track.
    import subprocess,sys
    with log.open('w') as stream:
        code=subprocess.run([sys.executable,str(EXP/'scripts'/script),*args],cwd=EXP,
                            stdout=stream,stderr=subprocess.STDOUT).returncode
    if code:
        write_json(EXP/'results/llm-later-failure.json',dict(script=script,args=args,exit_status=code,
            log=str(log.relative_to(EXP)),tail='\n'.join(log.read_text().splitlines()[-30:])))
        raise SystemExit('STOP: LLM release script failed; see results/llm-later-failure.json')


def main():
    releases=[r for r in csv.DictReader((EXP/'results/releases.csv').open()) if int(r['index'])>0]
    baseline=list(csv.DictReader((EXP/'results/llm-methods.csv').open()))
    eligible=[r for r in baseline if r['in_denominator_pooled']=='1' or r['in_denominator_technique']=='1']
    baseline_methods=defaultdict(list)
    for row in baseline:
        baseline_methods[(row['class'],row['technique'])].append(row)
    extracted={(r['class'],r['technique']):r for r in files()}
    started=time.monotonic()
    finals=[]
    for release in releases:
        tag=release['tag']
        root=EXP/'llm'/tag
        root.mkdir(parents=True,exist_ok=True)
        if (root/'final-summary.json').exists():
            final=json.loads((root/'final-summary.json').read_text())
        else:
            start=time.monotonic()
            invoke('llm_compile.py',['--release',tag,'--workers',str(WORKERS)],root/'compile.log')
            compiled=json.loads((root/'compile-summary.json').read_text())
            assert compiled['workers']==WORKERS
            invoke('llm_run.py',['--release',tag,'--run','1','--workers',str(WORKERS)],root/'run.log')
            run=json.loads((root/'run1/run-summary.json').read_text())
            final_methods=[]
            flaky=[]
            reruns=[]
            differences=[]
            rerun_tasks=[]
            initial_summaries={}
            for file in compiled['files_ledger']:
                key=(file['class'],file['technique'])
                if file['status']=='compiled':
                    summary=json.loads((root/'run1'/f'{key[0]}.{key[1]}.summary.json').read_text())
                    initial_summaries[key]=summary
                    include=[m['method'] for m in summary['methods'] if m['status'] in {'fail','error'}]
                    if include:
                        rerun_tasks.append((key,include))
            def rerun(task):
                key,include=task
                summary,failure=run_file(tag,extracted[key],root/'rerun'/f'{key[0]}.{key[1]}.json',include)
                assert {m['method'] for m in summary['methods']}==set(include)
                return key,{m['method']:m['status'] for m in summary['methods']}
            rerun_start=time.monotonic()
            with ThreadPoolExecutor(max_workers=WORKERS) as executor:
                repeats=dict(executor.map(rerun,rerun_tasks))
            rerun_seconds=round(time.monotonic()-rerun_start,3)
            for file in compiled['files_ledger']:
                key=(file['class'],file['technique'])
                if file['status']!='compiled':
                    statuses={r['method']:file['status'] for r in baseline_methods[key]}
                else:
                    summary=initial_summaries[key]
                    initial={m['method']:m['status'] for m in summary['methods']}
                    statuses={r['method']:initial.get(r['method'],'not-run') for r in baseline_methods[key]}
                    listed=set(summary['all_listed_methods']) if not summary['list_error'] else set()
                    expected={r['method'] for r in baseline_methods[key]}
                    if key in baseline_methods and (expected-listed or listed-expected):
                        differences.append(dict(tag=tag,**{'class':key[0]},technique=key[1],
                            missing=sorted(expected-listed),extra=sorted(listed-expected),list_error=summary['list_error']))
                    for method,status in repeats.get(key,{}).items():
                        reruns.append(dict(**{'class':key[0]},technique=key[1],method=method,initial=initial[method],rerun=status))
                        if status=='pass':
                            if method in statuses:
                                statuses[method]='pass'
                            flaky.append(dict(tag=tag,**{'class':key[0]},technique=key[1],
                                test_class=extracted[key]['package']+'.'+extracted[key]['test_class'] if extracted[key]['package'] else extracted[key]['test_class'],
                                method=method,initial=initial[method],rerun=status))
                final_methods.extend(dict(**{'class':key[0]},technique=key[1],test_class=r['test_class'],
                    method=r['method'],status=statuses[r['method']]) for r in baseline_methods[key])
            final=dict(tag=tag,workers=WORKERS,files_compiled=compiled['compiled'],files_compile_fail=compiled['compile_fail'],
                files_absent=compiled['absent'],methods_run=run['methods_measured'],seconds=round(time.monotonic()-start,3),
                compile_seconds=compiled['seconds'],run_seconds=run['seconds'],rerun_seconds=rerun_seconds,
                methods=final_methods,flaky=flaky,reruns=reruns,listing_differences=differences)
            write_json(root/'final-summary.json',final)
        finals.append(final)
        maps={f['tag']:{(r['class'],r['technique'],r['test_class'],r['method']):r['status'] for r in f['methods']} for f in finals}
        tags=[f['tag'] for f in finals]
        identity=['class','technique','test_class','method','body_hash','in_denominator_pooled','in_denominator_technique']
        matrix=[dict(**{k:r[k] for k in identity},**{tag:maps[tag][(r['class'],r['technique'],r['test_class'],r['method'])] for tag in tags}) for r in eligible]
        write_csv(EXP/'results/llm-matrix.csv',matrix,[*identity,*tags])
        write_csv(EXP/'results/llm-flaky.csv',[r for f in finals for r in f['flaky']],
            ['tag','class','technique','test_class','method','initial','rerun'])
        write_json(EXP/'results/llm-listing-differences.json',[r for f in finals for r in f['listing_differences']])
        fields=['tag','files_compiled','files_compile_fail','files_absent','methods_run','seconds','workers','compile_seconds','run_seconds','rerun_seconds']
        write_csv(EXP/'results/llm-releases-log.csv',[{k:f[k] for k in fields} for f in finals],fields)
        pooled=[r for r in matrix if r['in_denominator_pooled']=='1']
        print(json.dumps(dict(tag=tag,counts=dict(Counter(r[tag] for r in pooled)),workers=WORKERS,
            rerun_methods=len(final['reruns']),flaky=len(final['flaky']),listing_differences=len(final['listing_differences']),seconds=final['seconds'])),flush=True)
    write_json(EXP/'results/llm-later-summary.json',dict(releases_completed=len(finals),pooled_denominator=sum(r['in_denominator_pooled']=='1' for r in eligible),
        workers=WORKERS,seconds=round(time.monotonic()-started,3),
        compile_seconds=round(sum(f['compile_seconds'] for f in finals),3),run_seconds=round(sum(f['run_seconds'] for f in finals),3),
        rerun_seconds=round(sum(f['rerun_seconds'] for f in finals),3),rerun_methods=sum(len(f['reruns']) for f in finals),
        flaky_events=sum(len(f['flaky']) for f in finals),listing_differences=[r for f in finals for r in f['listing_differences']]))


if __name__=='__main__':
    main()
