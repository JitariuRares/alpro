import React, { useEffect } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';
import './App.css';
import { isJwtExpired } from './jwt';

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
  const role = localStorage.getItem('role');

  const handleLogout = () => {
    localStorage.clear();
    navigate('/login');
  };

  return (
    <div className="navbar">
      <div className="nav-container">
        <div className="nav-card">
          <h1 className="nav-header">ALPRo</h1>
          {isLoggedIn && (
            <>
              <nav className="nav-links">
                <NavLink to="/upload" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                  Upload
                </NavLink>
                <NavLink to="/search" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                  Caută plăcuță
                </NavLink>
                <NavLink to="/dashboard" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                  Dashboard
                </NavLink>

                {(role === 'INSURANCE' || role === 'POLICE') && (
                  <NavLink to="/search-insurance" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                    Cauta asigurare
                  </NavLink>
                )}
                {role === 'INSURANCE' && (
                  <NavLink to="/add-insurance" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                    Adauga asigurare
                  </NavLink>
                )}
                {role === 'POLICE' && (
                  <NavLink to="/add-plate-manual" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                    Adauga placuta manual
                  </NavLink>
                )}

                {role === 'PARKING' && (
                  <NavLink to="/add-parking" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                    Adauga parcare
                  </NavLink>
                )}
                {(role === 'PARKING' || role === 'POLICE') && (
                  <NavLink to="/search-parking" className={({ isActive }) => isActive ? 'nav-link active' : 'nav-link'}>
                    Cauta parcare
                  </NavLink>
                )}
                <button onClick={handleLogout} className="nav-link">
                  Logout
                </button>

              </nav>

              <div className="nav-user">
                Logat ca: <strong>{username}</strong>
              </div>
            </>
          )}
        </div>
      </div>
    </div>
  );
}

export default Navbar;
