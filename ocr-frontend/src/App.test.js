import {
  ROLE_INSURANCE,
  ROLE_PARKING,
  ROLE_POLICE,
  getDefaultRouteForRole,
  normalizeRole,
} from './authRouting';

test('normalizes known roles', () => {
  expect(normalizeRole(' police ')).toBe(ROLE_POLICE);
  expect(normalizeRole('parking')).toBe(ROLE_PARKING);
  expect(normalizeRole('INSURANCE')).toBe(ROLE_INSURANCE);
});

test('routes authenticated users to dashboard by default', () => {
  expect(getDefaultRouteForRole(ROLE_POLICE)).toBe('/dashboard');
  expect(getDefaultRouteForRole(ROLE_PARKING)).toBe('/dashboard');
  expect(getDefaultRouteForRole(ROLE_INSURANCE)).toBe('/dashboard');
});
