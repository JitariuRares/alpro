import React, { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { FaSearch, FaShieldAlt } from 'react-icons/fa';
import ModuleShell from './ModuleShell';
import AddInsurancePage from './AddInsurancePage';
import InsuranceSearchPage from './InsuranceSearchPage';
import { ROLE_INSURANCE, normalizeRole } from './authRouting';

const TABS = [
  { id: 'cautare', label: 'Cautare', icon: FaSearch },
  { id: 'gestiune', label: 'Gestiune polite', icon: FaShieldAlt },
];

function AsigurariPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const role = normalizeRole(localStorage.getItem('role'));
  const canManagePolicies = role === ROLE_INSURANCE;
  const tabs = useMemo(() => TABS.filter((tab) => canManagePolicies || tab.id !== 'gestiune'), [canManagePolicies]);
  const requestedTab = searchParams.get('tab') || 'cautare';
  const activeTab = tabs.some((tab) => tab.id === requestedTab) ? requestedTab : 'cautare';

  const changeTab = (tab) => {
    setSearchParams({ tab });
  };

  return (
    <ModuleShell
      title="Asigurari"
      subtitle="Cauta polite dupa placuta si gestioneaza intervalele de valabilitate."
      tabs={tabs}
      activeTab={activeTab}
      onTabChange={changeTab}
    >
      {activeTab === 'gestiune' ? <AddInsurancePage /> : <InsuranceSearchPage />}
    </ModuleShell>
  );
}

export default AsigurariPage;
