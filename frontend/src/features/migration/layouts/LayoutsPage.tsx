import { useQuery } from '@tanstack/react-query';
import { Download, FileSpreadsheet, Lock } from 'lucide-react';
import { useState } from 'react';
import { migrationApi } from '@/api/migration';
import type { Layout, LayoutColumn, MaskingRule, Rule } from '@/api/migration';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { CellStack } from '@/components/ui/CellStack';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { Tag } from '@/components/ui/Tag';
import { formatDateTime } from '@/utils/format';
import { ActionConfirm } from '../common/ActionConfirm';
import type { MigAction } from '../common/ActionConfirm';
import { MigStatus } from '../common/MigStatus';
import { MIG_SECTION, migLabel, withDetail } from '../common/migrationCodes';
import { useDownload } from '../common/useDownload';
import '../migration.css';

type Tab = 'layouts' | 'rules' | 'masking';

/**
 * Layouts and Rules (DATA_MIGRATION_DESIGN sections 5.1, 5.3 and 8): the extract layouts with their
 * versions and columns - the single source of the load templates, exported per object and as one
 * workbook with the control file and the filling instructions - the data-quality rules and the
 * masking rules of non-production extracts.
 */
export default function LayoutsPage() {
  const { can } = useAuth();
  const download = useDownload();
  const [tab, setTab] = useState<Tab>('layouts');
  const [layoutId, setLayoutId] = useState<number>();
  const [action, setAction] = useState<MigAction>();
  const layouts = useQuery({
    queryKey: ['migration', 'layouts'],
    queryFn: () => migrationApi.layouts(),
  });
  const columns = useQuery({
    queryKey: ['migration', 'layouts', layoutId, 'columns'],
    queryFn: () => migrationApi.layoutColumns(layoutId ?? 0),
    enabled: layoutId !== undefined,
  });
  const layout = layouts.data?.find((l) => l.id === layoutId);
  return (
    <div className="stack">
      <PageHeader
        section={MIG_SECTION}
        title="Layouts and Rules"
        description="Extract layouts in force, their load templates, the data-quality and masking rules."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<Download size={16} />}
              busy={download.busy}
              onClick={() => download.run(() => migrationApi.controlTemplate())}
            >
              Control File Template
            </Button>
            <Button
              variant="primary"
              icon={<FileSpreadsheet size={16} />}
              busy={download.busy}
              onClick={() => download.run(() => migrationApi.workbook())}
            >
              Load Templates Workbook
            </Button>
          </>
        }
      />
      <Tabs<Tab>
        tabs={[
          { id: 'layouts', label: 'Layouts' },
          { id: 'rules', label: 'Data-Quality Rules' },
          { id: 'masking', label: 'Masking Rules' },
        ]}
        active={tab}
        onChange={setTab}
      />
      <ErrorAlert
        error={layouts.error ?? columns.error ?? download.error}
        onRetry={() => void layouts.refetch()}
      />
      {tab === 'layouts' && (
        <>
          <Card flush>
            <DataTable<Layout>
              loading={layouts.isLoading}
              rows={layouts.data ?? []}
              rowKey={(l) => l.id}
              selectedKey={layoutId}
              onRowClick={(l) => setLayoutId(l.id)}
              emptyMessage="No layout"
              columns={[
                {
                  key: 'layout',
                  header: 'Layout',
                  render: (l) => (
                    <CellStack main={`${l.code} v${String(l.versionNo)}`} sub={l.title} />
                  ),
                },
                { key: 'object', header: 'Object', kind: 'code', render: (l) => l.objectCode },
                {
                  key: 'keys',
                  header: 'Key columns',
                  defaultHidden: true,
                  render: (l) => l.keyColumns,
                },
                {
                  key: 'hash',
                  header: 'Hash total',
                  defaultHidden: true,
                  render: (l) => migLabel(l.hashRule),
                },
                {
                  key: 'frozen',
                  header: 'Frozen',
                  kind: 'datetime',
                  render: (l) => formatDateTime(l.frozenAt),
                },
                {
                  key: 'status',
                  header: 'Status',
                  kind: 'status',
                  render: (l) => <MigStatus status={l.status} />,
                },
                {
                  key: 'template',
                  header: '',
                  render: (l) => (
                    <Button
                      variant="ghost"
                      size="sm"
                      aria-label={`Excel load template of ${l.code}`}
                      icon={<Download size={14} />}
                      onClick={(e) => {
                        e.stopPropagation();
                        download.run(() => migrationApi.layoutTemplate(l.code));
                      }}
                    />
                  ),
                },
              ]}
            />
          </Card>
          {layout !== undefined && (
            <Card
              title={`Columns of ${layout.code} v${String(layout.versionNo)}`}
              actions={
                <span className="mig-actions">
                  {can('MIG_MAPPING_EDIT') && layout.status === 'DRAFT' && (
                    <Button
                      variant="primary"
                      size="sm"
                      icon={<Lock size={14} />}
                      onClick={() =>
                        setAction({
                          title: `Freeze layout ${layout.code} v${String(layout.versionNo)}`,
                          effect:
                            'The layout becomes the version in force: extracts and templates follow it and the previous version is retired.',
                          confirmLabel: 'Freeze',
                          done: 'Layout frozen',
                          run: () => migrationApi.freezeLayout(layout.id),
                        })
                      }
                    >
                      Freeze
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="sm"
                    icon={<Download size={14} />}
                    onClick={() => download.run(() => migrationApi.layoutCsv(layout.code))}
                  >
                    CSV Layout
                  </Button>
                  <Button
                    variant="secondary"
                    size="sm"
                    icon={<FileSpreadsheet size={14} />}
                    onClick={() =>
                      download.run(() => migrationApi.objectTemplate(layout.objectCode))
                    }
                  >
                    Object Workbook
                  </Button>
                </span>
              }
            >
              <DataTable<LayoutColumn>
                loading={columns.isLoading}
                rows={columns.data ?? []}
                rowKey={(c) => c.seq}
                emptyMessage="No column"
                columns={[
                  { key: 'seq', header: '#', kind: 'center', render: (c) => c.seq },
                  {
                    key: 'name',
                    header: 'Column',
                    render: (c) => <CellStack main={c.name} sub={c.description} />,
                  },
                  {
                    key: 'type',
                    header: 'Type',
                    render: (c) => withDetail(migLabel(c.dataType), c.length),
                  },
                  {
                    key: 'mandatory',
                    header: 'Mandatory',
                    kind: 'center',
                    render: (c) => (c.mandatory === 'Y' ? <Tag tone="info">Yes</Tag> : 'No'),
                  },
                  { key: 'map', header: 'Code map', kind: 'code', render: (c) => c.mapSet ?? '' },
                  { key: 'format', header: 'Format', render: (c) => c.format ?? '' },
                  { key: 'example', header: 'Example', render: (c) => c.example ?? '' },
                  { key: 'validation', header: 'Validation', render: (c) => c.validation ?? '' },
                ]}
              />
            </Card>
          )}
        </>
      )}
      {tab === 'rules' && <RulesTab />}
      {tab === 'masking' && <MaskingTab />}
      <ActionConfirm action={action} onClose={() => setAction(undefined)} />
    </div>
  );
}

