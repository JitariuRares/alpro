import React, { useState } from 'react';
import { API_BASE_URL } from './config';
import { readApiError, friendlyErrorMessage } from './errorMessages';

function ParkingSearchPage() {
  const [plateNumber, setPlateNumber] = useState('');
  const [results, setResults] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const token = localStorage.getItem('token') || '';

  const normalizePlate = (value) => (value || '').trim().toUpperCase();

  const handleSearch = async (event) => {
    event.preventDefault();
    setError('');
    setResults([]);

    const normalized = normalizePlate(plateNumber);
    if (!normalized) {
      setError('Introdu un numar de placuta valid.');
      return;
    }

    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/parking/${encodeURIComponent(normalized)}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Nu s-a gasit istoric pentru aceasta placuta.'));
      }

      const data = await response.json();
      setResults(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Eroare de retea.'));
    } finally {
      setLoading(false);
    }
  };

  const openEvidence = async (sessionId, type) => {
    try {
      const response = await fetch(
        `${API_BASE_URL}/api/parking/sessions/${sessionId}/evidence/${type}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Dovada foto nu poate fi accesata.'));
      }

      const blob = await response.blob();
      const blobUrl = window.URL.createObjectURL(blob);
      window.open(blobUrl, '_blank', 'noopener,noreferrer');
      window.setTimeout(() => window.URL.revokeObjectURL(blobUrl), 30_000);
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Nu s-a putut deschide dovada foto.'));
    }
  };

  const formatDuration = (durationMinutes) => {
    if (!Number.isFinite(durationMinutes) || durationMinutes < 0) {
      return '-';
    }
    const hours = Math.floor(durationMinutes / 60);
    const minutes = durationMinutes % 60;
    return `${hours}h ${minutes}m`;
  };

  return (
    <>
      <div className="case-search-panel">
        <div>
          <h2>Cauta sesiuni de parcare</h2>
        </div>
        <form className="case-search-controls" onSubmit={handleSearch}>
          <input
            type="text"
            value={plateNumber}
            onChange={(e) => setPlateNumber(e.target.value)}
            className="search-input"
            required
          />
          <button type="submit" className="search-btn" disabled={loading}>
            {loading ? 'Se cauta...' : 'Cauta'}
          </button>
        </form>
      </div>

      {error && <div className="alert alert-error mt-4">{error}</div>}

      {results.length > 0 && (
        <div className="mt-6 table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Status</th>
                <th>Zona</th>
                <th>Intrare</th>
                <th>Iesire</th>
                <th>Durata</th>
                <th>Dovada ENTRY</th>
                <th>Dovada EXIT</th>
              </tr>
            </thead>
            <tbody>
              {results.map((session) => (
                <tr key={session.id}>
                  <td>{session.status || '-'}</td>
                  <td>{session.parkingZone || 'Nespecificata'}</td>
                  <td>{session.entryTime ? new Date(session.entryTime).toLocaleString() : '-'}</td>
                  <td>{session.exitTime ? new Date(session.exitTime).toLocaleString() : 'N/A'}</td>
                  <td>{formatDuration(session.durationMinutes)}</td>
                  <td>
                    {session.hasEntryImage ? (
                      <button
                        type="button"
                        className="download-btn"
                        onClick={() => openEvidence(session.id, 'entry')}
                      >
                        Vezi foto ENTRY
                      </button>
                    ) : (
                      '-'
                    )}
                  </td>
                  <td>
                    {session.hasExitImage ? (
                      <button
                        type="button"
                        className="download-btn"
                        onClick={() => openEvidence(session.id, 'exit')}
                      >
                        Vezi foto EXIT
                      </button>
                    ) : (
                      '-'
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </>
  );
}

export default ParkingSearchPage;
