# QA Package (Licenta)

## Fisiere
- `TEST_PLAN_LICENTA.md` - plan complet de testare.
- `UI_CHECKLIST.md` - checklist demo manual pe roluri.
- `api_smoke_tests.ps1` - teste API semi-automate cu export raport JSON+MD.
- `run_junit_docker.ps1` - rulare JUnit backend cu Java 21 in Docker.
- `sql_checks.sql` - verificari SQL de integritate/audit.
- `benchmark_video_template.md` - template benchmark performanta video.
- `reports/` - rezultate generate.

## Rulare rapida API smoke
Exemplu:

```powershell
powershell -ExecutionPolicy Bypass -File .\qa\api_smoke_tests.ps1 \
  -BaseUrl "http://localhost:8080" \
  -PlateNumber "B123ABC" \
  -PoliceUsername "police_test" -PolicePassword "Parola123!" \
  -ParkingUsername "parking_test" -ParkingPassword "Parola123!" \
  -InsuranceUsername "insurance_test" -InsurancePassword "Parola123!" \
  -PhotoPath "C:\\path\\car.jpg" \
  -VideoPath "C:\\path\\video.mp4" \
  -ParkingEntryImagePath "C:\\path\\entry.jpg" \
  -ParkingExitImagePath "C:\\path\\exit.jpg"
```

Daca fisierele media lipsesc, testele dependente de ele se marcheaza `SKIP` in raport.

## Rulare JUnit (Java 21, fara instalare locala)

```powershell
powershell -ExecutionPolicy Bypass -File .\qa\run_junit_docker.ps1 -Quiet
```

Rapoarte JUnit: `ocrbackend/target/surefire-reports`.

## Query SQL
Ruleaza `qa/sql_checks.sql` in baza PostgreSQL pentru dovezi de integritate.
