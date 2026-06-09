# Romanian Plate Validation Evaluation

Date: 2026-06-09
Component: `RomanianPlateValidator`
Test class: `OcrServicePlateExtractionTest`
Command: `./mvnw.cmd -Dtest=OcrServicePlateExtractionTest test`

## Result

- Total cases: 36
- Passed: 36
- Failed: 0
- Errors: 0
- Skipped: 0
- Controlled validation accuracy: 100% on this test set

## Coverage

The test set covers:

- Standard Romanian plates
- Bucharest plates
- Valid county-code plates
- Diplomatic plates: CD, TC, CO
- Temporary plates
- PROBE plates
- Military plates
- MAI plates
- Local municipal-style plates
- Invalid county codes
- Malformed plate strings
- Candidate extraction from longer OCR-like text

## Notes

During evaluation, the LOCAL pattern was found to be too permissive. It classified malformed official-looking inputs such as `CJ ABC 123` and `MAI 12` as `LOCAL`. The validator was tightened so that local plates remain supported, while malformed official formats are no longer silently accepted as local municipal plates.
