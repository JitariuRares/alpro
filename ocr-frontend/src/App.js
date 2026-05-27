import React from 'react';
import { BrowserRouter as Router, Navigate, Route, Routes, useLocation } from 'react-router-dom';

import LoginPage from './LoginPage';
import ProtectedRoute from './ProtectedRoute';
import Navbar from './Navbar';
import DashboardPage from './DashboardPage';
import DetectiiPage from './DetectiiPage';
import VehiculePage from './VehiculePage';
import ParcarePage from './ParcarePage';
import AsigurariPage from './AsigurariPage';
import AuditPage from './AuditPage';
import {
  ROLE_INSURANCE,
  ROLE_PARKING,
  ROLE_POLICE,
  getDefaultRouteForRole,
  isAuthenticated,
  normalizeRole,
} from './authRouting';

import './App.css';
import './index.css';

function RootRedirect() {
  if (!isAuthenticated()) {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('username');
    return <Navigate to="/login" replace />;
  }

  const role = normalizeRole(localStorage.getItem('role'));
  return <Navigate to={getDefaultRouteForRole(role)} replace />;
}

function LegacyRedirect({ to, tab, keepSearch = false }) {
  const location = useLocation();
  const search = new URLSearchParams(keepSearch ? location.search : '');
  if (tab) {
    search.set('tab', tab);
  }
  const query = search.toString() ? `?${search.toString()}` : '';
  return <Navigate to={`${to}${query}`} replace />;
}

function App() {
  return (
    <Router>
      <div className="app-shell">
        <Navbar />
        <main className="main-container">
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<RootRedirect />} />
            <Route
              path="/dashboard"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE, ROLE_PARKING, ROLE_INSURANCE]}><DashboardPage /></ProtectedRoute>}
            />
            <Route
              path="/detectii"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><DetectiiPage /></ProtectedRoute>}
            />
            <Route
              path="/vehicule"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><VehiculePage /></ProtectedRoute>}
            />
            <Route
              path="/parcare"
              element={<ProtectedRoute allowedRoles={[ROLE_PARKING, ROLE_POLICE]}><ParcarePage /></ProtectedRoute>}
            />
            <Route
              path="/asigurari"
              element={<ProtectedRoute allowedRoles={[ROLE_INSURANCE, ROLE_POLICE]}><AsigurariPage /></ProtectedRoute>}
            />
            <Route
              path="/audit"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><AuditPage /></ProtectedRoute>}
            />

            <Route
              path="/upload"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><LegacyRedirect to="/detectii" tab="foto" /></ProtectedRoute>}
            />
            <Route
              path="/video-alpr"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><LegacyRedirect to="/detectii" tab="video" /></ProtectedRoute>}
            />
            <Route
              path="/search"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><LegacyRedirect to="/vehicule" tab="cautare" keepSearch /></ProtectedRoute>}
            />
            <Route
              path="/add-plate-manual"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><LegacyRedirect to="/vehicule" tab="adauga" /></ProtectedRoute>}
            />
            <Route
              path="/add-parking"
              element={<ProtectedRoute allowedRoles={[ROLE_PARKING]}><LegacyRedirect to="/parcare" tab="operare" /></ProtectedRoute>}
            />
            <Route
              path="/search-parking"
              element={<ProtectedRoute allowedRoles={[ROLE_PARKING, ROLE_POLICE]}><LegacyRedirect to="/parcare" tab="sesiuni" /></ProtectedRoute>}
            />
            <Route
              path="/search-insurance"
              element={<ProtectedRoute allowedRoles={[ROLE_INSURANCE]}><LegacyRedirect to="/asigurari" tab="cautare" /></ProtectedRoute>}
            />
            <Route
              path="/add-insurance"
              element={<ProtectedRoute allowedRoles={[ROLE_INSURANCE]}><LegacyRedirect to="/asigurari" tab="gestiune" /></ProtectedRoute>}
            />
          </Routes>
        </main>
        <footer className="app-footer">
          ALPRo 2025 - versiune operationala
        </footer>
      </div>
    </Router>
  );
}

export default App;
