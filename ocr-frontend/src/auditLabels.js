export const AUDIT_ACTION_OPTIONS = [
  { value: '', label: 'Toate actiunile' },
  { value: 'POLICE_LOOKUP', label: 'Cautare dosar vehicul' },
  { value: 'INSURANCE_CREATE', label: 'Polita creata' },
  { value: 'INSURANCE_UPDATE', label: 'Polita actualizata' },
  { value: 'DETECTION_CONFIRMED', label: 'Detectie confirmata' },
  { value: 'DETECTION_REJECTED', label: 'Detectie respinsa' },
  { value: 'DETECTION_REOPENED', label: 'Detectie redeschisa' },
  { value: 'COPILOT_QUERY', label: 'Intrebare catre copilot' },
  { value: 'COPILOT_DENIED', label: 'Cerere copilot refuzata' },
  { value: 'COPILOT_ERROR', label: 'Eroare copilot' },
];

export function auditActionLabel(action) {
  const knownLabel = AUDIT_ACTION_OPTIONS.find((option) => option.value === action)?.label;
  if (knownLabel) {
    return knownLabel;
  }
  if (!action) {
    return '-';
  }
  return String(action)
    .toLowerCase()
    .replace(/_/g, ' ')
    .replace(/\b\w/g, (letter) => letter.toUpperCase());
}

export function auditDetailsLabel(details) {
  if (!details) {
    return '-';
  }

  const text = String(details);
  if (text === 'Police vehicle lookup') {
    return 'Cautare dosar vehicul';
  }
  if (text.startsWith('Created insurance id=')) {
    return text.replace('Created insurance id=', 'Polita creata, id=');
  }
  if (text.startsWith('Updated insurance id=')) {
    return text.replace('Updated insurance id=', 'Polita actualizata, id=');
  }

  return text;
}
