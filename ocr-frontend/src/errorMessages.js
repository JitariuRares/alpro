export function friendlyErrorMessage(message, fallback = 'Operatia nu a putut fi finalizata.') {
  if (!message) {
    return fallback;
  }

  const text = String(message);
  const lower = text.toLowerCase();

  if (lower.includes('access denied') || lower.includes('403')) {
    return 'Nu ai permisiunea necesara pentru aceasta actiune.';
  }
  if (lower.includes('failed to fetch') || lower.includes('networkerror') || lower.includes('network')) {
    return 'Aplicatia nu poate contacta serverul. Verifica daca backend-ul este pornit.';
  }
  if (lower.includes('jwt') || lower.includes('token') || lower.includes('unauthorized') || lower.includes('401')) {
    return 'Sesiunea a expirat sau nu esti autentificat. Intra din nou in cont.';
  }
  if (lower.includes('could not write json') || lower.includes('failed to lazily initialize') || lower.includes('could not initialize proxy')) {
    return 'Serverul a intors un raspuns incomplet. Reincarca pagina si incearca din nou.';
  }
  if (lower.includes('500') || lower.includes('internal server')) {
    return 'A aparut o eroare interna pe server. Verifica logurile backend.';
  }

  return text;
}

export async function readApiError(response, fallback = 'Operatia nu a putut fi finalizata.') {
  const raw = await response.text().catch(() => '');
  if (!raw) {
    return friendlyErrorMessage(`Status ${response.status}`, fallback);
  }

  try {
    const parsed = JSON.parse(raw);
    return friendlyErrorMessage(parsed?.error || parsed?.detail || raw, fallback);
  } catch (_) {
    return friendlyErrorMessage(raw, fallback);
  }
}
