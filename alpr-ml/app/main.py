import io
import base64
import math
import os
import re
import tempfile
import time
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

os.environ.setdefault("OMP_NUM_THREADS", "1")
os.environ.setdefault("OPENBLAS_NUM_THREADS", "1")
os.environ.setdefault("MKL_NUM_THREADS", "1")

import cv2
import numpy as np
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from PIL import Image

app = FastAPI(title="ALPR ML Service", version="0.2.0")

STANDARD_PATTERN = re.compile(r"^(B|[A-Z]{2})\d{2,3}[A-Z]{3}$")
DIPLOMATIC_PATTERN = re.compile(r"^(CD|TC|CO)\d{6}$")
TEMPORARY_PATTERN = re.compile(r"^(B|[A-Z]{2})0\d{2,5}$")
PROBE_PATTERN = re.compile(r"^(B|[A-Z]{2})\d{3}PROBE$")
MILITARY_PATTERN = re.compile(r"^A\d{3,7}$")
MAI_PATTERN = re.compile(r"^MAI\d{3,7}$")
VALID_COUNTY_CODES = {
    "AB",
    "AR",
    "AG",
    "B",
    "BC",
    "BH",
    "BN",
    "BT",
    "BV",
    "BR",
    "BZ",
    "CS",
    "CL",
    "CJ",
    "CT",
    "CV",
    "DB",
    "DJ",
    "GL",
    "GR",
    "GJ",
    "HR",
    "HD",
    "IL",
    "IS",
    "IF",
    "MM",
    "MH",
    "MS",
    "NT",
    "OT",
    "PH",
    "SM",
    "SJ",
    "SB",
    "SV",
    "TR",
    "TM",
    "TL",
    "VL",
    "VN",
    "VS",
}
FORBIDDEN_SERIES_PREFIX = {"I", "O"}
FORBIDDEN_SERIES_VALUES = {"III", "OOO"}

DIGIT_LIKE_MAP = {
    "O": "0",
    "Q": "0",
    "D": "0",
    "I": "1",
    "L": "1",
    "Z": "2",
    "S": "5",
    "G": "6",
    "T": "7",
    "B": "8",
}

LETTER_LIKE_MAP = {
    "0": "O",
    "1": "I",
    "2": "Z",
    "3": "B",
    "4": "A",
    "5": "S",
    "6": "G",
    "7": "T",
    "8": "B",
    "9": "P",
}


@dataclass
class Candidate:
    text: str
    confidence: float
    bbox: dict


@dataclass
class TrackState:
    track_id: int
    plate_text: str
    bbox: dict
    last_frame_index: int
    last_ocr_frame_index: int
    last_ocr_bbox: Optional[dict]
    last_ocr_confidence: float


