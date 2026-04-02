import React, { useState } from 'react';
import { API_BASE_URL } from './config';

function PlateSearchPage() {
  const [query, setQuery] = useState('');
  const [results, setResults] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const role = localStorage.getItem('role') || '';

  const handleSearch = async () => {
    const q = (query || '').trim();
    if (!q) {
      setError('Introdu o placuta valida (ex: SV15WDC)');
      setResults([]);
      return;
    }

    setLoading(true);
    setError('');
    setResults([]);

    try {
      const response = await fetch(
        `${API_BASE_URL}/plates/history/search?query=${encodeURIComponent(q)}`,
        {
          headers: {
            Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
          },
        }
      );

      if (!response.ok) {
        const text = await response.text().catch(() => '');
        throw new Error(text || `Eroare la cautare (status ${response.status})`);
      }

      const data = await response.json();
      if (Array.isArray(data)) {
        setResults(data);
      } else {
        setResults(Array.isArray(data.content) ? data.content : []);
      }
    } catch (err) {
      console.error(err);
      setError(err.message || 'Eroare neasteptata');
    } finally {
      setLoading(false);
    }
  };

  const handleDownloadPdf = async (plateNumber) => {
    try {
      const token = localStorage.getItem('token') || '';
      const response = await fetch(
        `${API_BASE_URL}/api/license-plates/pdf/${encodeURIComponent(plateNumber)}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error('Eroare la descarcarea PDF-ului');
      }

      const blob = await response.blob();
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `plate_${plateNumber}.pdf`;
      document.body.appendChild(link);
      link.click();
      link.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Eroare PDF:', err.message);
      alert('Nu s-a putut descarca PDF-ul');
    }
  };

  const onKeyDown = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  return (
    <>
      <div className="search-form">
        <h2>🔍 Cautare dupa placuta</h2>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={onKeyDown}
          className="search-input"
        />
        <button onClick={handleSearch} disabled={loading} className="search-btn">
          {loading ? 'Se cauta…' : 'Cauta'}
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {!loading && results.length > 0 && (
        <div className="table-container">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Placuta</th>
                <th>Marca</th>
                <th>Model</th>
                <th>Proprietar</th>
                <th>Imagine</th>
                <th>Data procesarii</th>
                {role === 'POLICE' && <th>PDF</th>}
              </tr>
            </thead>
            <tbody>
              {results.map((item) => (
                <tr key={item.id}>
                  <td>{item.id}</td>
                  <td>{item.plateNumber}</td>
                  <td>{item.brand ?? '-'}</td>
                  <td>{item.model ?? '-'}</td>
                  <td>{item.owner ?? '-'}</td>
                  <td>
                    {item.imagePath ? (
                      <span title={item.imagePath}>🖼️</span>
                    ) : (
                      '-'
                    )}
                  </td>

                  <td>
                    {item.processedAt
                      ? new Date(item.processedAt).toLocaleString()
                      : '-'}
                  </td>
                  {role === 'POLICE' && (
                    <td>
                      <button
                        onClick={() => handleDownloadPdf(item.plateNumber)}
                        className="download-btn"
                      >
                        ⬇️ 📄 PDF
                      </button>

                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {!loading && !error && results.length === 0 && query.trim() !== '' && (
        <div className="text-gray-600">Nu s-au gasit rezultate.</div>
      )}
    </>
  );
}

export default PlateSearchPage;
