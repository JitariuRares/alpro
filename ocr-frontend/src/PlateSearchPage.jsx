import React, { useCallback, useEffect, useState } from 'react';
import {
  FaCamera,
  FaCarSide,
  FaClipboardList,
  FaDownload,
  FaParking,
  FaSearch,
  FaShieldAlt,
  FaVideo,
} from 'react-icons/fa';
import { useLocation } from 'react-router-dom';
import { API_BASE_URL } from './config';
import { readApiError, friendlyErrorMessage } from './errorMessages';
import { auditActionLabel, auditDetailsLabel } from './auditLabels';

const normalizePlate = (value) => (value || '').trim().toUpperCase();

function CaseMetric({ label, value, tone = 'info' }) {
  return (
    <div className="case-metric">
      <span className={`status-badge ${tone}`}>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function CaseSection({ icon: Icon, title, count, children }) {
  return (
    <section className="case-section">
      <div className="case-section-heading">
        <div>
          <Icon aria-hidden="true" />
          <h3>{title}</h3>
        </div>
        {count != null && <span className="status-badge info">{count}</span>}
      </div>
      {children}
    </section>
  );
}

function EmptySection({ children }) {
  return <p className="case-empty">{children}</p>;
}

function PlateSearchPage() {
  const location = useLocation();
  const [query, setQuery] = useState('');
  const [lookup, setLookup] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const fetchLookup = useCallback(async (plateNumber) => {
    const normalizedPlate = normalizePlate(plateNumber);
    if (!normalizedPlate) {
      setError('Introdu o placuta valida (ex: SV15WDC)');
      setLookup(null);
      return;
    }

    setLoading(true);
    setError('');
    setLookup(null);

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/police/lookup/${encodeURIComponent(normalizedPlate)}`,
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Nu s-au putut incarca datele vehiculului.'));
      }

      const data = await response.json();
      setLookup(data);
      setQuery(normalizedPlate);
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Nu s-au putut incarca datele vehiculului.'));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    const plateFromQuery = normalizePlate(params.get('plate'));
    if (plateFromQuery) {
      setQuery(plateFromQuery);
      fetchLookup(plateFromQuery);
    }
  }, [fetchLookup, location.search]);

  const handleSearch = async () => {
    await fetchLookup(query);
  };

  const onKeyDown = (event) => {
    if (event.key === 'Enter') {
      handleSearch();
    }
  };

  const handleDownloadPdf = async () => {
    if (!lookup?.plateNumber) {
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/license-plates/pdf/${encodeURIComponent(lookup.plateNumber)}`,
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Nu s-a putut descarca PDF-ul.'));
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `plate_${lookup.plateNumber}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Nu s-a putut descarca PDF-ul.'));
    }
  };

  const hasAnyLookupData =
    !!lookup?.plate ||
    (lookup?.insurances?.length ?? 0) > 0 ||
    (lookup?.parkingHistory?.length ?? 0) > 0 ||
    (lookup?.recentOcrDetections?.length ?? 0) > 0 ||
    (lookup?.recentVideoDetections?.length ?? 0) > 0 ||
    (lookup?.auditEvents?.length ?? 0) > 0;

  const formatDateTime = (value) => (value ? new Date(value).toLocaleString('ro-RO') : '-');

  const formatDate = (value) => (value ? new Date(value).toLocaleDateString('ro-RO') : '-');

  const formatVideoTimestamp = (timestampMs) => {
    if (!Number.isFinite(timestampMs) || timestampMs < 0) {
      return '-';
    }
    const totalSeconds = Math.floor(timestampMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  };

  const plate = lookup?.plate;
  const openParkingCount = lookup?.parkingHistory?.filter((parking) => parking.status === 'OPEN').length || 0;
  const detectionCount = (lookup?.recentOcrDetections?.length || 0) + (lookup?.recentVideoDetections?.length || 0);
  const today = new Date();
  const validInsurances = lookup?.insurances?.filter((insurance) => {
    if (!insurance.validFrom || !insurance.validTo) {
      return false;
    }
    return new Date(insurance.validFrom) <= today && today <= new Date(insurance.validTo);
  }) || [];
  const insuranceStatus = validInsurances.length > 0 ? 'Activa' : 'Lipsa/expirata';

  const reviewTone = (status) => {
    if (status === 'CONFIRMED') return 'success';
    if (status === 'REJECTED') return 'danger';
    return 'warning';
  };

  const reviewLabel = (status) => ({
    CONFIRMED: 'Confirmat',
    REJECTED: 'Respins',
    DE_REVIEW: 'De revizuit',
  }[status] || 'De revizuit');

  return (
    <div className="vehicle-case-page">
      <div className="case-search-panel">
        <div>
          <span className="module-eyebrow">Vehicle Case</span>
          <h2>Cauta dosar vehicul</h2>
        </div>
        <div className="case-search-controls">
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={onKeyDown}
            className="search-input"
            placeholder="Ex: SV15WDC"
          />
          <button onClick={handleSearch} disabled={loading} className="search-btn">
            <FaSearch aria-hidden="true" />
            {loading ? 'Se cauta...' : 'Cauta'}
          </button>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {!loading && lookup && (
        <div className="vehicle-case-layout">
          <section className="case-summary-card">
            <div className="case-plate-mark">
              <FaCarSide aria-hidden="true" />
              <strong>{lookup.plateNumber}</strong>
            </div>

            {plate ? (
              <>
                <div className="case-title-row">
                  <div>
                    <span className="status-badge success">Inregistrat</span>
                    <h2>{plate.brand || 'Marca necunoscuta'} {plate.model || ''}</h2>
                    <p>{plate.owner || 'Proprietar necompletat'}</p>
                  </div>
                  <button onClick={handleDownloadPdf} className="download-btn case-pdf-btn">
                    <FaDownload aria-hidden="true" />
                    Export raport PDF
                  </button>
                </div>

                <div className="case-metrics-grid">
                  <CaseMetric
                    label="Ultima detectie"
                    value={formatDate(plate.detectedAt)}
                    tone="warning"
                  />
                  <CaseMetric label="Asigurare" value={insuranceStatus} tone={validInsurances.length > 0 ? 'success' : 'danger'} />
                  <CaseMetric label="Parking deschis" value={openParkingCount} tone={openParkingCount > 0 ? 'warning' : 'info'} />
                  <CaseMetric label="Detectii recente" value={detectionCount} tone="info" />
                  <CaseMetric label="Audit" value={lookup.auditEvents?.length || 0} tone="warning" />
                </div>
              </>
            ) : (
              <div className="alert alert-error">
                Placuta nu exista in tabelul principal de vehicule.
              </div>
            )}
          </section>

          <div className="case-sections-grid">
            <CaseSection icon={FaShieldAlt} title="Asigurari" count={lookup.insurances?.length || 0}>
              {lookup.insurances?.length > 0 ? (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Companie</th>
                        <th>Status</th>
                        <th>Valabil de la</th>
                        <th>Valabil pana la</th>
                      </tr>
                    </thead>
                    <tbody>
                      {lookup.insurances.map((insurance) => (
                        <tr key={insurance.id}>
                          <td>{insurance.company}</td>
                          <td>
                            <span className={`status-badge ${
                              insurance.validFrom && insurance.validTo && new Date(insurance.validFrom) <= today && today <= new Date(insurance.validTo)
                                ? 'success'
                                : 'danger'
                            }`}>
                              {insurance.validFrom && insurance.validTo && new Date(insurance.validFrom) <= today && today <= new Date(insurance.validTo)
                                ? 'Activa'
                                : 'Expirata'}
                            </span>
                          </td>
                          <td>{insurance.validFrom || '-'}</td>
                          <td>{insurance.validTo || '-'}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <EmptySection>Nu exista asigurari pentru aceasta placuta.</EmptySection>
              )}
            </CaseSection>

            <CaseSection icon={FaParking} title="Istoric parcare" count={lookup.parkingHistory?.length || 0}>
              {lookup.parkingHistory?.length > 0 ? (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Status</th>
                        <th>Zona</th>
                        <th>Intrare</th>
                        <th>Iesire</th>
                        <th>Durata</th>
                        <th>Dovezi</th>
                      </tr>
                    </thead>
                    <tbody>
                      {lookup.parkingHistory.map((parking) => (
                        <tr key={parking.id}>
                          <td>
                            <span className={`status-badge ${parking.status === 'OPEN' ? 'warning' : 'success'}`}>
                              {parking.status || '-'}
                            </span>
                          </td>
                          <td>{parking.parkingZone || 'Nespecificata'}</td>
                          <td>{formatDateTime(parking.entryTime)}</td>
                          <td>{parking.exitTime ? formatDateTime(parking.exitTime) : 'N/A'}</td>
                          <td>
                            {Number.isFinite(parking.durationMinutes)
                              ? `${Math.floor(parking.durationMinutes / 60)}h ${parking.durationMinutes % 60}m`
                              : '-'}
                          </td>
                          <td>
                            ENTRY: {parking.hasEntryImage ? 'da' : 'nu'} / EXIT: {parking.hasExitImage ? 'da' : 'nu'}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <EmptySection>Nu exista istoric parking pentru aceasta placuta.</EmptySection>
              )}
            </CaseSection>

            <CaseSection icon={FaCamera} title="Detectii foto recente" count={lookup.recentOcrDetections?.length || 0}>
              {lookup.recentOcrDetections?.length > 0 ? (
                <div className="timeline-list">
                  {lookup.recentOcrDetections.map((detection) => (
                    <div className="timeline-item" key={detection.id}>
                      <div>
                        <strong>{detection.processedAt ? new Date(detection.processedAt).toLocaleString() : '-'}</strong>
                        <small>
                          <span className={`status-badge ${reviewTone(detection.reviewStatus)}`}>
                            {reviewLabel(detection.reviewStatus)}
                          </span>
                        </small>
                        {detection.reviewReason && <small>Motiv: {detection.reviewReason}</small>}
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptySection>Nu exista detectii foto recente pentru aceasta placuta.</EmptySection>
              )}
            </CaseSection>

            <CaseSection icon={FaVideo} title="Detectii video recente" count={lookup.recentVideoDetections?.length || 0}>
              {lookup.recentVideoDetections?.length > 0 ? (
                <div className="table-container">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>Frame</th>
                        <th>Timp video</th>
                        <th>Track</th>
                        <th>Review</th>
                      </tr>
                    </thead>
                    <tbody>
                      {lookup.recentVideoDetections.map((detection) => (
                        <tr key={detection.id}>
                          <td>{detection.frameIndex ?? '-'}</td>
                          <td>{formatVideoTimestamp(detection.timestampMs)}</td>
                          <td>{detection.trackId ?? '-'}</td>
                          <td>
                            <span className={`status-badge ${reviewTone(detection.reviewStatus)}`}>
                              {reviewLabel(detection.reviewStatus)}
                            </span>
                            {detection.reviewReason && <small className="case-row-note">{detection.reviewReason}</small>}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              ) : (
                <EmptySection>Nu exista detectii video recente pentru aceasta placuta.</EmptySection>
              )}
            </CaseSection>

            <CaseSection icon={FaClipboardList} title="Audit relevant" count={lookup.auditEvents?.length || 0}>
              {lookup.auditEvents?.length > 0 ? (
                <div className="timeline-list">
                  {lookup.auditEvents.map((event) => (
                    <div className="timeline-item audit-event-item" key={event.id}>
                      <span className="status-badge warning">{auditActionLabel(event.action)}</span>
                      <div>
                        <strong>{event.actorUsername || '-'}</strong>
                        <small>{formatDateTime(event.createdAt)}</small>
                        <small>{auditDetailsLabel(event.details)}</small>
                      </div>
                    </div>
                  ))}
                </div>
              ) : (
                <EmptySection>Nu exista evenimente de audit pentru aceasta placuta.</EmptySection>
              )}
            </CaseSection>
          </div>
        </div>
      )}

      {!loading && !error && lookup && !hasAnyLookupData && (
        <div className="empty-state">Nu exista date pentru placuta cautata.</div>
      )}
    </div>
  );
}

export default PlateSearchPage;
