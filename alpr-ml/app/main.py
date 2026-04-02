import io
import os
import re
import time
from dataclasses import dataclass
from typing import List, Optional

# Keep CPU threading predictable in container startup.
os.environ.setdefault("OMP_NUM_THREADS", "1")
os.environ.setdefault("OPENBLAS_NUM_THREADS", "1")
os.environ.setdefault("MKL_NUM_THREADS", "1")

import cv2
import numpy as np
from fastapi import FastAPI, File, HTTPException, UploadFile
from PIL import Image

app = FastAPI(title="ALPR ML Service", version="0.2.0")

PLATE_PATTERN = re.compile(r"[A-Z]{1,2}\d{2}[A-Z]{3}")


@dataclass
class Candidate:
    text: str
    confidence: float
    bbox: dict


class AlprPipeline:
    def __init__(self) -> None:
        self.bootstrap_errors: List[str] = []
        self.detector = None
        self.ocr = None

        self.yolo_model_id = os.getenv("YOLO_MODEL_ID", "keremberke/yolov5n-license-plate")
        self.yolo_conf = float(os.getenv("YOLO_CONFIDENCE_THRESHOLD", "0.25"))
        self.yolo_size = int(os.getenv("YOLO_IMAGE_SIZE", "640"))
        self.ocr_lang = os.getenv("PADDLE_OCR_LANG", "en")

        self._load_detector()
        self._load_ocr()

    def _load_detector(self) -> None:
        try:
            import torch
            import yolov5

            original_torch_load = torch.load

            def _torch_load_compat(*args, **kwargs):
                # yolov5 checkpoints require the old default for compatibility.
                kwargs.setdefault("weights_only", False)
                return original_torch_load(*args, **kwargs)

            torch.load = _torch_load_compat
            try:
                detector = yolov5.load(self.yolo_model_id, device="cpu")
            finally:
                torch.load = original_torch_load

            detector.conf = self.yolo_conf
            self.detector = detector
            print(f"[ALPR] YOLO detector loaded: {self.yolo_model_id}")
        except Exception as exc:
            msg = f"YOLO unavailable: {exc}"
            self.bootstrap_errors.append(msg)
            print(f"[ALPR] {msg}")

    def _load_ocr(self) -> None:
        try:
            from paddleocr import PaddleOCR

            self.ocr = PaddleOCR(
                use_angle_cls=False,
                lang=self.ocr_lang,
                use_gpu=False,
                show_log=False,
            )
            print("[ALPR] PaddleOCR loaded on CPU")
        except Exception as exc:
            msg = f"PaddleOCR unavailable: {exc}"
            self.bootstrap_errors.append(msg)
            print(f"[ALPR] {msg}")

    def infer(self, image_bytes: bytes) -> dict:
        started_at = time.perf_counter()

        image = self._decode_image(image_bytes)
        if image is None:
            raise HTTPException(status_code=400, detail="Fisierul incarcat nu este o imagine valida")

        candidates = self._detect_and_read(image)
        best = candidates[0] if candidates else None

        processing_ms = int((time.perf_counter() - started_at) * 1000)

        if not best:
            return {
                "plateText": None,
                "confidence": 0.0,
                "bbox": None,
                "candidates": [],
                "processingMs": processing_ms,
            }

        return {
            "plateText": best.text,
            "confidence": round(best.confidence, 4),
            "bbox": best.bbox,
            "candidates": [
                {"text": cand.text, "confidence": round(cand.confidence, 4)}
                for cand in candidates
            ],
            "processingMs": processing_ms,
        }

    def _decode_image(self, image_bytes: bytes) -> Optional[np.ndarray]:
        np_buffer = np.frombuffer(image_bytes, dtype=np.uint8)
        image = cv2.imdecode(np_buffer, cv2.IMREAD_COLOR)
        return image

    def _detect_and_read(self, image: np.ndarray) -> List[Candidate]:
        # If detector or OCR is missing, return an empty list instead of crashing.
        if self.detector is None or self.ocr is None:
            return []

        detections = self._yolo_detect(image)
        if not detections:
            return []

        candidates: List[Candidate] = []
        for detection in detections:
            crop = self._crop_bbox(image, detection["bbox"])
            if crop is None:
                continue

            ocr_text, ocr_confidence = self._read_with_paddle(crop)
            normalized = self._normalize_plate(ocr_text)
            if not normalized:
                continue

            # Combined score favors strong OCR and confident detection.
            combined_conf = (ocr_confidence * 0.7) + (detection["confidence"] * 0.3)
            candidates.append(
                Candidate(
                    text=normalized,
                    confidence=combined_conf,
                    bbox=detection["bbox"],
                )
            )

        candidates.sort(key=lambda c: c.confidence, reverse=True)
        return candidates

    def _yolo_detect(self, image: np.ndarray) -> List[dict]:
        results = self.detector(image, size=self.yolo_size)
        pred = results.pred[0]

        detections: List[dict] = []
        for row in pred.tolist():
            x1, y1, x2, y2, score, *_ = row
            if score < self.yolo_conf:
                continue

            bbox = {
                "x": int(max(0, x1)),
                "y": int(max(0, y1)),
                "w": int(max(1, x2 - x1)),
                "h": int(max(1, y2 - y1)),
            }
            detections.append({"bbox": bbox, "confidence": float(score)})

        detections.sort(key=lambda item: item["confidence"], reverse=True)
        return detections[:3]

    def _crop_bbox(self, image: np.ndarray, bbox: dict) -> Optional[np.ndarray]:
        x, y, w, h = bbox["x"], bbox["y"], bbox["w"], bbox["h"]
        x2 = min(image.shape[1], x + w)
        y2 = min(image.shape[0], y + h)
        if x >= x2 or y >= y2:
            return None
        return image[y:y2, x:x2]

    def _read_with_paddle(self, crop: np.ndarray) -> tuple[str, float]:
        result = self.ocr.ocr(crop, cls=False)
        if not result or not result[0]:
            return "", 0.0

        parts: List[str] = []
        confidences: List[float] = []

        for item in result[0]:
            if len(item) < 2:
                continue
            text = item[1][0] if item[1] and len(item[1]) > 0 else ""
            confidence = float(item[1][1]) if item[1] and len(item[1]) > 1 else 0.0
            if text:
                parts.append(text)
                confidences.append(confidence)

        if not parts:
            return "", 0.0

        avg_conf = float(sum(confidences) / len(confidences)) if confidences else 0.0
        return " ".join(parts), avg_conf

    def _normalize_plate(self, text: str) -> Optional[str]:
        if not text:
            return None

        cleaned = re.sub(r"[^A-Za-z0-9]", "", text).upper()
        if not cleaned:
            return None

        matched = PLATE_PATTERN.search(cleaned)
        if matched:
            return matched.group(0)

        return None


pipeline = AlprPipeline()


@app.get("/health")
def health() -> dict:
    return {
        "status": "ok",
        "detectorLoaded": pipeline.detector is not None,
        "ocrLoaded": pipeline.ocr is not None,
        "bootstrapErrors": pipeline.bootstrap_errors,
    }


@app.post("/infer/car-image")
async def infer_car_image(image: UploadFile = File(...)) -> dict:
    content = await image.read()
    if not content:
        raise HTTPException(status_code=400, detail="Imagine goala")

    # Validate that upload is an actual image before inference.
    try:
        with Image.open(io.BytesIO(content)) as img:
            img.verify()
    except Exception as exc:
        raise HTTPException(status_code=400, detail="Fisierul incarcat nu este o imagine valida") from exc

    return pipeline.infer(content)
