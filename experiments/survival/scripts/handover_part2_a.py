#!/usr/bin/env python3
"""Write the pre-generation handover using only results-file evidence."""
from collections import Counter
import csv
import json
from dev_common import EXP, write_json
from dev_later import write_csv
from handover_a import table

RESULTS=EXP/'results'


def read_json(name):
    return json.loads((RESULTS/name).read_text())


def main():
    inventory=read_json('cut-inventory-summary.json')
    presence=list(csv.DictReader((RESULTS/'cut-presence.csv').open()))
    releases=[r for r in csv.DictReader((RESULTS/'releases.csv').open()) if int(r['index'])>0]
    exceptions=[]
    for cut in presence:
        absent=[r['tag'] for r in releases if cut[r['tag']]=='0']
        if absent:
            exceptions.append(dict(package=cut['package'],**{'class':cut['class']},path=cut['path'],
                first_absent=absent[0],last_absent=absent[-1],absent_releases=len(absent),
                unexpected=int(any(r['class']==cut['class'] and r['package']==cut['package'] for r in inventory['presence_unexpected']))))
    write_csv(RESULTS/'cut-presence-summary.csv',exceptions,
              ['package','class','path','first_absent','last_absent','absent_releases','unexpected'])
    tokens=list(csv.DictReader((RESULTS/'prompt-token-summary.csv').open()))
    render=read_json('prompt-render-summary.json')
    tools=read_json('llm-tool-validation.json')
    model=read_json('model-entry.json')
    probe=read_json('model-probe-summary.json')
    deviations=[
        'The observed abstract CUT count differs from the expected count; the selected CUT count and generation-call design are unchanged.',
        'Additional CUT source paths become absent at later releases; the observed presence matrix is retained under the explicit report-and-continue instruction.',
        'The first inventory-script version made the abstract-count mismatch fatal. This local check was corrected to follow the Step 10 report-and-continue instruction, and inventory was rerun; source data and selection were unchanged.',
        'Prompt rendering substitutes the three original template placeholder tokens in a single split/join operation. Inserted source and example text is never rescanned.',
        'The probe sends max_tokens exactly as specified by the request body; no input-length exclusion or truncation is applied.'
    ]
    summary=dict(observed_inventory=inventory['observed'],expected_inventory=inventory['expected'],
        abstract_classes=inventory['abstract_classes'],exclusions=inventory['exclusions'],
        source_path_absence_classes=len(exceptions),unexpected_presence_classes=sum(r['unexpected'] for r in exceptions),
        rendered_prompts=render['prompts'],model=model['model'],model_listed=model['listed'],probe_status=probe.get('status'),
        generation_started=False,deviations=deviations,
        stage_seconds=dict(copy=tools['seconds'],inventory=inventory['seconds'],render=render['seconds'],probe=probe.get('seconds')))
    write_json(RESULTS/'part2-pre-generation-summary.json',summary)
    text='# LLM track: pre-generation handover\n\n'
    text+='Prompt preparation and the single model probe are complete. The generation batch has not started. This is the required pre-generation review stop; the experiment branch has not been pushed. Existing builds and developer results were reused.\n\n'
    text+='## CUT inventory\n\n'+table([dict(item=k,expected=inventory['expected'][k],observed=inventory['observed'][k]) for k in inventory['expected']],
        [('Item','item'),('Expected','expected'),('Observed','observed')])
    text+='\nExclusions:\n\n'+table([dict(reason=k,files=v) for k,v in inventory['exclusions'].items()],[('Reason','reason'),('Files','files')])
    text+='\nAbstract CUTs:\n\n'+''.join('- `'+name+'`\n' for name in summary['abstract_classes'])
    text+='\n## CUT source-path presence exceptions\n\n'+table(exceptions,[('Package','package'),('Class','class'),('First absent','first_absent'),('Last absent','last_absent'),('Absent releases','absent_releases'),('Unexpected','unexpected')])
    text+='\nAbsence follows the original package/file-path definition; moved or renamed types are not matched to replacement files. Presence differences are reported and retained as Step 10 directs.\n'
    text+='\n## Prompt token summary\n\n'+table(tokens,[('Technique','technique'),('Prompts','prompts'),('Min','min'),('Median','median'),('Max','max'),('Above reference limit','exceed_4096')])
    text+='\nAll prompts are retained. Token counts use the cl100k_base encoding; the tokenizer version and rendering checks are recorded here:\n\n```json\n'+json.dumps(render,indent=2)+'\n```\n'
    text+='\n## Model entry\n\n```json\n'+json.dumps(model,indent=2)+'\n```\n'
    text+='\n## Probe result\n\n'+table([dict(item=k,value=probe.get(k)) for k in
        ['cut','source_tokens','status','http_status','model_requested','model_reported','provider','finish_reason','latency',
         'prompt_tokens','completion_tokens','reasoning_tokens','start_marker_present','end_marker_present','max_tokens_sent']],
        [('Item','item'),('Value','value')])
    text+='\nFirst lines of the assistant content (verbatim, from results/model-probe-summary.json):\n\n```text\n'+'\n'.join(probe.get('first_ten_lines',[]))+'\n```\n'
    text+='\nThe request body, raw response, assistant content, usage, and run metadata are saved under runs/probe/. Authentication headers and API-key contents were not saved or printed.\n'
    verification=RESULTS/'part2-pre-generation-check.json'
    if verification.exists():
        text+='\n## Preparation verification\n\n```json\n'+verification.read_text()+'```\n'
    text+='\n## Recorded wall times\n\n'+table([dict(stage=k,seconds=v) for k,v in summary['stage_seconds'].items()],[('Stage','stage'),('Seconds','seconds')])
    text+='\n## Deviations and interpretations\n\n'+''.join('- '+d+'\n' for d in summary['deviations'])
    text+='\n## Review and stop\n\nReview the model response, token distribution, abstract-count difference, and additional source-path absences before authorizing the generation batch. No generated test files were created. All numeric findings above come from results files.\n'
    (EXP/'docs/handover-part2-a.md').write_text(text)
    print(json.dumps({k:v for k,v in summary.items() if k not in ['deviations','abstract_classes']},indent=2))


if __name__=='__main__':
    main()
