import React, { useEffect, useMemo, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { FaCamera, FaCheck, FaCheckCircle, FaFilter, FaHistory, FaSearch, FaSyncAlt, FaTimes, FaVideo } from 'react-icons/fa';
import ModuleShell from './ModuleShell';
import UploadPage from './UploadPage';
import VideoAlprPage from './VideoAlprPage';
import { API_BASE_URL } from './config';
import { readApiError, friendlyErrorMessage } from './errorMessages';

const TABS = [
  { id: 'foto', label: 'Foto', icon: FaCamera },
  { id: 'video', label: 'Video', icon: FaVideo },
  { id: 'review', label: 'Review', icon: FaCheckCircle },
];

const REVIEW_STATUSES = [
  { value: 'DE_REVIEW', label: 'De revizuit' },
  { value: 'CONFIRMED', label: 'Confirmate' },
  { value: 'REJECTED', label: 'Respinse' },
  { value: 'ALL', label: 'Toate' },
];

const SOURCE_FILTERS = [
  { value: 'all', label: 'Foto + video' },
  { value: 'foto', label: 'Foto' },
  { value: 'video', label: 'Video' },
];

function DetectiiReviewPanel() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [statusFilter, setStatusFilter] = useState('DE_REVIEW');
  const [sourceFilter, setSourceFilter] = useState('all');
  const [plateFilter, setPlateFilter] = useState('');
  const [rejectingKey, setRejectingKey] = useState('');
  const [reviewReason, setReviewReason] = useState('');

  const authHeader = useMemo(() => ({
    Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
  }), []);

  const loadReviewQueue = async () => {
    setLoading(true);
    setError('');

    try {
      const fetchByStatus = async (status) => {
        const response = await fetch(`${API_BASE_URL}/api/detection-review?status=${status}&limit=100`, {
          headers: authHeader,
        });

        if (!response.ok) {
          throw new Error(await readApiError(response, 'Nu s-a putut incarca lista de review.'));
        }

        const data = await response.json();
        return Array.isArray(data) ? data : [];
      };

      const data = statusFilter === 'ALL'
        ? (await Promise.all(['DE_REVIEW', 'CONFIRMED', 'REJECTED'].map(fetchByStatus))).flat()
        : await fetchByStatus(statusFilter);

      setItems(data);
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Nu s-a putut incarca lista de review.'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadReviewQueue();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [statusFilter]);

  const statusFor = (item) => item.reviewStatus || 'DE_REVIEW';
  const reviewItemKey = (item) => `${item.sourceType}-${item.sourceId}-${item.plateNumber || ''}`;
  const visibleItems = useMemo(() => {
    const normalizedPlateFilter = plateFilter.trim().toUpperCase();

    return items.filter((item) => {
      const sourceMatches = sourceFilter === 'all' || item.sourceType === sourceFilter;
      const plateMatches = !normalizedPlateFilter || (item.plateNumber || '').toUpperCase().includes(normalizedPlateFilter);
      return sourceMatches && plateMatches;
    });
  }, [items, plateFilter, sourceFilter]);

  const pendingCount = visibleItems.filter((item) => statusFor(item) === 'DE_REVIEW').length;
  const confirmedCount = visibleItems.filter((item) => statusFor(item) === 'CONFIRMED').length;
  const rejectedCount = visibleItems.filter((item) => statusFor(item) === 'REJECTED').length;

  const setItemStatus = async (item, status, reason = '') => {
    setError('');

    try {
      const normalizedReason = reason.trim();
      if (status === 'REJECTED' && !normalizedReason) {
        setError('Motivul respingerii este obligatoriu.');
        return;
      }

      const response = await fetch(`${API_BASE_URL}/api/detection-review/${item.sourceType}/${item.sourceId}`, {
        method: 'PATCH',
        headers: {
          ...authHeader,
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ status, plateNumber: item.plateNumber, reviewReason: normalizedReason }),
      });

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Nu s-a putut actualiza statusul detectiei.'));
      }

      const updated = await response.json();
      setItems((prev) => {
        if (statusFilter !== 'ALL' && status !== statusFilter) {
          return prev.filter((candidate) => reviewItemKey(candidate) !== reviewItemKey(updated));
        }

        return prev.map((candidate) => (
          reviewItemKey(candidate) === reviewItemKey(updated)
            ? updated
            : candidate
        ));
      });
      setRejectingKey('');
      setReviewReason('');
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Nu s-a putut actualiza statusul detectiei.'));
    }
  };

  const startReject = (item) => {
    setRejectingKey(reviewItemKey(item));
    setReviewReason(item.reviewReason || '');
    setError('');
  };

  const cancelReject = () => {
    setRejectingKey('');
    setReviewReason('');
  };

  const openVehicleCase = (plate) => {
    if (!plate) {
      return;
    }
    navigate(`/vehicule?tab=cautare&plate=${encodeURIComponent(plate)}`);
  };

  return (
    <div className="review-workspace">
      <section className="review-summary-grid">
        <div className="review-metric-card">
          <span className="review-metric-icon warning"><FaHistory /></span>
          <small>De revizuit</small>
          <strong>{pendingCount}</strong>
        </div>
        <div className="review-metric-card">
          <span className="review-metric-icon success"><FaCheck /></span>
          <small>Confirmate local</small>
          <strong>{confirmedCount}</strong>
        </div>
        <div className="review-metric-card">
          <span className="review-metric-icon danger"><FaTimes /></span>
          <small>Respinse local</small>
          <strong>{rejectedCount}</strong>
        </div>
        <button type="button" className="review-refresh-btn" onClick={loadReviewQueue} disabled={loading}>
          <FaSyncAlt /> {loading ? 'Se incarca' : 'Reincarca'}
        </button>
      </section>

      {error && <div className="alert alert-error"><strong>Eroare:</strong> {error}</div>}

      <section className="review-table-card">
        <div className="review-filter-bar">
          <div className="review-filter-title">
            <FaFilter aria-hidden="true" />
            <strong>Filtre review</strong>
          </div>
          <select value={statusFilter} onChange={(event) => setStatusFilter(event.target.value)}>
            {REVIEW_STATUSES.map((status) => (
              <option key={status.value} value={status.value}>{status.label}</option>
            ))}
          </select>
          <select value={sourceFilter} onChange={(event) => setSourceFilter(event.target.value)}>
            {SOURCE_FILTERS.map((source) => (
              <option key={source.value} value={source.value}>{source.label}</option>
            ))}
          </select>
          <input
            type="text"
            value={plateFilter}
            onChange={(event) => setPlateFilter(event.target.value)}
            className="search-input"
            placeholder="Filtreaza placuta"
          />
        </div>

        {visibleItems.length === 0 && !loading ? (
          <div className="empty-state compact">
            <span className="status-badge success">Curat</span>
            <h2>Nu exista rezultate pentru filtrele selectate</h2>
            <p>Schimba statusul, sursa sau cautarea dupa placuta.</p>
          </div>
        ) : (
          <div className="review-list">
            {visibleItems.map((item) => {
              const status = statusFor(item);
              const itemKey = reviewItemKey(item);
              const isRejecting = rejectingKey === itemKey;
              return (
                <article className={`review-item ${status === 'CONFIRMED' ? 'confirmed' : status === 'REJECTED' ? 'rejected' : 'review'}`} key={itemKey}>
                  <div className="review-item-main">
                    <span className={`status-badge ${status === 'CONFIRMED' ? 'success' : status === 'REJECTED' ? 'danger' : 'warning'}`}>
                      {status === 'CONFIRMED' ? 'CONFIRMAT' : status === 'REJECTED' ? 'RESPINS' : 'DE REVIZUIT'}
                    </span>
                    <strong>{item.plateNumber || 'FARA TEXT'}</strong>
                    <small>
                      {item.sourceLabel} - {item.detectedAt ? new Date(item.detectedAt).toLocaleString('ro-RO') : '-'}
                      {item.occurrenceCount > 1 ? ` - ${item.occurrenceCount} aparitii` : ''}
                    </small>
                  </div>
                  <div className="review-item-detail">
                    <span>{item.detail}</span>
                    {item.reviewedBy && (
                      <span>Revizuit de {item.reviewedBy}</span>
                    )}
                    {item.reviewReason && (
                      <span className="review-reason">Motiv: {item.reviewReason}</span>
                    )}
                    <span className={`review-source-pill ${item.sourceType}`}>{item.sourceType === 'video' ? 'VIDEO' : 'FOTO'}</span>
                  </div>
                  <div className="review-item-actions">
                    <button type="button" className="page-btn success-action" onClick={() => setItemStatus(item, 'CONFIRMED')}>
                      <FaCheck /> Confirma
                    </button>
                    <button type="button" className="page-btn danger-action" onClick={() => startReject(item)}>
                      <FaTimes /> Respinge
                    </button>
                    <button type="button" className="page-btn" onClick={() => openVehicleCase(item.plateNumber)}>
                      <FaSearch /> Vehicul
                    </button>
                  </div>
                  {isRejecting && (
                    <div className="review-reject-panel">
                      <textarea
                        value={reviewReason}
                        onChange={(event) => setReviewReason(event.target.value)}
                        maxLength={500}
                        placeholder="Motiv respingere"
                      />
                      <div className="review-reject-actions">
                        <small>{reviewReason.trim().length}/500</small>
                        <button type="button" className="page-btn" onClick={cancelReject}>
                          Renunta
                        </button>
                        <button type="button" className="page-btn danger-action" onClick={() => setItemStatus(item, 'REJECTED', reviewReason)}>
                          <FaTimes /> Respinge
                        </button>
                      </div>
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>
    </div>
  );
}

function DetectiiPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') || 'foto';

  const changeTab = (tab) => {
    setSearchParams({ tab });
  };

  return (
    <ModuleShell
      title="Detectii"
      subtitle="Incarcare foto, procesare video si zona de verificare pentru rezultatele automate ML-OCR."
      tabs={TABS}
      activeTab={activeTab}
      onTabChange={changeTab}
    >
      {activeTab === 'video' ? <VideoAlprPage /> : activeTab === 'review' ? <DetectiiReviewPanel /> : <UploadPage />}
    </ModuleShell>
  );
}

export default DetectiiPage;
