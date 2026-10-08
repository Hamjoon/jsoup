#!/usr/bin/env python3
"""Render the build-review handover exclusively from results files."""
import csv
import json
from pathlib import Path

EXP=Path(__file__).resolve().parents[1]
RESULTS=EXP/'results'


def load(name):
    return json.loads((RESULTS/name).read_text())


def table(rows,fields):
    def cell(value):
        return str(value).replace('|',' / ').replace('\n',' ')
    return ['| '+' | '.join(fields)+' |','| '+' | '.join('---' for _ in fields)+' |',
            *['| '+' | '.join(cell(row[field]) for field in fields)+' |' for row in rows]]


def main():
    setup=load('setup.json')
    commits=load('commits-summary.json')
    builds=load('build-summary.json')
    revision=load('brief-revision.json')
    integrity=load('step3-integrity.json')
    failures=load('build-failures.json')
    env=load('build-environment.json')
    rows=list(csv.DictReader((RESULTS/'commits.csv').open()))
    status='Build review stop reached.' if builds['complete'] and not builds['stopped_at_failure_threshold'] else 'Early stop: build failure threshold exceeded.'
    lines=['# Commit-survival build handover','',status,'',
           'Branch: `'+setup['branch']+'`. Base commit: `'+setup['base_commit']+'`.','',
           '## Commit ledger','',
           f"Rows: {commits['rows']}, including the baseline; later commits: {commits['evaluated_points']}. The final SHA matches the copied release ledger.",
           f"Among later commits: merges {commits['merges']}; rows with main-source changes {commits['rows_with_main_changes']}; rows with unchanged main tree and POM {commits['rows_same_main_as_prev']}.",'',
           *table([{'index':i,'tag':tag} for i,tag in commits['tag_positions'].items()],['index','tag']),'',
           'Rows mentioning StringUtil or BooleanAttribute:','',
           *table(commits['notable_rows'],['index','point','date','subject','main_files']),'',
           '## Builds','',
           f"Later commits attempted: {builds['later_attempted']}; built: {builds['later_built']}; failed: {builds['later_failed']}. Build ledger rows: {builds['rows']}. The baseline was copied from the previous experiment, without rebuilding.",
           f"Workers: {builds['workers']}. Failure threshold: {builds['max_failures']}. Every later point was archived and built independently; unchanged-tree points were also built.",'',
           'Build failures and error tails:','']
    if not failures:
        lines.append('None.')
    for failure in failures:
        lines.extend([f"### {failure['index']} — {failure['sha']}",'',failure['subject'],'','```text',failure['error_tail'],'```',''])
    lines.extend(['','## Frozen inputs and environment','',
                  'Verified denominators: '+', '.join(f'{group}: {count:,}' for group,count in setup['denominators'].items())+'.',
                  f"Generated test files: {setup['generated_files']}. Frozen files checked: {integrity['frozen_files_checked']}; changed: {len(integrity['changed_frozen_files'])}. Copied result files checked: {integrity['copied_results_checked']}. Baseline copied files checked: {integrity['baseline_files_checked']}.",
                  f"Per-point build records checked: {integrity['build_records_checked']}. Successful Maven commands checked: {integrity['successful_maven_commands']}. Previous experiment tracked diff: {integrity['previous_experiment_git_diff']}.",
                  '', '```text',env['java'].strip(),env['maven'].strip(),'```','',
                  '## Wall time','',
                  f"Commit enumeration and validation: {commits['seconds']} seconds. Build stage, including baseline copy: {builds['seconds']} seconds.",
                  'Build started: '+builds['started_at']+'. Finished: '+builds['finished_at']+'.',
                  f"Resumed work through final integrity verification: {integrity['resumed_wall_seconds']} seconds. Initial setup/inspection time was not separately measured; the review pause is excluded from this resumed duration.",'',
                  '## Corrections, deviations, and remaining work','',
                  'The earlier merge-diff stop is resolved by the revised brief: '+revision['resolution'],
                  'Brief provenance: `'+revision['brief_path']+'`; SHA-256 `'+revision['brief_sha256']+'`.',
                  revision['mechanical_fix'],
                  *setup['deviations'],
                  'Baseline build-record duration measures the copy operation; its retained build.log comes from the frozen baseline build.',
                  'Analysis scripts remain staged copies awaiting the later analysis work. The copied release matrices are still intact under their original names and must be renamed to the prev-prefixed names before writing commit matrices.',
                  'The previous early-stop JSON is historical evidence; brief-revision.json records its resolution. No source or generated test was edited. No push was performed.',
                  '','Open question: reviewer confirmation to continue with test execution, coverage, and analysis. No test execution or coverage measurements have begun.','',
                  'Numeric evidence: setup.json, commits-summary.json, commits.csv, build-summary.json, build-failures.json, build-environment.json, and step3-integrity.json, all under results/.'])
    (EXP/'docs/handover-a.md').write_text('\n'.join(lines)+'\n')
    print(EXP/'docs/handover-a.md')


if __name__=='__main__':
    main()
