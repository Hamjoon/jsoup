#!/usr/bin/env python3
"""Audit completed ledgers, frozen inputs, source trees, and figure artifacts."""
import hashlib
import json
from pathlib import Path
import struct
import subprocess
import time
import xml.etree.ElementTree as ET
from collections import Counter
from dev_common import EXP,PREV,write_json
from survival import GROUPS,STATUSES,read,identity,key,members


def sha256(path): return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    started=time.monotonic(); repo=EXP.parents[1]; commits=read('commits.csv'); later=commits[1:]
    manifest=json.loads((EXP/'results/frozen-inputs.json').read_text())
    changed=[r['path'] for r in manifest if sha256(PREV/r['path'])!=r['sha256']]
    assert not changed,changed
    setup=json.loads((EXP/'results/setup.json').read_text())
    for r in setup['copied_results']:
        name=('prev-'+r['file']) if r['file'] in {'dev-matrix.csv','llm-matrix.csv'} else r['file']
        assert sha256(EXP/'results'/name)==r['sha256']==sha256(PREV/'results'/r['file'])
    subprocess.run(['git','diff','--exit-code',setup['base_commit'].split()[0],'--','experiments/survival'],cwd=repo,check=True)
    source_files=0
    for c in commits:
        tree=subprocess.check_output(['git','-c',f'safe.directory={repo}','-C',str(repo),'ls-tree','-rz',c['sha'],'--','src','pom.xml'])
        for item in tree.split(b'\0'):
            if not item: continue
            metadata,path=item.split(b'\t',1); mode,kind,digest=metadata.split()
            assert kind==b'blob'
            p=EXP/'checkouts'/c['point']/path.decode()
            data=str(p.readlink()).encode() if mode==b'120000' else p.read_bytes()
            actual=hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
            assert actual==digest.decode(), 'Checkout source changed: '+str(p)
            source_files+=1
    assert {r['point'] for r in read('dev-points-log.csv')}=={r['point'] for r in later}
    assert {r['point'] for r in read('llm-points-log.csv')}=={r['point'] for r in later}
    for track in ['dev','llm']:
        rows=read(track+'-matrix.csv')
        assert [k for k in rows[0] if k.startswith('c') and k[1:4].isdigit()]==[c['point'] for c in later]
        assert all(r[c['point']] in STATUSES for r in rows for c in later)
    checked_groups=[]
    for g in GROUPS:
        matrix=members(g,read(('dev' if g=='dev' else 'llm')+'-matrix.csv')); curve=read(g+'-survival.csv'); breaks=read(g+'-first-break.csv')
        assert len(curve)==len(commits)
        assert len(breaks)==len(matrix)==setup['denominators'][g]
        assert {key(r,g) for r in breaks}=={key(r,g) for r in matrix}
        expected={key(r,g):next(((int(c['index']),r[c['point']]) for c in later if r[c['point']] not in {'pass','not-evaluated'}),None) for r in matrix}
        for b in breaks:
            found=None if b['first_break_index']=='' else (int(b['first_break_index']),b['first_break_status'])
            assert found==expected[key(b,g)]
        for c,r in zip(commits,curve):
            i=int(c['index']); survivors=sum(v is None or v[0]>i for v in expected.values())
            assert int(r['survived'])==survivors
            assert int(r['point_pass'])+sum(int(r[s.replace('-','_')]) for s in STATUSES if s!='pass')==len(matrix)
        checked_groups.append(dict(group=g,denominator=len(matrix),final_survived=int(curve[-1]['survived']),final_point_pass=int(curve[-1]['point_pass'])))
    coverage=read('coverage-t.csv'); summary=read('coverage-t-summary.csv')
    cuts=[r for r in read('cut-classes.csv') if not r['excluded']]
    assert len(coverage)==len(cuts)*len(GROUPS)
    assert {(r['class'],r['group']) for r in coverage}=={(c['class'],g) for c in cuts for g in GROUPS}
    for r in summary:
        assert int(r['methods_executed'])==setup['denominators'][r['group']]
        sub=[c for c in coverage if c['group']==r['group']]
        for column in ['lines_covered','lines_total','branches_covered','branches_total']:
            assert sum(int(c[column]) for c in sub)==int(r[column])
    data=(EXP/'results/figure1-survival-by-commit.png').read_bytes()
    assert data[:8]==b'\x89PNG\r\n\x1a\n'
    width,height=struct.unpack('>II',data[16:24]); assert (width,height)==(1600,900)
    svg=ET.parse(EXP/'results/figure1-survival-by-commit.svg').getroot()
    assert svg.attrib['width']=='1600px' and svg.attrib['height']=='900px'
    record=dict(frozen_files_checked=len(manifest),changed_frozen_files=changed,copied_results_checked=len(setup['copied_results']),
                checkout_source_files_checked=source_files,points=len(later),groups=checked_groups,coverage_rows=len(coverage),
                figure_width=width,figure_height=height,seconds=round(time.monotonic()-started,3),verified=True)
    write_json(EXP/'results/final-integrity.json',record)
    print(json.dumps(record,indent=2))


if __name__=='__main__': main()
