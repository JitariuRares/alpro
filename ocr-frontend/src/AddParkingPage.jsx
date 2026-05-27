import React, { useState } from 'react';
import { API_BASE_URL } from './config';
import { readApiError, friendlyErrorMessage } from './errorMessages';

function AddParkingPage() {
  const [entryPlateNumber, setEntryPlateNumber] = useState('');
  const [entryTime, setEntryTime] = useState('');
  const [entryImage, setEntryImage] = useState(null);

  const [exitPlateNumber, setExitPlateNumber] = useState('');
  const [exitTime, setExitTime] = useState('');
  const [exitImage, setExitImage] = useState(null);

  const [successData, setSuccessData] = useState(null);
  const [error, setError] = useState('');
  const [loadingAction, setLoadingAction] = useState('');

  const token = localStorage.getItem('token') || '';

  const toLocalDateTimeParam = (value) => {
    if (!value) {
      return null;
    }
    if (value.length === 16) {
      return `${value}:00`;
    }
    return value;
  };

  const submitEntry = async (event) => {
    event.preventDefault();
    setError('');
    setSuccessData(null);

    if (!entryPlateNumber.trim()) {
      setError('Numarul placutei este obligatoriu pentru ENTRY.');
      return;
    }
    if (!entryImage) {
      setError('Dovada foto pentru ENTRY este obligatorie.');
      return;
    }

    setLoadingAction('entry');
    try {
      const formData = new FormData();
      formData.append('plateNumber', entryPlateNumber.trim().toUpperCase());
      const isoEntryTime = toLocalDateTimeParam(entryTime);
      if (isoEntryTime) {
        formData.append('entryTime', isoEntryTime);
      }
      formData.append('image', entryImage, entryImage.name);

      const response = await fetch(`${API_BASE_URL}/api/parking/entry`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });

      if (!response.ok) {
        throw new Error(await readApiError(response, 'A aparut o eroare la inregistrarea ENTRY.'));
      }

      const data = await response.json();
      setSuccessData({ type: 'ENTRY', payload: data });
      setEntryPlateNumber('');
      setEntryTime('');
      setEntryImage(null);
      const entryInput = document.getElementById('parking-entry-image-input');
      if (entryInput) {
        entryInput.value = '';
      }
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Eroare de retea la ENTRY.'));
    } finally {
      setLoadingAction('');
    }
  };

  const submitExit = async (event) => {
    event.preventDefault();
    setError('');
    setSuccessData(null);

    if (!exitPlateNumber.trim()) {
      setError('Numarul placutei este obligatoriu pentru EXIT.');
      return;
    }
    if (!exitImage) {
      setError('Dovada foto pentru EXIT este obligatorie.');
      return;
    }

    setLoadingAction('exit');
    try {
      const formData = new FormData();
      formData.append('plateNumber', exitPlateNumber.trim().toUpperCase());
      const isoExitTime = toLocalDateTimeParam(exitTime);
      if (isoExitTime) {
        formData.append('exitTime', isoExitTime);
      }
      formData.append('image', exitImage, exitImage.name);

      const response = await fetch(`${API_BASE_URL}/api/parking/exit`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body: formData,
      });

      if (!response.ok) {
        throw new Error(await readApiError(response, 'A aparut o eroare la inregistrarea EXIT.'));
      }

      const data = await response.json();
      setSuccessData({ type: 'EXIT', payload: data });
      setExitPlateNumber('');
      setExitTime('');
      setExitImage(null);
      const exitInput = document.getElementById('parking-exit-image-input');
      if (exitInput) {
        exitInput.value = '';
      }
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Eroare de retea la EXIT.'));
    } finally {
      setLoadingAction('');
    }
  };

  return (
    <>
      <div className="card mb-4">
        <h2 className="text-xl font-semibold mb-3">Inregistrare ENTRY</h2>
        <form onSubmit={submitEntry}>
          <input
            type="text"
            value={entryPlateNumber}
            onChange={(e) => setEntryPlateNumber(e.target.value)}
            placeholder="Numar placuta (ex: SV15WDC)"
            className="search-input"
            required
          />
          <input
            type="datetime-local"
            value={entryTime}
            onChange={(e) => setEntryTime(e.target.value)}
            className="search-input"
          />
          <input
            id="parking-entry-image-input"
            type="file"
            accept="image/*"
            onChange={(e) => setEntryImage(e.target.files?.[0] || null)}
            className="search-input"
            required
          />
          <button type="submit" className="search-btn" disabled={loadingAction === 'entry'}>
            {loadingAction === 'entry' ? 'Se salveaza...' : 'Salveaza ENTRY'}
          </button>
        </form>
      </div>

      <div className="card">
        <h2 className="text-xl font-semibold mb-3">Inregistrare EXIT</h2>
        <form onSubmit={submitExit}>
          <input
            type="text"
            value={exitPlateNumber}
            onChange={(e) => setExitPlateNumber(e.target.value)}
            placeholder="Numar placuta (ex: SV15WDC)"
            className="search-input"
            required
          />
          <input
            type="datetime-local"
            value={exitTime}
            onChange={(e) => setExitTime(e.target.value)}
            className="search-input"
          />
          <input
            id="parking-exit-image-input"
            type="file"
            accept="image/*"
            onChange={(e) => setExitImage(e.target.files?.[0] || null)}
            className="search-input"
            required
          />
          <button type="submit" className="search-btn" disabled={loadingAction === 'exit'}>
            {loadingAction === 'exit' ? 'Se salveaza...' : 'Salveaza EXIT'}
          </button>
        </form>
      </div>

      {successData && (
        <div className="alert alert-success mt-4">
          Operatie {successData.type} salvata cu succes. Sesiune #{successData.payload?.id}
          {' '}({successData.payload?.status || '-'})
        </div>
      )}

      {error && (
        <div className="alert alert-error mt-4">
          {error}
        </div>
      )}
    </>
  );
}

export default AddParkingPage;
