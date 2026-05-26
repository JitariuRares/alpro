import { isJwtExpired } from './jwt';

export const ROLE_POLICE = 'POLICE';
export const ROLE_PARKING = 'PARKING';
export const ROLE_INSURANCE = 'INSURANCE';

const ROLE_DEFAULT_ROUTE = {
  [ROLE_POLICE]: '/dashboard',
  [ROLE_PARKING]: '/dashboard',
  [ROLE_INSURANCE]: '/dashboard',
};

export function normalizeRole(role) {
  if (!role || typeof role !== 'string') {
    return null;
  }

  const normalized = role.trim().toUpperCase();
  if (!Object.prototype.hasOwnProperty.call(ROLE_DEFAULT_ROUTE, normalized)) {
    return null;
  }

  return normalized;
}

export function getDefaultRouteForRole(role) {
  const normalized = normalizeRole(role);
  if (!normalized) {
    return '/login';
  }
  return ROLE_DEFAULT_ROUTE[normalized];
}

export function isAuthenticated() {
  const token = localStorage.getItem('token');
  return !!token && !isJwtExpired(token);
}
