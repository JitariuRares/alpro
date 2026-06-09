# ALPRo Evaluation Workspace

This folder stores measured results and annotation templates for the thesis evaluation.

## Completed

- Romanian plate validation evaluation: 36/36 controlled cases passed.
- Backend regression suite: 55/55 tests passed.

## Next datasets to complete

1. `photo_evaluation_template.csv`
   - Fill one row per photo.
   - Add the real plate, expected plate type and visual condition.
   - After running the app, fill detected plate, detected type, result and processing time.

2. `video_evaluation_template.csv`
   - Fill one row per video.
   - Add the real unique plates visible in the clip.
   - After processing, fill detected unique plates, raw detections and processing time.

3. `copilot_evaluation_template.csv`
   - Fill one row per natural-language prompt.
   - After testing, mark whether the intent, role handling and navigation were correct.

Use measured values only. Do not invent final accuracy numbers.
