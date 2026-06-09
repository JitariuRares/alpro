import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './UploadPage.css';
import { API_BASE_URL } from './config';
import { readApiError, friendlyErrorMessage } from './errorMessages';

function UploadPage() {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [carData, setCarData] = useState(null);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [editData, setEditData] = useState({ brand: '', model: '', owner: '' });
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const normalizeFileForUpload = async (file) => {
    if (!file || file.type !== 'image/webp') {
      return file;
    }

    const bitmap = await createImageBitmap(file);
    const canvas = document.createElement('canvas');
    canvas.width = bitmap.width;
    canvas.height = bitmap.height;
    const ctx = canvas.getContext('2d');
    ctx.drawImage(bitmap, 0, 0);

    const convertedBlob = await new Promise((resolve) => {
      canvas.toBlob(resolve, 'image/jpeg', 0.92);
    });

    if (!convertedBlob) {
      return file;
    }

    const normalizedName = file.name.replace(/\.[^/.]+$/, '') + '.jpg';
    return new File([convertedBlob], normalizedName, { type: 'image/jpeg' });
  };

  const handleFileChange = async (event) => {
    const inputFile = event.target.files[0];

    setCarData(null);
    setSuccessMessage('');
    setError('');

    if (!inputFile) {
      setSelectedFile(null);
      setPreviewUrl(null);
      return;
    }

    let file = inputFile;
    try {
      file = await normalizeFileForUpload(inputFile);
    } catch (err) {
      setError('Imaginea selectata nu a putut fi pregatita pentru upload.');
      setSelectedFile(null);
      setPreviewUrl(null);
      return;
    }

    if (previewUrl) {
      URL.revokeObjectURL(previewUrl);
    }

    setSelectedFile(file);
    setPreviewUrl(URL.createObjectURL(file));
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Nu ai selectat o imagine.');
      return;
    }

    try {
      setUploading(true);
      setError('');
      setSuccessMessage('');
      const formData = new FormData();
      formData.append('image', selectedFile, selectedFile.name);

      const response = await fetch(`${API_BASE_URL}/api/ocr/full`, {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
        },
        body: formData,
      });

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Nu s-a putut procesa imaginea.'));
      }

      const data = await response.json();
      setCarData(data);
      setEditData({ brand: data.brand || '', model: data.model || '', owner: data.owner || '' });
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Nu s-a putut procesa imaginea.'));
    } finally {
      setUploading(false);
    }
  };

  const handleEditChange = (e) => {
    const { name, value } = e.target;
    setEditData((prev) => ({ ...prev, [name]: value }));
  };

  const handleSaveDetails = async () => {
    if (!carData || !carData.id) {
      setError('Nu exista o placuta valida pentru actualizare.');
      return;
    }

    try {
      const response = await fetch(`${API_BASE_URL}/api/license-plates/${carData.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
        },
        body: JSON.stringify(editData),
      });

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Nu s-au putut salva detaliile vehiculului.'));
      }

      const updated = await response.json();
      setCarData((prev) => ({
        ...updated,
        confidence: prev?.confidence ?? null,
        aiAttributes: prev?.aiAttributes ?? null,
      }));
      setSuccessMessage('Detaliile au fost actualizate cu succes!');
      setError('');
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Nu s-au putut salva detaliile vehiculului.'));
    }
  };

  const handleLookupVehicle = () => {
    if (!carData?.plateNumber) {
      return;
    }
    navigate(`/search?plate=${encodeURIComponent(carData.plateNumber)}`);
  };

  const handleApplyAiSuggestion = () => {
    if (!carData?.aiAttributes) {
      return;
    }

    setEditData((prev) => ({
      ...prev,
      brand: carData.aiAttributes.make || prev.brand,
      model: carData.aiAttributes.model || prev.model,
    }));
    setSuccessMessage('Sugestia AI a fost copiata in campurile editabile. Poti ajusta si salva.');
    setError('');
  };

  const plateTypeLabel = (type) => {
    const labels = {
      STANDARD: 'Standard',
      DIPLOMATIC: 'Diplomatica',
      TEMPORARY: 'Temporara',
      PROBE: 'Probe',
      MILITARY: 'Militara',
      MAI: 'MAI',
      LOCAL: 'Locala',
      UNKNOWN: 'Necunoscuta',
    };
    return labels[type] || type || 'Necunoscuta';
  };

  const colorLabel = (value) => {
    if (!value) {
      return 'Necunoscuta';
    }
    const normalized = String(value).trim().toLowerCase();
    const colors = {
      white: 'Alb',
      alb: 'Alb',
      black: 'Negru',
      negru: 'Negru',
      gray: 'Gri',
      grey: 'Gri',
      gri: 'Gri',
      silver: 'Argintiu',
      argintiu: 'Argintiu',
      red: 'Rosu',
      rosu: 'Rosu',
      blue: 'Albastru',
      albastru: 'Albastru',
      green: 'Verde',
      verde: 'Verde',
      yellow: 'Galben',
      galben: 'Galben',
      orange: 'Portocaliu',
      portocaliu: 'Portocaliu',
      brown: 'Maro',
      maro: 'Maro',
      beige: 'Bej',
      bej: 'Bej',
      gold: 'Auriu',
      auriu: 'Auriu',
      purple: 'Mov',
      mov: 'Mov',
      burgundy: 'Visiniu',
      visiniu: 'Visiniu',
    };
    return colors[normalized] || normalized.charAt(0).toUpperCase() + normalized.slice(1);
  };

  const aiAttributes = carData?.aiAttributes;

  return (
    <div className="upload-container">
      <div className="upload-left">
        <h2 className="upload-title">Previzualizare imagine</h2>

        {previewUrl && (
          <div className="preview-wrapper">
            <img
              src={previewUrl}
              alt="Preview imagine selectata"
              className="upload-preview"
            />
          </div>
        )}

        <label className="primary-btn cursor-pointer">
          Alege imagine
          <input type="file" accept="image/*" onChange={handleFileChange} hidden />
        </label>

        {previewUrl && (
          <button onClick={handleUpload} className="primary-btn" disabled={uploading}>
            {uploading ? 'Analizez imaginea...' : 'Trimite imaginea'}
          </button>
        )}
      </div>

      <div className="upload-right">
        <h2 className="upload-title">Detalii placuta</h2>

        {carData && (
          <>
            <p><strong>Placuta:</strong> {carData.plateNumber}</p>
            {carData.plateType && (
              <p><strong>Tip placuta:</strong> {plateTypeLabel(carData.plateType)}</p>
            )}
            <button onClick={handleLookupVehicle} className="primary-btn mt-2">
              Cauta date vehicul
            </button>

            {aiAttributes && (
              <div className="ai-vehicle-card">
                <div className="ai-vehicle-header">
                  <span className="ai-vehicle-badge">Sugestie AI</span>
                </div>
                <p>
                  <strong>Marca / model:</strong>{' '}
                  {[aiAttributes.make, aiAttributes.model].filter(Boolean).join(' ') || 'Necunoscut'}
                </p>
                <p>
                  <strong>Culoare:</strong> {colorLabel(aiAttributes.color)}
                </p>
                <p>
                  <strong>Caroserie:</strong> {aiAttributes.bodyType || 'Necunoscuta'}
                </p>
                {aiAttributes.reasoning && (
                  <p className="ai-vehicle-reasoning">{aiAttributes.reasoning}</p>
                )}
                {(aiAttributes.make || aiAttributes.model) && (
                  <button onClick={handleApplyAiSuggestion} className="secondary-btn">
                    Aplica marca/model
                  </button>
                )}
              </div>
            )}

            <label className="label">Marca:</label>
            <input type="text" name="brand" value={editData.brand} onChange={handleEditChange} className="input" />

            <label className="label">Model:</label>
            <input type="text" name="model" value={editData.model} onChange={handleEditChange} className="input" />

            <label className="label">Proprietar:</label>
            <input type="text" name="owner" value={editData.owner} onChange={handleEditChange} className="input" />

            <button onClick={handleSaveDetails} className="primary-btn mt-2">Salveaza detaliile</button>
          </>
        )}

        {successMessage && <div className="alert alert-success">{successMessage}</div>}
        {error && <div className="alert alert-error"><strong>Eroare:</strong> {error}</div>}
      </div>
    </div>
  );
}

export default UploadPage;
