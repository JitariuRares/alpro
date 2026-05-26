import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';

import UploadPage from './UploadPage';
import PlateSearchPage from './PlateSearchPage';
import LoginPage from './LoginPage';
import ProtectedRoute from './ProtectedRoute';
import AddInsurancePage from './AddInsurancePage';
import InsuranceSearchPage from './InsuranceSearchPage';
import AddParkingPage from './AddParkingPage';
import ParkingSearchPage from './ParkingSearchPage';
import Navbar from './Navbar'; 
import AddPlateWithoutImagePage from './AddPlateWithoutImagePage';
import DashboardPage from './DashboardPage';
import VideoAlprPage from './VideoAlprPage';
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

function App() {
  return (
    <Router>
      <div className="min-h-screen flex flex-col">
        <Navbar />
        <main className="main-container flex-1">
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<RootRedirect />} />
            <Route
              path="/upload"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><UploadPage /></ProtectedRoute>}
            />
            <Route
              path="/video-alpr"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><VideoAlprPage /></ProtectedRoute>}
            />
            <Route
              path="/search"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><PlateSearchPage /></ProtectedRoute>}
            />
            <Route
              path="/add-insurance"
              element={<ProtectedRoute allowedRoles={[ROLE_INSURANCE]}><AddInsurancePage /></ProtectedRoute>}
            />
            <Route
              path="/search-insurance"
              element={<ProtectedRoute allowedRoles={[ROLE_INSURANCE]}><InsuranceSearchPage /></ProtectedRoute>}
            />
            <Route
              path="/add-parking"
              element={<ProtectedRoute allowedRoles={[ROLE_PARKING]}><AddParkingPage /></ProtectedRoute>}
            />
            <Route
              path="/search-parking"
              element={<ProtectedRoute allowedRoles={[ROLE_PARKING, ROLE_POLICE]}><ParkingSearchPage /></ProtectedRoute>}
            />
            <Route
              path="/add-plate-manual"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE]}><AddPlateWithoutImagePage /></ProtectedRoute>}
            />
            <Route
              path="/dashboard"
              element={<ProtectedRoute allowedRoles={[ROLE_POLICE, ROLE_PARKING, ROLE_INSURANCE]}><DashboardPage /></ProtectedRoute>}
            />
          </Routes>
        </main>
        <footer className="bg-white py-4">
          <div className="max-w-3xl mx-auto text-center text-gray-500 text-sm">
            © 2025 ALPR App - versiune 1.0.0
          </div>
        </footer>
      </div>
    </Router>
  );
}

export default App;
