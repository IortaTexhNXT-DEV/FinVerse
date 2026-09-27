import { useQuery } from '@tanstack/react-query';
import { Download, FileSpreadsheet } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { migrationApi } from '@/api/migration';
import type { DataObject } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tag } from '@/components/ui/Tag';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION, migLabel } from '../common/migrationCodes';
import { useDownload } from '../common/useDownload';
import '../migration.css';
import { ObjectPanel } from './ObjectPanel';
import { UserName } from '@/components/ui/UserName';

const CATEGORIES = ['REFERENCE', 'CLIENT', 'POLICY', 'OPEN_ITEM', 'GL', 'HISTORY'];

/**
 * Data Objects (DATA_MIGRATION_DESIGN sections 2 and 22): the register of the legacy data objects
 * with their class (migrate, carry forward, archive, excluded or conditional), the four criteria,
 * owners, dependencies and load order; the decision of the business owner (gate G1) and the load
 * template of each object.
 */
export default function ObjectsPage() {
  const [params, setParams] = useSearchParams();
  const [category, setCategory] = useState('');
  const { can } = useAuth();
  const download = useDownload();
  const objects = useQuery({
    queryKey: ['migration', 'objects'],
    queryFn: () => migrationApi.objects(),
  });
  const selected = params.get('object') ?? undefined;
  const rows = (objects.data ?? []).filter((o) => category === '' || o.category === category);
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Data Objects"
        description="What moves from the legacy systems, in which class and in which order."
        actions={
          <Button
            variant="secondary"
            icon={<FileSpreadsheet size={16} />}
            busy={download.busy}
            onClick={() => download.run(() => migrationApi.workbook())}
          >
            All Load Templates
          </Button>
        }
      />
      <ErrorAlert error={objects.error ?? download.error} onRetry={() => void objects.refetch()} />
      <Card flush>
        <div className="worklist-filters form-grid">
          <Field label="Category">
            {(id) => (
              <select
                id={id}
                className="select"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              >
                <option value="">All categories</option>
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {migLabel(c)}
                  </option>
                ))}
              </select>
            )}
          </Field>
        </div>
        <DataTable<DataObject>
          loading={objects.isLoading}
          rows={rows}
          rowKey={(o) => o.code}
          selectedKey={selected}
          onRowClick={(o) => setParams({ object: o.code })}
          emptyMessage="No data object in the register"
          columns={[
            { key: 'order', header: 'Order', kind: 'center', render: (o) => o.loadOrder },
            {
              key: 'object',
              header: 'Object',
              render: (o) => <CellStack main={`${o.code} ${o.name}`} sub={migLabel(o.category)} />,
            },
            { key: 'sources', header: 'Sources', render: (o) => o.sourceSystems.join(', ') },
            {
              key: 'class',
              header: 'Class',
              render: (o) => (
                <CellStack
                  main={migLabel(o.decidedClass ?? o.proposedClass)}
                  sub={o.decidedClass === undefined ? 'Proposed' : 'Decided'}
                />
              ),
            },
            {
              key: 'flags',
              header: '',
              render: (o) => (
                <span className="mig-actions">
                  {o.financial && <Tag tone="info">Financial</Tag>}
                  {o.dependsOn.length > 0 && (
                    <Tag tone="neutral">After {o.dependsOn.join(', ')}</Tag>
                  )}
                </span>
              ),
            },
            {
              key: 'owner',
              header: 'Business owner',
              render: (o) => <UserName login={o.businessOwner} empty="" />,
            },
            {
              key: 'status',
              header: 'Status',
              kind: 'status',
              render: (o) => <MigStatus status={o.status} />,
            },
            {
              key: 'template',
              header: '',
              render: (o) => (
                <Button
                  variant="ghost"
                  size="sm"
                  aria-label={`Load template of ${o.code}`}
                  icon={<Download size={14} />}
                  onClick={(e) => {
                    e.stopPropagation();
                    download.run(() => migrationApi.objectTemplate(o.code));
                  }}
                />
              ),
            },
          ]}
        />
      </Card>
      {selected !== undefined && (
        <ObjectPanel
          code={selected}
          canManage={can('MIG_OBJECT_MANAGE')}
          canDecide={can('MIG_DECISION_APPROVE')}
          onClose={() => setParams({})}
        />
      )}
    </div>
  );
}