function RulesTab() {
  const rules = useQuery({ queryKey: ['migration', 'rules'], queryFn: () => migrationApi.rules() });
  return (
    <>
      <ErrorAlert error={rules.error} />
      <Card flush>
        <DataTable<Rule>
          loading={rules.isLoading}
          rows={rules.data ?? []}
          rowKey={(r) => r.code}
          emptyMessage="No rule"
          columns={[
            { key: 'code', header: 'Rule', kind: 'code', render: (r) => r.code },
            { key: 'scope', header: 'Layouts', render: (r) => r.layoutScope },
            { key: 'columns', header: 'Columns', render: (r) => r.columns },
            {
              key: 'desc',
              header: 'Check',
              render: (r) => <CellStack main={r.description} sub={r.message} />,
            },
            {
              key: 'severity',
              header: 'Severity',
              render: (r) => (
                <Tag tone={r.severity === 'ERROR' ? 'danger' : 'neutral'}>
                  {migLabel(r.severity)}
                </Tag>
              ),
            },
            { key: 'fixed', header: 'Fixed by', render: (r) => r.fixedBy ?? '' },
            {
              key: 'active',
              header: 'Active',
              kind: 'center',
              render: (r) => (r.active ? 'Yes' : 'No'),
            },
          ]}
        />
      </Card>
    </>
  );
}

function MaskingTab() {
  const masking = useQuery({
    queryKey: ['migration', 'masking'],
    queryFn: () => migrationApi.maskingRules(),
  });
  return (
    <>
      <ErrorAlert error={masking.error} />
      <Card flush>
        <DataTable<MaskingRule>
          loading={masking.isLoading}
          rows={masking.data ?? []}
          rowKey={(m) => m.id}
          emptyMessage="No masking rule"
          columns={[
            { key: 'layout', header: 'Layout', kind: 'code', render: (m) => m.layoutCode },
            { key: 'column', header: 'Column', render: (m) => m.columnName },
            { key: 'rule', header: 'Masking', render: (m) => migLabel(m.rule) },
            {
              key: 'active',
              header: 'Active',
              kind: 'center',
              render: (m) => (m.active ? 'Yes' : 'No'),
            },
          ]}
        />
      </Card>
    </>
  );
}
