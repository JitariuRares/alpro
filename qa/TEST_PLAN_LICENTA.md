# Test Plan - ALPRo (Licenta)

## 1. Scop
Acest plan valideaza obiectivele sistemului ALPRo pentru 3 roluri (`POLICE`, `PARKING`, `INSURANCE`) si produce dovezi reproductibile pentru lucrarea de licenta.

## 2. Intrebari de validare
1. Sistemul respecta separarea pe roluri (RBAC)?
2. Fluxurile business sunt functionale cap-coada?
3. Datele persistate sunt corecte si consistente?
4. Optimizarea video reduce costul de procesare fara degradare evidenta a utilitatii detectiilor?

## 3. Mediu de test
1. Backend: `http://localhost:8080`
2. Frontend: `http://localhost:3000`
3. ALPR ML: `http://localhost:8000`
4. Baza de date: PostgreSQL (docker service `db`)
5. Rulare recomandata: `docker compose up --build`

## 4. Set minim de date
1. 3 conturi test: `POLICE`, `PARKING`, `INSURANCE`
2. 1 imagine vehicul pentru OCR foto
3. 1 video pentru OCR video (baseline curent)
4. 2 imagini pentru parking (`entry`, `exit`)
5. 1 placa de test ex: `B123ABC`

## 5. Matrice teste obligatorii

### 5.1 Functional - fluxuri pozitive
| ID | Flux | Pas | Rezultat asteptat |
|---|---|---|---|
| F-POL-01 | POLICE Foto | Upload imagine la `/api/ocr/full` | 200 + `plateNumber`, `bbox`, `confidence` |
| F-POL-02 | POLICE Lookup | GET `/api/police/lookup/{plate}` | 200 + date agregate (vehicul, asigurari, parking, detectii) |
| F-POL-03 | POLICE Video | Upload video + poll job | job `COMPLETED`, detectii listate |
| F-PARK-01 | PARKING Entry | POST `/api/parking/entry` cu foto | 201 + sesiune `OPEN` |
| F-PARK-02 | PARKING Exit | POST `/api/parking/exit` cu foto | 200 + sesiune `CLOSED` |
| F-PARK-03 | PARKING Search | GET `/api/parking/{plate}` | 200 + durata + dovezi disponibile |
| F-INS-01 | INSURANCE Create | POST `/api/insurance` | 200 + polita salvata |
| F-INS-02 | INSURANCE Update | PUT `/api/insurance/{id}` | 200 + polita actualizata |

### 5.2 Securitate - roluri negative
| ID | Caz | Rezultat asteptat |
|---|---|---|
| S-NEG-01 | `PARKING` -> `/api/police/lookup/{plate}` | 403 |
| S-NEG-02 | `INSURANCE` -> `/api/video-jobs` | 403 |
| S-NEG-03 | `POLICE` -> `POST /api/insurance` | 403 |
| S-NEG-04 | `INSURANCE` -> `/api/parking/entry` | 403 |
| S-NEG-05 | `PARKING` -> `/api/ocr/full` | 403 |

### 5.3 Integritate date
| ID | Caz | Rezultat asteptat |
|---|---|---|
| D-INT-01 | Double ENTRY pe aceeasi placa cu sesiune OPEN | al doilea request respins |
| D-INT-02 | EXIT fara sesiune OPEN | request respins |
| D-INT-03 | Dovada foto entry/exit | endpoint evidence returneaza fisier valid |
| D-INT-04 | Audit asigurare create/update | rand nou in `audit_logs` |
| D-INT-05 | Audit lookup politie | rand nou in `audit_logs` |

### 5.4 Performanta video
| ID | Caz | Rezultat asteptat |
|---|---|---|
| V-PERF-01 | 3 rulari pe acelasi clip | timp mediu stabil (fara variatii extreme) |
| V-PERF-02 | Comparatie baseline vs optimizat | optimizat <= baseline ca timp |
| V-PERF-03 | Corectitudine practica | detectiile relevante raman utilizabile |

## 6. Dovezi pentru licenta
1. Export JSON raport API (script `qa/api_smoke_tests.ps1`)
2. Capturi UI pentru fiecare flux si fiecare rol
3. Export query SQL pentru audit/integritate (script `qa/sql_checks.sql`)
4. Raport benchmark video completat (`qa/benchmark_video_template.md`)
5. Optional: video scurt de demo (2-3 minute)

## 7. Criterii Definition of Done
1. Toate testele `Functional` trec.
2. Toate testele `Securitate` returneaza blocare corecta (403/401).
3. Testele de integritate confirma reguli business (`OPEN/CLOSED`, evidenta foto, audit).
4. Benchmark-ul video indica imbunatatire fata de baseline sau cel putin mentinere cu stabilitate mai buna.
5. Dovezile sunt arhivate in `qa/reports/`.

## 8. Procedura de executie recomandata
1. Ruleaza backend/frontend/ml.
2. Ruleaza scriptul API smoke.
3. Ruleaza testele UI manual (pe roluri).
4. Ruleaza benchmark video (3 repetari).
5. Exporta toate artefactele in `qa/reports/`.