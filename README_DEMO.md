# ALPRo Demo Checklist

## Pornire rapida

1. Porneste aplicatia:
   ```powershell
   docker compose up -d --build
   ```
2. Deschide frontend-ul:
   ```text
   http://localhost:3000
   ```
3. Verifica serviciile:
   ```powershell
   docker compose ps
   docker compose logs --tail=50 backend
   ```

## Flux recomandat pentru prezentare

1. Login cu rol `POLICE`.
2. Dashboard: arata cardurile principale, detectiile de revizuit si auditul recent.
3. Detectii:
   - incarcare foto
   - verifica sugestia AI pentru marca/model/culoare/tip caroserie
   - foloseste `Aplica marca/model`, apoi ajusteaza manual daca este nevoie
   - incarcare video si verifica sugestia AI pe placutele unice detectate
   - review detectii
   - confirma o detectie
   - respinge o detectie cu motiv
4. Vehicule:
   - cauta o placuta
   - arata dosarul vehiculului
   - exporta PDF
5. Parcare:
   - cauta istoricul unei placute
   - arata sesiuni deschise/inchise
6. Asigurari:
   - cauta polite
   - adauga sau actualizeaza o polita, daca rolul permite
7. Audit:
   - filtreaza dupa actor, actiune sau placuta
   - exporta CSV
8. Copilot AI:
   - deschide butonul `Copilot` din dreapta-jos
   - intreaba: `dosar B123ABC`, `parcare B123ABC`, `asigurare B123ABC`
   - foloseste `Deschide` pentru deep-link direct in modulul relevant

## Checklist inainte de comisie

- [ ] `localhost:3000` se incarca si meniul nou este vizibil.
- [ ] Login-ul functioneaza pentru rolurile folosite in demo.
- [ ] Dashboard-ul incarca statistici fara eroare.
- [ ] Detectiile se pot confirma si respinge cu motiv.
- [ ] Dosarul vehiculului afiseaza date, detectii, parcare, asigurari si audit.
- [ ] Exportul PDF descarca raportul.
- [ ] Auditul se filtreaza si exportul CSV descarca fisier.
- [ ] Frontend-ul servit este build-ul curent, nu unul vechi.
- [ ] Copilot raspunde si deschide corect paginile tinta (deep-link).
- [ ] Upload-ul foto afiseaza sugestia AI pentru vehicul, daca `OPENAI_VEHICLE_ATTRIBUTES_ENABLED=true` si cheia OpenAI este setata.
- [ ] Upload-ul video afiseaza sugestii AI fara sa depaseasca limita `OPENAI_VEHICLE_ATTRIBUTES_VIDEO_MAX_CALLS_PER_JOB`.

## Comenzi de verificare

```powershell
cd ocrbackend
.\mvnw.cmd test -q
```

```powershell
cd ocr-frontend
npm test -- --watchAll=false
npm run build
```

```powershell
cd ..
docker compose up -d --build backend frontend
(Invoke-WebRequest -UseBasicParsing http://localhost:3000).Content | Select-String -Pattern 'main\.[a-f0-9]+\.js|main\.[a-f0-9]+\.css' -AllMatches | ForEach-Object { $_.Matches.Value }
```

## Note pentru demo

- AI-ul pentru atributele vehiculului foloseste aceeasi cheie ca si Copilot: `OPENAI_API_KEY_DOCKER` in Docker sau `OPENAI_API_KEY` local.
- Pentru costuri mai mici, lasa modelul `OPENAI_VEHICLE_ATTRIBUTES_MODEL=gpt-4o-mini`; pentru oprire completa seteaza `OPENAI_VEHICLE_ATTRIBUTES_ENABLED=false`.
- Pentru video, backend-ul trimite la OpenAI doar crop-uri reprezentative si se opreste dupa `OPENAI_VEHICLE_ATTRIBUTES_VIDEO_MAX_CALLS_PER_JOB` apeluri per job.
- Daca UI-ul pare neschimbat, ruleaza `docker compose up -d --build frontend` si apoi refresh hard in browser.
- Daca backend-ul da eroare dupa schimbari de model, ruleaza `docker compose logs --tail=80 backend`.
- Pentru un reset complet al containerelor, foloseste `docker compose down` doar cand vrei oprirea serviciilor; datele din volume raman, daca nu folosesti optiuni de stergere volume.

## Reguli placute romanesti

Aplicatia normalizeaza textul OCR prin eliminarea spatiilor si conversia la litere mari. Backend-ul valideaza codurile de judet cunoscute si clasifica placutele in ordinea:

1. `DIPLOMATIC` - `CD 123 101`, `TC 156 234`, `CO 205 113`
2. `MAI` - `MAI 12345`
3. `MILITARY` - `A 123456`
4. `PROBE` - `CJ 101 PROBE`, `B 234 PROBE`
5. `TEMPORARY` - `CJ 012345`, `B 012345`
6. `STANDARD` - `CJ 01 ABC`, `CJ 123 ABC`, `B 12 XYZ`, `B 123 XYZ`
7. `LOCAL` - suport conservator pentru exemple locale precum `CJ-N 1234`

Textele care nu respecta aceste reguli sunt tratate ca `UNKNOWN` si nu sunt salvate ca detectii valide.
