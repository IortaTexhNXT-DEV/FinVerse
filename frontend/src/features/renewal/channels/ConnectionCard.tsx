import { useQuery } from '@tanstack/react-query';
import { renewalChannelsApi } from '@/api/renewalChannels';
import { Card } from '@/components/ui/Card';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';

/** The connection of a channel: live interface or simulator, endpoint, key and its answer. */
export function ConnectionCard({ channel }: Readonly<{ channel: string }>) {
  const companyId = useCompanyId();
  const c = useQuery({
    queryKey: ['renewal', 'channel-connection', companyId, channel],
    queryFn: () => renewalChannelsApi.connection(companyId, channel),
  });
  const d = c.data;
  return (
    <Card title={`${channel} Connection`}>
      <ErrorAlert error={c.error} />
      {d && (
        <DefinitionGrid
          items={[
            { label: 'Mode', value: d.settings.mode === 'LIVE' ? 'Live interface' : 'Simulator' },
            { label: 'Endpoint', value: d.settings.endpoint || 'Not set' },
            {
              label: 'Access key',
              value: d.settings.keySet ? `Set (${d.settings.keySetting})` : 'Not set',
            },
            {
              label: 'Check',
              value: (
                <StatusBadge
                  status={d.check.reachable ? 'ACTIVE' : 'FAILED'}
                  label={d.check.detail}
                />
              ),
            },
          ]}
        />
      )}
    </Card>
  );
}
