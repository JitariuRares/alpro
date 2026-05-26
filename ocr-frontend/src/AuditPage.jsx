import React from 'react';
import { FaClipboardList } from 'react-icons/fa';
import ModuleShell from './ModuleShell';

function AuditPage() {
  return (
    <ModuleShell
      eyebrow="Trasabilitate"
      title="Audit"
      subtitle="Zona pentru actiuni sensibile: cautari politie, modificari polite si operatii importante."
      actions={<span className="status-badge info">In pregatire</span>}
    >
      <div className="empty-state">
        <FaClipboardList className="empty-state-icon" aria-hidden="true" />
        <h2>Jurnal audit</h2>
        <p>
          Backendul salveaza deja evenimente de audit pentru lookup-uri si polite.
          Urmatorul pas este endpointul de listare si filtrele pe actor, actiune si placuta.
        </p>
      </div>
    </ModuleShell>
  );
}

export default AuditPage;
