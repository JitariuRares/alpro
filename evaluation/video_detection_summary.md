# Video Detection Evaluation - Raw System Output

Date: 2026-06-09
Endpoint: `POST /api/video-jobs` and `GET /api/video-jobs/{id}/results`
Video folder: `C:\Users\asus\Downloads\Phone Link\converted_mp4`
Results: `evaluation\video_detection_results.csv`
Raw JSON: `evaluation\video_detection_raw_jobs.json`

## Raw Result Before Ground Truth Comparison

- Total videos: 14
- Completed jobs: 14
- Failed/non-completed jobs: 0
- Videos with at least one detected plate: 8
- Total unique detected plate strings across videos: 15
- Total raw detection records: 38
- Average processing wait time: 28418 ms/video

## Notes

These are raw application detections. Final video accuracy requires manual ground truth: the visible plate count and real plate numbers in each video.
