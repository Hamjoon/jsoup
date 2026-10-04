# LLM baseline and denominator handover

Generation and baseline evaluation are complete. This is the required denominator review stop; no later-release LLM execution or second generation round has started.

## Generation summary

```json
{
  "expected_calls": 260,
  "completed_calls": 260,
  "responses_with_content": 260,
  "finish_reasons": {
    "stop": 259,
    "error": 1
  },
  "statuses": {
    "ok": 260
  },
  "empty_contents": 0,
  "length_finishes": 0,
  "attempts": 260,
  "maximum_concurrency": 2,
  "seconds": 7023.558,
  "token_distributions": {
    "prompt_tokens": {
      "count": 260,
      "min": 183,
      "median": 1595.0,
      "max": 13384,
      "total": 698362
    },
    "completion_tokens": {
      "count": 260,
      "min": 1031,
      "median": 2917.5,
      "max": 10465,
      "total": 787336
    },
    "reasoning_tokens": {
      "count": 260,
      "min": 0,
      "median": 689.5,
      "max": 3128,
      "total": 196596
    }
  },
  "total_tokens": {
    "prompt_tokens": 698362,
    "completion_tokens": 787336,
    "reasoning_tokens": 196596
  }
}
```

Responses with a non-stop finish (content preserved; well-formed responses were not retried):

| CUT | Technique | Finish | Status | Attempts |
| --- | --- | --- | --- | --- |
| Elements | GToT | error | ok | 1 |

## Token distributions

| Technique | Metric | Reported | Min | Median | Max | Total |
| --- | --- | --- | --- | --- | --- | --- |
| ZSL | prompt_tokens | 52 | 183 | 1382.0 | 12987 | 131716 |
| ZSL | completion_tokens | 52 | 1295 | 2949.0 | 7700 | 152670 |
| ZSL | reasoning_tokens | 52 | 0 | 786.5 | 3128 | 44932 |
| FSL | prompt_tokens | 52 | 582 | 1773.0 | 13384 | 152479 |
| FSL | completion_tokens | 52 | 1076 | 2697.5 | 6213 | 148514 |
| FSL | reasoning_tokens | 52 | 0 | 709.0 | 2666 | 39766 |
| CoT | prompt_tokens | 52 | 289 | 1481.0 | 13094 | 137219 |
| CoT | completion_tokens | 52 | 1102 | 3230.0 | 10465 | 170260 |
| CoT | reasoning_tokens | 52 | 0 | 676.5 | 2482 | 39911 |
| ToT | prompt_tokens | 52 | 232 | 1436.0 | 13035 | 134949 |
| ToT | completion_tokens | 52 | 1053 | 2818.5 | 5727 | 150681 |
| ToT | reasoning_tokens | 52 | 0 | 601.5 | 1524 | 30895 |
| GToT | prompt_tokens | 52 | 369 | 1562.0 | 13175 | 141999 |
| GToT | completion_tokens | 52 | 1031 | 3125.5 | 6417 | 165211 |
| GToT | reasoning_tokens | 52 | 0 | 737.0 | 2892 | 41092 |
| pooled | prompt_tokens | 260 | 183 | 1595.0 | 13384 | 698362 |
| pooled | completion_tokens | 260 | 1031 | 2917.5 | 10465 | 787336 |
| pooled | reasoning_tokens | 260 | 0 | 689.5 | 3128 | 196596 |

Reasoning tokens are a reported subset of completion usage and are not added again to completion totals. Missing usage fields are omitted from distributions, rather than imputed.

## Extraction

| Technique | Responses | MSR | CSR v2 | Extracted | No class | HTML flagged |
| --- | --- | --- | --- | --- | --- | --- |
| ZSL | 52 | 52 | 34 | 34 | 0 | 18 |
| FSL | 52 | 52 | 36 | 36 | 0 | 16 |
| CoT | 52 | 52 | 38 | 38 | 0 | 14 |
| ToT | 52 | 52 | 33 | 33 | 0 | 19 |
| GToT | 52 | 52 | 34 | 34 | 1 | 17 |

## Baseline compilation

| Technique | Extracted files | Compiled | Compile fail | Error lines |
| --- | --- | --- | --- | --- |
| ZSL | 34 | 17 | 17 | 293 |
| FSL | 36 | 21 | 15 | 263 |
| CoT | 38 | 19 | 19 | 216 |
| ToT | 33 | 17 | 16 | 201 |
| GToT | 34 | 20 | 14 | 195 |

Compiler error categories:

| Category | Diagnostic lines |
| --- | --- |
| CFS | 834 |
| PDNE | 19 |
| PAI | 81 |
| IT | 105 |
| CAM | 31 |
| MAM | 0 |
| DCD | 0 |
| SCI | 1 |
| UE | 11 |
| VNI | 0 |
| other | 86 |

## Baseline funnel and deduplication

