#!/usr/bin/env python3
"""Verify source immutability and the measured run/rerun protocol."""
import csv
import hashlib
import json
import subprocess
import tarfile
import time
from dev_common import BASELINE, EXP, write_json


def main():
    start=time.monotonic()
    repo=EXP.parents[1]
    releases=list(csv.DictReader((EXP/'results/releases.csv').open()))
    provenance=json.loads((EXP/'results/tool-validation.json').read_text())
    tools=[dict(file=r['file'],unchanged=hashlib.sha256((EXP/r['file']).read_bytes()).hexdigest()==r['copied_sha256'])
           for r in provenance['file_hashes']]
    assert all(r['unchanged'] for r in tools)
    libraries=[dict(file=r['file'],unchanged=hashlib.sha256((EXP/'lib/t'/r['file']).read_bytes()).hexdigest()==r['sha256'])
               for r in csv.DictReader((EXP/'results/lib-t.csv').open())]
    assert all(r['unchanged'] for r in libraries)
    source_rows=[]
    for release in releases:
        tag=release['tag']
        checkout=EXP/'checkouts'/tag
        archive=subprocess.Popen(['git','-c',f'safe.directory={repo}','-C',str(repo),'archive',tag],stdout=subprocess.PIPE)
        checked=0
        differences=[]
        with tarfile.open(fileobj=archive.stdout,mode='r|') as stream:
            for member in stream:
                if member.isfile() and member.name.startswith('src/'):
                    original=stream.extractfile(member).read()
                    path=checkout/member.name
                    checked+=1
                    if not path.exists() or path.read_bytes()!=original:
                        differences.append(member.name)
        archive.stdout.close()
        assert archive.wait()==0
        source_rows.append(dict(tag=tag,source_and_resource_files_checked=checked,differences=differences))
    assert not any(r['differences'] for r in source_rows)
    protocol=[]
    baseline_keys={(r['test_class'],r['method']) for r in csv.DictReader((EXP/'results/dev-denominator.csv').open()) if r['in_denominator']=='1'}
    for release in releases[1:]:
        tag=release['tag']
        root=EXP/'dev'/tag
        final=json.loads((root/'final-summary.json').read_text())
        compiled=json.loads((root/'compile-summary.json').read_text())
        expected_reruns=set()
        for file in compiled['files']:
            name=file['test_class']
            output=root/'run1'/f'{name}.summary.json'
            if not output.exists():
                continue
            run=json.loads(output.read_text())
            if run.get('orchestration_class_ignore'):
                assert not run['launches'] and run['listing'] is None
                continue
            for record in [run['listing'],*run['launches']]:
                cp=record['command'][record['command'].index('-cp')+1]
                assert str(EXP/'checkouts'/BASELINE/'src/test/resources') in cp.split(':')
                assert all('test-classes' not in p and '/src/test/java' not in p for p in cp.split(':'))
                assert run['cwd']==str(EXP/'checkouts'/tag)
                assert run['timeout_ms']==30000
            expected_reruns.update((name,r['method']) for r in run['methods'] if r['status'] in {'fail','error'})
        observed={(r['test_class'],r['method']) for r in final['reruns']}
        assert observed==expected_reruns and len(observed)==len(final['reruns'])
        finals={(r['test_class'],r['method']):r['status'] for r in final['methods']}
        assert baseline_keys<=set(finals)
        flaky={(r['test_class'],r['method']) for r in final['flaky']}
        assert flaky=={(r['test_class'],r['method']) for r in final['reruns'] if r['rerun']=='pass'}
        for name in {r['test_class'] for r in final['reruns']}:
            rerun=json.loads((root/'rerun'/f'{name}.summary.json').read_text())
            wanted={m for c,m in expected_reruns if c==name}
            assert set(rerun['selected_methods'])==wanted
            assert {r['method'] for r in rerun['methods']}==wanted
        protocol.append(dict(tag=tag,denominator_methods=len(baseline_keys),rerun_methods=len(observed),
                             flaky_methods=len(flaky),checks_passed=True))
    write_json(EXP/'results/dev-integrity-check.json',dict(tools=tools,frozen_libraries=libraries,releases=source_rows,protocol=protocol,
        all_source_and_resource_files_unchanged=True,all_protocol_checks_passed=True,
        seconds=round(time.monotonic()-start,3)))
    print('Source, resource, copied-tool, classpath, and rerun checks passed.')


if __name__=='__main__':
    main()
