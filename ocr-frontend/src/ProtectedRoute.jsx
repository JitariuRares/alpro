import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { isJwtExpired } from './jwt';

function ProtectedRoute({ children }) {
  const token = localStorage.getItem('token');
  const location = useLocation();

  if (!token || isJwtExpired(token)) {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('username');
    return <Navigate to={`/login?expired=true&redirect=${encodeURIComponent(location.pathname)}`} replace />;
  }

  return children;
}

export default ProtectedRoute;
