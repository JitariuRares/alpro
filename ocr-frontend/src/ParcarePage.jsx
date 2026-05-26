import React from 'react';
import { useSearchParams } from 'react-router-dom';
import { FaParking, FaSearchLocation } from 'react-icons/fa';
import ModuleShell from './ModuleShell';
import AddParkingPage from './AddParkingPage';
import ParkingSearchPage from './ParkingSearchPage';

const TABS = [
  { id: 'operare', label: 'Entry / Exit', icon: FaParking },
  { id: 'sesiuni', label: 'Sesiuni', icon: FaSearchLocation },
];

function ParcarePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const activeTab = searchParams.get('tab') || 'operare';

  const changeTab = (tab) => {
    setSearchParams({ tab });
  };

  return (
    <ModuleShell
      eyebrow="Parking Ops"
      title="Parcare"
      subtitle="Inregistreaza intrari si iesiri, apoi verifica rapid sesiuni si dovezi foto."
      tabs={TABS}
      activeTab={activeTab}
      onTabChange={changeTab}
    >
      {activeTab === 'sesiuni' ? <ParkingSearchPage /> : <AddParkingPage />}
    </ModuleShell>
  );
}

export default ParcarePage;
