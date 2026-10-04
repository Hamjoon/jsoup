# Model probe

Requested model: `openai/gpt-oss-120b`.

Model entry recorded from the OpenRouter model-list endpoint:

```json
{
  "timestamp": "2026-10-04T09:54:25.905761+00:00",
  "model": "openai/gpt-oss-120b",
  "listed": true,
  "entry": {
    "id": "openai/gpt-oss-120b",
    "canonical_slug": "openai/gpt-oss-120b",
    "hugging_face_id": "openai/gpt-oss-120b",
    "name": "OpenAI: gpt-oss-120b",
    "created": 1754414231,
    "description": "gpt-oss-120b is an open-weight, 117B-parameter Mixture-of-Experts (MoE) language model from OpenAI designed for high-reasoning, agentic, and general-purpose production use cases. It activates 5.1B parameters per forward pass and is optimized...",
    "context_length": 131072,
    "architecture": {
      "modality": "text->text",
      "input_modalities": [
        "text"
      ],
      "output_modalities": [
        "text"
      ],
      "tokenizer": "GPT",
      "instruct_type": null
    },
    "pricing": {
      "prompt": "0.000000037",
      "completion": "0.00000017"
    },
    "top_provider": {
      "context_length": 131072,
      "max_completion_tokens": 117964,
      "is_moderated": false
    },
    "per_request_limits": null,
    "supported_parameters": [
      "frequency_penalty",
      "include_reasoning",
      "logit_bias",
      "logprobs",
      "max_tokens",
      "min_p",
      "presence_penalty",
      "reasoning",
      "reasoning_effort",
      "repetition_penalty",
      "response_format",
      "seed",
      "stop",
      "structured_outputs",
      "temperature",
      "tool_choice",
      "tools",
      "top_a",
      "top_k",
      "top_logprobs",
      "top_p"
    ],
    "default_parameters": {
      "temperature": null,
      "top_p": null,
      "frequency_penalty": null
    },
    "supported_voices": null,
    "knowledge_cutoff": "2024-06-30",
    "expiration_date": null,
    "links": {
      "details": "/api/v1/models/openai/gpt-oss-120b/endpoints"
    },
    "benchmarks": {
      "design_arena": [
        {
          "arena": "models",
          "category": "3d",
          "elo": 916,
          "win_rate": 29.4,
          "rank": 111
        },
        {
          "arena": "models",
          "category": "codecategories",
          "elo": 969,
          "win_rate": 33.4,
          "rank": 120
        },
        {
          "arena": "models",
          "category": "dataviz",
          "elo": 995,
          "win_rate": 43.6,
          "rank": 110
        },
        {
          "arena": "models",
          "category": "gamedev",
          "elo": 1005,
          "win_rate": 40.5,
          "rank": 110
        },
        {
          "arena": "models",
          "category": "uicomponent",
          "elo": 934,
          "win_rate": 35.7,
          "rank": 116
        },
        {
          "arena": "models",
          "category": "website",
          "elo": 974,
          "win_rate": 32.5,
          "rank": 122
        }
      ],
      "artificial_analysis": {
        "intelligence_index": 11.6,
        "coding_index": 30.4,
        "agentic_index": 3.7
      }
    },
    "reasoning": {
      "mandatory": true,
      "supported_efforts": [
        "high",
        "medium",
        "low"
      ],
      "default_effort": "medium"
    }
  }
}
```

Probe result:

```json
{
  "timestamp": "2026-10-04T09:54:25.909398+00:00",
  "latency": 3.092,
  "status": "ok",
  "http_status": 200,
  "finish_reason": "stop",
  "provider": "SambaNova",
  "prompt_tokens": 183,
  "completion_tokens": 1350,
  "reasoning_tokens": 495,
  "attempt": 1,
  "model_requested": "openai/gpt-oss-120b",
  "model_reported": "openai/gpt-oss-120b",
  "request_id": "gen-1791107665-pMD7Gqrko2u9OCCyOJa8",
  "cut": "UncheckedIOException",
  "prompt_path": "prompts/rendered/UncheckedIOException/ZSL.txt",
  "prompt_sha256": "9526ee76eb4e3dd14d067222f069c8b7beba5d68e7797bd860b6810c96fa3bf6",
  "source_tokens": 51,
  "start_marker_present": true,
  "end_marker_present": true,
  "first_ten_lines": [
    "###Test START##",
    "package org.jsoup;",
    "",
    "import org.junit.Test;",
    "import static org.junit.Assert.*;",
    "",
    "import java.io.IOException;",
    "",
    "/**",
    " * Unit tests for {@link UncheckedIOException}."
  ],
  "response_sha256": "7f0098c52099bc1c7ad7908f16d8095fabcf2338578e82ca16066d60ec1d5be5",
  "seconds": 3.27,
  "input_limit_applied": false,
  "max_tokens_sent": 16384
}
```
