import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { FaParking, FaSearchLocation } from 'react-icons/fa';
import ModuleShell from './ModuleShell';
import AddParkingPage from './AddParkingPage';
import ParkingSearchPage from './ParkingSearchPage';
import { ROLE_PARKING, ROLE_POLICE, normalizeRole } from './authRouting';

const TABS = [
  { id: 'operare', label: 'Entry / Exit', icon: FaParking },
  { id: 'sesiuni', label: 'Sesiuni', icon: FaSearchLocation },
];

function ParcarePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const role = normalizeRole(localStorage.getItem('role'));
  const canOperateParking = role === ROLE_PARKING;
  const visibleTabs = canOperateParking ? TABS : TABS.filter((tab) => tab.id === 'sesiuni');
  const requestedTab = searchParams.get('tab');
  const activeTab = visibleTabs.some((tab) => tab.id === requestedTab)
    ? requestedTab
    : canOperateParking ? 'operare' : 'sesiuni';

  const changeTab = (tab) => {
    setSearchParams({ tab });
  };

  return (
    <ModuleShell
      eyebrow="Parking Ops"
      title="Parcare"
      subtitle={role === ROLE_POLICE
        ? 'Consulta sesiunile deja inregistrate pe baza placutei.'
        : 'Inregistreaza intrari si iesiri, apoi verifica rapid sesiuni si dovezi foto.'}
      tabs={visibleTabs}
      activeTab={activeTab}
      onTabChange={changeTab}
    >
      {activeTab === 'sesiuni' ? <ParkingSearchPage /> : <AddParkingPage />}
    </ModuleShell>
  );
}

export default ParcarePage;