class AlprPipeline:
    def __init__(self) -> None:
        self.bootstrap_errors: List[str] = []
        self.detector = None
        self.ocr = None

        self.yolo_model_id = os.getenv("YOLO_MODEL_ID", "keremberke/yolov5n-license-plate")
        self.yolo_conf = float(os.getenv("YOLO_CONFIDENCE_THRESHOLD", "0.25"))
        self.yolo_size = int(os.getenv("YOLO_IMAGE_SIZE", "640"))
        self.ocr_lang = os.getenv("PADDLE_OCR_LANG", "en")
        self.video_default_frame_step = int(os.getenv("VIDEO_DEFAULT_FRAME_STEP", "3"))
        self.video_default_max_frames = int(os.getenv("VIDEO_DEFAULT_MAX_FRAMES", "0"))
        self.video_target_processed_frames = int(os.getenv("VIDEO_TARGET_PROCESSED_FRAMES", "700"))
        self.video_max_detections_per_frame = int(os.getenv("VIDEO_MAX_DETECTIONS_PER_FRAME", "2"))
        self.video_track_iou_threshold = float(os.getenv("VIDEO_TRACK_IOU_THRESHOLD", "0.2"))
        self.video_track_max_gap_frames = int(os.getenv("VIDEO_TRACK_MAX_GAP_FRAMES", "10"))
        self.video_min_confidence = float(os.getenv("VIDEO_MIN_CONFIDENCE", "0.70"))
        self.video_emit_min_frame_gap = int(os.getenv("VIDEO_EMIT_MIN_FRAME_GAP", "12"))
        self.video_track_ocr_min_frame_gap = max(1, int(os.getenv("VIDEO_TRACK_OCR_MIN_FRAME_GAP", "6")))
        self.video_track_force_ocr_max_frame_gap = max(
            self.video_track_ocr_min_frame_gap,
            int(os.getenv("VIDEO_TRACK_FORCE_OCR_MAX_FRAME_GAP", "24")),
        )
        self.video_reuse_bbox_iou_threshold = max(
            0.0, min(1.0, float(os.getenv("VIDEO_REUSE_BBOX_IOU_THRESHOLD", "0.85")))
        )
        self.video_text_stability_min_hits = max(1, int(os.getenv("VIDEO_TEXT_STABILITY_MIN_HITS", "2")))
        self.video_emit_vehicle_image = os.getenv("VIDEO_EMIT_VEHICLE_IMAGE", "true").lower() == "true"
        self.video_vehicle_context_max_dim = max(320, int(os.getenv("VIDEO_VEHICLE_CONTEXT_MAX_DIM", "768")))

        self._load_detector()
        self._load_ocr()

    def _load_detector(self) -> None:
        try:
            import torch
            import yolov5

            original_torch_load = torch.load

            def _torch_load_compat(*args, **kwargs):
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

    def infer_video(self, video_bytes: bytes, frame_step: Optional[int], max_frames: Optional[int]) -> dict:
        started_at = time.perf_counter()

        temp_path = ""
        try:
            with tempfile.NamedTemporaryFile(delete=False, suffix=".mp4") as tmp:
                tmp.write(video_bytes)
                temp_path = tmp.name

            cap = cv2.VideoCapture(temp_path)
            if not cap.isOpened():
                raise HTTPException(status_code=400, detail="Fisierul incarcat nu este un video valid")

            fps = float(cap.get(cv2.CAP_PROP_FPS) or 0.0)
            total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
            normalized_frame_step, normalized_max_frames = self._resolve_video_sampling(
                frame_step=frame_step,
                max_frames=max_frames,
                total_frames=total_frames,
            )

            detections = []
            tracks: List[TrackState] = []
            track_text_scores: Dict[int, Dict[str, float]] = {}
            track_text_hits: Dict[int, Dict[str, int]] = {}
            track_last_emitted: Dict[int, Tuple[int, str]] = {}
            track_vehicle_images: Dict[int, dict] = {}
            next_track_id = 1
            frame_index = -1
            sampled_frames = 0

            while True:
                ok, frame = cap.read()
                if not ok:
                    break

                frame_index += 1
                if frame_index % normalized_frame_step != 0:
                    continue

                sampled_frames += 1
                if normalized_max_frames is not None and sampled_frames > normalized_max_frames:
                    break

                frame_detections = self._yolo_detect(frame)
                if not frame_detections:
                    self._prune_tracks(tracks, frame_index)
                    self._cleanup_track_caches(tracks, track_text_scores, track_text_hits, track_last_emitted)
                    continue

                timestamp_ms = int(cap.get(cv2.CAP_PROP_POS_MSEC) or 0)
                for detection in frame_detections[: self.video_max_detections_per_frame]:
                    candidate = Candidate(
                        text="",
                        confidence=float(detection["confidence"]),
                        bbox=detection["bbox"],
                    )
                    matched_track = self._match_track(candidate, frame_index, tracks)
                    if matched_track is None:
                        matched_track = TrackState(
                            track_id=next_track_id,
                            plate_text="",
                            bbox=candidate.bbox,
                            last_frame_index=frame_index,
                            last_ocr_frame_index=-10_000,
                            last_ocr_bbox=None,
                            last_ocr_confidence=0.0,
                        )
                        tracks.append(matched_track)
                        next_track_id += 1
                    else:
                        matched_track.bbox = candidate.bbox
                        matched_track.last_frame_index = frame_index

                    track_id = matched_track.track_id
                    if self._should_run_track_ocr(matched_track, frame_index):
                        ocr_text, ocr_combined_confidence = self._ocr_candidate_for_bbox(
                            frame,
                            matched_track.bbox,
                            candidate.confidence,
                        )
                        matched_track.last_ocr_frame_index = frame_index
                        matched_track.last_ocr_bbox = dict(matched_track.bbox)

                        if ocr_text:
                            matched_track.last_ocr_confidence = ocr_combined_confidence
                            text_scores = track_text_scores.setdefault(track_id, {})
                            text_hits = track_text_hits.setdefault(track_id, {})

                            text_scores[ocr_text] = text_scores.get(ocr_text, 0.0) + ocr_combined_confidence
                            text_hits[ocr_text] = text_hits.get(ocr_text, 0) + 1
                            matched_track.plate_text = max(text_scores.items(), key=lambda entry: entry[1])[0]
                            self._update_track_vehicle_image(
                                track_vehicle_images,
                                track_id,
                                frame,
                                matched_track.bbox,
                                ocr_combined_confidence,
                                frame_index,
                            )

                    stable_plate = matched_track.plate_text
                    if not stable_plate:
                        continue

                    if matched_track.last_ocr_confidence < self.video_min_confidence:
                        continue

                    stable_hits = track_text_hits.get(track_id, {}).get(stable_plate, 0)
                    if stable_hits < self.video_text_stability_min_hits and track_id not in track_last_emitted:
                        continue

                    last_emitted = track_last_emitted.get(track_id)
                    if (
                        last_emitted is not None
                        and last_emitted[1] == stable_plate
                        and (frame_index - last_emitted[0]) < self.video_emit_min_frame_gap
                    ):
                        continue

                    vehicle_image = track_vehicle_images.get(track_id, {})
                    detections.append(
                        {
                            "frameIndex": frame_index,
                            "timestampMs": timestamp_ms,
                            "trackId": track_id,
                            "plateText": stable_plate,
                            "confidence": round(matched_track.last_ocr_confidence, 4),
                            "bbox": dict(matched_track.bbox),
                            "vehicleImageBase64": vehicle_image.get("imageBase64"),
                            "vehicleImageMimeType": vehicle_image.get("mimeType"),
                        }
                    )
                    track_last_emitted[track_id] = (frame_index, stable_plate)

                self._prune_tracks(tracks, frame_index)
                self._cleanup_track_caches(tracks, track_text_scores, track_text_hits, track_last_emitted)

            cap.release()

            processing_ms = int((time.perf_counter() - started_at) * 1000)
            return {
                "frameStep": normalized_frame_step,
                "maxFrames": normalized_max_frames,
                "fps": round(fps, 3) if fps > 0 else None,
                "totalFrames": total_frames,
                "processedFrames": sampled_frames,
                "detections": detections,
                "processingMs": processing_ms,
            }
        finally:
            if temp_path and os.path.exists(temp_path):
                try:
                    os.remove(temp_path)
                except OSError:
                    pass

    def _decode_image(self, image_bytes: bytes) -> Optional[np.ndarray]:
        np_buffer = np.frombuffer(image_bytes, dtype=np.uint8)
        image = cv2.imdecode(np_buffer, cv2.IMREAD_COLOR)
        return image

    def _detect_and_read(self, image: np.ndarray) -> List[Candidate]:
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

    def _update_track_vehicle_image(
        self,
        track_vehicle_images: Dict[int, dict],
        track_id: int,
        frame: np.ndarray,
        plate_bbox: dict,
        confidence: float,
        frame_index: int,
    ) -> None:
        if not self.video_emit_vehicle_image:
            return

        existing = track_vehicle_images.get(track_id)
        if existing is not None and existing.get("confidence", 0.0) >= confidence:
            return
    
        vehicle_crop = self._crop_vehicle_context(frame, plate_bbox)
        encoded = self._encode_jpeg_base64(vehicle_crop)
        if encoded is None:
            return

        track_vehicle_images[track_id] = {
            "imageBase64": encoded,
            "mimeType": "image/jpeg",
            "confidence": confidence,
            "frameIndex": frame_index,
        }

    def _crop_vehicle_context(self, image: np.ndarray, plate_bbox: dict) -> np.ndarray:
        frame_h, frame_w = image.shape[:2]
        x, y, w, h = plate_bbox["x"], plate_bbox["y"], plate_bbox["w"], plate_bbox["h"]

        center_x = x + (w / 2)
        crop_w = max(w * 8, frame_w * 0.28)
        crop_h = max(h * 9, frame_h * 0.30)

        x1 = int(max(0, center_x - crop_w / 2))
        x2 = int(min(frame_w, center_x + crop_w / 2))
        y1 = int(max(0, y - crop_h * 0.60))
        y2 = int(min(frame_h, y + h + crop_h * 0.40))

        if x2 <= x1 or y2 <= y1:
            return image
        return image[y1:y2, x1:x2]

    def _encode_jpeg_base64(self, image: np.ndarray) -> Optional[str]:
        if image is None or image.size == 0:
            return None

        height, width = image.shape[:2]
        max_dim = max(height, width)
        if max_dim > self.video_vehicle_context_max_dim:
            scale = self.video_vehicle_context_max_dim / max_dim
            image = cv2.resize(
                image,
                (max(1, int(width * scale)), max(1, int(height * scale))),
                interpolation=cv2.INTER_AREA,
            )

        ok, buffer = cv2.imencode(".jpg", image, [int(cv2.IMWRITE_JPEG_QUALITY), 78])
        if not ok:
            return None
        return base64.b64encode(buffer).decode("ascii")

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

        compact = re.sub(r"[^A-Za-z0-9]", "", text).upper()
        if not compact:
            return None

        candidate_patterns = (
            re.compile(r"(CD|TC|CO)\d{6}"),
            re.compile(r"MAI\d{3,7}"),
            re.compile(r"A\d{3,7}"),
            re.compile(r"(B|[A-Z]{2})\d{3}PROBE"),
            re.compile(r"(B|[A-Z]{2})0\d{2,5}"),
            re.compile(r"(B|[A-Z]{2})\d{2,3}[A-Z]{3}"),
        )

        for pattern in candidate_patterns:
            for match in pattern.finditer(compact):
                normalized = self._normalize_ro_candidate(match.group())
                if normalized:
                    return normalized

        return None

    def _normalize_ro_candidate(self, candidate: str) -> Optional[str]:
        if DIPLOMATIC_PATTERN.match(candidate):
            return candidate

        if MAI_PATTERN.match(candidate):
            return candidate

        if MILITARY_PATTERN.match(candidate):
            return candidate

        probe = PROBE_PATTERN.match(candidate)
        if probe and probe.group(1) in VALID_COUNTY_CODES:
            return candidate

        temporary = TEMPORARY_PATTERN.match(candidate)
        if temporary and temporary.group(1) in VALID_COUNTY_CODES:
            return candidate

        standard = STANDARD_PATTERN.match(candidate)
        if not standard or standard.group(1) not in VALID_COUNTY_CODES:
            return None

        if candidate[0] == "B":
            prefix_len = 1
        else:
            prefix_len = 2

        serial_start = len(candidate) - 3
        county = candidate[:prefix_len]
        digits = "".join(self._normalize_digit_slot(ch) for ch in candidate[prefix_len:serial_start])
        letters = "".join(self._normalize_letter_slot(ch) for ch in candidate[serial_start:])
        normalized = f"{county}{digits}{letters}"

        if STANDARD_PATTERN.match(normalized) and self._is_valid_series_letters(letters):
            return normalized
        return None

    def _normalize_digit_slot(self, value: str) -> str:
        if value.isdigit():
            return value
        return DIGIT_LIKE_MAP.get(value, value)

    def _normalize_letter_slot(self, value: str) -> str:
        if "A" <= value <= "Z":
            return value
        return LETTER_LIKE_MAP.get(value, value)

    def _is_valid_series_letters(self, letters: str) -> bool:
        if len(letters) != 3:
            return False
        if letters in FORBIDDEN_SERIES_VALUES:
            return False
        if letters[0] in FORBIDDEN_SERIES_PREFIX:
            return False
        return True

    def _normalize_frame_step(self, frame_step: Optional[int]) -> int:
        if frame_step is None:
            return max(1, self.video_default_frame_step)
        return max(1, min(int(frame_step), 120))

    def _normalize_max_frames(self, max_frames: Optional[int]) -> Optional[int]:
        if max_frames is None:
            max_frames = self.video_default_max_frames

        if max_frames is None or int(max_frames) <= 0:
            return None

        return min(int(max_frames), 5000)

    def _resolve_video_sampling(
        self,
        frame_step: Optional[int],
        max_frames: Optional[int],
        total_frames: int,
    ) -> tuple[int, Optional[int]]:
        normalized_frame_step = self._normalize_frame_step(frame_step)
        normalized_max_frames = self._normalize_max_frames(max_frames)

        if frame_step is None and total_frames > 0 and self.video_target_processed_frames > 0:
            auto_step = max(1, math.ceil(total_frames / self.video_target_processed_frames))
            normalized_frame_step = max(normalized_frame_step, min(auto_step, 120))

        if max_frames is None and normalized_max_frames is None and self.video_target_processed_frames > 0:
            normalized_max_frames = self.video_target_processed_frames

        return normalized_frame_step, normalized_max_frames

    def _ocr_candidate_for_bbox(
        self, image: np.ndarray, bbox: dict, detection_confidence: float
    ) -> tuple[Optional[str], float]:
        crop = self._crop_bbox(image, bbox)
        if crop is None:
            return None, 0.0

        ocr_text, ocr_confidence = self._read_with_paddle(crop)
        normalized = self._normalize_plate(ocr_text)
        if not normalized:
            return None, 0.0

        combined_conf = (ocr_confidence * 0.7) + (detection_confidence * 0.3)
        return normalized, float(combined_conf)

    def _should_run_track_ocr(self, track: TrackState, frame_index: int) -> bool:
        frame_gap = frame_index - track.last_ocr_frame_index

        if track.last_ocr_frame_index < 0:
            return True

        if frame_gap >= self.video_track_force_ocr_max_frame_gap:
            return True

        if frame_gap < self.video_track_ocr_min_frame_gap:
            return False

        if track.last_ocr_bbox is None:
            return True

        return self._bbox_iou(track.bbox, track.last_ocr_bbox) < self.video_reuse_bbox_iou_threshold

    def _match_track(
        self, candidate: Candidate, frame_index: int, tracks: List[TrackState]
    ) -> Optional[TrackState]:
        best_track = None
        best_score = self.video_track_iou_threshold

        for track in tracks:
            frame_gap = frame_index - track.last_frame_index
            if frame_gap > self.video_track_max_gap_frames:
                continue

            iou_score = self._bbox_iou(candidate.bbox, track.bbox)
            if candidate.text == track.plate_text:
                iou_score += 0.15

            if iou_score >= best_score:
                best_score = iou_score
                best_track = track

        return best_track

    def _prune_tracks(self, tracks: List[TrackState], frame_index: int) -> None:
        tracks[:] = [
            track
            for track in tracks
            if (frame_index - track.last_frame_index) <= self.video_track_max_gap_frames
        ]

    def _cleanup_track_caches(self, tracks: List[TrackState], *caches: Dict[int, object]) -> None:
        active_track_ids = {track.track_id for track in tracks}
        for cache in caches:
            stale_track_ids = [track_id for track_id in list(cache.keys()) if track_id not in active_track_ids]
            for track_id in stale_track_ids:
                cache.pop(track_id, None)

    def _bbox_iou(self, first: dict, second: dict) -> float:
        if not first or not second:
            return 0.0

        ax1, ay1 = first["x"], first["y"]
        ax2, ay2 = ax1 + first["w"], ay1 + first["h"]
        bx1, by1 = second["x"], second["y"]
        bx2, by2 = bx1 + second["w"], by1 + second["h"]

        inter_x1 = max(ax1, bx1)
        inter_y1 = max(ay1, by1)
        inter_x2 = min(ax2, bx2)
        inter_y2 = min(ay2, by2)

        inter_w = max(0, inter_x2 - inter_x1)
        inter_h = max(0, inter_y2 - inter_y1)
        intersection = inter_w * inter_h
        if intersection <= 0:
            return 0.0

        area_a = max(1, first["w"] * first["h"])
        area_b = max(1, second["w"] * second["h"])
        union = area_a + area_b - intersection
        if union <= 0:
            return 0.0

        return float(intersection / union)


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

    try:
        with Image.open(io.BytesIO(content)) as img:
            img.verify()
    except Exception as exc:
        raise HTTPException(status_code=400, detail="Fisierul incarcat nu este o imagine valida") from exc

    return pipeline.infer(content)


@app.post("/infer/video")
async def infer_video(
    video: UploadFile = File(...),
    frame_step: Optional[int] = Form(None),
    max_frames: Optional[int] = Form(None),
) -> dict:
    content = await video.read()
    if not content:
        raise HTTPException(status_code=400, detail="Video gol")

    return pipeline.infer_video(content, frame_step=frame_step, max_frames=max_frames)
