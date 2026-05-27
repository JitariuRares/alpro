import React, { useState } from 'react';
import { API_BASE_URL } from './config';
import { readApiError, friendlyErrorMessage } from './errorMessages';

function AddInsurancePage() {
  const [plateNumber, setPlateNumber] = useState('');
  const [company, setCompany] = useState('');
  const [validFrom, setValidFrom] = useState('');
  const [validTo, setValidTo] = useState('');
  const [existingInsurances, setExistingInsurances] = useState([]);
  const [editingInsuranceId, setEditingInsuranceId] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const token = localStorage.getItem('token') || '';

  const normalizePlate = (value) => (value || '').trim().toUpperCase();

  const resetFormFields = () => {
    setCompany('');
    setValidFrom('');
    setValidTo('');
    setEditingInsuranceId(null);
  };

  const loadExistingInsurances = async () => {
    setError('');
    setMessage('');
    setExistingInsurances([]);

    const normalizedPlate = normalizePlate(plateNumber);
    if (!normalizedPlate) {
      setError('Introdu un numar de placuta.');
      return;
    }

    try {
      const response = await fetch(
        `${API_BASE_URL}/api/insurance/${encodeURIComponent(normalizedPlate)}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Eroare la cautarea asigurarilor.'));
      }

      const data = await response.json();
      setExistingInsurances(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Eroare la cautarea asigurarilor.'));
    }
  };

  const handlePickForEdit = (insurance) => {
    if (!insurance?.id) {
      return;
    }
    setEditingInsuranceId(insurance.id);
    setCompany(insurance.company || '');
    setValidFrom(insurance.validFrom || '');
    setValidTo(insurance.validTo || '');
    setMessage(`Editezi polita #${insurance.id}`);
    setError('');
  };

  const handleSubmit = async () => {
    setError('');
    setMessage('');

    const normalizedPlate = normalizePlate(plateNumber);
    if (!normalizedPlate || !company || !validFrom || !validTo) {
      setError('Completeaza toate campurile.');
      return;
    }

    try {
      if (editingInsuranceId) {
        const response = await fetch(`${API_BASE_URL}/api/insurance/${editingInsuranceId}`, {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            company: company.trim(),
            validFrom,
            validTo,
          }),
        });

        if (!response.ok) {
          throw new Error(await readApiError(response, 'Eroare la actualizarea politei.'));
        }

        setMessage('Polita a fost actualizata cu succes.');
        resetFormFields();
        await loadExistingInsurances();
        return;
      }

      const plateRes = await fetch(
        `${API_BASE_URL}/api/license-plates/${encodeURIComponent(normalizedPlate)}`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );

      if (!plateRes.ok) {
        throw new Error(await readApiError(plateRes, 'Nu s-a putut valida placuta.'));
      }

      const plates = await plateRes.json();
      if (!Array.isArray(plates) || plates.length === 0) {
        throw new Error('Placuta nu exista in baza de date.');
      }

      const plate = plates[0];
      const payload = {
        company: company.trim(),
        validFrom,
        validTo,
        licensePlate: {
          id: plate.id,
          plateNumber: normalizedPlate,
        },
      };

      const insuranceRes = await fetch(`${API_BASE_URL}/api/insurance`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      if (!insuranceRes.ok) {
        throw new Error(await readApiError(insuranceRes, 'Eroare la salvarea politei.'));
      }

      setMessage('Polita a fost adaugata cu succes.');
      resetFormFields();
      await loadExistingInsurances();
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Eroare neasteptata.'));
    }
  };

  return (
    <div className="card">
      <h2 className="text-xl font-semibold mb-4 text-gray-700">Gestiune polite asigurare</h2>

      <div className="mb-4">
        <label className="block font-medium mb-1">Numar placuta:</label>
        <input
          type="text"
          value={plateNumber}
          onChange={(e) => setPlateNumber(e.target.value)}
          className="input input-bordered w-full"
          placeholder="Ex: SV15WDC"
        />
      </div>

      <button onClick={loadExistingInsurances} className="search-btn mb-4">
        Cauta polite existente
      </button>

      {existingInsurances.length > 0 && (
        <div className="table-container mb-4">
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>Companie</th>
                <th>Valabil de la</th>
                <th>Valabil pana la</th>
                <th>Actiune</th>
              </tr>
            </thead>
            <tbody>
              {existingInsurances.map((insurance) => (
                <tr key={insurance.id}>
                  <td>{insurance.id}</td>
                  <td>{insurance.company}</td>
                  <td>{insurance.validFrom}</td>
                  <td>{insurance.validTo}</td>
                  <td>
                    <button className="download-btn" onClick={() => handlePickForEdit(insurance)}>
                      Editeaza
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <div className="mb-4">
        <label className="block font-medium mb-1">Companie de asigurari:</label>
        <input
          type="text"
          value={company}
          onChange={(e) => setCompany(e.target.value)}
          className="input input-bordered w-full"
        />
      </div>

      <div className="mb-4">
        <label className="block font-medium mb-1">Valabil de la:</label>
        <input
          type="date"
          value={validFrom}
          onChange={(e) => setValidFrom(e.target.value)}
          className="input input-bordered w-full"
        />
      </div>

      <div className="mb-4">
        <label className="block font-medium mb-1">Valabil pana la:</label>
        <input
          type="date"
          value={validTo}
          onChange={(e) => setValidTo(e.target.value)}
          className="input input-bordered w-full"
        />
      </div>

      <div className="flex gap-2">
        <button onClick={handleSubmit} className="primary-btn">
          {editingInsuranceId ? 'Actualizeaza polita' : 'Adauga polita'}
        </button>
        {editingInsuranceId && (
          <button
            onClick={() => {
              resetFormFields();
              setMessage('Editarea a fost anulata.');
            }}
            className="search-btn"
          >
            Anuleaza editarea
          </button>
        )}
      </div>

      {message && <div className="alert alert-success mt-4">{message}</div>}
      {error && <div className="alert alert-error mt-4">{error}</div>}
    </div>
  );
}

export default AddInsurancePage;
