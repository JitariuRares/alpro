import React, { useCallback, useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { API_BASE_URL } from './config';

const normalizePlate = (value) => (value || '').trim().toUpperCase();

function PlateSearchPage() {
  const location = useLocation();
  const [query, setQuery] = useState('');
  const [lookup, setLookup] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const readError = useCallback(async (response) => {
    const raw = await response.text().catch(() => '');
    if (!raw) {
      return `Eroare la cautare (status ${response.status})`;
    }

    try {
      const parsed = JSON.parse(raw);
      if (parsed?.error) {
        return parsed.error;
      }
    } catch (_) {
      // fallback to raw text
    }
    return raw;
  }, []);

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
        throw new Error(await readError(response));
      }

      const data = await response.json();
      setLookup(data);
      setQuery(normalizedPlate);
    } catch (err) {
      setError(err.message || 'Eroare neasteptata');
    } finally {
      setLoading(false);
    }
  }, [readError]);

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
        throw new Error('Eroare la descarcarea PDF-ului');
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
      setError(err.message || 'Nu s-a putut descarca PDF-ul');
    }
  };

  const hasAnyLookupData =
    !!lookup?.plate ||
    (lookup?.insurances?.length ?? 0) > 0 ||
    (lookup?.parkingHistory?.length ?? 0) > 0 ||
    (lookup?.recentOcrDetections?.length ?? 0) > 0 ||
    (lookup?.recentVideoDetections?.length ?? 0) > 0;

  const formatVideoTimestamp = (timestampMs) => {
    if (!Number.isFinite(timestampMs) || timestampMs < 0) {
      return '-';
    }
    const totalSeconds = Math.floor(timestampMs / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${String(minutes).padStart(2, '0')}:${String(seconds).padStart(2, '0')}`;
  };

  return (
    <>
      <div className="search-form">
        <h2>Cautare date vehicul (POLICE)</h2>
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={onKeyDown}
          className="search-input"
          placeholder="Ex: SV15WDC"
        />
        <button onClick={handleSearch} disabled={loading} className="search-btn">
          {loading ? 'Se cauta...' : 'Cauta'}
        </button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {!loading && lookup && (
        <div className="card">
          <h3 className="text-lg font-semibold mb-3">Rezultat lookup: {lookup.plateNumber}</h3>

          {lookup.plate ? (
            <div className="mb-4">
              <p><strong>Marca:</strong> {lookup.plate.brand || '-'}</p>
              <p><strong>Model:</strong> {lookup.plate.model || '-'}</p>
              <p><strong>Proprietar:</strong> {lookup.plate.owner || '-'}</p>
              <p>
                <strong>Ultima detectie:</strong>{' '}
                {lookup.plate.detectedAt ? new Date(lookup.plate.detectedAt).toLocaleString() : '-'}
              </p>
              <p>
                <strong>Confidence:</strong>{' '}
                {lookup.plate.confidence != null ? `${(lookup.plate.confidence * 100).toFixed(1)}%` : '-'}
              </p>
              <button onClick={handleDownloadPdf} className="download-btn mt-2">
                Descarca PDF
              </button>
            </div>
          ) : (
            <div className="alert alert-error mb-4">Placa nu exista in tabelul principal de vehicule.</div>
          )}

          <h4 className="font-semibold mb-2">Asigurari</h4>
          {lookup.insurances?.length > 0 ? (
            <div className="table-container mb-4">
              <table className="table">
                <thead>
                  <tr>
                    <th>Companie</th>
                    <th>Valabil de la</th>
                    <th>Valabil pana la</th>
                  </tr>
                </thead>
                <tbody>
                  {lookup.insurances.map((insurance) => (
                    <tr key={insurance.id}>
                      <td>{insurance.company}</td>
                      <td>{insurance.validFrom || '-'}</td>
                      <td>{insurance.validTo || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="mb-4">Nu exista asigurari pentru aceasta placuta.</p>
          )}

          <h4 className="font-semibold mb-2">Istoric parking (ultimele intrari/iesiri)</h4>
          {lookup.parkingHistory?.length > 0 ? (
            <div className="table-container mb-4">
              <table className="table">
                <thead>
                  <tr>
                    <th>Status</th>
                    <th>Intrare</th>
                    <th>Iesire</th>
                    <th>Durata</th>
                    <th>Dovezi foto</th>
                  </tr>
                </thead>
                <tbody>
                  {lookup.parkingHistory.map((parking) => (
                    <tr key={parking.id}>
                      <td>{parking.status || '-'}</td>
                      <td>{parking.entryTime ? new Date(parking.entryTime).toLocaleString() : '-'}</td>
                      <td>{parking.exitTime ? new Date(parking.exitTime).toLocaleString() : 'N/A'}</td>
                      <td>
                        {Number.isFinite(parking.durationMinutes)
                          ? `${Math.floor(parking.durationMinutes / 60)}h ${parking.durationMinutes % 60}m`
                          : '-'}
                      </td>
                      <td>
                        ENTRY: {parking.hasEntryImage ? 'da' : 'nu'} | EXIT: {parking.hasExitImage ? 'da' : 'nu'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="mb-4">Nu exista istoric parking pentru aceasta placuta.</p>
          )}

          <h4 className="font-semibold mb-2">Detectii foto recente</h4>
          {lookup.recentOcrDetections?.length > 0 ? (
            <div className="table-container mb-4">
              <table className="table">
                <thead>
                  <tr>
                    <th>Procesat la</th>
                    <th>Confidence</th>
                    <th>BBox</th>
                  </tr>
                </thead>
                <tbody>
                  {lookup.recentOcrDetections.map((detection) => (
                    <tr key={detection.id}>
                      <td>{detection.processedAt ? new Date(detection.processedAt).toLocaleString() : '-'}</td>
                      <td>{detection.confidence != null ? `${(detection.confidence * 100).toFixed(1)}%` : '-'}</td>
                      <td>
                        {detection.bbox
                          ? `x:${detection.bbox.x}, y:${detection.bbox.y}, w:${detection.bbox.w}, h:${detection.bbox.h}`
                          : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p className="mb-4">Nu exista detectii foto recente pentru aceasta placuta.</p>
          )}

          <h4 className="font-semibold mb-2">Detectii video recente</h4>
          {lookup.recentVideoDetections?.length > 0 ? (
            <div className="table-container">
              <table className="table">
                <thead>
                  <tr>
                    <th>Frame</th>
                    <th>Timp video</th>
                    <th>Track</th>
                    <th>Confidence</th>
                    <th>BBox</th>
                  </tr>
                </thead>
                <tbody>
                  {lookup.recentVideoDetections.map((detection) => (
                    <tr key={detection.id}>
                      <td>{detection.frameIndex ?? '-'}</td>
                      <td>{formatVideoTimestamp(detection.timestampMs)}</td>
                      <td>{detection.trackId ?? '-'}</td>
                      <td>{detection.confidence != null ? `${(detection.confidence * 100).toFixed(1)}%` : '-'}</td>
                      <td>
                        {detection.bbox
                          ? `x:${detection.bbox.x}, y:${detection.bbox.y}, w:${detection.bbox.w}, h:${detection.bbox.h}`
                          : '-'}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <p>Nu exista detectii video recente pentru aceasta placuta.</p>
          )}
        </div>
      )}

      {!loading && !error && lookup && !hasAnyLookupData && (
        <div className="text-gray-600">Nu exista date pentru placuta cautata.</div>
      )}
    </>
  );
}

export default PlateSearchPage;
