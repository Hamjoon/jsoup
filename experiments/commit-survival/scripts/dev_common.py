"""Shared paths and classpaths for frozen developer tests."""
import csv
import json
from pathlib import Path

EXP=Path(__file__).resolve().parents[1]
PREV=EXP.parent/'survival'
FROZEN_TAG='jsoup-1.11.2'
FROZEN_SHA=next(r['commit'] for r in csv.DictReader((PREV/'results/releases.csv').open()) if r['tag']==FROZEN_TAG)
BASELINE='c000-'+FROZEN_SHA[:7]
T=PREV/'checkouts'/FROZEN_TAG


def inventory():
    return list(csv.DictReader((PREV/'results/dev-files.csv').open()))


def test_classes():
    return [r for r in inventory() if r['role']=='test']


def fqcn(row):
    return row['package']+'.'+row['class']


def point_info(tag):
    rows=list(csv.DictReader((EXP/'results/commits.csv').open()))
    release=next((r for r in rows if r['point']==tag),None)
    if release is None:
        raise ValueError('Unknown commit point: '+tag)
    build=next(r for r in csv.DictReader((EXP/'results/builds.csv').open()) if r['point']==tag)
    if int(build['build_fail']):
        raise ValueError('Point was not built: '+tag)
    return release,build


def classpath(tag, test=None, helpers=True):
    _,build=point_info(tag)
    parts=[]
    if helpers:
        parts.append(str(EXP/'dev'/tag/'helpers'))
    if test:
        parts.append(str(EXP/'dev'/tag/'classes'/test))
    parts.extend([str(PREV/'tools/runner'),str(T/'src/test/resources'),
                  str(EXP/'checkouts'/tag/'target/classes')])
    parts.extend(p for p in build['cp_compile'].split(':') if p)
    parts.extend([str(PREV/'lib/t/*'),str(PREV/'tools/junit-4.13.2.jar'),
                  str(PREV/'tools/hamcrest-core-1.3.jar')])
    return ':'.join(parts)


def write_json(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n')