| Group | Generated | Extracted | Compiled | Listed | Pass all runs | Flaky | Passing duplicates removed | Denominator |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| ZSL | 52 | 34 | 17 | 247 | 220 | 0 | 0 | 220 |
| FSL | 52 | 36 | 21 | 300 | 263 | 0 | 0 | 263 |
| CoT | 52 | 38 | 19 | 305 | 280 | 0 | 0 | 280 |
| ToT | 52 | 33 | 17 | 258 | 232 | 0 | 1 | 231 |
| GToT | 52 | 34 | 20 | 358 | 320 | 0 | 0 | 320 |
| pooled | 260 | 175 | 94 | 1468 | 1315 | 0 | 91 | 1224 |

Duplicate method-name records:

| CUT | Technique | Test class | Method | Occurrences |
| --- | --- | --- | --- | --- |

Baseline flaky methods:

| CUT | Technique | Test class | Method | Run one | Run two | Run three |
| --- | --- | --- | --- | --- | --- | --- |

Denominator comparison: pooled LLM 1224 versus developer 594.

## Run summaries

```json
{
  "tag": "jsoup-1.11.2",
  "run": 1,
  "complete": true,
  "files_run": 94,
  "methods_measured": 1468,
  "methods_listed": 1468,
  "counts": {
    "error": 35,
    "pass": 1315,
    "fail": 118
  },
  "seconds": 19.732,
  "listing_failure_files": 0
}
```

```json
{
  "tag": "jsoup-1.11.2",
  "run": 2,
  "complete": true,
  "files_run": 94,
  "methods_measured": 1468,
  "methods_listed": 1468,
  "counts": {
    "error": 35,
    "pass": 1315,
    "fail": 118
  },
  "seconds": 19.014,
  "listing_failure_files": 0
}
```

```json
{
  "tag": "jsoup-1.11.2",
  "run": 3,
  "complete": true,
  "files_run": 94,
  "methods_measured": 1468,
  "methods_listed": 1468,
  "counts": {
    "error": 35,
    "pass": 1315,
    "fail": 118
  },
  "seconds": 19.837,
  "listing_failure_files": 0
}
```


## Coverage summary

```json
{
  "cuts": 52,
  "groups_reported": [
    "ZSL",
    "FSL",
    "CoT",
    "ToT",
    "GToT",
    "pooled",
    "Dev"
  ],
  "failures": [],
  "seconds": 33.287,
  "measurement": "One full run per compiled LLM test file, pooled across those classes; one whole developer JUnit suite run. Source-file line counters from JaCoCo XML accompany the requested raw CSV reports.",
  "group_totals": {
    "Dev": {
      "covered": 5581,
      "total": 7075
    },
    "pooled": {
      "covered": 2465,
      "total": 7075
    },
    "ZSL": {
      "covered": 1387,
      "total": 7075
    },
    "FSL": {
      "covered": 1562,
      "total": 7075
    },
    "CoT": {
      "covered": 1340,
      "total": 7075
    },
    "ToT": {
      "covered": 1733,
      "total": 7075
    },
    "GToT": {
      "covered": 1498,
      "total": 7075
    }
  }
}
```

## Recorded stage wall times

| Stage | Seconds |
| --- | --- |
| generation | 7023.558 |
| extraction | 0.648 |
| compilation | 47.353 |
| deduplication | 0.337 |
| coverage | 33.287 |
| run1 | 19.732 |
| run2 | 19.014 |
| run3 | 19.837 |

## Deviations and interpretation notes

- Gary accepted the observed abstract CUT count and all source-path absence cases; the accepted presence matrix will be used unchanged.
- All primary prompt, response, and generated-source bytes are protected from Git newline normalization with experiment-local attributes.
- Only HTTP errors are retried. Every attempt is retained; extraction uses the final selected attempt recorded in the generation ledger.
- Materialization follows the unchanged extractor CSR-v2 acceptance, including its HTML-detection flag. HTML-looking text can be flagged even inside Java string literals; no extraction or source repair was applied.
- Listing failures retain the original wrapper output and a separate source-inventoried JUnitListingFailure record; their methods are not denominator-eligible.
- Deduplication representatives are selected from compiled-file source methods in the fixed technique/source order before intersection with all-three-pass. Flags and source-body hashes remain separate from run status.
- Coverage executes complete compiled LLM classes and the whole developer suite. It is supplementary coverage, not a measurement limited to deduplicated denominator methods; JaCoCo XML source-file counters accompany its requested CSV reports.
- Whitespace present in prompts, raw responses, or extracted source is preserved as primary data rather than normalized.

## Integrity verification

```json
{
  "requests_checked": 260,
  "attempts_checked": 260,
  "extracted_files_checked": 175,
  "copied_tool_files_checked": 15,
  "all_copied_tools_unchanged": true,
  "baseline_source_and_resource_files_checked": 133,
  "baseline_source_differences": [],
  "pooled_denominator": 1224,
  "primary_data_unchanged": true,
  "request_settings_checked": true,
  "no_non_http_retries": true,
  "denominator_checks_passed": true,
  "llm_classpaths_isolated": true
}
```

## Open questions and stop

The pooled denominator meets the developer size threshold. Review the recorded funnel, deduplication, and extraction behavior before authorizing later-release LLM execution.

The API key remains ignored and was neither printed nor committed. The experiment branch was not pushed. All figures above were generated from results files.
