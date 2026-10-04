#!/usr/bin/env python3
"""Audit release absence, classpaths, rerun selection, frozen rows and denominators."""
import json
import time
from dev_common import EXP, write_json
from llm_common import files,source
from survival_common import GROUPS, read_csv, releases, matrix, key


def main():
    start=time.monotonic()
    extracted={(r['class'],r['technique']):r for r in files()}
    for row in extracted.values():
        source(row)
    baseline=read_csv('llm-methods.csv')
    eligible={(r['class'],r['technique'],r['test_class'],r['method']) for r in baseline
        if r['in_denominator_pooled']=='1' or r['in_denominator_technique']=='1'}
    rows=read_csv('llm-matrix.csv')
    assert len(rows)==len(eligible) and {key(r,'pooled') for r in rows}==eligible
    presence={r['class']:r for r in read_csv('cut-presence.csv')}
    cuts={r['class']:r for r in read_csv('cut-classes.csv') if not r['excluded']}
    protocol=[]
    for release in releases():
        tag=release['tag']
        root=EXP/'llm'/tag
        compiled=json.loads((root/'compile-summary.json').read_text())
        final=json.loads((root/'final-summary.json').read_text())
        assert compiled['workers']==final['workers']==4 and len(compiled['files_ledger'])==len(extracted)
        expected=set()
        for file in compiled['files_ledger']:
            name,tech=file['class'],file['technique']
            exists=(EXP/'checkouts'/tag/cuts[name]['path']).exists()
            assert exists==(presence[name][tag]=='1')
            if file['status']=='absent':
                assert not exists
                assert not (root/'classes'/name/tech).exists()
                assert not (root/'javac'/f'{name}.{tech}.err').exists()
                continue
            assert exists
            if file['status']!='compiled':
                continue
            run=json.loads((root/'run1'/f'{name}.{tech}.summary.json').read_text())
            assert run['cwd']==str(EXP/'checkouts'/tag) and run['timeout_ms']==30000
            cp=run['classpath'].split(':')
            assert cp[0]==str(root/'classes'/name/tech)
            assert all('/dev/' not in p and '/lib/t' not in p and '/src/test/' not in p and 'test-classes' not in p for p in cp)
            expected.update((name,tech,m['method']) for m in run['methods'] if m['status'] in {'fail','error'})
        observed={(r['class'],r['technique'],r['method']) for r in final['reruns']}
        assert observed==expected and len(observed)==len(final['reruns'])
        flaky={(r['class'],r['technique'],r['method']) for r in final['flaky']}
        assert flaky=={(r['class'],r['technique'],r['method']) for r in final['reruns'] if r['rerun']=='pass'}
        finals={key(r,'pooled'):r['status'] for r in final['methods']}
        assert eligible<=set(finals)
        for row in rows:
            assert row[tag]==finals[key(row,'pooled')]
        for name,tech in {(c,t) for c,t,m in expected}:
            rerun=json.loads((root/'rerun'/f'{name}.{tech}.summary.json').read_text())
            wanted={m for c,t,m in expected if (c,t)==(name,tech)}
            assert set(rerun['selected_methods'])==wanted and {m['method'] for m in rerun['methods']}==wanted
        protocol.append(dict(tag=tag,workers=4,rerun_methods=len(observed),flaky_methods=len(flaky),checks_passed=True))
    accepted={'pooled':1862,'ZSL':342,'FSL':381,'CoT':427,'ToT':345,'GToT':465,'dev':594}
    for group in GROUPS:
        assert len(matrix(group))==accepted[group]
        strict=read_csv(f'{group}-survival.csv')
        lenient=read_csv(f'{group}-survival-lenient.csv')
        assert len(strict)==len(lenient)==len(releases())
        assert all(int(r['denominator'])==accepted[group] for r in strict)
        assert all(int(b['survived'])>=int(a['survived']) for a,b in zip(strict,lenient))
    sequential=json.loads((EXP/'results/llm-parallel-verification.json').read_text())
    assert sequential['all_identical'] and len(sequential['releases'])==len(releases())
    write_json(EXP/'results/llm-survival-integrity.json',dict(generated_files_unchanged=len(extracted),
        eligible_matrix_rows=len(rows),accepted_denominators=accepted,protocol=protocol,
        all_presence_skip_checks_passed=True,all_classpath_and_rerun_checks_passed=True,
        all_parallel_sequential_comparisons_passed=True,seconds=round(time.monotonic()-start,3)))
    print('LLM source, denominator, presence, classpath, rerun and parallel-result audits passed.')


if __name__=='__main__':
    main()
