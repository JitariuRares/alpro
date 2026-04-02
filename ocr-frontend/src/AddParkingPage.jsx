import React, { useState } from 'react';
import { API_BASE_URL } from './config';

function AddParkingPage() {
  const [plateNumber, setPlateNumber] = useState('');
  const [entryTime, setEntryTime] = useState('');
  const [exitTime, setExitTime] = useState('');
  const [successData, setSuccessData] = useState(null);
  const [error, setError] = useState(null);

  const token = localStorage.getItem('token');

  const handleSubmit = async (e) => {
    e.preventDefault();
    setSuccessData(null);
    setError(null);

    try {
      const normalizedPlate = plateNumber.trim().toUpperCase();
      const response = await fetch(`${API_BASE_URL}/api/parking`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`
        },
        body: JSON.stringify({
          entryTime,
          exitTime,
          licensePlate: {
            plateNumber: normalizedPlate
          }
        })
      });

      if (response.ok) {
        const data = await response.json();
        setSuccessData(data);
        setPlateNumber('');
        setEntryTime('');
        setExitTime('');
      } else {
        setError('A aparut o eroare la salvarea parcarii.');
      }
    } catch (err) {
      setError('Eroare de retea.');
    }
  };

  return (
    <>
      <div className="search-form">
        <h2>🅿️ Adauga Istoric de Parcare</h2>
        <form onSubmit={handleSubmit}>
          <input
            type="text"
            value={plateNumber}
            onChange={(e) => setPlateNumber(e.target.value)}
            placeholder="Numar placuta"
            className="search-input"
            required
          />
          <input
            type="datetime-local"
            value={entryTime}
            onChange={(e) => setEntryTime(e.target.value)}
            className="search-input"
            required
          />
          <input
            type="datetime-local"
            value={exitTime}
            onChange={(e) => setExitTime(e.target.value)}
            className="search-input"
          />
          <button type="submit" className="search-btn">Salveaza</button>
        </form>
      </div>

      {successData && (
        <div className="alert alert-success">
          Parcarea a fost salvata
        </div>
      )}

      {error && (
        <div className="alert alert-error">
          {error}
        </div>
      )}
    </>
  );
}

export default AddParkingPage;
