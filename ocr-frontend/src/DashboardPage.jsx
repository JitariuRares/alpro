import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  FaArrowRight,
  FaCarSide,
  FaClipboardList,
  FaExclamationTriangle,
  FaHistory,
  FaParking,
  FaShieldAlt,
  FaVideo,
} from 'react-icons/fa';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { API_BASE_URL } from './config';
import { ROLE_INSURANCE, ROLE_PARKING, ROLE_POLICE, normalizeRole } from './authRouting';
import { readApiError, friendlyErrorMessage } from './errorMessages';
import { auditActionLabel } from './auditLabels';

function DashboardPage() {
  const navigate = useNavigate();
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');
  const role = normalizeRole(localStorage.getItem('role'));
  const canUsePoliceTools = role === ROLE_POLICE;
  const canUseParking = [ROLE_PARKING, ROLE_POLICE].includes(role);
  const canUseInsurance = [ROLE_INSURANCE, ROLE_POLICE].includes(role);
  const canManageInsurance = role === ROLE_INSURANCE;
  const parkingRoute = role === ROLE_PARKING ? '/parcare?tab=operare' : '/parcare?tab=sesiuni';

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/dashboard/stats`, {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token')}`,
          },
        });

        if (!res.ok) {
          throw new Error(await readApiError(res, 'Nu s-au putut incarca statisticile.'));
        }

        const data = await res.json();

        const countiesArray = Object.entries(data.topCountiesLast7Days || {}).map(
          ([county, count]) => ({ county, count })
        );

        setStats({
          totalPlates: data.totalPlates,
          totalInsurances: data.totalInsurances,
          totalParkings: data.totalParkings,
          activeInsurances: data.activeInsurances || 0,
          expiredInsurances: data.expiredInsurances || 0,
          expiringInsurancesNext30Days: data.expiringInsurancesNext30Days || 0,
          openParkings: data.openParkings || 0,
          pendingDetectionReviews: data.pendingDetectionReviews || 0,
          runningVideoJobs: data.runningVideoJobs || 0,
          topCounties: countiesArray,
          recentOpenParkings: data.recentOpenParkings || [],
          recentVideoJobs: data.recentVideoJobs || [],
          recentAuditEvents: data.recentAuditEvents || [],
        });
      } catch (err) {
        setError(friendlyErrorMessage(err.message, 'Nu s-au putut incarca statisticile.'));
      }
    };

    fetchStats();
  }, []);

  const formatDateTime = (value) => (value ? new Date(value).toLocaleString('ro-RO') : '-');

  const videoStatusLabel = (status) => ({
    COMPLETED: 'Finalizat',
    RUNNING: 'In desfasurare',
    PENDING: 'In asteptare',
    FAILED: 'Esuat',
  }[status] || status || '-');

  const roleLabel = ({
    [ROLE_POLICE]: 'POLICE',
    [ROLE_PARKING]: 'PARKING',
    [ROLE_INSURANCE]: 'INSURANCE',
  }[role] || 'USER');

  const openVehicle = (plate) => {
    if (canUsePoliceTools && plate) {
      navigate(`/vehicule?tab=cautare&plate=${encodeURIComponent(plate)}`);
    }
  };

  return (
    <div className="dashboard-page">
      <section className="module-hero dashboard-hero">
        <div>
          <h1>Dashboard</h1>
          <p className="module-subtitle">
            Privire rapida peste vehicule, asigurari, sesiuni de parcare si distributia detectiilor recente.
          </p>
        </div>
        <div className="dashboard-hero-status">
          <span className="status-badge success">Online</span>
          <span className="status-badge info">{roleLabel}</span>
        </div>
      </section>

      {error && <div className="alert alert-error">{error}</div>}

      {stats && (
        <>
          <section className="dashboard-stats-grid">
            {canUsePoliceTools && (
              <button type="button" className="stat-card clickable" onClick={() => navigate('/vehicule')}>
                <div className="stat-icon blue"><FaCarSide aria-hidden="true" /></div>
                <span>Vehicule</span>
                <strong>{stats.totalPlates}</strong>
              </button>
            )}
            {canUseInsurance && (
              <button type="button" className="stat-card clickable" onClick={() => navigate('/asigurari')}>
                <div className="stat-icon green"><FaShieldAlt aria-hidden="true" /></div>
                <span>Polite active</span>
                <strong>{stats.activeInsurances}</strong>
                <small>{stats.expiringInsurancesNext30Days} expira in 30 zile</small>
              </button>
            )}
            {canUseParking && (
              <button type="button" className="stat-card clickable" onClick={() => navigate(parkingRoute)}>
                <div className="stat-icon orange"><FaParking aria-hidden="true" /></div>
                <span>Sesiuni parcare</span>
                <strong>{stats.openParkings}</strong>
                <small>{stats.totalParkings} sesiuni total</small>
              </button>
            )}
            {canUsePoliceTools && (
              <button type="button" className="stat-card clickable urgent" onClick={() => navigate('/detectii?tab=review')}>
                <div className="stat-icon red"><FaExclamationTriangle aria-hidden="true" /></div>
                <span>Detectii de revizuit</span>
                <strong>{stats.pendingDetectionReviews}</strong>
                <small>Foto + video brute</small>
              </button>
            )}
            {canUsePoliceTools && (
              <button type="button" className="stat-card clickable" onClick={() => navigate('/detectii?tab=video')}>
                <div className="stat-icon violet"><FaVideo aria-hidden="true" /></div>
                <span>Procesari video</span>
                <strong>{stats.runningVideoJobs}</strong>
                <small>In asteptare sau in lucru</small>
              </button>
            )}
            {canUsePoliceTools && (
              <button type="button" className="stat-card clickable" onClick={() => navigate('/audit')}>
                <div className="stat-icon dark"><FaClipboardList aria-hidden="true" /></div>
                <span>Audit recent</span>
                <strong>{stats.recentAuditEvents.length}</strong>
                <small>Ultimele evenimente</small>
              </button>
            )}
          </section>

          <section className="dashboard-flow-row">
            {canUsePoliceTools && (
              <button type="button" onClick={() => navigate('/detectii?tab=review')}>
                <span className="status-badge warning">1</span>
                <strong>Review detectii</strong>
                <small>Valideaza rezultatele automate.</small>
              </button>
            )}
            {canUsePoliceTools && (
              <button type="button" onClick={() => navigate('/vehicule')}>
                <span className="status-badge info">2</span>
                <strong>Dosar vehicul</strong>
                <small>Verifica dosarul unui vehicul.</small>
              </button>
            )}
            {canUseParking && (
              <button type="button" onClick={() => navigate(parkingRoute)}>
                <span className="status-badge success">3</span>
                <strong>Parcare</strong>
                <small>{role === ROLE_PARKING ? 'Controleaza intrari si iesiri.' : 'Consulta sesiuni dupa placuta.'}</small>
              </button>
            )}
            {canUsePoliceTools && (
              <button type="button" onClick={() => navigate('/audit')}>
                <span className="status-badge danger">4</span>
                <strong>Audit</strong>
                <small>Vezi cine a facut modificari si cand.</small>
              </button>
            )}
          </section>

          <section className="dashboard-ops-grid">
            <div className="dashboard-panel">
              <div className="section-heading">
                <div>
                  <span className="module-eyebrow">Actiuni rapide</span>
                  <h2>De verificat</h2>
                </div>
                <FaArrowRight aria-hidden="true" />
              </div>
              <div className="ops-list">
                {canUsePoliceTools && (
                  <button type="button" onClick={() => navigate('/detectii?tab=review')}>
                    <span className="status-badge warning">{stats.pendingDetectionReviews}</span>
                    <strong>Detectii asteapta review</strong>
                    <small>Confirma sau respinge cazurile generate de ML-OCR.</small>
                  </button>
                )}
                {canUseInsurance && (
                  <button type="button" onClick={() => navigate(`/asigurari${canManageInsurance ? '?tab=gestiune' : ''}`)}>
                    <span className="status-badge danger">{stats.expiredInsurances}</span>
                    <strong>Polite expirate</strong>
                    <small>Verifica politele depasite sau lipsa pentru vehicule.</small>
                  </button>
                )}
                {canUseParking && (
                  <button type="button" onClick={() => navigate(parkingRoute)}>
                    <span className="status-badge info">{stats.openParkings}</span>
                    <strong>Sesiuni parcare</strong>
                    <small>{role === ROLE_PARKING ? 'Urmeaza intrarile fara iesire inregistrata.' : 'Verifica istoricul deja inregistrat.'}</small>
                  </button>
                )}
              </div>
            </div>

            {canUseParking && (
            <div className="dashboard-panel">
              <div className="section-heading">
                <div>
                  <span className="module-eyebrow">Parcare</span>
                  <h2>Sesiuni active</h2>
                </div>
                <span className="status-badge info">{stats.recentOpenParkings.length}</span>
              </div>
              {stats.recentOpenParkings.length > 0 ? (
                <div className="compact-list">
                  {stats.recentOpenParkings.map((parking) => (
                    <button type="button" key={parking.id} onClick={() => openVehicle(parking.plateNumber)}>
                      <strong>{parking.plateNumber || '-'}</strong>
                      <small>{parking.parkingZone || 'Zona nespecificata'} - {formatDateTime(parking.entryTime)}</small>
                    </button>
                  ))}
                </div>
              ) : (
                <p className="case-empty">Nu exista parcari deschise.</p>
              )}
            </div>
            )}
          </section>

          <section className="dashboard-bottom-grid">
            <div className="dashboard-panel dashboard-chart-card">
              <div className="section-heading">
                <div>
                  <span className="module-eyebrow">Ultimele 7 zile</span>
                  <h2>Top judete detectate</h2>
                </div>
                <span className="status-badge info">{stats.topCounties.length} active</span>
              </div>
              {stats.topCounties.length > 0 ? (
                <ResponsiveContainer width="100%" height={300}>
                  <BarChart
                    data={stats.topCounties}
                    margin={{ top: 10, right: 30, left: 0, bottom: 5 }}
                  >
                    <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                    <XAxis dataKey="county" />
                    <YAxis allowDecimals={false} />
                    <Tooltip />
                    <Bar dataKey="count" fill="#2563eb" radius={[8, 8, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <p className="case-empty">Nu exista detectii recente pentru chart.</p>
              )}
            </div>

            {canUsePoliceTools && (
            <div className="dashboard-panel">
              <div className="section-heading">
                <div>
                  <span className="module-eyebrow">Procesare</span>
                  <h2>Video jobs recente</h2>
                </div>
                <FaVideo aria-hidden="true" />
              </div>
              <div className="compact-list">
                {stats.recentVideoJobs.map((job) => (
                  <button type="button" key={job.id} onClick={() => navigate('/detectii?tab=video')}>
                    <strong>{job.sourceFilename || `Job #${job.id}`}</strong>
                    <small>{videoStatusLabel(job.status)} - {job.progressPercent ?? 0}% - {formatDateTime(job.createdAt)}</small>
                  </button>
                ))}
                {stats.recentVideoJobs.length === 0 && <p className="case-empty">Nu exista joburi video recente.</p>}
              </div>
            </div>
            )}
          </section>

          {canUsePoliceTools && (
          <section className="dashboard-panel">
            <div className="section-heading">
              <div>
                <span className="module-eyebrow">Istoric actiuni</span>
                <h2>Audit recent</h2>
              </div>
              <FaHistory aria-hidden="true" />
            </div>
            <div className="audit-strip">
              {stats.recentAuditEvents.map((event) => (
                <button type="button" key={event.id} onClick={() => openVehicle(event.targetPlateNumber)}>
                  <span className="status-badge info">{auditActionLabel(event.action)}</span>
                  <strong>{event.targetPlateNumber || 'General'}</strong>
                  <small>{event.actorUsername || '-'} - {formatDateTime(event.createdAt)}</small>
                </button>
              ))}
              {stats.recentAuditEvents.length === 0 && <p className="case-empty">Nu exista audit recent.</p>}
            </div>
          </section>
          )}
        </>
      )}
    </div>
  );
}

export default DashboardPage;
