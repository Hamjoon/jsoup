#!/usr/bin/env python3
"""Render the generation/baseline review handover from results files."""
from collections import Counter
import csv
import json
import statistics
from dev_common import EXP, write_json
from dev_later import write_csv
from extract_v2 import TECHS
from handover_a import table

RESULTS=EXP/'results'


def csv_rows(name):
    return list(csv.DictReader((RESULTS/name).open()))


def load(name):
    return json.loads((RESULTS/name).read_text())


def main():
    generation=load('generation-summary.json')
    calls=csv_rows('generation.csv')
    distributions=[]
    for tech in [*TECHS,'pooled']:
        group=calls if tech=='pooled' else [r for r in calls if r['technique']==tech]
        for metric in ['prompt_tokens','completion_tokens','reasoning_tokens']:
            values=[int(r[metric]) for r in group if r[metric]!='']
            distributions.append(dict(technique=tech,metric=metric,reported=len(values),min=min(values) if values else '',
                median=statistics.median(values) if values else '',max=max(values) if values else '',total=sum(values)))
    write_csv(RESULTS/'generation-token-summary.csv',distributions,['technique','metric','reported','min','median','max','total'])
    extraction=csv_rows('extraction-summary.csv')
    compiled=load('llm-compile-t-summary.json')
    compile_table=[]
    for tech in TECHS:
        group=[r for r in compiled['files_ledger'] if r['technique']==tech]
        compile_table.append(dict(technique=tech,files=len(group),compiled=sum(r['status']=='compiled' for r in group),
                                 compile_fail=sum(r['status']=='compile-fail' for r in group),error_lines=sum(r['error_count'] for r in group)))
    write_csv(RESULTS/'llm-compile-t-by-technique.csv',compile_table,list(compile_table[0]))
    error_rows=csv_rows('llm-compile-t-errors.csv')
    counts=Counter(r['category'] for r in error_rows)
    categories=[dict(category=c,count=counts[c]) for c in ['CFS','PDNE','PAI','IT','CAM','MAM','DCD','SCI','UE','VNI','other']]
    write_csv(RESULTS/'llm-compile-t-error-summary.csv',categories,['category','count'])
    baseline=load('llm-t-summary.json')
    groups=[dict(group=tech,**baseline['groups'][tech]) for tech in [*TECHS,'pooled']]
    write_csv(RESULTS/'llm-t-funnel.csv',groups,list(groups[0]))
    methods=csv_rows('llm-methods.csv')
    flaky=[{k:r[k] for k in ['class','technique','test_class','method','run1','run2','run3']} for r in methods if r['flaky']=='1']
    write_csv(RESULTS/'llm-flaky-t.csv',flaky,['class','technique','test_class','method','run1','run2','run3'])
    comparison=baseline['comparison']
    coverage=load('coverage-t-summary.json')
    stages=dict(generation=generation['seconds'],extraction=load('extraction-timing.json')['seconds'],
        compilation=compiled['seconds'],deduplication=baseline['seconds'],coverage=coverage['seconds'])
    stages.update({f'run{k}':load(f'llm-t-run{k}.json')['seconds'] for k in [1,2,3]})
    deviations=[
        'Gary accepted the observed abstract CUT count and all source-path absence cases; the accepted presence matrix will be used unchanged.',
        'All primary prompt, response, and generated-source bytes are protected from Git newline normalization with experiment-local attributes.',
        'Only HTTP errors are retried. Every attempt is retained; extraction uses the final selected attempt recorded in the generation ledger.',
        'Materialization follows the unchanged extractor CSR-v2 acceptance, including its HTML-detection flag. HTML-looking text can be flagged even inside Java string literals; no extraction or source repair was applied.',
        'Listing failures retain the original wrapper output and a separate source-inventoried JUnitListingFailure record; their methods are not denominator-eligible.',
        'Deduplication representatives are selected from compiled-file source methods in the fixed technique/source order before intersection with all-three-pass. Flags and source-body hashes remain separate from run status.',
        'Coverage executes complete compiled LLM classes and the whole developer suite. It is supplementary coverage, not a measurement limited to deduplicated denominator methods; JaCoCo XML source-file counters accompany its requested CSV reports.',
        'Whitespace present in prompts, raw responses, or extracted source is preserved as primary data rather than normalized.'
    ]
    report=dict(comparison=comparison,generation=generation,groups=baseline['groups'],
        coverage_failures=len(coverage['failures']),stage_seconds=stages,deviations=deviations,
        generation_complete=True,later_release_llm_execution_started=False,branch_pushed=False)
    write_json(RESULTS/'part2-baseline-summary.json',report)
    text=''
    if comparison['pooled_below_developer']:
        text+=f"Pooled LLM denominator {comparison['pooled_denominator']} is below developer denominator {comparison['developer_denominator']}; a second round requires Gary’s decision.\n\n"
    text+='# LLM baseline and denominator handover\n\n'
    text+='Generation and baseline evaluation are complete. This is the required denominator review stop; no later-release LLM execution or second generation round has started.\n\n'
    text+='## Generation summary\n\n```json\n'+json.dumps(generation,indent=2)+'\n```\n'
    unusual=[{k:r[k] for k in ['class','technique','finish_reason','status','attempts']} for r in calls if r['finish_reason']!='stop']
    if unusual:
        text+='\nResponses with a non-stop finish (content preserved; well-formed responses were not retried):\n\n'+table(unusual,[('CUT','class'),('Technique','technique'),('Finish','finish_reason'),('Status','status'),('Attempts','attempts')])
    text+='\n## Token distributions\n\n'+table(distributions,[('Technique','technique'),('Metric','metric'),('Reported','reported'),('Min','min'),('Median','median'),('Max','max'),('Total','total')])
    text+='\nReasoning tokens are a reported subset of completion usage and are not added again to completion totals. Missing usage fields are omitted from distributions, rather than imputed.\n'
    text+='\n## Extraction\n\n'+table(extraction,[('Technique','technique'),('Responses','responses'),('MSR','msr'),('CSR v2','csr'),('Extracted','extracted'),('No class','no_class'),('HTML flagged','html_flagged')])
    text+='\n## Baseline compilation\n\n'+table(compile_table,[('Technique','technique'),('Extracted files','files'),('Compiled','compiled'),('Compile fail','compile_fail'),('Error lines','error_lines')])
    text+='\nCompiler error categories:\n\n'+table(categories,[('Category','category'),('Diagnostic lines','count')])
    text+='\n## Baseline funnel and deduplication\n\n'+table(groups,[('Group','group'),('Generated','files_generated'),('Extracted','files_extracted'),('Compiled','files_compiled'),('Listed','methods_listed'),('Pass all runs','pass_in_all_three'),('Flaky','flaky'),('Passing duplicates removed','duplicates_removed'),('Denominator','denominator')])
    text+='\nDuplicate method-name records:\n\n'+table(csv_rows('llm-duplicate-names.csv'),[('CUT','class'),('Technique','technique'),('Test class','test_class'),('Method','method'),('Occurrences','occurrences')])
    text+='\nBaseline flaky methods:\n\n'+table(flaky,[('CUT','class'),('Technique','technique'),('Test class','test_class'),('Method','method'),('Run one','run1'),('Run two','run2'),('Run three','run3')])
    text+=f"\nDenominator comparison: pooled LLM {comparison['pooled_denominator']} versus developer {comparison['developer_denominator']}.\n"
    text+='\n## Run summaries\n\n'
    for k in [1,2,3]:
        record=load(f'llm-t-run{k}.json')
        failures=record.pop('listing_failures')
        record['listing_failure_files']=len(failures)
        text+='```json\n'+json.dumps(record,indent=2)+'\n```\n\n'
    text+='\n## Coverage summary\n\n```json\n'+json.dumps(coverage,indent=2)+'\n```\n'
    text+='\n## Recorded stage wall times\n\n'+table([dict(stage=k,seconds=v) for k,v in stages.items()],[('Stage','stage'),('Seconds','seconds')])
    text+='\n## Deviations and interpretation notes\n\n'+''.join('- '+d+'\n' for d in deviations)
    verification=RESULTS/'llm-baseline-integrity.json'
    if verification.exists():
        text+='\n## Integrity verification\n\n```json\n'+verification.read_text()+'```\n'
    text+='\n## Open questions and stop\n\n'
    if comparison['pooled_below_developer']:
        text+='Gary decides whether to authorize a second generation round. No additional calls have been made for that purpose.\n'
    else:
        text+='The pooled denominator meets the developer size threshold. Review the recorded funnel, deduplication, and extraction behavior before authorizing later-release LLM execution.\n'
    text+='\nThe API key remains ignored and was neither printed nor committed. The experiment branch was not pushed. All figures above were generated from results files.\n'
    (EXP/'docs/handover-part2-b.md').write_text(text)
    print(json.dumps(dict(comparison=comparison,coverage_failures=len(coverage['failures'])),indent=2))


if __name__=='__main__':
    main()
