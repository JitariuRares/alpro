import React, { useState, useCallback, useEffect } from 'react';
import Cropper from 'react-easy-crop';
import getCroppedImg from './cropImage';
import './UploadPage.css';

function UploadPage() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [carData, setCarData] = useState(null);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [editData, setEditData] = useState({ brand: '', model: '', owner: '' });

  const [crop, setCrop] = useState({ x: 0, y: 0 });
  const [zoom, setZoom] = useState(1);
  const [croppedAreaPixels, setCroppedAreaPixels] = useState(null);
  const [showCropper, setShowCropper] = useState(false);

  const onCropComplete = useCallback((_, croppedPixels) => {
    setCroppedAreaPixels(croppedPixels);
  }, []);

  const handleCropConfirm = useCallback(async () => {
    if (!previewUrl || !croppedAreaPixels) return;

    try {
      const croppedBlob = await getCroppedImg(previewUrl, croppedAreaPixels);
      const croppedPreviewUrl = URL.createObjectURL(croppedBlob);
      setPreviewUrl(croppedPreviewUrl);
      setSelectedFile(croppedBlob);
      setShowCropper(false);
    } catch (err) {
      setError('Eroare la crop!');
    }
  }, [previewUrl, croppedAreaPixels]);

  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.key === 'Enter') {
        handleCropConfirm();
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [handleCropConfirm]);

  const handleFileChange = (event) => {
    const file = event.target.files[0];
    setCarData(null);
    setSuccessMessage('');
    if (file) {
      const url = URL.createObjectURL(file);
      setPreviewUrl(url);
      setShowCropper(true);
    }
  };

  const handleUpload = async () => {
    if (!previewUrl) {
      setError('Nu ai selectat o imagine.');
      return;
    }

    try {
      const formData = new FormData();
      formData.append('image', selectedFile, 'cropped.png');

      const response = await fetch('http://localhost:8080/api/ocr/full', {
        method: 'POST',
        headers: {
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
        },
        body: formData,
      });

      if (!response.ok) {
        const msg = await response.text().catch(() => null);
        throw new Error(msg || `Eroare (status ${response.status})`);
      }

      const data = await response.json();
      setCarData(data);
      setEditData({ brand: data.brand || '', model: data.model || '', owner: data.owner || '' });
      setError('');
      setSuccessMessage('');
    } catch (err) {
      setError(err.message);
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
      const response = await fetch(`http://localhost:8080/api/license-plates/${carData.id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
        },
        body: JSON.stringify(editData),
      });

      if (!response.ok) {
        const msg = await response.text().catch(() => null);
        throw new Error(msg || `Eroare la actualizare (status ${response.status})`);
      }

      const updated = await response.json();
      setCarData(updated);
      setSuccessMessage('Detaliile au fost actualizate cu succes!');
      setError('');
    } catch (err) {
      setError(err.message);
    }
  };

  return (
    <div className="upload-container">
      <div className="upload-left">
        <h2 className="upload-title">🔍 Previzualizare imagine</h2>

        {showCropper && previewUrl && (
          <div className="crop-container">
            <Cropper
              image={previewUrl}
              crop={crop}
              zoom={zoom}
              aspect={4 / 1}
              onCropChange={setCrop}
              onZoomChange={setZoom}
              onCropComplete={onCropComplete}
            />
            <p className="text-sm text-center text-gray-600 mt-2">Apasa ENTER pentru a confirma selectia</p>
          </div>
        )}

        {!showCropper && previewUrl && (
          <img src={previewUrl} alt="Preview crop-uit" className="upload-preview" />
        )}

        <label className="primary-btn cursor-pointer">
          Alege imagine
          <input type="file" accept="image/*" onChange={handleFileChange} hidden />
        </label>

        {!showCropper && previewUrl && (
          <button onClick={handleUpload} className="primary-btn">Trimite imaginea</button>
        )}
      </div>

      <div className="upload-right">
        <h2 className="upload-title">📋 Detalii Plăcuta</h2>

        {carData && (
          <>
            <p><strong>Placuta:</strong> {carData.plateNumber}</p>

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
