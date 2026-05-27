import React, { useEffect, useMemo, useState } from 'react';
import { FaClipboardList, FaDownload, FaFilter, FaRedo, FaSearch } from 'react-icons/fa';
import ModuleShell from './ModuleShell';
import { API_BASE_URL } from './config';

const ACTION_OPTIONS = [
  { value: '', label: 'Toate actiunile' },
  { value: 'POLICE_LOOKUP', label: 'Police lookup' },
  { value: 'INSURANCE_CREATE', label: 'Polita creata' },
  { value: 'INSURANCE_UPDATE', label: 'Polita actualizata' },
  { value: 'DETECTION_CONFIRMED', label: 'Detectie confirmata' },
  { value: 'DETECTION_REJECTED', label: 'Detectie respinsa' },
  { value: 'DETECTION_REOPENED', label: 'Detectie redeschisa' },
];

function actionTone(action) {
  if (action === 'POLICE_LOOKUP') return 'info';
  if (action === 'INSURANCE_CREATE') return 'success';
  if (action === 'INSURANCE_UPDATE') return 'warning';
  if (action === 'DETECTION_CONFIRMED') return 'success';
  if (action === 'DETECTION_REJECTED') return 'danger';
  if (action === 'DETECTION_REOPENED') return 'warning';
  return 'info';
}

function actionLabel(action) {
  return ACTION_OPTIONS.find((option) => option.value === action)?.label || action || '-';
}

function formatDate(value) {
  if (!value) {
    return '-';
  }
  return new Date(value).toLocaleString();
}

function csvCell(value) {
  const normalized = value == null || value === '' ? '-' : String(value);
  return `"${normalized.replace(/"/g, '""')}"`;
}

function AuditPage() {
  const [logs, setLogs] = useState([]);
  const [filters, setFilters] = useState({
    actor: '',
    action: '',
    plate: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const queryString = useMemo(() => {
    const params = new URLSearchParams();
    params.set('limit', '150');
    if (filters.actor.trim()) params.set('actor', filters.actor.trim());
    if (filters.action.trim()) params.set('action', filters.action.trim());
    if (filters.plate.trim()) params.set('plate', filters.plate.trim().toUpperCase());
    return params.toString();
  }, [filters]);

  const readError = async (response) => {
    const raw = await response.text().catch(() => '');
    if (!raw) {
      return `Eroare la incarcarea auditului (status ${response.status})`;
    }

    try {
      const parsed = JSON.parse(raw);
      if (parsed?.error) {
        return parsed.error;
      }
    } catch (_) {
      // keep plain text fallback
    }
    return raw;
  };

  const loadAuditLogs = async () => {
    setLoading(true);
    setError('');

    try {
      const response = await fetch(`${API_BASE_URL}/api/audit?${queryString}`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
        },
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const data = await response.json();
      setLogs(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Nu s-a putut incarca auditul.');
      setLogs([]);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAuditLogs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const updateFilter = (name, value) => {
    setFilters((prev) => ({ ...prev, [name]: value }));
  };

  const resetFilters = () => {
    setFilters({ actor: '', action: '', plate: '' });
  };

  const exportCsv = () => {
    if (logs.length === 0) {
      return;
    }

    const header = ['Timp', 'Actor', 'Actiune', 'Placuta', 'Detalii'];
    const rows = logs.map((log) => [
      formatDate(log.createdAt),
      log.actorUsername || '-',
      actionLabel(log.action),
      log.targetPlateNumber || '-',
      log.details || '-',
    ]);
    const csv = [header, ...rows]
      .map((row) => row.map(csvCell).join(','))
      .join('\r\n');
    const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-');
    link.href = url;
    link.download = `alpro-audit-${timestamp}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  };

  return (
    <ModuleShell
      eyebrow="Trasabilitate"
      title="Audit"
      subtitle="Jurnal pentru actiuni sensibile: lookup politie, creare/modificare polite si operatii importante."
      actions={<span className="status-badge success">{logs.length} evenimente</span>}
    >
      <section className="audit-filter-panel">
        <div className="section-heading">
          <div>
            <span className="module-eyebrow">Filtre</span>
            <h2>Jurnal audit</h2>
          </div>
          <FaFilter aria-hidden="true" />
        </div>

        <div className="audit-filter-grid">
          <input
            type="text"
            className="search-input"
            placeholder="Actor"
            value={filters.actor}
            onChange={(e) => updateFilter('actor', e.target.value)}
          />
          <select
            className="search-input"
            value={filters.action}
            onChange={(e) => updateFilter('action', e.target.value)}
          >
            {ACTION_OPTIONS.map((option) => (
              <option key={option.value || 'all'} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <input
            type="text"
            className="search-input"
            placeholder="Placuta"
            value={filters.plate}
            onChange={(e) => updateFilter('plate', e.target.value)}
          />
          <div className="audit-filter-actions">
            <button type="button" className="search-btn" onClick={loadAuditLogs} disabled={loading}>
              <FaSearch aria-hidden="true" />
              {loading ? 'Se incarca...' : 'Aplica'}
            </button>
            <button type="button" className="page-btn" onClick={resetFilters}>
              <FaRedo aria-hidden="true" />
              Reset
            </button>
            <button type="button" className="download-btn" onClick={exportCsv} disabled={logs.length === 0}>
              <FaDownload aria-hidden="true" />
              Export CSV
            </button>
          </div>
        </div>
      </section>

      {error && <div className="alert alert-error">{error}</div>}

      <section className="audit-table-card">
        {logs.length > 0 ? (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Timp</th>
                  <th>Actor</th>
                  <th>Actiune</th>
                  <th>Placuta</th>
                  <th>Detalii</th>
                </tr>
              </thead>
              <tbody>
                {logs.map((log) => (
                  <tr key={log.id}>
                    <td>{formatDate(log.createdAt)}</td>
                    <td><strong>{log.actorUsername || '-'}</strong></td>
                    <td>
                      <span className={`status-badge ${actionTone(log.action)}`}>
                        {actionLabel(log.action)}
                      </span>
                    </td>
                    <td>{log.targetPlateNumber || '-'}</td>
                    <td>{log.details || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <div className="empty-state">
            <FaClipboardList className="empty-state-icon" aria-hidden="true" />
            <h2>Nu exista evenimente pentru filtrele selectate</h2>
            <p>
              Evenimentele apar aici dupa lookup-uri, creare sau actualizare de polite si alte actiuni auditate.
            </p>
          </div>
        )}
      </section>
    </ModuleShell>
  );
}

export default AuditPage;
