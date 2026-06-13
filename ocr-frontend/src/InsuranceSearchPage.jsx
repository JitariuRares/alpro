import React, { useState } from 'react';
import { API_BASE_URL } from './config';

function InsuranceSearchPage() {
  const [plateNumber, setPlateNumber] = useState('');
  const [insuranceList, setInsuranceList] = useState([]);
  const [error, setError] = useState('');

  const isActivePolicy = (insurance) => {
    if (!insurance?.validFrom || !insurance?.validTo) {
      return false;
    }
    const today = new Date();
    return new Date(insurance.validFrom) <= today && today <= new Date(insurance.validTo);
  };

  const handleSearch = async () => {
    setError('');
    setInsuranceList([]);

    if (!plateNumber) {
      setError('Introdu un numar de inmatriculare');
      return;
    }

    try {
      const normalizedPlate = plateNumber.trim().toUpperCase();
      const res = await fetch(`${API_BASE_URL}/api/insurance/${encodeURIComponent(normalizedPlate)}`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token')}`
        }
      });

      if (!res.ok) {
        throw new Error('Eroare la cautare');
      }

      const data = await res.json();
      setInsuranceList(data);
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <>
      <div className="case-search-panel">
        <div>
          <h2>Cauta asigurare</h2>
        </div>

        <div className="case-search-controls">
          <input
            type="text"
            value={plateNumber}
            onChange={(e) => setPlateNumber(e.target.value)}
            className="search-input"
          />

          <button onClick={handleSearch} className="search-btn">Cauta</button>
        </div>
      </div>

      {error && <div className="alert alert-error mb-4">{error}</div>}

      {insuranceList.length > 0 && (
        <div className="card bg-green-50 mt-4">
          <h3 className="font-semibold mb-2">Rezultate:</h3>
          {insuranceList.map((ins, idx) => (
            <div key={idx} className="mb-4">
              <p><strong>Numar polita:</strong> {ins.policyNumber || '-'}</p>
              <p><strong>Tip polita:</strong> {ins.policyType || '-'}</p>
              <p><strong>Companie:</strong> {ins.company}</p>
              <p>
                <strong>Status:</strong>{' '}
                <span className={`status-badge ${isActivePolicy(ins) ? 'success' : 'danger'}`}>
                  {isActivePolicy(ins) ? 'Activa' : 'Expirata'}
                </span>
              </p>
              <p><strong>De la:</strong> {ins.validFrom}</p>
              <p><strong>Pana la:</strong> {ins.validTo}</p>
              {ins.notes && <p><strong>Observatii:</strong> {ins.notes}</p>}
              <hr className="my-2" />
            </div>
          ))}
        </div>
      )}
    </>
  );
}

export default InsuranceSearchPage;
