import React, { useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import {
  FaBell,
  FaCarSide,
  FaChartLine,
  FaClipboardList,
  FaParking,
  FaPowerOff,
  FaShieldAlt,
  FaVideo,
} from 'react-icons/fa';
import './App.css';
import { isJwtExpired } from './jwt';
import { ROLE_INSURANCE, ROLE_PARKING, ROLE_POLICE, normalizeRole } from './authRouting';

const ROLE_LABELS = {
  [ROLE_POLICE]: 'POLICE',
  [ROLE_PARKING]: 'PARKING',
  [ROLE_INSURANCE]: 'INSURANCE',
};

function navClass({ isActive }) {
  return isActive ? 'nav-link active' : 'nav-link';
}

function Navbar() {
  const navigate = useNavigate();
  const token = localStorage.getItem('token');
  const isLoggedIn = !!token && !isJwtExpired(token);

  useEffect(() => {
    if (token && !isLoggedIn) {
      localStorage.removeItem('token');
      localStorage.removeItem('role');
      localStorage.removeItem('username');
      navigate('/login');
    }
  }, [isLoggedIn, navigate, token]);

  const username = localStorage.getItem('username');
  const role = normalizeRole(localStorage.getItem('role'));

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  const canSeeDetectii = role === ROLE_POLICE;
  const canSeeVehicule = role === ROLE_POLICE;
  const canSeeParcare = [ROLE_PARKING, ROLE_POLICE].includes(role);
  const canSeeAsigurari = [ROLE_INSURANCE, ROLE_POLICE].includes(role);
  const canSeeAudit = role === ROLE_POLICE;

  return (
    <header className="navbar">
      <div className="nav-brand" onClick={() => navigate('/dashboard')} role="button" tabIndex={0}>
        <div className="brand-mark">A</div>
        <div>
          <h1>ALPRo</h1>
          <span>Operational control</span>
        </div>
      </div>

      {isLoggedIn && (
        <>
          <nav className="nav-links" aria-label="Navigatie principala">
            <NavLink to="/dashboard" className={navClass}>
              <FaChartLine aria-hidden="true" />
              <span>Dashboard</span>
            </NavLink>

            {canSeeDetectii && (
              <NavLink to="/detectii" className={navClass}>
                <FaVideo aria-hidden="true" />
                <span>Detectii</span>
              </NavLink>
            )}

            {canSeeVehicule && (
              <NavLink to="/vehicule" className={navClass}>
                <FaCarSide aria-hidden="true" />
                <span>Vehicule</span>
              </NavLink>
            )}

            {canSeeParcare && (
              <NavLink to="/parcare" className={navClass}>
                <FaParking aria-hidden="true" />
                <span>Parcare</span>
              </NavLink>
            )}

            {canSeeAsigurari && (
              <NavLink to="/asigurari" className={navClass}>
                <FaShieldAlt aria-hidden="true" />
                <span>Asigurari</span>
              </NavLink>
            )}

            {canSeeAudit && (
              <NavLink to="/audit" className={navClass}>
                <FaClipboardList aria-hidden="true" />
                <span>Audit</span>
              </NavLink>
            )}
          </nav>

          <div className="nav-actions">
            <button type="button" className="icon-btn" aria-label="Notificari">
              <FaBell aria-hidden="true" />
            </button>
            <div className="nav-user">
              <span>{ROLE_LABELS[role] || 'USER'}</span>
              <strong>{username}</strong>
            </div>
            <button type="button" className="logout-btn" onClick={handleLogout}>
              <FaPowerOff aria-hidden="true" />
              <span>Logout</span>
            </button>
          </div>
        </>
      )}
    </header>
  );
}

export default Navbar;
