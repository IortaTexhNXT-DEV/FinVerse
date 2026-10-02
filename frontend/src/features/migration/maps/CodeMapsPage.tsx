import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { migrationApi } from '@/api/migration';
import type { MapSet, UnmappedCode } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { Tag } from '@/components/ui/Tag';
import { useCompanyId } from '@/context/workspaceContext';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION, migLabel } from '../common/migrationCodes';
import '../migration.css';
import { MapSetPanel } from './MapSetPanel';
import { BRAND } from '@/branding';

type Tab = 'sets' | 'unmapped';

/**
 * Code Maps (DATA_MIGRATION_DESIGN section 7): the code map sets with their approved and open
 * versions, the entries of a version (edited in DRAFT, submitted by the Data Steward and approved
 * by the business owner, gate G2), the difference between versions, Excel import and export, and
 * the legacy codes found unmapped by the validation.
 */
export default function CodeMapsPage() {
  const [params, setParams] = useSearchParams();
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [tab, setTab] = useState<Tab>(params.get('tab') === 'unmapped' ? 'unmapped' : 'sets');
  const [filter, setFilter] = useState('');
  const sets = useQuery({ queryKey: ['migration', 'maps'], queryFn: () => migrationApi.mapSets() });
  const unmapped = useQuery({
    queryKey: ['migration', 'unmapped', companyId],
    queryFn: () => migrationApi.unmapped(companyId),
    enabled: tab === 'unmapped' && companyId > 0,
  });
  const selected = params.get('set') ?? undefined;
  const rows = (sets.data ?? []).filter(
    (s) => filter === '' || `${s.code} ${s.name}`.toLowerCase().includes(filter.toLowerCase()),
  );
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Code Maps"
        description={`Legacy codes mapped to ${BRAND.product} values, versioned and approved by the business owner.`}
      />
      <Tabs<Tab>
        tabs={[
          { id: 'sets', label: 'Code Maps' },
          { id: 'unmapped', label: 'Unmapped Codes' },
        ]}
        active={tab}
        onChange={setTab}
      />
      <ErrorAlert error={sets.error ?? unmapped.error} onRetry={() => void sets.refetch()} />
      {tab === 'sets' && (
        <>
          <Card flush>
            <div className="worklist-filters form-grid">
              <input
                className="input"
                aria-label="Search code maps"
                placeholder="Search code map"
                value={filter}
                onChange={(e) => setFilter(e.target.value)}
              />
            </div>
            <DataTable<MapSet>
              loading={sets.isLoading}
              rows={rows}
              rowKey={(s) => s.code}
              selectedKey={selected}
              onRowClick={(s) => setParams({ set: s.code })}
              emptyMessage="No code map"
              columns={[
                {
                  key: 'set',
                  header: 'Code map',
                  render: (s) => <CellStack main={s.code} sub={s.name} />,
                },
                {
                  key: 'target',
                  header: `${BRAND.product} values`,
                  render: (s) => <CellStack main={s.targetDomain} sub={migLabel(s.targetKind)} />,
                },
                { key: 'used', header: 'Used by', render: (s) => s.usedBy ?? '' },
                {
                  key: 'approved',
                  header: 'Approved version',
                  kind: 'center',
                  render: (s) =>
                    s.approvedVersion === undefined ? (
                      <Tag tone="danger">None</Tag>
                    ) : (
                      `v${String(s.approvedVersion)}`
                    ),
                },
                {
                  key: 'open',
                  header: 'Open version',
                  kind: 'status',
                  render: (s) => <MigStatus status={s.openStatus ?? undefined} />,
                },
              ]}
            />
          </Card>
          {selected !== undefined && (
            <MapSetPanel
              setCode={selected}
              canEdit={can('MIG_MAPPING_EDIT')}
              canApprove={can('MIG_MAPPING_APPROVE')}
              onClose={() => setParams({})}
            />
          )}
        </>
      )}
      {tab === 'unmapped' && (
        <Card flush>
          <DataTable<UnmappedCode>
            loading={unmapped.isLoading}
            rows={unmapped.data ?? []}
            rowKey={(u) => `${u.setCode}|${u.sourceSystem}|${u.legacyCode}`}
            emptyMessage="Every legacy code of the batches in validation is mapped"
            columns={[
              { key: 'set', header: 'Code map', kind: 'code', render: (u) => u.setCode },
              { key: 'source', header: 'Source', render: (u) => u.sourceSystem },
              { key: 'code', header: 'Legacy code', kind: 'code', render: (u) => u.legacyCode },
              { key: 'rows', header: 'Rows', kind: 'center', render: (u) => u.rows },
              { key: 'keys', header: 'Example records', render: (u) => u.sampleKeys },
              {
                key: 'severity',
                header: 'Blocks the load',
                kind: 'center',
                render: (u) => (u.error ? <Tag tone="danger">Yes</Tag> : 'No'),
              },
            ]}
          />
        </Card>
      )}
    </div>
  );
}
