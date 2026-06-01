import React, { useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import { FaPaperPlane, FaRobot, FaRoute, FaTimes } from 'react-icons/fa';
import { API_BASE_URL } from './config';
import { friendlyErrorMessage, readApiError } from './errorMessages';
import { ROLE_INSURANCE, ROLE_PARKING, ROLE_POLICE, isAuthenticated, normalizeRole } from './authRouting';

const MAX_MESSAGES = 18;

function summarizeData(data) {
  if (!data || typeof data !== 'object') {
    return [];
  }

  return Object.entries(data)
    .filter(([_, value]) => value !== null && value !== undefined && value !== '')
    .slice(0, 5)
    .map(([key, value]) => {
      if (Array.isArray(value)) {
        return `${key}: ${value.length}`;
      }
      if (typeof value === 'object') {
        return `${key}: obiect`;
      }
      return `${key}: ${value}`;
    });
}

function roleQuickPrompts(role) {
  if (role === ROLE_POLICE) {
    return ['Dosar B123ABC', 'Detectii de revizuit', 'Audit recent'];
  }
  if (role === ROLE_PARKING) {
    return ['Parcare B123ABC', 'Sesiuni deschise', 'Dashboard'];
  }
  if (role === ROLE_INSURANCE) {
    return ['Asigurare B123ABC', 'Polite active', 'Dashboard'];
  }
  return ['Dashboard'];
}

function CopilotWidget() {
  const navigate = useNavigate();
  const location = useLocation();
  const role = normalizeRole(localStorage.getItem('role'));
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState([
    {
      id: 1,
      role: 'assistant',
      text: 'Copilot este activ. Spune-mi ce vrei sa caut in sistem.',
      suggestions: roleQuickPrompts(role),
    },
  ]);

  const quickPrompts = useMemo(() => roleQuickPrompts(role), [role]);
  const canRender = isAuthenticated() && location.pathname !== '/login';

  if (!canRender) {
    return null;
  }

  const appendMessage = (message) => {
    setMessages((prev) => [...prev, message].slice(-MAX_MESSAGES));
  };

  const sendMessage = async (rawText) => {
    const text = (rawText ?? input).trim();
    if (!text || loading) {
      return;
    }

    appendMessage({
      id: Date.now(),
      role: 'user',
      text,
    });

    setInput('');
    setLoading(true);
    try {
      const response = await fetch(`${API_BASE_URL}/api/copilot/chat`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${localStorage.getItem('token') || ''}`,
        },
        body: JSON.stringify({ message: text }),
      });

      if (!response.ok) {
        throw new Error(await readApiError(response, 'Copilot nu a putut procesa cererea.'));
      }

      const payload = await response.json();
      appendMessage({
        id: Date.now() + 1,
        role: 'assistant',
        text: payload?.answer || 'Am primit cererea, dar nu am putut formula un raspuns clar.',
        deepLink: payload?.deepLink || null,
        intent: payload?.intent || null,
        tool: payload?.tool || null,
        dataSummary: summarizeData(payload?.data),
        suggestions: Array.isArray(payload?.suggestions) ? payload.suggestions.slice(0, 3) : [],
      });
    } catch (error) {
      appendMessage({
        id: Date.now() + 2,
        role: 'assistant',
        text: friendlyErrorMessage(error.message, 'Copilot este indisponibil momentan.'),
      });
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    await sendMessage();
  };

  return (
    <div className={`copilot-widget ${open ? 'open' : ''}`}>
      {open && (
        <section className="copilot-panel" aria-label="Copilot panel">
          <header className="copilot-header">
            <div>
              <strong>ALPRo Copilot</strong>
              <small>Asistent operational</small>
            </div>
            <button type="button" className="copilot-icon-btn" onClick={() => setOpen(false)}>
              <FaTimes aria-hidden="true" />
            </button>
          </header>

          <div className="copilot-quick-row">
            {quickPrompts.map((prompt) => (
              <button
                key={prompt}
                type="button"
                className="copilot-quick-btn"
                onClick={() => sendMessage(prompt)}
                disabled={loading}
              >
                {prompt}
              </button>
            ))}
          </div>

          <div className="copilot-messages">
            {messages.map((message) => (
              <article key={message.id} className={`copilot-message ${message.role === 'assistant' ? 'assistant' : 'user'}`}>
                <p>{message.text}</p>

                {Array.isArray(message.dataSummary) && message.dataSummary.length > 0 && (
                  <ul className="copilot-data-list">
                    {message.dataSummary.map((item) => (
                      <li key={`${message.id}-${item}`}>{item}</li>
                    ))}
                  </ul>
                )}

                {message.intent && message.tool && (
                  <small className="copilot-meta">{message.intent} / {message.tool}</small>
                )}

                {message.deepLink && (
                  <button
                    type="button"
                    className="copilot-link-btn"
                    onClick={() => navigate(message.deepLink)}
                  >
                    <FaRoute aria-hidden="true" />
                    Deschide
                  </button>
                )}

                {Array.isArray(message.suggestions) && message.suggestions.length > 0 && (
                  <div className="copilot-suggestions-row">
                    {message.suggestions.map((suggestion) => (
                      <button
                        type="button"
                        key={`${message.id}-${suggestion}`}
                        onClick={() => sendMessage(suggestion)}
                        disabled={loading}
                      >
                        {suggestion}
                      </button>
                    ))}
                  </div>
                )}
              </article>
            ))}
          </div>

          <form className="copilot-form" onSubmit={handleSubmit}>
            <input
              type="text"
              value={input}
              onChange={(event) => setInput(event.target.value)}
              placeholder="Ex: dosar B123ABC"
              disabled={loading}
            />
            <button type="submit" disabled={loading || !input.trim()}>
              <FaPaperPlane aria-hidden="true" />
            </button>
          </form>
        </section>
      )}

      {!open && (
        <button type="button" className="copilot-fab" onClick={() => setOpen(true)}>
          <FaRobot aria-hidden="true" />
          <span>Copilot</span>
        </button>
      )}
    </div>
  );
}

export default CopilotWidget;
