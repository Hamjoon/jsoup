"""Generated-file ledger, immutable source checks, and isolated LLM classpaths."""
import csv
import hashlib
from dev_common import BASELINE, EXP, release_info


def files():
    return [r for r in csv.DictReader((EXP/'results/extraction.csv').open()) if r['status']=='extracted']


def source(row):
    path=EXP/row['file']
    assert hashlib.sha256(path.read_bytes()).hexdigest()==row['sha256'], 'Generated source changed: '+str(path)
    return path


def fqcn(row):
    return (row['package']+'.' if row['package'] else '')+row['test_class']


def classpath(tag,cut,tech):
    _,build=release_info(tag)
    parts=[str(EXP/'llm'/tag/'classes'/cut/tech),str(EXP/'tools/runner'),
           str(EXP/'checkouts'/tag/'target/classes')]
    parts.extend(p for p in build['cp_compile'].split(':') if p)
    parts.extend([str(EXP/'tools/junit-4.13.2.jar'),str(EXP/'tools/hamcrest-core-1.3.jar')])
    return ':'.join(parts)


def error_category(message):
    import re
    patterns=[('CFS',r'cannot find symbol'),('PDNE',r'package .* does not exist'),('PAI',r'has private access'),
        ('IT',r'incompatible types'),('CAM',r'constructor .* cannot be applied'),('MAM',r'method .* cannot be applied'),
        ('DCD',r'duplicate class'),('SCI',r'non-static .* cannot be referenced from a static context'),
        ('UE',r'unreported exception'),('VNI',r'might not have been initialized')]
    return next((c for c,p in patterns if re.search(p,message)),'other')
