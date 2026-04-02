import React, { useEffect, useRef, useState } from 'react';
import './UploadPage.css';

function UploadPage() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [previewBox, setPreviewBox] = useState(null);
  const [carData, setCarData] = useState(null);
  const [error, setError] = useState('');
  const [successMessage, setSuccessMessage] = useState('');
  const [editData, setEditData] = useState({ brand: '', model: '', owner: '' });
  const imgRef = useRef(null);

  useEffect(() => {
    return () => {
      if (previewUrl) {
        URL.revokeObjectURL(previewUrl);
      }
    };
  }, [previewUrl]);

  const handleFileChange = (event) => {
    const file = event.target.files[0];

    setCarData(null);
    setSuccessMessage('');
    setError('');

    if (!file) {
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
      const formData = new FormData();
      formData.append('image', selectedFile, selectedFile.name);

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

  const handlePreviewLoad = () => {
    if (!imgRef.current) {
      setPreviewBox(null);
      return;
    }

    const img = imgRef.current;
    if (!img.naturalWidth || !img.naturalHeight) {
      setPreviewBox(null);
      return;
    }

    setPreviewBox({
      renderedWidth: img.clientWidth,
      renderedHeight: img.clientHeight,
      naturalWidth: img.naturalWidth,
      naturalHeight: img.naturalHeight,
    });
  };

  const getOverlayStyle = () => {
    if (!carData?.bbox || !previewBox) {
      return null;
    }

    const scaleX = previewBox.renderedWidth / previewBox.naturalWidth;
    const scaleY = previewBox.renderedHeight / previewBox.naturalHeight;

    return {
      left: `${carData.bbox.x * scaleX}px`,
      top: `${carData.bbox.y * scaleY}px`,
      width: `${carData.bbox.w * scaleX}px`,
      height: `${carData.bbox.h * scaleY}px`,
    };
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
      setCarData((prev) => ({
        ...updated,
        confidence: prev?.confidence ?? null,
        bbox: prev?.bbox ?? null,
      }));
      setSuccessMessage('Detaliile au fost actualizate cu succes!');
      setError('');
    } catch (err) {
      setError(err.message);
    }
  };

  const overlayStyle = getOverlayStyle();
  const confidencePercent = carData?.confidence != null
    ? `${(carData.confidence * 100).toFixed(1)}%`
    : null;

  return (
    <div className="upload-container">
      <div className="upload-left">
        <h2 className="upload-title">Previzualizare imagine</h2>

        {previewUrl && (
          <div className="preview-wrapper">
            <img
              ref={imgRef}
              src={previewUrl}
              alt="Preview imagine selectata"
              className="upload-preview"
              onLoad={handlePreviewLoad}
            />
            {overlayStyle && (
              <div className="bbox-overlay" style={overlayStyle}>
                <span className="bbox-label">
                  {carData.plateNumber || 'PLACUTA'}
                </span>
              </div>
            )}
          </div>
        )}

        <label className="primary-btn cursor-pointer">
          Alege imagine
          <input type="file" accept="image/*" onChange={handleFileChange} hidden />
        </label>

        {previewUrl && (
          <button onClick={handleUpload} className="primary-btn">Trimite imaginea</button>
        )}
      </div>

      <div className="upload-right">
        <h2 className="upload-title">Detalii placuta</h2>

        {carData && (
          <>
            <p><strong>Placuta:</strong> {carData.plateNumber}</p>
            {confidencePercent && (
              <p><strong>Incredere detectie:</strong> {confidencePercent}</p>
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
