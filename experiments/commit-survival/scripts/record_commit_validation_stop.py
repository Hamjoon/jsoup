#!/usr/bin/env python3
"""Record the merge-diff contradiction without choosing a replacement protocol."""
import json
from pathlib import Path
import time
from commits import EXP, START, END, git


def main():
    shas=[git('rev-parse',START+'^{commit}'),*git('rev-list','--first-parent','--reverse',START+'..'+END).splitlines()]
    for index,sha in enumerate(shas):
        parents=git('show','-s','--format=%P',sha).split()
        specified=['diff-tree','--no-commit-id','--name-only','-r','-m','--first-parent',sha]
        explicit=['diff-tree','--no-commit-id','--name-only','-r',parents[0],sha]
        paths=git(*specified).splitlines()
        first_parent_paths=git(*explicit).splitlines()
        if paths != first_parent_paths:
            break
    else:
        raise SystemExit('No merge-diff contradiction found')
    tags={str(i):git('tag','--points-at',s).splitlines() for i,s in enumerate(shas)}
    tags={i:ts for i,ts in tags.items() if ts}
    record=dict(status='stopped-before-builds',reason='The prescribed diff-tree command includes changes against both parents at a merge; the design requires first-parent changes only.',
        index=index,point=f'c{index:03d}-{sha[:7]}',sha=sha,parents=parents,
        subject=git('show','-s','--format=%s',sha),git_version=git('--version'),
        specified_command=['git',*specified],specified_paths=paths,specified_path_count=len(paths),
        proposed_command=['git',*explicit],first_parent_paths=first_parent_paths,first_parent_path_count=len(first_parent_paths),
        extra_paths=sorted(set(paths)-set(first_parent_paths)),
        total_rows=len(shas),later_commits=len(shas)-1,tag_positions=tags,builds_started=0,
        seconds_since_setup_record=round(time.time()-json.loads((EXP/'results/setup.json').read_text())['started_unix'],3))
    (EXP/'results/commits-validation-error.json').write_text(json.dumps(record,indent=2)+'\n')
    setup=json.loads((EXP/'results/setup.json').read_text())
    text=['# Early-stop handover: merge-diff contradiction','',record['reason'],'',
          'Branch: `'+setup['branch']+'`. Base commit: `'+setup['base_commit']+'`.','',
          'The setup was committed. Frozen denominator counts verified from the copied CSV files: '+', '.join(f'{g}: {n:,}' for g,n in setup['denominators'].items())+'.',
          f"Generated files: {setup['generated_files']}. Copied script imports passed in the reused run container; the previous experiment has no tracked changes.",'',
          f"The raw first-parent list contains {record['later_commits']} later commits ({record['total_rows']} rows including the baseline). Tag positions from Git:",'',
          *[f"- {i}: {', '.join(ts)}" for i,ts in record['tag_positions'].items()],'',
          f"The metadata assertion failed at index {record['index']}, `{record['sha']}`: {record['subject']}.",'',
          'Specified command:','```text',' '.join(record['specified_command']),'```',
          f"It returned {record['specified_path_count']} paths:",'```text',*record['specified_paths'],'```','',
          'Proposed explicit first-parent command (used read-only for diagnosis):','```text',' '.join(record['proposed_command']),'```',
          f"It returned {record['first_parent_path_count']} paths. Extra paths in the specified command:",'',
          *['- `'+p+'`' for p in record['extra_paths']],'',
          f"Builds started: {record['builds_started']}. Elapsed since the setup evidence record: {record['seconds_since_setup_record']} seconds (excludes initial inspection). No commit ledger was published after the assertion failed.",'',
          'Mechanical setup deviation: '+setup['deviations'][0],'',
          'Pending review: confirm explicit parent-to-commit diff-tree arguments for the intended first-parent metadata. Then rerun commit validation and continue builds to the normal build-review stop. Test execution and coverage have not begun. No push was performed.','',
          'Evidence: `results/setup.json`, `results/commits-validation-error.json`, and `results/frozen-inputs.json`. All numeric values above are rendered from those result files.']
    (EXP/'docs/handover-a.md').write_text('\n'.join(text)+'\n')
    print(json.dumps(record,indent=2))


if __name__=='__main__':
    main()
