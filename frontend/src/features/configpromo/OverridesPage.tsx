import { useQuery } from '@tanstack/react-query';
import { configPromotionApi } from '@/api/configPromotion';
import type { EnvironmentOverride, OverrideItem } from '@/api/configPromotion';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { windowState } from './promotion';

/** Settings of the deployment that each environment keeps and no package carries. */
const DEPLOYMENT_SETTINGS = [
  { id: 'jobs', setting: 'Schedules of the jobs', where: 'Deployment settings of the environment' },
  {
    id: 'endpoints',
    setting: 'Addresses and credentials of the interfaces, mail server and file store',
    where: 'Deployment settings and secrets store',
  },
  { id: 'keys', setting: 'Signing, masking and encryption keys', where: 'Secrets store' },
  {
    id: 'users',
    setting: 'Passwords, second factor, sessions and sign-in tokens of the users',
    where: 'Never moved',
  },
  { id: 'counters', setting: 'Running document numbers', where: 'Never moved' },
];

/**
 * Environment Overrides (Configuration Promotion): this environment (name, production, change
 * window, signing key, versions) and the values it keeps for itself, which an import never
 * changes: parameters of the environment, schedules and recipients, running numbers and balances.
 */
export default function OverridesPage() {
  const env = useQuery({
    queryKey: ['config-promotion', 'environment'],
    queryFn: configPromotionApi.environment,
  });
  const overrides = useQuery({
    queryKey: ['config-promotion', 'overrides'],
    queryFn: configPromotionApi.overrides,
  });
  const e = env.data;
  return (
    <div className="stack">
      <PageHeader
        section="Configuration Promotion"
        title="Environment Overrides"
        description="The values this environment keeps for itself: an import never changes them."
      />
      <ErrorAlert error={env.error ?? overrides.error} />
      {e !== undefined && (
        <Card title="This Environment">
          <DefinitionGrid
            columns={2}
            items={[
              { label: 'Environment', value: e.environment },
              {
                label: 'Production',
                value: (
                  <StatusBadge
                    status={e.production ? 'ACTIVE' : 'INACTIVE'}
                    label={e.production ? 'Yes' : 'No'}
                  />
                ),
              },
              { label: 'Change window', value: e.changeWindow === '' ? 'Not set' : e.changeWindow },
              {
                label: 'Window now',
                value: windowState(e),
              },
              {
                label: 'Signing key',
                value: e.signingConfigured ? `Set (identifier ${e.keyId ?? ''})` : 'Not set',
              },
              { label: 'Pipeline may apply', value: e.pipelineApply ? 'Yes' : 'No' },
              { label: 'Platform version', value: e.platformVersion },
              { label: 'Schema version', value: e.schemaVersion },
            ]}
          />
        </Card>
      )}
      {(overrides.data ?? []).map((o) => (
        <OverrideCard key={o.code} override={o} />
      ))}
      <Card title="Kept Outside the Configuration" flush>
        <DataTable
          callout="deployment-settings"
          rows={DEPLOYMENT_SETTINGS}
          rowKey={(r) => r.id}
          columns={[
            { key: 'setting', header: 'Setting', render: (r) => r.setting },
            { key: 'where', header: 'Kept In', render: (r) => r.where },
          ]}
        />
      </Card>
    </div>
  );
}

function OverrideCard({ override }: Readonly<{ override: EnvironmentOverride }>) {
  const fields = override.kind === 'ROWS' ? ['Value'] : override.fields;
  return (
    <Card title={override.name} flush>
      <DataTable<OverrideItem>
        callout={`override-${override.code.toLowerCase()}`}
        rows={override.items}
        rowKey={(i) => i.key}
        emptyMessage="Nothing set in this environment."
        columns={[
          { key: 'key', header: 'Item', kind: 'code', render: (i) => <strong>{i.key}</strong> },
          ...fields.map((f) => ({
            key: f,
            header: f,
            render: (i: OverrideItem) => i.values[f] ?? '—',
          })),
        ]}
      />
    </Card>
  );
}
