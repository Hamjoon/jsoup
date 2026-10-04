# Tool provenance

Copied unchanged from the LLM4TS replication repository at commit `eb5088094ee51a7abb945f8cfabc5558803134e1`.

| File | Source SHA-256 | Copied SHA-256 |
|---|---|---|
| tools/junit-4.13.2.jar | 8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3 | 8e495b634469d64fb8acfa3495a065cbacc8a0fff55ce1e31007be4c16dc57d3 |
| tools/hamcrest-core-1.3.jar | 66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9 | 66fdef91e9739348df7a096aa384a5685f4e875584cce89386a7a47251c4d8e9 |
| tools/jacocoagent.jar | 67de51e9ca1db044f3a3d10613518befb02e8eee1015f2ff6d56cfb9d4506546 | 67de51e9ca1db044f3a3d10613518befb02e8eee1015f2ff6d56cfb9d4506546 |
| tools/jacococli.jar | c449591174982bbc003d1290003fcbc7b939215436922d2f0f25239d110d531a | c449591174982bbc003d1290003fcbc7b939215436922d2f0f25239d110d531a |
| tools/VERSIONS.md | 519ad230af3cbfa0029a1a7944f0caef80960eb33b63d5e63483c41e74e8c943 | 519ad230af3cbfa0029a1a7944f0caef80960eb33b63d5e63483c41e74e8c943 |
| tools/runner/JsonRunner.java | a327f70c735214a5cb83b8c0e327db18c030abd65e2c128c1f6385f4d01d8bde | a327f70c735214a5cb83b8c0e327db18c030abd65e2c128c1f6385f4d01d8bde |
| scripts/run_class.py | b40b267d791d48dad23bf861f40856f19939090c149aa4d38ef8f1014bc0de48 | b40b267d791d48dad23bf861f40856f19939090c149aa4d38ef8f1014bc0de48 |

## Interface (five lines)

JsonRunner takes FQCN, --list, --timeout-ms, and --exclude; run_class.py takes FQCN, required --classpath/--output, optional --timeout-ms/--cwd/--include.
JsonRunner emits JSON lines; the wrapper writes method/status/message/exception/milliseconds/launch JSON lines and a sibling .summary.json containing listing, launch logs, counts and timing.
Runner statuses are pass, fail, error, ignored, timeout; the wrapper adds not-run, but does not implement compile-fail.
At method timeout JsonRunner emits timeout and halts the JVM; the wrapper starts another JVM excluding completed and unselected methods.
Listing uses a separate JVM; --cwd resolves classpath entries to absolute paths; --include selects named methods and marks missing selections not-run.

## Status-contract clarification

The initial exact status-set check triggered an early stop because run_class.py does not emit compile-fail. Gary confirmed that compile-fail is assigned by dev_compile.py at the file level. The Step 3 check is satisfied by pass/fail/error/ignored/timeout from JsonRunner and not-run from run_class.py. Both copied files remain unchanged. Release builds may proceed; the denominator review stop remains required.
