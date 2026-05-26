import React, { useEffect, useState } from 'react';
import { FaCarSide, FaParking, FaShieldAlt } from 'react-icons/fa';
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

function DashboardPage() {
  const [stats, setStats] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    const fetchStats = async () => {
      try {
        const res = await fetch(`${API_BASE_URL}/api/dashboard/stats`, {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token')}`,
          },
        });

        if (!res.ok) {
          throw new Error('Eroare la incarcarea statisticilor');
        }

        const data = await res.json();

        const countiesArray = Object.entries(data.topCountiesLast7Days || {}).map(
          ([county, count]) => ({ county, count })
        );

        setStats({
          totalPlates: data.totalPlates,
          totalInsurances: data.totalInsurances,
          totalParkings: data.totalParkings,
          topCounties: countiesArray,
        });
      } catch (err) {
        setError(err.message);
      }
    };

    fetchStats();
  }, []);

  return (
    <div className="dashboard-page">
      <section className="module-hero dashboard-hero">
        <div>
          <p className="module-eyebrow">ALPRo Command Center</p>
          <h1>Dashboard</h1>
          <p className="module-subtitle">
            Privire rapida peste vehicule, asigurari, sesiuni de parcare si distributia detectiilor recente.
          </p>
        </div>
        <span className="status-badge success">Online</span>
      </section>

      {error && <div className="alert alert-error">{error}</div>}

      {stats && (
        <>
          <section className="dashboard-stats-grid">
            <div className="stat-card">
              <div className="stat-icon blue"><FaCarSide aria-hidden="true" /></div>
              <span>Vehicule</span>
              <strong>{stats.totalPlates}</strong>
            </div>
            <div className="stat-card">
              <div className="stat-icon green"><FaShieldAlt aria-hidden="true" /></div>
              <span>Polite</span>
              <strong>{stats.totalInsurances}</strong>
            </div>
            <div className="stat-card">
              <div className="stat-icon orange"><FaParking aria-hidden="true" /></div>
              <span>Sesiuni parcare</span>
              <strong>{stats.totalParkings}</strong>
            </div>
          </section>

          <section className="card dashboard-chart-card">
            <div className="section-heading">
              <div>
                <span className="module-eyebrow">Ultimele 7 zile</span>
                <h2>Top judete detectate</h2>
              </div>
              <span className="status-badge info">{stats.topCounties.length} active</span>
            </div>
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
          </section>
        </>
      )}
    </div>
  );
}

export default DashboardPage;
