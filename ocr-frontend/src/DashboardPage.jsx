import React, { useEffect, useState } from 'react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid
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
            Authorization: `Bearer ${localStorage.getItem('token')}`
          }
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
          topCounties: countiesArray
        });
      } catch (err) {
        setError(err.message);
      }
    };

    fetchStats();
  }, []);

  return (
    <div className="card">
      <h2 className="text-xl font-semibold mb-4 text-gray-700">📊 Dashboard</h2>

      {error && <div className="alert alert-error">{error}</div>}

      {stats && (
        <>
          <div className="mb-6">
            <p className="text-lg font-medium">
              Numar total de placute: <strong>{stats.totalPlates}</strong>
            </p>
          </div>

          <div>
            <h3 className="text-md font-semibold mb-2">
              🗺️ Top judete (ultimele 7 zile):
            </h3>
            <ResponsiveContainer width="100%" height={300}>
              <BarChart
                data={stats.topCounties}
                margin={{ top: 10, right: 30, left: 0, bottom: 5 }}
              >
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="county" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Bar dataKey="count" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      )}
    </div>
  );
}

export default DashboardPage;
