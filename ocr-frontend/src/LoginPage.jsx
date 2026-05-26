import React, { useEffect, useState } from 'react';
import { FaLock, FaUser, FaUserShield } from 'react-icons/fa';
import { useLocation, useNavigate } from 'react-router-dom';
import './LoginPage.css';
import { API_BASE_URL } from './config';
import { parseJwt } from './jwt';
import { getDefaultRouteForRole, normalizeRole } from './authRouting';

function LoginPage() {
  const [isRegistering, setIsRegistering] = useState(false);
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('PARKING');
  const [error, setError] = useState('');
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const params = new URLSearchParams(location.search);
    if (params.get('expired') === 'true') {
      setError('Sesiunea a expirat. Te rugam sa te autentifici din nou.');
    }
  }, [location]);

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');

    try {
      const response = await fetch(`${API_BASE_URL}/auth/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password }),
      });

      if (response.ok) {
        const data = await response.json();
        const payload = parseJwt(data.token);
        localStorage.setItem('token', data.token);
        localStorage.setItem('username', username.trim());
        if (payload?.role) {
          const normalizedRole = normalizeRole(payload.role);
          if (normalizedRole) {
            localStorage.setItem('role', normalizedRole);
          } else {
            localStorage.removeItem('role');
          }
        } else {
          localStorage.removeItem('role');
        }

        const params = new URLSearchParams(location.search);
        const redirectTo = params.get('redirect') || getDefaultRouteForRole(payload?.role);
        navigate(redirectTo);
      } else {
        const responseText = await response.text();
        let msg = responseText;
        try {
          const parsed = JSON.parse(responseText);
          if (parsed?.error) {
            msg = parsed.error;
          }
        } catch (_) {
          // keep plain text fallback
        }
        setError(msg || 'Autentificare esuata');
      }
    } catch (err) {
      setError('Eroare de retea. Incearca din nou.');
    }
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    try {
      const response = await fetch(`${API_BASE_URL}/auth/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username.trim(), password, role }),
      });

      if (response.ok) {
        alert('Cont creat cu succes. Te poti loga acum.');
        setIsRegistering(false);
        setUsername('');
        setPassword('');
        setRole('PARKING');
      } else {
        const responseText = await response.text();
        let msg = responseText;
        try {
          const parsed = JSON.parse(responseText);
          if (parsed?.error) {
            msg = parsed.error;
          }
        } catch (_) {
          // keep plain text fallback
        }
        setError(msg || 'Inregistrare esuata');
      }
    } catch (err) {
      setError('Eroare de retea. Incearca din nou.');
    }
  };

  return (
    <div className="login-background">
      <div className="login-card">
        <div className="login-brand">
          <div className="brand-mark">A</div>
          <div>
            <h1>ALPRo</h1>
            <span>Serviciu operational ALPR</span>
          </div>
        </div>

        <div className="login-header">
          <h2>{isRegistering ? 'Inregistrare cont nou' : 'Autentificare'}</h2>
          <p className="subtitle">
            {isRegistering
              ? 'Creeaza un cont pentru rolul tau operational.'
              : 'Introdu datele contului pentru a continua.'}
          </p>
        </div>

        <form onSubmit={isRegistering ? handleRegister : handleLogin} className="login-form">
          <div className="input-group">
            <FaUser className="input-icon" aria-hidden="true" />
            <input
              type="text"
              placeholder="Username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
            />
          </div>

          <div className="input-group">
            <FaLock className="input-icon" aria-hidden="true" />
            <input
              type="password"
              placeholder="Parola"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
            />
          </div>

          {isRegistering && (
            <div className="input-group">
              <FaUserShield className="input-icon" aria-hidden="true" />
              <select
                value={role}
                onChange={(e) => setRole(e.target.value)}
                className="input"
              >
                <option value="POLICE">Politie</option>
                <option value="INSURANCE">Asigurator</option>
                <option value="PARKING">Operator Parcari</option>
              </select>
            </div>
          )}

          <button type="submit" className="login-button">
            {isRegistering ? 'Inregistreaza-te' : 'Login'}
          </button>
        </form>

        {error && (
          <div className="alert alert-error mt-4">
            <strong>Eroare:</strong> {error}
          </div>
        )}

        <p className="subtitle mt-4">
          {isRegistering ? 'Ai deja cont?' : 'Nu ai cont?'}
          <button
            type="button"
            onClick={() => setIsRegistering(!isRegistering)}
            className="login-link-button"
          >
            {isRegistering ? 'Autentifica-te' : 'Creeaza unul'}
          </button>
        </p>
      </div>
    </div>
  );
}

export default LoginPage;
