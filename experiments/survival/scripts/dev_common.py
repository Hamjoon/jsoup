"""Shared paths and classpaths for frozen developer tests."""
import csv
import json
from pathlib import Path

EXP=Path(__file__).resolve().parents[1]
BASELINE='jsoup-1.11.2'
T=EXP/'checkouts'/BASELINE


def inventory():
    return list(csv.DictReader((EXP/'results/dev-files.csv').open()))


def test_classes():
    return [r for r in inventory() if r['role']=='test']


def fqcn(row):
    return row['package']+'.'+row['class']


def release_info(tag):
    rows=list(csv.DictReader((EXP/'results/releases.csv').open()))
    release=next((r for r in rows if r['tag']==tag),None)
    if release is None:
        raise ValueError('Unknown frozen release: '+tag)
    build=next(r for r in csv.DictReader((EXP/'results/builds.csv').open()) if r['tag']==tag)
    if int(build['mvn_exit']):
        raise ValueError('Release was not built: '+tag)
    return release,build


def classpath(tag, test=None, helpers=True):
    _,build=release_info(tag)
    parts=[]
    if helpers:
        parts.append(str(EXP/'dev'/tag/'helpers'))
    if test:
        parts.append(str(EXP/'dev'/tag/'classes'/test))
    parts.extend([str(EXP/'tools/runner'),str(T/'src/test/resources'),
                  str(EXP/'checkouts'/tag/'target/classes')])
    parts.extend(p for p in build['cp_compile'].split(':') if p)
    parts.extend([str(EXP/'lib/t/*'),str(EXP/'tools/junit-4.13.2.jar'),
                  str(EXP/'tools/hamcrest-core-1.3.jar')])
    return ':'.join(parts)


def write_json(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,indent=2)+'\n')
