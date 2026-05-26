import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { isJwtExpired } from './jwt';
import { getDefaultRouteForRole, normalizeRole } from './authRouting';

function ProtectedRoute({ children, allowedRoles }) {
  const token = localStorage.getItem('token');
  const location = useLocation();

  if (!token || isJwtExpired(token)) {
    localStorage.removeItem('token');
    localStorage.removeItem('role');
    localStorage.removeItem('username');
    return <Navigate to={`/login?expired=true&redirect=${encodeURIComponent(location.pathname)}`} replace />;
  }

  const userRole = normalizeRole(localStorage.getItem('role'));
  if (Array.isArray(allowedRoles) && allowedRoles.length > 0) {
    const normalizedAllowedRoles = allowedRoles
      .map((role) => normalizeRole(role))
      .filter((role) => !!role);

    if (!userRole || !normalizedAllowedRoles.includes(userRole)) {
      return <Navigate to={getDefaultRouteForRole(userRole)} replace />;
    }
  }

  return children;
}

export default ProtectedRoute;
