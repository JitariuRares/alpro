import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import './VideoAlprPage.css';
import { API_BASE_URL } from './config';

function VideoAlprPage() {
  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);

  const [jobs, setJobs] = useState([]);
  const [currentJob, setCurrentJob] = useState(null);
  const [results, setResults] = useState([]);
  const [selectedPlate, setSelectedPlate] = useState(null);

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
    const allItems = [];
    let page = 0;
    let totalPages = 1;
    const size = 500;

    while (page < totalPages && page < 100) {
      const response = await fetch(`${API_BASE_URL}/api/video-jobs/${jobId}/results?page=${page}&size=${size}`, {
        headers: authHeader,
      });
      if (!response.ok) {
        throw new Error(await readError(response));
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
      throw new Error(await readError(response));
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
    setSelectedPlate(null);
    setCurrentVideoMs(0);

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
        summaries.push({
          plateText,
          firstTimestampMs: moments[0]._timestampMs,
          firstDetection: moments[0],
          moments,
        });
      }
    }

    summaries.sort((first, second) => first.firstTimestampMs - second.firstTimestampMs);
    return summaries;
  }, [results]);

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
        trackId: sourceDetection.trackId,
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
        <p className="video-muted">Setarile de frame sunt optimizate automat.</p>

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
                  onTimeUpdate={handleVideoTimeUpdate}
                />
                {activeOverlays.map((overlay) => (
                  <div
                    key={overlay.key}
                    className={`video-bbox-overlay ${selectedPlate === overlay.plateText ? 'focused' : ''}`}
                    style={overlay.style}
                  >
                    <span className="video-bbox-label">
                      {overlay.plateText || 'PLATE'}
                    </span>
                  </div>
                ))}
              </>
            ) : (
              <p className="video-muted">Selecteaza un job pentru a incarca preview-ul video.</p>
            )}
          </div>

          {videoUrl && (
            <p className="video-muted">
              Timp curent: {formatTimestampForUi(currentVideoMs)} | Box-uri active: {activeOverlays.length}
            </p>
          )}
        </div>
      </section>

      <section className="video-card">
        <h3>Rezultate detectii</h3>
        {plateSummaries.length === 0 && <p className="video-muted">Job-ul nu are detectii sau nu este finalizat.</p>}
        {plateSummaries.length > 0 && (
          <div className="table-container">
            <p className="video-muted">
              Placute unice: {plateSummaries.length} | Detectii brute: {results.length}
            </p>
            <table className="table video-simple-table">
              <thead>
                <tr>
                  <th>Placuta</th>
                  <th>Momente detectie</th>
                </tr>
              </thead>
              <tbody>
                {plateSummaries.map((item) => (
                  <tr
                    key={item.plateText}
                    className={selectedPlate === item.plateText ? 'video-selected-row' : ''}
                    onClick={() => jumpToDetection(item.firstDetection)}
                  >
                    <td>{item.plateText}</td>
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
