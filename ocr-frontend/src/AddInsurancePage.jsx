import React, { useState } from 'react';
import { API_BASE_URL } from './config';
import { readApiError, friendlyErrorMessage } from './errorMessages';

function AddInsurancePage() {
  const [plateNumber, setPlateNumber] = useState('');
  const [policyNumber, setPolicyNumber] = useState('');
  const [policyType, setPolicyType] = useState('RCA');
  const [company, setCompany] = useState('');
  const [validFrom, setValidFrom] = useState('');
  const [validTo, setValidTo] = useState('');
  const [notes, setNotes] = useState('');
  const [existingInsurances, setExistingInsurances] = useState([]);
  const [editingInsuranceId, setEditingInsuranceId] = useState(null);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  const token = localStorage.getItem('token') || '';

  const normalizePlate = (value) => (value || '').trim().toUpperCase();
  const isActivePolicy = (insurance) => {
    if (!insurance?.validFrom || !insurance?.validTo) {
      return false;
    }
    const today = new Date();
    return new Date(insurance.validFrom) <= today && today <= new Date(insurance.validTo);
  };

  const resetFormFields = () => {
    setPolicyNumber('');
    setPolicyType('RCA');
    setCompany('');
    setValidFrom('');
    setValidTo('');
    setNotes('');
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
    setPolicyNumber(insurance.policyNumber || '');
    setPolicyType(insurance.policyType || 'RCA');
    setCompany(insurance.company || '');
    setValidFrom(insurance.validFrom || '');
    setValidTo(insurance.validTo || '');
    setNotes(insurance.notes || '');
    setMessage(`Editezi polita #${insurance.id}`);
    setError('');
  };

  const handleSubmit = async () => {
    setError('');
    setMessage('');

    const normalizedPlate = normalizePlate(plateNumber);
    if (!normalizedPlate || !policyNumber || !company || !validFrom || !validTo) {
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
            policyNumber: policyNumber.trim(),
            policyType,
            company: company.trim(),
            validFrom,
            validTo,
            notes: notes.trim(),
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
        policyNumber: policyNumber.trim(),
        policyType,
        company: company.trim(),
        validFrom,
        validTo,
        notes: notes.trim(),
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
                <th>Numar polita</th>
                <th>Tip</th>
                <th>Companie</th>
                <th>Status</th>
                <th>Valabil de la</th>
                <th>Valabil pana la</th>
                <th>Actiune</th>
              </tr>
            </thead>
            <tbody>
              {existingInsurances.map((insurance) => (
                <tr key={insurance.id}>
                  <td>{insurance.id}</td>
                  <td>{insurance.policyNumber || '-'}</td>
                  <td>{insurance.policyType || '-'}</td>
                  <td>{insurance.company}</td>
                  <td>
                    <span className={`status-badge ${isActivePolicy(insurance) ? 'success' : 'danger'}`}>
                      {isActivePolicy(insurance) ? 'Activa' : 'Expirata'}
                    </span>
                  </td>
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
        <label className="block font-medium mb-1">Numar polita:</label>
        <input
          type="text"
          value={policyNumber}
          onChange={(e) => setPolicyNumber(e.target.value)}
          className="input input-bordered w-full"
        />
      </div>

      <div className="mb-4">
        <label className="block font-medium mb-1">Tip polita:</label>
        <select
          value={policyType}
          onChange={(e) => setPolicyType(e.target.value)}
          className="input input-bordered w-full"
        >
          <option value="RCA">RCA</option>
          <option value="CASCO">CASCO</option>
          <option value="ALT TIP">Alt tip</option>
        </select>
      </div>

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
        <label className="block font-medium mb-1">Observatii:</label>
        <textarea
          value={notes}
          onChange={(e) => setNotes(e.target.value)}
          className="input input-bordered w-full"
          maxLength={500}
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
