#!/usr/bin/env python3
"""Join unchanged source-body identities with three runs and de-duplicate."""
from collections import Counter
import csv
import json
import time
from dev_common import BASELINE, EXP, write_json
from dev_later import write_csv
from dedup_lib import source_tests
from extract_v2 import TECHS
from llm_common import files,source,fqcn


def main():
    start=time.monotonic()
    compiled=json.loads((EXP/'llm'/BASELINE/'compile-summary.json').read_text())
    statuses={(r['class'],r['technique']):r for r in compiled['files_ledger']}
    run_summaries=[json.loads((EXP/'results'/f'llm-t-run{k}.json').read_text()) for k in [1,2,3]]
    assert all(r['complete'] for r in run_summaries)
    rows=[]
    duplicate_names=[]
    identities=[]
    for file in sorted(files(),key=lambda r:(r['class'],TECHS.index(r['technique']))):
        if statuses[(file['class'],file['technique'])]['status']!='compiled':
            continue
        text=source(file).read_bytes().decode('utf-8')
        tests=source_tests(text,file['package'])
        counts=Counter(m['method'] for m in tests)
        runs=[]
        for k in [1,2,3]:
            path=EXP/'llm'/BASELINE/f'run{k}'/f"{file['class']}.{file['technique']}.summary.json"
            summary=json.loads(path.read_text())
            methods={r['method']:r['status'] for r in summary['methods']}
            assert len(methods)==len(summary['methods'])
            runs.append((methods,summary))
        for method_name,count in counts.items():
            if count>1:
                duplicate_names.append(dict(**{'class':file['class']},technique=file['technique'],
                    test_class=fqcn(file),method=method_name,occurrences=count,file=file['file']))
        located=set()
        for method in tests:
            owner=method['test_class']
            name=method['method']
            outcomes=[]
            for measured,summary in runs:
                value=measured.get(name,'not-run') if owner==fqcn(file) and not summary['list_error'] else 'not-run'
                outcomes.append(value)
            duplicate_name=counts[name]>1
            passed=all(s=='pass' for s in outcomes) and not duplicate_name
            if owner==fqcn(file):
                located.add(name)
            row=dict(**{'class':file['class']},technique=file['technique'],test_class=owner,method=name,
                body_hash=method['body_hash'],opening_line=method['opening_line'],
                closing_line=text.count('\n',0,method['closing_offset'])+1,
                opening_offset=method['opening_offset'],closing_offset=method['closing_offset'],source_order=method['source_order'],
                run1=outcomes[0],run2=outcomes[1],run3=outcomes[2],pass_all_three=int(passed),
                kept_in_technique=0,kept_in_pooled=0,in_denominator_technique=0,in_denominator_pooled=0,
                duplicate_name=int(duplicate_name),flaky=int('pass' in outcomes and any(s!='pass' for s in outcomes)),
                file=file['file'],source_sha256=file['sha256'])
            rows.append(row)
        observed=set().union(*(set(m) for m,s in runs if not s['list_error']))
        unmatched=observed-located
        if unmatched:
            identities.append(dict(**{'class':file['class']},technique=file['technique'],
                unmatched_methods=[dict(method=m,statuses=[measured.get(m,'not-run') for measured,summary in runs]) for m in sorted(unmatched)]))
    # Flags are independent of passing: keep the first source occurrence as
    # the design specifies, then intersect those flags with all-three-pass.
    seen_tech=set()
    seen_pool=set()
    for row in rows:
        technique_key=(row['class'],row['technique'],row['body_hash'])
        pool_key=(row['class'],row['body_hash'])
        row['kept_in_technique']=int(technique_key not in seen_tech)
        row['kept_in_pooled']=int(pool_key not in seen_pool)
        seen_tech.add(technique_key)
        seen_pool.add(pool_key)
        row['in_denominator_technique']=int(row['pass_all_three'] and row['kept_in_technique'])
        row['in_denominator_pooled']=int(row['pass_all_three'] and row['kept_in_pooled'])
    write_csv(EXP/'results/llm-methods.csv',rows,list(rows[0]) if rows else ['class','technique','test_class','method'])
    write_csv(EXP/'results/llm-duplicate-names.csv',duplicate_names,['class','technique','test_class','method','occurrences','file'])
    write_json(EXP/'results/llm-unmatched-identities.json',identities)
    generated=list(csv.DictReader((EXP/'results/generation.csv').open()))
    extracted=list(csv.DictReader((EXP/'results/extraction.csv').open()))
    groups={}
    for group in [*TECHS,'pooled']:
        methods=rows if group=='pooled' else [r for r in rows if r['technique']==group]
        ledger=generated if group=='pooled' else [r for r in generated if r['technique']==group]
        extraction=extracted if group=='pooled' else [r for r in extracted if r['technique']==group]
        compilation=compiled['files_ledger'] if group=='pooled' else [r for r in compiled['files_ledger'] if r['technique']==group]
        passed=sum(r['pass_all_three'] for r in methods)
        flag='in_denominator_pooled' if group=='pooled' else 'in_denominator_technique'
        denominator=sum(r[flag] for r in methods)
        actual_listings=[]
        for file in compilation:
            if file['status']=='compiled':
                path=EXP/'llm'/BASELINE/'run1'/f"{file['class']}.{file['technique']}.summary.json"
                actual_listings.extend(json.loads(path.read_text())['all_listed_methods'])
        groups[group]=dict(files_generated=len(ledger),files_extracted=sum(r['status']=='extracted' for r in extraction),
            files_compiled=sum(r['status']=='compiled' for r in compilation),methods_listed=len(actual_listings),
            source_methods=len(methods),pass_in_all_three=passed,flaky=sum(r['flaky'] for r in methods),
            duplicates_removed=passed-denominator,duplicate_name_methods=sum(r['duplicate_name'] for r in methods),denominator=denominator)
    dev=json.loads((EXP/'results/dev-t-summary.json').read_text())['pass_in_all_three']
    summary=dict(groups=groups,comparison=dict(pooled_denominator=groups['pooled']['denominator'],
        developer_denominator=dev,pooled_below_developer=groups['pooled']['denominator']<dev),
        duplicate_scope='CUT and whitespace-stripped body hash across compiled-file source methods; first representative chosen before intersecting with all-three-pass',
        representative_order=[*TECHS,'source order'],unmatched_identities=identities,
        seconds=round(time.monotonic()-start,3))
    write_json(EXP/'results/llm-t-summary.json',summary)
    print(json.dumps(summary,indent=2))
    if any('pass' in method['statuses'] for record in identities for method in record['unmatched_methods']):
        raise SystemExit('STOP: passing JUnit identities were not located in frozen source; see llm-unmatched-identities.json')


if __name__=='__main__':
    main()
