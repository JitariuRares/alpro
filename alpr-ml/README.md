# ALPR ML Service

Serviciu Python folosit de backend pentru OCR pe imagine completa de masina.

Pipeline curent (CPU):

1. Detectie placuta cu YOLOv5 (`keremberke/yolov5n-license-plate`).
2. OCR pe crop-ul placutei cu PaddleOCR.
3. Normalizare text (`A-Z`, `0-9`, uppercase, regex RO simplu).

Endpoint-uri:

- `POST /infer/car-image` (multipart field: `image`)
- `GET /health`

Variabile utile:

- `YOLO_MODEL_ID` (default: `keremberke/yolov5n-license-plate`)
- `YOLO_CONFIDENCE_THRESHOLD` (default: `0.25`)
- `YOLO_IMAGE_SIZE` (default: `640`)
- `PADDLE_OCR_LANG` (default: `en`)
- `VIDEO_TARGET_PROCESSED_FRAMES` (default: `700`)
- `VIDEO_TRACK_OCR_MIN_FRAME_GAP` (default: `6`)
- `VIDEO_TRACK_FORCE_OCR_MAX_FRAME_GAP` (default: `24`)
- `VIDEO_REUSE_BBOX_IOU_THRESHOLD` (default: `0.85`)
- `VIDEO_TEXT_STABILITY_MIN_HITS` (default: `2`)
