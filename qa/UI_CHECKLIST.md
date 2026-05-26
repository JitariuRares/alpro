# UI Checklist - Demo Comisie

## POLICE
1. Login ca `POLICE`.
2. Verifica meniu: `Detectare foto`, `Detectare video`, `Cautare date vehicul`, `Dashboard`.
3. Upload foto -> confirma placa + bbox + confidence.
4. Click `Cauta date vehicul` -> pagina lookup populata automat.
5. Upload video -> dupa completare job verifica lista detectii + buton lookup per placa.
6. Verifica ca nu exista acces UI la editare asigurari.

## PARKING
1. Login ca `PARKING`.
2. Verifica meniu: `Adauga parcare`, `Cauta sesiuni parking`, `Dashboard`.
3. Inregistrare ENTRY cu placa + foto.
4. Inregistrare EXIT cu aceeasi placa + foto.
5. In `Cauta sesiuni parking` verifica: status, entry/exit, durata, buton `Vezi foto ENTRY/EXIT`.
6. Verifica lipsa acces la pagini politie/asigurator.

## INSURANCE
1. Login ca `INSURANCE`.
2. Verifica meniu: `Cauta asigurare`, `Adauga asigurare`, `Dashboard`.
3. Cauta polite existente dupa placa.
4. Adauga polita noua.
5. Editeaza polita existenta.
6. Verifica lipsa acces la parking/video/police lookup.

## Evidence
1. Captura ecran pentru fiecare pas important.
2. Salveaza capturile in `qa/reports/screenshots/`.
3. Denumire recomandata: `ROL_pas_timestamp.png`.