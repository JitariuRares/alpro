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
   - upload foto
   - review detectii
   - confirma o detectie
   - respinge o detectie cu motiv
4. Vehicule:
   - cauta o placuta
   - arata Vehicle Case
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

## Checklist inainte de comisie

- [ ] `localhost:3000` se incarca si meniul nou este vizibil.
- [ ] Login-ul functioneaza pentru rolurile folosite in demo.
- [ ] Dashboard-ul incarca statistici fara eroare.
- [ ] Detectiile se pot confirma si respinge cu motiv.
- [ ] Vehicle Case afiseaza date, detectii, parking, asigurari si audit.
- [ ] Exportul PDF descarca raportul.
- [ ] Auditul se filtreaza si exportul CSV descarca fisier.
- [ ] Frontend-ul servit este build-ul curent, nu unul vechi.

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

- Daca UI-ul pare neschimbat, ruleaza `docker compose up -d --build frontend` si apoi refresh hard in browser.
- Daca backend-ul da eroare dupa schimbari de model, ruleaza `docker compose logs --tail=80 backend`.
- Pentru un reset complet al containerelor, foloseste `docker compose down` doar cand vrei oprirea serviciilor; datele din volume raman, daca nu folosesti optiuni de stergere volume.
