import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';

function ProtectedRoute({ children }) {
  const token = localStorage.getItem('token');
  const location = useLocation();

  if (!token) {
    return <Navigate to={`/login?expired=true&redirect=${encodeURIComponent(location.pathname)}`} replace />;
  }

  return children;
}

export default ProtectedRoute;
