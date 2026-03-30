import React, { useState } from 'react';

function ParkingSearchPage() {
  const [plateNumber, setPlateNumber] = useState('');
  const [results, setResults] = useState([]);
  const [error, setError] = useState(null);

  const token = localStorage.getItem('token');

  const handleSearch = async (e) => {
    e.preventDefault();
    setError(null);
    setResults([]);

    try {
      const response = await fetch(`http://localhost:8080/api/parking/${plateNumber}`, {
        headers: {
          Authorization: `Bearer ${token}`
        }
      });

      if (response.ok) {
        const data = await response.json();
        setResults(data);
      } else {
        setError('Nu s-a gasit niciun istoric pentru aceasta placuta.');
      }
    } catch (err) {
      setError('Eroare de retea.');
    }
  };

  return (
    <>
      <div className="search-form">
        <h2>🅿️ Cauta Istoric de Parcare</h2>
        <form onSubmit={handleSearch}>
          <input
            type="text"
            value={plateNumber}
            onChange={(e) => setPlateNumber(e.target.value)}
            placeholder="Numar placuta"
            className="search-input"
            required
          />
          <button type="submit" className="search-btn">Cauta</button>
        </form>
      </div>

      {error && <div className="alert alert-error mt-4">{error}</div>}

      {results.length > 0 && (
        <div className="mt-6 table-container">
          <table className="table">
            <thead>
              <tr>
                <th>Intrare</th>
                <th>Iesire</th>
              </tr>
            </thead>
            <tbody>
              {results.map((r) => (
                <tr key={r.id}>
                  <td>{r.entryTime?.replace('T', ' ')}</td>
                  <td>{r.exitTime?.replace('T', ' ') || 'N/A'}</td>
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
