import React, { useState } from 'react';

function AddInsurancePage() {
  const [plateNumber, setPlateNumber] = useState('');
  const [company, setCompany] = useState('');
  const [validFrom, setValidFrom] = useState('');
  const [validTo, setValidTo] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [savedInsurance, setSavedInsurance] = useState(null);

  const handleSubmit = async () => {
    setError('');
    setMessage('');
    setSavedInsurance(null);

    if (!plateNumber || !company || !validFrom || !validTo) {
      setError('Completeaza toate campurile');
      return;
    }

    try {
      const plateRes = await fetch(`http://localhost:8080/api/license-plates/${plateNumber}`, {
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`
        }
      });

      const plates = await plateRes.json();
      if (!Array.isArray(plates) || plates.length === 0) {
        throw new Error('Placuta nu exista in baza de date');
      }

      const plate = plates[0];
      const insurancePayload = {
        company,
        validFrom,
        validTo,
        licensePlate: { id: plate.id }
      };

      const insuranceRes = await fetch(`http://localhost:8080/api/insurance`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`
        },
        body: JSON.stringify(insurancePayload)
      });

      if (!insuranceRes.ok) {
        const msg = await insuranceRes.text();
        throw new Error(msg || 'Eroare la salvarea asigurarii');
      }

      const responseText = await insuranceRes.text();
      let insuranceData = null;

      if (responseText && responseText.trim()) {
        insuranceData = JSON.parse(responseText);
        setSavedInsurance(insuranceData);
      }

      setMessage('Polita a fost adaugata cu succes!');
      setPlateNumber('');
      setCompany('');
      setValidFrom('');
      setValidTo('');
    } catch (err) {
      console.error(err);
      setError(err.message);
    }
  };

  return (
    <div className="card">
      <h2 className="text-xl font-semibold mb-4 text-gray-700">Adauga Polita de Asigurare</h2>

      <div className="mb-4">
        <label className="block font-medium mb-1">Numar placuta existent:</label>
        <input
          type="text"
          value={plateNumber}
          onChange={(e) => setPlateNumber(e.target.value)}
          className="input input-bordered w-full"
          placeholder="Ex: SV15WDC"
        />
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

      <button onClick={handleSubmit} className="primary-btn">Salveaza polita</button>

      {message && <div className="alert alert-success mt-4">{message}</div>}
      {error && <div className="alert alert-error mt-4">{error}</div>}

      {savedInsurance && (
        <div className="alert alert-success mt-4">
          <p><strong>Companie:</strong> {savedInsurance.company}</p>
          <p><strong>De la:</strong> {savedInsurance.validFrom}</p>
          <p><strong>Pana la:</strong> {savedInsurance.validTo}</p>
        </div>
      )}
    </div>
  );
}

export default AddInsurancePage;
