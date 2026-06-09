# Photo Detection Evaluation

Date: 2026-06-09
Endpoint: `POST /api/ocr/full`
Photo folder: `C:\Users\asus\Desktop\poze masini`
Ground truth: `evaluation\photo_ground_truth.csv`
Results: `evaluation\photo_detection_results.csv`

## Result

- Total images: 20
- Images with a detected valid plate: 18
- Images without a valid detected plate: 2
- Exact plate matches: 18/20 (90.0%)
- Plate type matches: 18/20 (90.0%)
- Average client-observed processing time: 2729 ms/image

## Notes

The measured time is client-observed request duration from upload start to backend response. It includes backend orchestration, ML inference and any enabled AI vehicle-attribute enrichment.
