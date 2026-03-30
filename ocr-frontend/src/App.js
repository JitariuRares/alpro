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


import './App.css';
import './index.css';

function App() {
  return (
    <Router>
      <div className="min-h-screen flex flex-col">
        <Navbar />
        <main className="main-container flex-1">
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/" element={<Navigate to="/upload" replace />} />
            <Route path="/upload" element={<ProtectedRoute><UploadPage /></ProtectedRoute>} />
            <Route path="/search" element={<ProtectedRoute><PlateSearchPage /></ProtectedRoute>} />
            <Route path="/add-insurance" element={<ProtectedRoute><AddInsurancePage /></ProtectedRoute>} />
            <Route path="/search-insurance" element={<ProtectedRoute><InsuranceSearchPage /></ProtectedRoute>} />
            <Route path="/add-parking" element={<ProtectedRoute><AddParkingPage /></ProtectedRoute>} />
            <Route path="/search-parking" element={<ProtectedRoute><ParkingSearchPage /></ProtectedRoute>} />
            <Route
              path="/add-plate-manual"
              element={<ProtectedRoute><AddPlateWithoutImagePage /></ProtectedRoute>}
            />
            <Route path="/dashboard" element={<ProtectedRoute><DashboardPage /></ProtectedRoute>} />
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