import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { frbsApi } from './api';
import type { Schedule } from './api';
import { ScheduleEditor } from './ScheduleEditor';
import { ScheduleRunner } from './ScheduleRunner';
import { FAMILY_LABELS } from './schedules';
import './frbs.css';

type TabId = 'run' | 'definitions';

const COLUMNS: Column<Schedule>[] = [
  {
    key: 'code',
    header: 'Schedule',
    render: (s) => (
      <>
        <strong>{s.code}</strong>
        <span className="cell-sub">{s.values.name}</span>
      </>
    ),
  },
  { key: 'family', header: 'Family', render: (s) => FAMILY_LABELS[s.values.family] },
  {
    key: 'accounts',
    header: 'Accounts',
    render: (s) => (
      <>
        {s.values.accountSelector}
        <span className="cell-sub">
          {humanize(s.values.grouping)} rows{s.values.currency ? ` · ${s.values.currency}` : ''}
        </span>
      </>
    ),
  },
  {
    key: 'figures',
    header: 'Figures',
    render: (s) => (
      <>
        {s.values.columns.map((c) => c.label).join(', ')}
        <span className="cell-sub">
          {s.values.ageingSlots ? `Ageing ${s.values.ageingSlots}` : humanize(s.values.basis)}
          {s.values.comparative === 'NONE' ? '' : ` · ${humanize(s.values.comparative)}`}
        </span>
      </>
    ),
  },
  {
    key: 'layout',
    header: 'Layout',
    render: (s) => (
      <span className="tag-list">
        <StatusBadge status={s.values.layoutStatus} />
        {!s.values.active && <StatusBadge status="INACTIVE" />}
      </span>
    ),
  },
];

/**
 * Account Schedules (FRBS 3.2.0, Appendix A II-IV; report list #9, #45, #46): run a schedule of the
 * report pack and export it, keep the commentary of the variance analyses, and maintain the
 * definitions - accounts, rows, figures, ageing and comparative - as configuration.
 */
export default function SchedulesPage() {
  const [params] = useSearchParams();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<TabId>('run');
  const [editing, setEditing] = useState<Schedule | 'new'>();
  const schedules = useQuery({
    queryKey: ['frbs', 'schedules'],
    queryFn: () => frbsApi.schedules(),
  });
  const all = schedules.data ?? [];
  const maintain = can('MASTER_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        backTo="/frbs"
        section="Finance · Accounting Reports"
        title="Account Schedules"
        description="GARD, subsidiaries and ageing schedules of the report pack, run from their definitions and exported to Excel or PDF."
        actions={
          maintain && (
            <Button icon={<Plus size={16} />} onClick={() => setEditing('new')}>
              New Schedule
            </Button>
          )
        }
      />
      <ErrorAlert error={schedules.error} />
      <Tabs<TabId>
        tabs={[
          { id: 'run', label: 'Run Schedule' },
          { id: 'definitions', label: `Definitions (${String(all.length)})` },
        ]}
        active={tab}
        onChange={setTab}
      />
      {tab === 'run' && schedules.isLoading && <span className="spinner" aria-label="Loading" />}
      {tab === 'run' && schedules.data && (
        <ScheduleRunner
          schedules={all.filter((s) => s.values.active)}
          initialCode={params.get('code') ?? ''}
        />
      )}
      {tab === 'definitions' && (
        <Card flush>
          <DataTable
            caption="Account schedule definitions"
            columns={COLUMNS}
            rows={all}
            rowKey={(s) => s.code}
            loading={schedules.isLoading}
            emptyMessage="No schedules defined"
            onRowClick={maintain ? (s) => setEditing(s) : undefined}
          />
        </Card>
      )}
      {editing !== undefined && (
        <ScheduleEditor
          schedule={editing === 'new' ? undefined : editing}
          onClose={() => setEditing(undefined)}
          onSaved={(s) => {
            setEditing(undefined);
            toast.success(`Schedule ${s.code} saved`);
            void queryClient.invalidateQueries({ queryKey: ['frbs', 'schedules'] });
          }}
        />
      )}
    </div>
  );
}
