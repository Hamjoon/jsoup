#!/usr/bin/env python3
"""Audit primary bytes, fixed requests, source identities, and denominators."""
import csv
import hashlib
import json
import re
import subprocess
import tarfile
from dev_common import BASELINE, EXP, write_json
from extract_jsoup import extract, TECHS


def main():
    manifest=json.loads((EXP/'prompts/manifest.json').read_text())
    hashes={(r['class'],r['technique']):r['sha256'] for r in manifest['files']}
    calls=list(csv.DictReader((EXP/'results/generation.csv').open()))
    assert len(calls)==len(hashes)==260
    assert {(r['class'],r['technique']) for r in calls}==set(hashes)
    attempts_checked=0
    for row in calls:
        root=EXP/'runs'/row['class']/row['technique']
        for k in range(1,int(row['attempts'])+1):
            folder=root if k==1 else root/f'attempt-{k}'
            body=json.loads((folder/'request.json').read_text())
            assert body['model']=='openai/gpt-oss-120b' and body['temperature']==0.7 and body['max_tokens']==16384
            assert set(body)=={'model','temperature','max_tokens','messages'}
            assert len(body['messages'])==1 and body['messages'][0]['role']=='user'
            assert hashlib.sha256(body['messages'][0]['content'].encode()).hexdigest()==hashes[(row['class'],row['technique'])]
            raw=json.loads((folder/'raw-response.json').read_text())
            content=((raw.get('choices') or [{}])[0].get('message') or {}).get('content')
            expected=content if isinstance(content,str) else ''
            assert (folder/'response.md').read_bytes()==expected.encode()
            record=json.loads((folder/'run.json').read_text())
            assert record['attempt']==k
            if k<int(row['attempts']):
                authorized=(row['class'],row['technique'],k)==('Elements','GToT',1) and record['finish_reason']=='error'
                assert record['status']=='http-error' or authorized, 'An unauthorized response was retried'
            attempts_checked+=1
    extracted=list(csv.DictReader((EXP/'results/extraction.csv').open()))
    checked=0
    for row in extracted:
        result=extract((EXP/row['response_path']).read_bytes().decode('utf-8'))
        source=result['combine_v2'].encode('utf-8')
        assert hashlib.sha256(source).hexdigest()==row['sha256']
        assert int(row['csr_v2'])==int(result['csr_v2'])
        if row['file']:
            assert (EXP/row['file']).read_bytes()==source
            checked+=1
    tools=[]
    for ledger in ['tool-validation.json','llm-tool-validation.json']:
        for row in json.loads((EXP/'results'/ledger).read_text())['file_hashes']:
            tools.append(dict(file=row['file'],unchanged=hashlib.sha256((EXP/row['file']).read_bytes()).hexdigest()==row['copied_sha256']))
    assert all(r['unchanged'] for r in tools)
    original=(EXP/'scripts/extract_v2.py').read_bytes()
    expected=original.replace(b"html = bool(re.search(r'<(?:html|body|div|script)\\b', comments, re.I))",
                              b"html = bool(re.search(r'<(?:html|body|div|script)\\b', code, re.I))")
    assert original!=expected and (EXP/'scripts/extract_jsoup.py').read_bytes()==expected
    snapshot=EXP/'results/part2-baseline-before-jsoup-extractor'
    protected=json.loads((snapshot/'protected-files.json').read_text())
    coverage_aggregates={g+suffix for g in [*TECHS,'pooled','Dev'] for suffix in ['.merged.exec','.xml','.csv','.merge.log','.report.log']}
    retained=[]
    ignored_metadata=[]
    for row in protected:
        path=EXP/row['file']
        if path.name=='.DS_Store':
            ignored_metadata.append(row['file'])
            continue
        if path.name=='run-summary.json' or ('/coverage/' in row['file'] and path.name in coverage_aggregates):
            continue
        assert hashlib.sha256(path.read_bytes()).hexdigest()==row['sha256'], 'Accepted artifact changed: '+row['file']
        retained.append(row)
    old_extraction=list(csv.DictReader((snapshot/'extraction.csv').open()))
    new_by_key={(r['class'],r['technique']):r for r in extracted}
    assert all(new_by_key[(r['class'],r['technique'])]==r for r in old_extraction if r['status']=='extracted')
    old_methods=list(csv.DictReader((snapshot/'llm-methods.csv').open()))
    # Pooled representative flags may change when earlier-technique files join;
    # source identity, statuses, and per-technique flags remain immutable.
    repo=EXP.parents[1]
    archive=subprocess.Popen(['git','-c',f'safe.directory={repo}','-C',str(repo),'archive',BASELINE],stdout=subprocess.PIPE)
    source_count=0
    differences=[]
    with tarfile.open(fileobj=archive.stdout,mode='r|') as tar:
        for member in tar:
            if member.isfile() and member.name.startswith('src/'):
                source_count+=1
                path=EXP/'checkouts'/BASELINE/member.name
                original=tar.extractfile(member).read()
                if not path.exists() or path.read_bytes()!=original:
                    differences.append(member.name)
    archive.stdout.close()
    assert archive.wait()==0 and not differences
    rows=list(csv.DictReader((EXP/'results/llm-methods.csv').open()))
    revised_methods={(r['file'],r['method'],r['source_order']):r for r in rows}
    mutable={'kept_in_pooled','in_denominator_pooled'}
    for old in old_methods:
        new=revised_methods[(old['file'],old['method'],old['source_order'])]
        assert {k:v for k,v in old.items() if k not in mutable}=={k:v for k,v in new.items() if k not in mutable}
    summary=json.loads((EXP/'results/llm-t-summary.json').read_text())
    pooled=[r for r in rows if r['in_denominator_pooled']=='1']
    assert len(pooled)==summary['groups']['pooled']['denominator']
    assert len({(r['class'],r['body_hash']) for r in pooled})==len(pooled)
    assert all(r['run1']==r['run2']==r['run3']=='pass' for r in pooled)
    for tech in TECHS:
        group=[r for r in rows if r['technique']==tech and r['in_denominator_technique']=='1']
        assert len(group)==summary['groups'][tech]['denominator']
        assert len({(r['class'],r['body_hash']) for r in group})==len(group)
        assert all(r['run1']==r['run2']==r['run3']=='pass' for r in group)
    for k in [1,2,3]:
        folder=EXP/'llm'/BASELINE/f'run{k}'
        for path in folder.glob('*.summary.json'):
            run=json.loads(path.read_text())
            assert run['cwd']==str(EXP/'checkouts'/BASELINE) and run['timeout_ms']==30000
            cp=run['classpath'].split(':')
            assert all('/dev/' not in p and '/lib/t' not in p and '/src/test/resources' not in p for p in cp)
    result=dict(requests_checked=len(calls),attempts_checked=attempts_checked,extracted_files_checked=checked,
        copied_tool_files_checked=len(tools),all_copied_tools_unchanged=True,
        baseline_source_and_resource_files_checked=source_count,baseline_source_differences=differences,
        pooled_denominator=len(pooled),primary_data_unchanged=True,request_settings_checked=True,
        only_authorized_non_http_reissue=True,denominator_checks_passed=True,llm_classpaths_isolated=True,
        accepted_artifacts_checked=len(retained),accepted_artifacts_unchanged=True,extractor_single_line_diff_verified=True,
        ignored_os_metadata=ignored_metadata)
    write_json(EXP/'results/llm-baseline-integrity.json',result)
    print(json.dumps(result,indent=2))


if __name__=='__main__':
    main()
