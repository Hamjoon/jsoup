#!/usr/bin/env python3
"""Render the environment/baseline handover exclusively from results files."""
import csv
import json
from pathlib import Path

EXP=Path(__file__).resolve().parents[1]
RESULTS=EXP/'results'


def read_csv(name):
    path=RESULTS/name
    return list(csv.DictReader(path.open())) if path.exists() else []


def table(rows, columns):
    return '| '+' | '.join(title for title,key in columns)+' |\n| '+' | '.join('---' for _ in columns)+' |\n'+''.join(
        '| '+' | '.join(str(row.get(key,'' )).replace('|','\\|').replace('\n','<br>') for title,key in columns)+' |\n' for row in rows)


def main():
    env=json.loads((RESULTS/'env.json').read_text())
    text='# Environment and developer-baseline handover\n\n'
    text+='The copied runner and wrapper remain unchanged. Gary clarified that compile-fail is assigned by the developer compilation orchestrator at the file level; the runner/wrapper status check is satisfied. The experiment branch remains local. Only the pre-approved upstream tag push was performed.\n\n'
    text+='Gary also confirmed that the observed inventory is correct: all integration subpackages belong to integration. The corrected expected inventory and original annotation count are recorded in results/dev-inventory-summary.json.\n\n'
    text+='## Environment\n\n'
    text+=table([dict(item=k,value=env[k]) for k in ['image_id','image_digest','docker_desktop_version','host_architecture','docker_memory_bytes','python_packages']], [('Item','item'),('Value','value')])
    for name in ['java_version','maven_version']:
        text+='\n'+name+':\n\n```text\n'+env[name]+'```\n'
    text+='\nNetwork check (verbatim):\n\n```text\n'+(RESULTS/'network-check.txt').read_text()+'```\n'
    builds={r['tag']:r for r in read_csv('builds.csv')}
    releases=[dict(r,**{k:v for k,v in builds.get(r['tag'],{}).items() if k!='tag'}) for r in read_csv('releases.csv')]
    text+='\n## Releases and builds\n\n'+table(releases,[('Tag','tag'),('Date','date'),('Days','days_since_t'),('Build exit','mvn_exit'),('Seconds','seconds'),('Class files','class_files')])
    summary_path=RESULTS/'build-summary.json'
    if summary_path.exists():
        text+='\nBuild summary:\n\n```json\n'+summary_path.read_text()+'```\n'
    failure=RESULTS/'build-failure.json'
    if failure.exists():
        text+='\nRequired build stop:\n\n```json\n'+failure.read_text()+'```\n'
    text+='\n## Frozen test libraries\n\n'+table(read_csv('lib-t.csv'), [('File','file'),('SHA-256','sha256')])
    inventory=RESULTS/'dev-inventory-summary.json'
    if inventory.exists():
        data=json.loads(inventory.read_text())
        text+='\n## Test inventory\n\n```json\n'+inventory.read_text()+'```\n'
        text+='\nIntegration-package sources:\n\n'+table([r for r in read_csv('dev-files.csv') if r['integration']=='1'], [('Path','path'),('Package','package'),('Has @Test','has_test_annotation'),('Role','role')])
        if data['errors']:
            text+='\n## Required inventory stop\n\nThe baseline source inventory contradicts the document’s expected counts. No developer tests were compiled or executed, and the denominator has not been fixed. Source files were not changed. The package-first role counts are also recorded to distinguish the role-definition ambiguity from the actual number of executable test files.\n\nGary/Cowork must confirm the observed inventory or provide a corrected baseline decision before baseline compilation and execution proceed.\n'
    baseline=RESULTS/'dev-t-summary.json'
    stop=RESULTS/'dev-baseline-stop.json'
    if stop.exists():
        stop_data=json.loads(stop.read_text())
        if stop_data.get('resolved'):
            text+='\n## Resolved class-level ignore limitation\n\n'
            text+='Gary authorized dev_run.py to record the class-level ignored methods without executing them at the baseline and every later release. Copied tools and release sources remain unchanged.\n\n'
            text+='```json\n'+(RESULTS/'dev-run-policy.json').read_text()+'```\n'
            text+='\nThe interrupted baseline attempt was preserved and excluded from the measured runs. All measured runs are complete runs started after the orchestration correction.\n\n'
            text+='```json\n'+(RESULTS/'dev-run-restart.json').read_text()+'```\n'
        else:
            text+='\n## Required baseline execution stop\n\n```json\n'+stop.read_text()+'```\n'
            text+='\nThe first baseline run is incomplete. The ignored class has no method description tree for the unchanged runner to list. The denominator is unset and later runs were not started. Gary must confirm whether the orchestration layer may inventory the class’s annotated methods and record them as ignored without executing them.\n'
    compilation_path=RESULTS/'dev-t-compile.json'
    if compilation_path.exists():
        compilation=json.loads(compilation_path.read_text())
        text+='\n## Baseline compilation\n\n'
        text+=table([dict(item=k,value=compilation[k]) for k in ['helpers_failed','helper_attempts','files_compiled','files_compile_fail','cross_test_references','seconds']], [('Item','item'),('Value','value')])
    verification=RESULTS/'baseline-source-verification.json'
    if verification.exists():
        data=json.loads(verification.read_text())
        data.pop('files',None)
        text+='\nBaseline source and copied-tool integrity check:\n\n```json\n'+json.dumps(data,indent=2)+'\n```\n'
    if baseline.exists():
        text+='\n## Baseline summary\n\n```json\n'+baseline.read_text()+'```\n'
        text+='\nFlaky baseline methods:\n\n'+table([r for r in read_csv('dev-denominator.csv') if r['flaky_at_t']=='1'], [('Class','test_class'),('Method','method'),('Run one','run1'),('Run two','run2'),('Run three','run3')])
        text+='\nNon-denominator methods:\n\n'+table([r for r in read_csv('dev-denominator.csv') if r['in_denominator']=='0'], [('Class','test_class'),('Method','method'),('Run one','run1'),('Run two','run2'),('Run three','run3')])
        text+='\nBaseline run summaries:\n\n'
        for run in ['dev-t-run1.json','dev-t-run2.json','dev-t-run3.json']:
            text+='```json\n'+(RESULTS/run).read_text()+'```\n\n'
        summary=json.loads(baseline.read_text())
        if not summary['errors']:
            text+='The baseline denominator is now fixed. This is the required denominator review stop; later-release test execution has not started. Continue only after Gary confirms.\n'
        else:
            text+='The baseline summary records an unexpected result requiring review before further work.\n'
    text+='\n## Deviations and open questions\n\nThe initial exact tool status check was resolved by Gary: compile-fail belongs to dev_compile.py, while the copied wrapper adds only not-run. The original inventory expectations were corrected by Gary to the observed counts, with package membership taking precedence for integration helpers. Gary authorized orchestration of the class-level ignored methods, correcting the expected ignored count as recorded in dev-run-policy.json. The interrupted first attempt is preserved separately and excluded from the denominator calculation. The production-class map records the observed count as requested, using top-level production Java files excluding package-info (including examples). Maven quiet output contained no obsolete/deprecated warnings in the completed build logs; warnings are recorded from the output rather than inferred from the source level. No JDK was switched. The API-key file remains ignored and its contents were not inspected. No mechanical fixes were needed. All figures above are generated from results files.\n'
    (EXP/'docs/handover-part1-a.md').write_text(text)
    print('Updated docs/handover-part1-a.md from results files.')


if __name__=='__main__':
    main()
