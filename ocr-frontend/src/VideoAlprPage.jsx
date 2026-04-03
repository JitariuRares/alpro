import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import './VideoAlprPage.css';
import { API_BASE_URL } from './config';

function VideoAlprPage() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [frameStep, setFrameStep] = useState(3);
  const [maxFrames, setMaxFrames] = useState('');
  const [uploading, setUploading] = useState(false);

  const [jobs, setJobs] = useState([]);
  const [currentJob, setCurrentJob] = useState(null);
  const [results, setResults] = useState([]);
  const [selectedDetection, setSelectedDetection] = useState(null);

  const [videoUrl, setVideoUrl] = useState(null);
  const [videoMeta, setVideoMeta] = useState(null);
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

  const readError = async (response) => {
    const raw = await response.text().catch(() => '');
    if (raw) {
      try {
        const data = JSON.parse(raw);
        if (data?.error) return data.error;
        if (data?.detail) return data.detail;
      } catch (_) {
        return raw;
      }
      return raw;
    }

    return `Eroare (status ${response.status})`;
  };

  const fetchJobs = useCallback(async () => {
    const response = await fetch(`${API_BASE_URL}/api/video-jobs`, {
      headers: authHeader,
    });
    if (!response.ok) {
      throw new Error(await readError(response));
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
      throw new Error(await readError(response));
    }
    const data = await response.json();
    setCurrentJob(data);
    return data;
  }, [authHeader]);

  const loadResults = useCallback(async (jobId) => {
    const response = await fetch(`${API_BASE_URL}/api/video-jobs/${jobId}/results?page=0&size=500`, {
      headers: authHeader,
    });
    if (!response.ok) {
      throw new Error(await readError(response));
    }
    const data = await response.json();
    const items = Array.isArray(data?.items) ? data.items : [];
    setResults(items);
    setSelectedDetection(items[0] || null);
    return items;
  }, [authHeader]);

  const loadVideoBlob = useCallback(async (jobId) => {
    revokeVideoUrl();

    const response = await fetch(`${API_BASE_URL}/api/video-jobs/${jobId}/video`, {
      headers: authHeader,
    });
    if (!response.ok) {
      throw new Error(await readError(response));
    }
    const blob = await response.blob();
    const objectUrl = URL.createObjectURL(blob);
    setVideoUrl(objectUrl);
    setVideoMeta(null);
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
          setError(err.message || 'Nu s-au putut incarca job-urile video.');
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
        setError(err.message || 'Polling status a esuat.');
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
    setSelectedDetection(null);

    try {
      const form = new FormData();
      form.append('video', selectedFile, selectedFile.name);
      if (frameStep && Number(frameStep) > 0) {
        form.append('frameStep', String(frameStep));
      }
      if (maxFrames && Number(maxFrames) > 0) {
        form.append('maxFrames', String(maxFrames));
      }

      const response = await fetch(`${API_BASE_URL}/api/video-jobs`, {
        method: 'POST',
        headers: authHeader,
        body: form,
      });

      if (!response.ok) {
        throw new Error(await readError(response));
      }

      const createdJob = await response.json();
      setCurrentJob(createdJob);
      setInfo('Video incarcat. Job-ul ruleaza in background.');
      await fetchJobs();
      await loadVideoBlob(createdJob.id);
    } catch (err) {
      setError(err.message || 'Upload video esuat.');
    } finally {
      setUploading(false);
    }
  };

  const openJob = async (jobId) => {
    setError('');
    setInfo('');
    setResults([]);
    setSelectedDetection(null);

    try {
      const job = await loadJob(jobId);
      await loadVideoBlob(job.id);
      if (job.status === 'COMPLETED') {
        await loadResults(job.id);
      }
    } catch (err) {
      setError(err.message || 'Nu s-a putut deschide job-ul selectat.');
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

  useEffect(() => {
    if (!selectedDetection || !videoRef.current) {
      return;
    }
    if (typeof selectedDetection.timestampMs === 'number') {
      videoRef.current.currentTime = selectedDetection.timestampMs / 1000;
      videoRef.current.pause();
    }
  }, [selectedDetection]);

  const selectDetection = (detection) => {
    setSelectedDetection(detection);
  };

  const overlayStyle = useMemo(() => {
    if (!selectedDetection?.bbox || !videoMeta) {
      return null;
    }
    if (!videoMeta.naturalWidth || !videoMeta.naturalHeight) {
      return null;
    }
    const scaleX = videoMeta.renderedWidth / videoMeta.naturalWidth;
    const scaleY = videoMeta.renderedHeight / videoMeta.naturalHeight;
    return {
      left: `${selectedDetection.bbox.x * scaleX}px`,
      top: `${selectedDetection.bbox.y * scaleY}px`,
      width: `${selectedDetection.bbox.w * scaleX}px`,
      height: `${selectedDetection.bbox.h * scaleY}px`,
    };
  }, [selectedDetection, videoMeta]);

  return (
    <div className="video-page">
      <section className="video-card">
        <h2>Video ALPR</h2>
        <p className="video-muted">Upload video, procesare async, status job, rezultate cu bounding box pe frame.</p>

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

        <div className="video-options">
          <label>
            Frame step
            <input
              type="number"
              min="1"
              max="120"
              value={frameStep}
              onChange={(e) => setFrameStep(e.target.value)}
            />
          </label>
          <label>
            Max frames (optional)
            <input
              type="number"
              min="1"
              max="5000"
              value={maxFrames}
              onChange={(e) => setMaxFrames(e.target.value)}
              placeholder="nelimitat"
            />
          </label>
        </div>

        <button className="primary-btn" onClick={handleUpload} disabled={uploading}>
          {uploading ? 'Se incarca...' : 'Porneste job video'}
        </button>

        {info && <div className="alert alert-success">{info}</div>}
        {error && <div className="alert alert-error"><strong>Eroare:</strong> {error}</div>}
      </section>

      <section className="video-grid">
        <div className="video-card">
          <h3>Job-uri video</h3>
          <div className="video-jobs-list">
            {jobs.length === 0 && <p className="video-muted">Nu exista job-uri video inca.</p>}
            {jobs.map((job) => (
              <div className={`video-job-item ${currentJob?.id === job.id ? 'active' : ''}`} key={job.id}>
                <div>
                  <p><strong>#{job.id}</strong> {job.sourceFilename}</p>
                  <p className="video-muted">
                    Status: {job.status} | Detectii: {job.detectionCount ?? 0}
                  </p>
                </div>
                <button className="page-btn" onClick={() => openJob(job.id)}>Deschide</button>
              </div>
            ))}
          </div>
        </div>

        <div className="video-card">
          <h3>Preview + overlay</h3>
          {currentJob && (
            <p className="video-muted">
              Job #{currentJob.id} | {currentJob.status}
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
                />
                {overlayStyle && (
                  <div className="video-bbox-overlay" style={overlayStyle}>
                    <span className="video-bbox-label">
                      {selectedDetection?.plateText || 'PLATE'}
                    </span>
                  </div>
                )}
              </>
            ) : (
              <p className="video-muted">Selecteaza un job pentru a incarca preview-ul video.</p>
            )}
          </div>

          {selectedDetection && (
            <p className="video-muted">
              Frame {selectedDetection.frameIndex} | track {selectedDetection.trackId ?? '-'} |&nbsp;
              {selectedDetection.confidence != null ? `${(selectedDetection.confidence * 100).toFixed(1)}%` : 'fara scor'}
            </p>
          )}
        </div>
      </section>

      <section className="video-card">
        <h3>Rezultate detectii</h3>
        {results.length === 0 && <p className="video-muted">Job-ul nu are detectii sau nu este finalizat.</p>}
        {results.length > 0 && (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Frame</th>
                  <th>Timp (ms)</th>
                  <th>Track</th>
                  <th>Placuta</th>
                  <th>Confidence</th>
                </tr>
              </thead>
              <tbody>
                {results.map((item) => (
                  <tr
                    key={item.id}
                    className={selectedDetection?.id === item.id ? 'video-selected-row' : ''}
                    onClick={() => selectDetection(item)}
                  >
                    <td>{item.frameIndex}</td>
                    <td>{item.timestampMs ?? '-'}</td>
                    <td>{item.trackId ?? '-'}</td>
                    <td>{item.plateText}</td>
                    <td>{item.confidence != null ? `${(item.confidence * 100).toFixed(1)}%` : '-'}</td>
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
