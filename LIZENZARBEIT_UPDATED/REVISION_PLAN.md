# ALPRo Bachelor Thesis Revision Plan

## Goal

Keep the final thesis close to the current length of the old version, around 40 pages, but replace outdated content with an accurate description of the current ALPRo application.

The original folder remains unchanged. This folder is the working copy.

## Main Rule

Do not simply add many new pages. Replace obsolete sections with updated sections so the final document stays balanced.

## Current Problems In The Old Thesis

- The thesis describes the old pipeline as Tesseract OCR plus GPT fallback.
- The current project uses a stronger modular stack: React frontend, Spring Boot backend, PostgreSQL, FastAPI ML service, YOLO-based ALPR, strict Romanian plate validation, OpenAI Copilot, and OpenAI vehicle attribute recognition.
- Several Romanian/German characters are affected by encoding issues.
- The database schema section describes old entities that no longer match the actual application.
- The testing chapter contains old measurements and must be replaced with new real measurements later.
- The AI usage statement should be reviewed after the final writing workflow.

## Target Structure

### 1. Introduction

Keep the general motivation, but update the thesis contribution:

- Romanian ALPR ecosystem
- image and video plate detection
- role-based traffic service modules
- AI assistant for navigation and operational queries
- AI vehicle attribute recognition
- audit and review workflow

Target length: 3-4 pages.

### 2. Theoretical Background

Keep and update:

- ALPR evolution
- Romanian license plate formats
- OCR and object detection
- YOLO and modern detector evaluation
- LLMs and AI assistants in software systems

Reduce old Tesseract-centered explanations. Tesseract may remain as background, not as the main implemented pipeline if the current code no longer relies on it.

Target length: 8-10 pages.

### 3. System Requirements And Design

Rewrite around current roles and workflows:

- POLICE: detections, video ALPR, vehicle case, audit, PDF export, review queue
- PARKING: parking sessions and zone-based parking lookup
- INSURANCE: insurance policy search and management
- Copilot: role-aware assistant with deep links
- OpenAI vehicle recognition: make, model, color, body type

Target length: 5-6 pages.

### 4. Implementation Architecture

This is the most important technical chapter.

Update subsections:

- Backend overview: controllers, services, repositories, DTOs
- Frontend overview: dashboard, detections, vehicle case, parking, insurance, audit, copilot
- ML service overview: FastAPI, YOLO, OCR output, photo and video pipeline
- Database schema: current entities only
- Authentication and RBAC: JWT, Spring Security, role restrictions
- Romanian plate validation and classification
- OpenAI integrations: Copilot and vehicle attributes
- Docker deployment

Target length: 14-16 pages.

### 5. Testing And Evaluation

Do this later, after measurement.

Planned datasets:

- photo dataset: 50-100 images
- video dataset: 5-10 short videos
- AI vehicle dataset: 20-30 vehicle images
- Copilot dataset: 15-20 natural language prompts

Planned metrics:

- exact plate accuracy
- character accuracy
- precision, recall, F1
- valid Romanian format rate
- plate type classification accuracy
- video unique plate accuracy
- AI vehicle color/make/model correctness
- Copilot intent and navigation accuracy
- processing time
- OpenAI call count for cost awareness

Target length: 5-6 pages.

### 6. Challenges And Innovations

Update around current project:

- Romanian plate rules and special plate types
- efficient video processing
- review queue for uncertain detections
- controlled OpenAI usage for cost and latency
- role-based operational modules
- audit trail and PDF/CSV export

Target length: 3-4 pages.

### 7. Future Work

Keep, but update:

- larger evaluation dataset
- training/fine-tuning a dedicated vehicle make/model recognizer
- mobile deployment
- additional institutional integrations
- better model monitoring and offline mode

Target length: 2-3 pages.

### 8. Conclusion

Rewrite only after all main chapters are updated.

Target length: 1-2 pages.

## Measurement Reminder

Before writing the final Testing and Evaluation chapter, create a real evaluation dataset and compute the metrics. Do not invent final statistics. If the dataset is small, describe it honestly as a local prototype evaluation.

## Suggested Work Order

1. Fix encoding and compile the copied thesis.
2. Update title/abstract/introduction.
3. Update theoretical background and add newer sources.
4. Rewrite requirements and architecture.
5. Update screenshots/figures.
6. Build evaluation dataset and calculate metrics.
7. Rewrite testing/evaluation.
8. Rewrite future work and conclusion.
9. Final language cleanup.
10. Final LaTeX compile and PDF review.

## Draft Screenshot Set

Current screenshots provided by the user and usable as a first draft:

1. Photo detection result with AI vehicle suggestion
   - Shows image upload, normalized plate, plate type and AI make/model/color/body suggestion.
   - Later improvement: crop browser/taskbar and keep only the application area.

2. Dashboard overview
   - Shows main operational counters: vehicles, active policies, parking sessions, detection review, video processing and audit.
   - Later improvement: use a moment with more balanced data if available.

3. Video detection results
   - Shows unique plate result, raw detections and AI vehicle suggestion.
   - Later improvement: add a screenshot with video preview and live plate overlay.

4. Detection review queue
   - Shows confirmed detections and review controls.
   - Later improvement: capture a state with at least one pending item if possible.

5. Vehicle case page
   - Shows search by plate, vehicle summary, insurance status, parking status, recent detections and audit count.
   - Later improvement: use a case with parking/insurance data populated if available.

6. Audit page
   - Shows activity journal with Romanian action labels and plate-related events.
   - Later improvement: crop browser/taskbar and use a clean filtered view.

7. Copilot widget
   - Shows natural language lookup and action buttons.
   - Later improvement: hide technical metadata from the UI before using in the final thesis.

8. PDF vehicle report
   - Shows exported report with vehicle image and operational sections.
   - Later improvement: translate remaining English labels and use a cleaner report with parking/insurance entries if available.

Screenshots still recommended for the final version:

- Parking module screen, preferably sessions/search view.
- Insurance module screen, preferably policy search or policy management.
- Video preview with visible live detection overlay.
