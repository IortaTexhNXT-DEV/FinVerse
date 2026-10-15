import { useQuery } from '@tanstack/react-query';
import { identityApi } from '@/api/identity';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useTabParam } from '@/components/ui/useTabParam';
import { IdentityEventsTab } from '@/features/admin/IdentityEventsTab';
import { SimulatorTab } from '@/features/admin/SimulatorTab';
import { IdentityStatusTab } from './IdentityStatusTab';
import { ReconciliationTab } from './ReconciliationTab';
import { RefusedEventsTab } from './RefusedEventsTab';

type Tab = 'status' | 'events' | 'refused' | 'reconciliation' | 'simulator';

/**
 * Identity Sync (BDOI FRS FRUM.002 and FRUM.003): the connection with UIDM-ISC (last call, last
 * success, errors of the last 24 hours, Synchronise Now), the events received with their outcome,
 * the refused events to retry or dismiss, the reconciliation mismatches to resolve and, in SIT and
 * UAT only, the Enterprise SSO simulator.
 */
export default function IdentitySyncPage() {
  const settings = useQuery({ queryKey: ['identity-settings'], queryFn: identityApi.settings });
  const simulator = settings.data?.simulator === true;
  const tabs = [
    { id: 'status' as const, label: 'Status' },
    { id: 'events' as const, label: 'Events' },
    { id: 'refused' as const, label: 'Refused' },
    { id: 'reconciliation' as const, label: 'Reconciliation' },
    ...(simulator ? [{ id: 'simulator' as const, label: 'Simulator' }] : []),
  ];
  const [tab, setTab] = useTabParam<Tab>(
    tabs.map((t) => t.id),
    'status',
  );
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="Identity Sync"
        description="Users and their group profiles as UIDM-ISC sends them, and their reconciliation with the directory."
      />
      {settings.data !== undefined && !settings.data.provisioning && (
        <Notice tone="warning">
          The events of UIDM-ISC are not applied on this system (UAM_SSO_PROVISIONING is off).
        </Notice>
      )}
      <Tabs tabs={tabs} active={tab} onChange={setTab} />
      {tab === 'status' && <IdentityStatusTab directoryName={settings.data?.directoryName} />}
      {tab === 'events' && <IdentityEventsTab />}
      {tab === 'refused' && <RefusedEventsTab />}
      {tab === 'reconciliation' && <ReconciliationTab />}
      {tab === 'simulator' && simulator && <SimulatorTab />}
    </div>
  );
}
