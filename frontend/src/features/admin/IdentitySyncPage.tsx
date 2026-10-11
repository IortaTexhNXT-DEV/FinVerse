import { useQuery, useQueryClient } from '@tanstack/react-query';
import { UserPlus } from 'lucide-react';
import { useState } from 'react';
import { identityApi } from '@/api/identity';
import { Button } from '@/components/ui/Button';
import { Notice } from '@/components/ui/Notice';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { CreateFromDirectoryDialog } from './CreateFromDirectoryDialog';
import { IdentityEventsTab } from './IdentityEventsTab';
import { SimulatorTab } from './SimulatorTab';

type Tab = 'events' | 'simulator';

/**
 * Identity Synchronisation (BDOI FRS FRUM.002 and FRUM.003): the users from the Enterprise SSO
 * platform and UIDM-ISC - the events received with their outcome and reprocessing, the creation of
 * a user from an active Enterprise SSO account, and, in SIT and UAT, the Enterprise SSO simulator.
 */
export default function IdentitySyncPage() {
  const client = useQueryClient();
  const [tab, setTab] = useState<Tab>('events');
  const [creating, setCreating] = useState(false);
  const settings = useQuery({ queryKey: ['identity-settings'], queryFn: identityApi.settings });
  const tabs = [
    { id: 'events' as const, label: 'Events' },
    ...(settings.data?.simulator === true
      ? [{ id: 'simulator' as const, label: 'Enterprise SSO Simulator' }]
      : []),
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Identity Synchronisation"
        description="Users created, updated, deactivated and reactivated from the Enterprise SSO platform and UIDM-ISC."
        actions={
          <Button variant="accent" icon={<UserPlus size={16} />} onClick={() => setCreating(true)}>
            Create User from Enterprise SSO
          </Button>
        }
      />
      {settings.data !== undefined && !settings.data.provisioning && (
        <Notice tone="warning">
          The events of the Enterprise SSO platform are not applied on this system: users change
          only through access requests.
        </Notice>
      )}
      {settings.data !== undefined && (
        <p className="muted">Directory in use: {settings.data.directoryName}</p>
      )}
      <Tabs tabs={tabs} active={tab} onChange={setTab} />
      {tab === 'events' ? <IdentityEventsTab /> : <SimulatorTab />}
      <CreateFromDirectoryDialog
        open={creating}
        onClose={() => setCreating(false)}
        onCreated={() => void client.invalidateQueries({ queryKey: ['identity-events'] })}
      />
    </div>
  );
}
