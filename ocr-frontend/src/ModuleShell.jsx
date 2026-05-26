import React from 'react';

function ModuleShell({ title, subtitle, eyebrow, actions, tabs, activeTab, onTabChange, children }) {
  return (
    <section className="module-shell">
      <div className="module-hero">
        <div>
          {eyebrow && <p className="module-eyebrow">{eyebrow}</p>}
          <h1>{title}</h1>
          {subtitle && <p className="module-subtitle">{subtitle}</p>}
        </div>
        {actions && <div className="module-actions">{actions}</div>}
      </div>

      {Array.isArray(tabs) && tabs.length > 0 && (
        <div className="module-tabs" role="tablist">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = activeTab === tab.id;
            return (
              <button
                key={tab.id}
                type="button"
                className={isActive ? 'module-tab active' : 'module-tab'}
                onClick={() => onTabChange(tab.id)}
                role="tab"
                aria-selected={isActive}
              >
                {Icon && <Icon aria-hidden="true" />}
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      )}

      <div className="module-content">
        {children}
      </div>
    </section>
  );
}

export default ModuleShell;
