import React, { useMemo } from 'react';
import { useSearchParams } from 'react-router-dom';
import { FaCarSide, FaPlusCircle } from 'react-icons/fa';
import ModuleShell from './ModuleShell';
import PlateSearchPage from './PlateSearchPage';
import AddPlateWithoutImagePage from './AddPlateWithoutImagePage';
import { ROLE_POLICE, normalizeRole } from './authRouting';

const TABS = [
  { id: 'cautare', label: 'Cautare', icon: FaCarSide },
  { id: 'adauga', label: 'Adauga manual', icon: FaPlusCircle },
];

function VehiculePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const role = normalizeRole(localStorage.getItem('role'));
  const canAddPlate = role === ROLE_POLICE;
  const tabs = useMemo(() => TABS.filter((tab) => canAddPlate || tab.id !== 'adauga'), [canAddPlate]);
  const requestedTab = searchParams.get('tab') || 'cautare';
  const activeTab = tabs.some((tab) => tab.id === requestedTab) ? requestedTab : 'cautare';

  const changeTab = (tab) => {
    const next = new URLSearchParams(searchParams);
    next.set('tab', tab);
    setSearchParams(next);
  };

  return (
    <ModuleShell
      eyebrow="Dosar vehicul"
      title="Vehicule"
      subtitle="Cauta o placuta, vezi datele agregate si adauga manual vehicule cand lipsesc din baza."
      tabs={tabs}
      activeTab={activeTab}
      onTabChange={changeTab}
    >
      {activeTab === 'adauga' ? <AddPlateWithoutImagePage /> : <PlateSearchPage />}
    </ModuleShell>
  );
}

export default VehiculePage;
