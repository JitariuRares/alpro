-- SQL checks pentru validare licenta (PostgreSQL)

-- 1) Sesiuni parking OPEN duplicate pe aceeasi placa (trebuie sa fie 0 randuri)
SELECT lp.plate_number, COUNT(*) AS open_sessions
FROM parking_history ph
JOIN license_plate lp ON lp.id = ph.plate_id
WHERE ph.status = 'OPEN'
GROUP BY lp.plate_number
HAVING COUNT(*) > 1;

-- 2) Sesiuni CLOSED fara exit_time (trebuie sa fie 0 randuri)
SELECT ph.id, lp.plate_number, ph.entry_time, ph.exit_time, ph.status
FROM parking_history ph
JOIN license_plate lp ON lp.id = ph.plate_id
WHERE ph.status = 'CLOSED' AND ph.exit_time IS NULL;

-- 3) Dovada foto lipsa pe sesiuni ENTRY/EXIT (inspectie)
SELECT ph.id, lp.plate_number, ph.status, ph.entry_image_path, ph.exit_image_path
FROM parking_history ph
JOIN license_plate lp ON lp.id = ph.plate_id
ORDER BY ph.id DESC
LIMIT 100;

-- 4) Audit log pentru lookup politie
SELECT id, actor_username, action, target_plate_number, created_at
FROM audit_logs
WHERE action = 'POLICE_LOOKUP'
ORDER BY created_at DESC
LIMIT 50;

-- 5) Audit log pentru asigurari
SELECT id, actor_username, action, target_plate_number, details, created_at
FROM audit_logs
WHERE action IN ('INSURANCE_CREATE', 'INSURANCE_UPDATE')
ORDER BY created_at DESC
LIMIT 50;

-- 6) Ultimele polite de asigurare (inspectie)
SELECT i.id, lp.plate_number, i.company, i.valid_from, i.valid_to
FROM insurance i
JOIN license_plate lp ON lp.id = i.plate_id
ORDER BY i.id DESC
LIMIT 100;