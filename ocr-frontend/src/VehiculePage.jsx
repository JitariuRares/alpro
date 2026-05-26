import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { FaCarSide, FaPlusCircle } from 'react-icons/fa';
import ModuleShell from './ModuleShell';
import PlateSearchPage from './PlateSearchPage';
import AddPlateWithoutImagePage from './AddPlateWithoutImagePage';

const TABS = [
  { id: 'cautare', label: 'Cautare', icon: FaCarSide },
  { id: 'adauga', label: 'Adauga manual', icon: FaPlusCircle },
];

function VehiculePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') || 'cautare';

  const changeTab = (tab) => {
    const next = new URLSearchParams(searchParams);
    next.set('tab', tab);
    setSearchParams(next);
  };

  return (
    <ModuleShell
      eyebrow="Vehicle Case"
      title="Vehicule"
      subtitle="Cauta o placuta, vezi datele agregate si adauga manual vehicule cand lipsesc din baza."
      tabs={TABS}
      activeTab={activeTab}
      onTabChange={changeTab}
    >
      {activeTab === 'adauga' ? <AddPlateWithoutImagePage /> : <PlateSearchPage />}
    </ModuleShell>
  );
}

export default VehiculePage;
