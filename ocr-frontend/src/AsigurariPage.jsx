import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { FaSearch, FaShieldAlt } from 'react-icons/fa';
import ModuleShell from './ModuleShell';
import AddInsurancePage from './AddInsurancePage';
import InsuranceSearchPage from './InsuranceSearchPage';

const TABS = [
  { id: 'cautare', label: 'Cautare', icon: FaSearch },
  { id: 'gestiune', label: 'Gestiune polite', icon: FaShieldAlt },
];

function AsigurariPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') || 'cautare';

  const changeTab = (tab) => {
    setSearchParams({ tab });
  };

  return (
    <ModuleShell
      eyebrow="Insurance Desk"
      title="Asigurari"
      subtitle="Cauta polite dupa placuta si gestioneaza intervalele de valabilitate."
      tabs={TABS}
      activeTab={activeTab}
      onTabChange={changeTab}
    >
      {activeTab === 'gestiune' ? <AddInsurancePage /> : <InsuranceSearchPage />}
    </ModuleShell>
  );
}

export default AsigurariPage;
