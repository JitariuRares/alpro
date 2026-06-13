import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './VideoAlprPage.css';
import { API_BASE_URL } from './config';
import { readApiError, friendlyErrorMessage } from './errorMessages';

function VideoAlprPage() {
  const navigate = useNavigate();
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);

  const [jobs, setJobs] = useState([]);
  const [currentJob, setCurrentJob] = useState(null);
  const [results, setResults] = useState([]);
  const [selectedPlate, setSelectedPlate] = useState(null);
  const [vehicleDetails, setVehicleDetails] = useState({ brand: '', model: '', owner: '' });
  const [vehicleSaving, setVehicleSaving] = useState(false);
  const [vehicleSaveMessage, setVehicleSaveMessage] = useState('');

  const [videoUrl, setVideoUrl] = useState(null);
  const [videoMeta, setVideoMeta] = useState(null);
  const [currentVideoMs, setCurrentVideoMs] = useState(0);
  const videoRef = useRef(null);

  const [error, setError] = useState('');
  const [info, setInfo] = useState('');

  const authHeader = useMemo(() => {
    const token = localStorage.getItem('token') || '';
    return { Authorization: `Bearer ${token}` };
  }, []);

  const revokeVideoUrl = useCallback(() => {
    setVideoUrl((oldUrl) => {
      if (oldUrl) {
        URL.revokeObjectURL(oldUrl);
      }
      return null;
    });
  }, []);

  useEffect(() => () => revokeVideoUrl(), [revokeVideoUrl]);

  const getDetectionTimestampMs = (detection) => {
    if (Number.isFinite(detection?.timestampMs)) {
      return Number(detection.timestampMs);
    }
    return 0;
  };

  const PLATE_MOMENT_MIN_GAP_MS = 1500;

  const formatTimestampForUi = (timestampMs) => {
    if (!Number.isFinite(timestampMs) || timestampMs < 0) {
      return '-';
    }

    const totalSeconds = Math.floor(timestampMs / 1000);
    const hours = Math.floor(totalSeconds / 3600);
    const minutes = Math.floor((totalSeconds % 3600) / 60);
    const seconds = totalSeconds % 60;

    const twoDigits = (value) => String(value).padStart(2, '0');
    if (hours > 0) {
      return `${twoDigits(hours)}:${twoDigits(minutes)}:${twoDigits(seconds)}`;
    }
    return `${twoDigits(minutes)}:${twoDigits(seconds)}`;
  };

  const colorLabel = (value) => {
    if (!value) {
      return null;
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

  const fetchJobs = useCallback(async () => {
    const response = await fetch(`${API_BASE_URL}/api/video-jobs`, {
      headers: authHeader,
    });
    if (!response.ok) {
      throw new Error(await readApiError(response, 'Nu s-au putut incarca procesarile video.'));
    }
    const data = await response.json();
    setJobs(Array.isArray(data) ? data : []);
    return Array.isArray(data) ? data : [];
  }, [authHeader]);

  const loadJob = useCallback(async (jobId) => {
    const response = await fetch(`${API_BASE_URL}/api/video-jobs/${jobId}`, {
      headers: authHeader,
    });
    if (!response.ok) {
      throw new Error(await readApiError(response, 'Nu s-a putut incarca procesarea video.'));
    }
    const data = await response.json();
    setCurrentJob(data);
    return data;
  }, [authHeader]);

  const loadResults = useCallback(async (jobId) => {
    const allItems = [];
    let page = 0;
    let totalPages = 1;
    const size = 500;

    while (page < totalPages && page < 100) {
      const response = await fetch(`${API_BASE_URL}/api/video-jobs/${jobId}/results?page=${page}&size=${size}`, {
        headers: authHeader,
      });
      if (!response.ok) {
        throw new Error(await readApiError(response, 'Nu s-au putut incarca rezultatele video.'));
      }
      const data = await response.json();
      const items = Array.isArray(data?.items) ? data.items : [];
      allItems.push(...items);

      const serverTotalPages = Number.isFinite(data?.totalPages) ? Number(data.totalPages) : 1;
      totalPages = Math.max(1, serverTotalPages);
      page += 1;
    }

    allItems.sort((first, second) => {
      const timeCompare = getDetectionTimestampMs(first) - getDetectionTimestampMs(second);
      if (timeCompare !== 0) {
        return timeCompare;
      }

      const frameCompare = (first?.frameIndex ?? 0) - (second?.frameIndex ?? 0);
      if (frameCompare !== 0) {
        return frameCompare;
      }

      return (first?.id ?? 0) - (second?.id ?? 0);
    });

    setResults(allItems);
    setSelectedPlate(allItems[0]?.plateText || null);
    return allItems;
  }, [authHeader]);

  const loadVideoBlob = useCallback(async (jobId) => {
    revokeVideoUrl();

    const response = await fetch(`${API_BASE_URL}/api/video-jobs/${jobId}/video`, {
      headers: authHeader,
    });
    if (!response.ok) {
      throw new Error(await readApiError(response, 'Nu s-a putut incarca fisierul video.'));
    }
    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);
    setVideoUrl(objectUrl);
    setVideoMeta(null);
    setCurrentVideoMs(0);
  }, [authHeader, revokeVideoUrl]);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const list = await fetchJobs();
        if (!cancelled && list.length > 0) {
          setCurrentJob(list[0]);
        }
      } catch (err) {
        if (!cancelled) {
          setError(friendlyErrorMessage(err.message, 'Nu s-au putut incarca procesarile video.'));
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [fetchJobs]);

  useEffect(() => {
    if (!currentJob || currentJob.status === 'COMPLETED' || currentJob.status === 'FAILED') {
      return;
    }

    const intervalId = window.setInterval(async () => {
      try {
        const refreshed = await loadJob(currentJob.id);
        await fetchJobs();
        if (refreshed.status === 'COMPLETED') {
          setInfo('Procesarea video s-a terminat.');
          await loadResults(refreshed.id);
        }
      } catch (err) {
        setError(friendlyErrorMessage(err.message, 'Polling status a esuat.'));
      }
    }, 2500);

    return () => window.clearInterval(intervalId);
  }, [currentJob, fetchJobs, loadJob, loadResults]);

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Selecteaza un fisier video mai intai.');
      return;
    }

    setUploading(true);
    setError('');
    setInfo('');
    setResults([]);
    setSelectedPlate(null);
    setCurrentVideoMs(0);

    try {
      const form = new FormData();
      form.append('video', selectedFile, selectedFile.name);

      const response = await fetch(`${API_BASE_URL}/api/video-jobs`, {
        method: 'POST',
        headers: authHeader,
        body: form,
      });

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Upload video esuat.'));
      }

      const createdJob = await response.json();
      setCurrentJob(createdJob);
      setInfo('Video incarcat. Detectarea ruleaza in fundal.');
      await fetchJobs();
      await loadVideoBlob(createdJob.id);
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Upload video esuat.'));
    } finally {
      setUploading(false);
    }
  };

  const openJob = async (jobId) => {
    setError('');
    setInfo('');
    setResults([]);
    setSelectedPlate(null);
    setCurrentVideoMs(0);

    try {
      const job = await loadJob(jobId);
      await loadVideoBlob(job.id);
      if (job.status === 'COMPLETED') {
        await loadResults(job.id);
      }
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Nu s-a putut deschide procesarea selectata.'));
    }
  };

  const handleVideoLoaded = () => {
    if (!videoRef.current) {
      return;
    }

    const el = videoRef.current;
    setVideoMeta({
      renderedWidth: el.clientWidth,
      renderedHeight: el.clientHeight,
      naturalWidth: el.videoWidth,
      naturalHeight: el.videoHeight,
    });
    setCurrentVideoMs(el.currentTime * 1000);
  };

  useEffect(() => {
    const onResize = () => {
      if (!videoRef.current) {
        return;
      }
      const el = videoRef.current;
      setVideoMeta({
        renderedWidth: el.clientWidth,
        renderedHeight: el.clientHeight,
        naturalWidth: el.videoWidth,
        naturalHeight: el.videoHeight,
      });
    };
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, []);

  const handleVideoTimeUpdate = () => {
    if (!videoRef.current) {
      return;
    }
    setCurrentVideoMs(videoRef.current.currentTime * 1000);
  };

  useEffect(() => {
    if (!videoRef.current) {
      return undefined;
    }

    const videoElement = videoRef.current;
    let animationFrameId = null;

    const updateFromVideo = () => {
      setCurrentVideoMs(videoElement.currentTime * 1000);
      if (!videoElement.paused && !videoElement.ended) {
        animationFrameId = window.requestAnimationFrame(updateFromVideo);
      }
    };

    const startLoop = () => {
      if (animationFrameId != null) {
        window.cancelAnimationFrame(animationFrameId);
      }
      animationFrameId = window.requestAnimationFrame(updateFromVideo);
    };

    const stopLoop = () => {
      if (animationFrameId != null) {
        window.cancelAnimationFrame(animationFrameId);
      }
      animationFrameId = null;
    };

    videoElement.addEventListener('play', startLoop);
    videoElement.addEventListener('pause', stopLoop);
    videoElement.addEventListener('ended', stopLoop);
    videoElement.addEventListener('seeked', updateFromVideo);

    if (!videoElement.paused) {
      startLoop();
    }

    return () => {
      videoElement.removeEventListener('play', startLoop);
      videoElement.removeEventListener('pause', stopLoop);
      videoElement.removeEventListener('ended', stopLoop);
      videoElement.removeEventListener('seeked', updateFromVideo);
      stopLoop();
    };
  }, [videoUrl]);

  const trackTimelines = useMemo(() => {
    const grouped = new Map();

    for (const detection of results) {
      if (!detection?.bbox) {
        continue;
      }

      const key = detection.trackId != null ? `track-${detection.trackId}` : `det-${detection.id}`;
      if (!grouped.has(key)) {
        grouped.set(key, []);
      }
      grouped.get(key).push({
        ...detection,
        _timestampMs: getDetectionTimestampMs(detection),
      });
    }

    for (const timeline of grouped.values()) {
      timeline.sort((first, second) => first._timestampMs - second._timestampMs);
    }

    return grouped;
  }, [results]);

  const plateSummaries = useMemo(() => {
    const groupedByPlate = new Map();

    for (const detection of results) {
      if (!detection?.plateText) {
        continue;
      }

      const timestampMs = getDetectionTimestampMs(detection);
      if (!groupedByPlate.has(detection.plateText)) {
        groupedByPlate.set(detection.plateText, []);
      }

      groupedByPlate.get(detection.plateText).push({
        ...detection,
        _timestampMs: timestampMs,
      });
    }

    const summaries = [];
    for (const [plateText, detections] of groupedByPlate.entries()) {
      detections.sort((first, second) => {
        const timeCompare = first._timestampMs - second._timestampMs;
        if (timeCompare !== 0) {
          return timeCompare;
        }

        const frameCompare = (first?.frameIndex ?? 0) - (second?.frameIndex ?? 0);
        if (frameCompare !== 0) {
          return frameCompare;
        }

        return (first?.id ?? 0) - (second?.id ?? 0);
      });

      const moments = [];
      for (const detection of detections) {
        const lastKept = moments[moments.length - 1];
        if (
          !lastKept ||
          detection._timestampMs - lastKept._timestampMs >= PLATE_MOMENT_MIN_GAP_MS
        ) {
          moments.push(detection);
        }
      }

      if (moments.length > 0) {
        const aiAttributes = detections.find((detection) => detection.aiAttributes)?.aiAttributes || null;
        summaries.push({
          plateText,
          firstTimestampMs: moments[0]._timestampMs,
          firstDetection: moments[0],
          moments,
          aiAttributes,
        });
      }
    }

    summaries.sort((first, second) => first.firstTimestampMs - second.firstTimestampMs);
    return summaries;
  }, [results]);

  const selectedPlateSummary = useMemo(() => (
    plateSummaries.find((summary) => summary.plateText === selectedPlate) || null
  ), [plateSummaries, selectedPlate]);

  useEffect(() => {
    setVehicleSaveMessage('');

    if (!selectedPlateSummary?.plateText) {
      setVehicleDetails({ brand: '', model: '', owner: '' });
      return undefined;
    }

    const aiDefaults = {
      brand: selectedPlateSummary.aiAttributes?.make || '',
      model: selectedPlateSummary.aiAttributes?.model || '',
      owner: '',
    };
    setVehicleDetails(aiDefaults);

    let cancelled = false;
    const loadExistingVehicle = async () => {
      try {
        const response = await fetch(
          `${API_BASE_URL}/api/license-plates/${encodeURIComponent(selectedPlateSummary.plateText)}`,
          { headers: authHeader }
        );
        if (!response.ok) {
          return;
        }

        const data = await response.json();
        const existing = Array.isArray(data) ? data[0] : null;
        if (!cancelled && existing) {
          setVehicleDetails({
            brand: existing.brand || aiDefaults.brand,
            model: existing.model || aiDefaults.model,
            owner: existing.owner || '',
          });
        }
      } catch {
        // Existing details are optional; the operator can still save new ones.
      }
    };

    loadExistingVehicle();
    return () => {
      cancelled = true;
    };
  }, [
    authHeader,
    selectedPlateSummary?.plateText,
    selectedPlateSummary?.aiAttributes?.make,
    selectedPlateSummary?.aiAttributes?.model,
  ]);

  const activeOverlays = useMemo(() => {
    if (!videoMeta || !videoMeta.naturalWidth || !videoMeta.naturalHeight) {
      return [];
    }

    const scaleX = videoMeta.renderedWidth / videoMeta.naturalWidth;
    const scaleY = videoMeta.renderedHeight / videoMeta.naturalHeight;
    const keepTrackVisibleMs = 1500;
    const overlays = [];

    for (const [key, timeline] of trackTimelines.entries()) {
      if (!Array.isArray(timeline) || timeline.length === 0) {
        continue;
      }

      let nextIndex = timeline.length;
      let left = 0;
      let right = timeline.length - 1;
      while (left <= right) {
        const middle = Math.floor((left + right) / 2);
        if (timeline[middle]._timestampMs >= currentVideoMs) {
          nextIndex = middle;
          right = middle - 1;
        } else {
          left = middle + 1;
        }
      }

      const previous = nextIndex > 0 ? timeline[nextIndex - 1] : null;
      const next = nextIndex < timeline.length ? timeline[nextIndex] : null;
      let sourceDetection = null;
      let interpolatedBbox = null;

      if (
        previous &&
        next &&
        next._timestampMs > previous._timestampMs &&
        currentVideoMs >= previous._timestampMs &&
        currentVideoMs <= next._timestampMs
      ) {
        const span = next._timestampMs - previous._timestampMs;
        const ratio = Math.max(0, Math.min(1, (currentVideoMs - previous._timestampMs) / span));
        interpolatedBbox = {
          x: Math.round(previous.bbox.x + (next.bbox.x - previous.bbox.x) * ratio),
          y: Math.round(previous.bbox.y + (next.bbox.y - previous.bbox.y) * ratio),
          w: Math.round(previous.bbox.w + (next.bbox.w - previous.bbox.w) * ratio),
          h: Math.round(previous.bbox.h + (next.bbox.h - previous.bbox.h) * ratio),
        };
        sourceDetection = previous;
      } else if (previous && currentVideoMs - previous._timestampMs <= keepTrackVisibleMs) {
        interpolatedBbox = previous.bbox;
        sourceDetection = previous;
      }

      if (!sourceDetection || !interpolatedBbox) {
        continue;
      }

      overlays.push({
        key,
        plateText: sourceDetection.plateText,
        style: {
          left: `${interpolatedBbox.x * scaleX}px`,
          top: `${interpolatedBbox.y * scaleY}px`,
          width: `${interpolatedBbox.w * scaleX}px`,
          height: `${interpolatedBbox.h * scaleY}px`,
        },
      });
    }

    return overlays;
  }, [trackTimelines, currentVideoMs, videoMeta]);

  const jumpToDetection = (detection) => {
    if (!detection) {
      return;
    }

    setSelectedPlate(detection.plateText || null);
    const timestampMs = getDetectionTimestampMs(detection);
    if (videoRef.current && Number.isFinite(timestampMs)) {
      videoRef.current.currentTime = timestampMs / 1000;
      videoRef.current.pause();
      setCurrentVideoMs(timestampMs);
    }
  };

  const goToVehicleLookup = (plateText) => {
    if (!plateText) {
      return;
    }
    navigate(`/search?plate=${encodeURIComponent(plateText)}`);
  };

  const handleVehicleDetailsChange = (event) => {
    const { name, value } = event.target;
    setVehicleDetails((prev) => ({ ...prev, [name]: value }));
    setVehicleSaveMessage('');
  };

  const applyVideoAiSuggestion = () => {
    if (!selectedPlateSummary?.aiAttributes) {
      return;
    }

    setVehicleDetails((prev) => ({
      ...prev,
      brand: selectedPlateSummary.aiAttributes.make || prev.brand,
      model: selectedPlateSummary.aiAttributes.model || prev.model,
    }));
    setVehicleSaveMessage('Sugestia AI a fost copiata in campurile editabile.');
  };

  const saveVideoVehicleDetails = async () => {
    if (!currentJob?.id || !selectedPlateSummary?.plateText) {
      setError('Selecteaza o placuta detectata inainte de salvare.');
      return;
    }

    try {
      setVehicleSaving(true);
      setError('');
      setVehicleSaveMessage('');

      const response = await fetch(
        `${API_BASE_URL}/api/video-jobs/${currentJob.id}/plates/${encodeURIComponent(selectedPlateSummary.plateText)}/vehicle-details`,
        {
          method: 'PUT',
          headers: {
            ...authHeader,
            'Content-Type': 'application/json',
          },
          body: JSON.stringify(vehicleDetails),
        }
      );

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Nu s-au putut salva detaliile vehiculului.'));
      }

      const saved = await response.json();
      setVehicleDetails({
        brand: saved.brand || '',
        model: saved.model || '',
        owner: saved.owner || '',
      });
      setVehicleSaveMessage('Detaliile vehiculului au fost salvate. Placa poate fi cautata in dosarul vehiculului.');
    } catch (err) {
      setError(friendlyErrorMessage(err.message, 'Nu s-au putut salva detaliile vehiculului.'));
    } finally {
      setVehicleSaving(false);
    }
  };

  const jobStatusLabel = (status) => {
    const labels = {
      PENDING: 'In asteptare',
      RUNNING: 'In procesare',
      COMPLETED: 'Finalizat',
      FAILED: 'Esuat',
    };
    return labels[status] || status || 'Necunoscut';
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

  return (
    <div className="video-page">
      <section className="video-card">
        <h2>Video ALPR</h2>
        <p className="video-muted">Incarcare video, procesare in fundal si momente de detectie pe inregistrare.</p>

        <div className="video-form-row">
          <label className="video-file-label">
            Selecteaza video
            <input
              type="file"
              accept="video/*"
              onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
              hidden
            />
          </label>
          <span className="video-filename">{selectedFile ? selectedFile.name : 'Niciun fisier selectat'}</span>
        </div>
        <p className="video-muted">Setarile de frame sunt optimizate automat.</p>

        <button className="primary-btn" onClick={handleUpload} disabled={uploading}>
          {uploading ? 'Se incarca...' : 'Porneste procesarea'}
        </button>

        {info && <div className="alert alert-success">{info}</div>}
        {error && <div className="alert alert-error"><strong>Eroare:</strong> {error}</div>}
      </section>

      <section className="video-grid">
        <div className="video-card">
          <h3>Procesari video</h3>
          <div className="video-jobs-list">
            {jobs.length === 0 && <p className="video-muted">Nu exista procesari video inca.</p>}
            {jobs.map((job) => (
              <div className={`video-job-item ${currentJob?.id === job.id ? 'active' : ''}`} key={job.id}>
                <div>
                  <p><strong>Procesare #{job.id}</strong></p>
                  <small className="video-job-file">{job.sourceFilename}</small>
                  <p className="video-muted">
                    Status: {jobStatusLabel(job.status)} | Detectii: {job.detectionCount ?? 0}
                  </p>
                </div>
                <button className="page-btn" onClick={() => openJob(job.id)}>Deschide</button>
              </div>
            ))}
          </div>
        </div>

        <div className="video-card">
          <h3>Preview video</h3>
          {currentJob && (
            <p className="video-muted">
              Procesare #{currentJob.id} | {jobStatusLabel(currentJob.status)}
              {currentJob.progressPercent != null ? ` | ${currentJob.progressPercent}%` : ''}
            </p>
          )}
          {currentJob?.status === 'FAILED' && currentJob?.errorMessage && (
            <div className="alert alert-error">
              <strong>Motiv esec:</strong> {currentJob.errorMessage}
            </div>
          )}

          <div className="video-preview-wrapper">
            {videoUrl ? (
              <>
                <video
                  ref={videoRef}
                  className="video-preview"
                  controls
                  src={videoUrl}
                  onLoadedMetadata={handleVideoLoaded}
                  onSeeked={handleVideoLoaded}
                  onTimeUpdate={handleVideoTimeUpdate}
                />
                {activeOverlays.map((overlay) => (
                  <div
                    key={overlay.key}
                    className={`video-bbox-overlay ${selectedPlate === overlay.plateText ? 'focused' : ''}`}
                    style={overlay.style}
                  >
                    <span className="video-bbox-label">
                      {overlay.plateText || 'PLACUTA'}
                    </span>
                  </div>
                ))}
              </>
            ) : (
              <p className="video-muted">Selecteaza o procesare pentru a incarca preview-ul video.</p>
            )}
          </div>

          {videoUrl && (
            <p className="video-muted">
              Timp curent: {formatTimestampForUi(currentVideoMs)}
            </p>
          )}
        </div>
      </section>

      <section className="video-card">
        <h3>Rezultate detectii</h3>
        {plateSummaries.length === 0 && <p className="video-muted">Procesarea nu are detectii sau nu este finalizata.</p>}
        {plateSummaries.length > 0 && (
          <div className="table-container">
            {selectedPlateSummary && (
              <div className="video-vehicle-editor">
                <div className="video-vehicle-editor-header">
                  <div>
                    <span className="video-ai-badge">Dosar vehicul</span>
                    <h4>{selectedPlateSummary.plateText}</h4>
                  </div>
                  <button type="button" className="page-btn" onClick={() => goToVehicleLookup(selectedPlateSummary.plateText)}>
                    Deschide dosar
                  </button>
                </div>

                {selectedPlateSummary.aiAttributes && (
                  <div className="video-ai-suggestion">
                    <strong>Sugestie AI:</strong>{' '}
                    {[selectedPlateSummary.aiAttributes.make, selectedPlateSummary.aiAttributes.model].filter(Boolean).join(' ') || 'Marca/model necunoscute'}
                    {selectedPlateSummary.aiAttributes.color ? ` - ${colorLabel(selectedPlateSummary.aiAttributes.color)}` : ''}
                    {(selectedPlateSummary.aiAttributes.make || selectedPlateSummary.aiAttributes.model) && (
                      <button type="button" className="secondary-btn" onClick={applyVideoAiSuggestion}>
                        Aplica sugestia
                      </button>
                    )}
                  </div>
                )}

                <div className="video-details-grid">
                  <label>
                    Marca
                    <input name="brand" value={vehicleDetails.brand} onChange={handleVehicleDetailsChange} />
                  </label>
                  <label>
                    Model
                    <input name="model" value={vehicleDetails.model} onChange={handleVehicleDetailsChange} />
                  </label>
                  <label>
                    Proprietar
                    <input name="owner" value={vehicleDetails.owner} onChange={handleVehicleDetailsChange} />
                  </label>
                  <button type="button" className="primary-btn" onClick={saveVideoVehicleDetails} disabled={vehicleSaving}>
                    {vehicleSaving ? 'Se salveaza...' : 'Salveaza detaliile'}
                  </button>
                </div>

                {vehicleSaveMessage && <div className="alert alert-success">{vehicleSaveMessage}</div>}
              </div>
            )}

            <p className="video-muted">
              Placute unice: {plateSummaries.length} | Detectii brute: {results.length}
            </p>
            <table className="table video-simple-table">
              <thead>
                <tr>
                  <th>Placuta</th>
                  <th>AI vehicul</th>
                  <th>Momente detectie</th>
                  <th>Dosar vehicul</th>
                </tr>
              </thead>
              <tbody>
                {plateSummaries.map((item) => (
                  <tr
                    key={item.plateText}
                    className={selectedPlate === item.plateText ? 'video-selected-row' : ''}
                    onClick={() => jumpToDetection(item.firstDetection)}
                  >
                    <td>
                      <strong>{item.plateText}</strong>
                      {item.firstDetection?.plateType && (
                        <span className="video-plate-type">
                          {plateTypeLabel(item.firstDetection.plateType)}
                        </span>
                      )}
                    </td>
                    <td>
                      {item.aiAttributes ? (
                        <div className="video-ai-summary">
                          <span className="video-ai-badge">AI</span>
                          <strong>
                            {[item.aiAttributes.make, item.aiAttributes.model].filter(Boolean).join(' ') || 'Necunoscut'}
                          </strong>
                          <small>
                            {[colorLabel(item.aiAttributes.color), item.aiAttributes.bodyType].filter(Boolean).join(' / ') || 'atribute partiale'}
                          </small>
                        </div>
                      ) : (
                        <span className="video-muted">-</span>
                      )}
                    </td>
                    <td>
                      <div className="video-moment-list">
                        {item.moments.map((moment) => (
                          <button
                            key={moment.id}
                            type="button"
                            className="video-moment-btn"
                            onClick={(event) => {
                              event.stopPropagation();
                              jumpToDetection(moment);
                            }}
                          >
                            {formatTimestampForUi(moment._timestampMs)}
                          </button>
                        ))}
                      </div>
                    </td>
                    <td>
                      <button
                        type="button"
                        className="page-btn"
                        onClick={(event) => {
                          event.stopPropagation();
                          goToVehicleLookup(item.plateText);
                        }}
                      >
                        Cauta vehicul
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </div>
  );
}

export default VideoAlprPage;
