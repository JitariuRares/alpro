import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { FaCamera, FaCheckCircle, FaVideo } from 'react-icons/fa';
import ModuleShell from './ModuleShell';
import UploadPage from './UploadPage';
import VideoAlprPage from './VideoAlprPage';

const TABS = [
  { id: 'foto', label: 'Foto', icon: FaCamera },
  { id: 'video', label: 'Video', icon: FaVideo },
  { id: 'review', label: 'Review', icon: FaCheckCircle },
];

function DetectiiReviewPanel() {
  return (
    <div className="empty-state">
      <span className="status-badge warning">Urmatorul pas</span>
      <h2>Coada de review pentru detectii</h2>
      <p>
        Aici vom aduce detectiile automate care au nevoie de confirmare umana:
        placuta propusa, confidence, imagine/video sursa si actiunile Confirmat / Respins.
      </p>
      <div className="review-preview-grid">
        <div className="review-preview-card">
          <span className="status-badge info">DE REVIZUIT</span>
          <strong>B123ABC</strong>
          <small>Foto OCR - confidence 72%</small>
        </div>
        <div className="review-preview-card">
          <span className="status-badge success">CONFIRMAT</span>
          <strong>SV15WDC</strong>
          <small>Video ALPR - track #4</small>
        </div>
        <div className="review-preview-card">
          <span className="status-badge danger">RESPINS</span>
          <strong>--</strong>
          <small>Text invalid / crop neclar</small>
        </div>
      </div>
    </div>
  );
}

function DetectiiPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') || 'foto';

  const changeTab = (tab) => {
    setSearchParams({ tab });
  };

  return (
    <ModuleShell
      eyebrow="Pipeline ALPR"
      title="Detectii"
      subtitle="Upload foto, procesare video si zona de review pentru rezultatele automate ML-OCR."
      tabs={TABS}
      activeTab={activeTab}
      onTabChange={changeTab}
    >
      {activeTab === 'video' ? <VideoAlprPage /> : activeTab === 'review' ? <DetectiiReviewPanel /> : <UploadPage />}
    </ModuleShell>
  );
}

export default DetectiiPage;
