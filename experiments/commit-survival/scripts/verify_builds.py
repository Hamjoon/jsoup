#!/usr/bin/env python3
"""Verify recorded builds and frozen input integrity without rerunning builds."""
import csv
import hashlib
import json
from pathlib import Path
import subprocess
from build_commits import COMMANDS, EXP, PREV, REPO, FIELDS


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    results=EXP/'results'
    setup=json.loads((results/'setup.json').read_text())
    manifest=json.loads((results/'frozen-inputs.json').read_text())
    changed=[r['path'] for r in manifest if digest(PREV/r['path'])!=r['sha256']]
    assert not changed,changed
    for record in setup['copied_results']:
        assert digest(results/record['file'])==digest(PREV/'results'/record['file'])==record['sha256']
    commits=list(csv.DictReader((results/'commits.csv').open()))
    builds=list(csv.DictReader((results/'builds.csv').open()))
    summary=json.loads((results/'build-summary.json').read_text())
    assert summary['complete'] and not summary['stopped_at_failure_threshold']
    assert len(builds)==len(commits)
    assert [r['point'] for r in builds]==[r['point'] for r in commits]
    commands=0
    for commit,row in zip(commits,builds):
        checkout=EXP/'checkouts'/row['point']
        record=json.loads((checkout/'build-record.json').read_text())
        assert record['sha']==commit['sha']
        assert all(str(record[f])==row[f] for f in FIELDS)
        assert record['source_unchanged']
        assert record['class_files']==sum(1 for _ in (checkout/'target/classes').rglob('*.class'))
        assert (checkout/'build.log').is_file()
        if int(row['index']):
            assert not record['baseline_copied']
            executed=record['commands']
            assert [r['command'] for r in executed]==COMMANDS[:len(executed)]
            assert row['build_fail']==str(int(any(r['exit_status'] for r in executed)))
            if row['build_fail']=='0':
                assert len(executed)==len(COMMANDS) and (checkout/'cp-compile.txt').is_file()
            commands+=sum(r['exit_status']==0 for r in executed)
    baseline=EXP/'checkouts'/commits[0]['point']
    baseline_files=[p for p in (PREV/'checkouts/jsoup-1.11.2').rglob('*') if p.is_file()]
    for p in baseline_files:
        assert digest(p)==digest(baseline/p.relative_to(PREV/'checkouts/jsoup-1.11.2'))
    subprocess.run(['git','diff','--exit-code',setup['base_commit'].split()[0],'--','experiments/survival'],cwd=REPO,check=True)
    record=dict(frozen_files_checked=len(manifest),changed_frozen_files=changed,
                copied_results_checked=len(setup['copied_results']),baseline_files_checked=len(baseline_files),
                build_records_checked=len(builds),successful_maven_commands=commands,
                previous_experiment_git_diff='empty')
    (results/'step3-integrity.json').write_text(json.dumps(record,indent=2)+'\n')
    print(json.dumps(record,indent=2))


if __name__=='__main__':
    main()
