import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { History, Play, Plus } from 'lucide-react';
import { useState } from 'react';
import { recurringApi } from '@/api/journalAutomation';
import type { RecurringTemplate } from '@/api/journalAutomation';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId, useDefaultBranchId, useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { HistoryDialog, RunDialog, TemplateDialog } from './RecurringDialogs';
import { describeSchedule, newTemplate, templateForm } from './recurringModel';
import type { TemplateForm } from './recurringModel';

const COLUMNS: Column<RecurringTemplate>[] = [
  { key: 'n', header: 'Name', render: (t) => <strong>{t.name}</strong> },
  { key: 'y', header: 'Type', render: (t) => humanize(t.journalType) },
  { key: 's', header: 'Schedule', render: (t) => describeSchedule(t.frequency, t.dayOfMonth) },
  {
    key: 'a',
    header: 'Amount',
    numeric: true,
    render: (t) =>
      formatAmount(t.lines.filter((l) => l.side === 'DEBIT').reduce((s, l) => s + l.amount, 0)),
  },
  { key: 'l', header: 'Last generated', render: (t) => formatDate(t.lastOccurrenceDate) },
  { key: 'x', header: 'Next', render: (t) => formatDate(t.nextOccurrence) },
  {
    key: 'f',
    header: 'Options',
    render: (t) =>
      [t.autoReverse ? 'Auto-reverse' : '', t.autoSubmit ? 'Auto-submit' : '']
        .filter(Boolean)
        .join(', '),
  },
  {
    key: 'st',
    header: 'Status',
    render: (t) => <StatusBadge status={t.active ? 'ACTIVE' : 'INACTIVE'} />,
  },
];

/**
 * Recurring and accrual journal templates. Due occurrences are generated daily by the
 * RECURRING_JOURNALS job (or with "Run now"); each occurrence is generated exactly once.
 */
export default function RecurringJournalsPage() {
  const companyId = useCompanyId();
  const { company } = useWorkspace();
  const defaultBranch = useDefaultBranchId();
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<TemplateForm | null>(null);
  const [running, setRunning] = useState<RecurringTemplate | null>(null);
  const [historyOf, setHistoryOf] = useState<RecurringTemplate | null>(null);
  const canCreate = can('JOURNAL_CREATE');

  const templates = useQuery({
    queryKey: ['recurring', companyId],
    queryFn: () => recurringApi.list(companyId),
    enabled: companyId > 0,
  });
  const toggle = useMutation({
    mutationFn: (t: RecurringTemplate) => recurringApi.setActive(t.id, !t.active),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['recurring'] }),
  });

  const actions: Column<RecurringTemplate> = {
    key: 'act',
    header: 'Actions',
    render: (t) => (
      <div className="row">
        <Button
          size="sm"
          variant="ghost"
          icon={<History size={14} />}
          aria-label={`History of ${t.name}`}
          onClick={(e) => {
            e.stopPropagation();
            setHistoryOf(t);
          }}
        />
        {canCreate && t.active && (
          <Button
            size="sm"
            variant="secondary"
            icon={<Play size={14} />}
            onClick={(e) => {
              e.stopPropagation();
              setRunning(t);
            }}
          >
            Run now
          </Button>
        )}
        {canCreate && (
          <Button
            size="sm"
            variant="ghost"
            onClick={(e) => {
              e.stopPropagation();
              toggle.mutate(t);
            }}
          >
            {t.active ? 'Deactivate' : 'Activate'}
          </Button>
        )}
      </div>
    ),
  };

  return (
    <div className="stack">
      <PageHeader
        section="General Ledger"
        title="Recurring Journals"
        description="Standing and accrual journals generated automatically on schedule as drafts (or submitted for approval). Accruals can reverse on the first day of the next period."
        actions={
          canCreate && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => setForm(newTemplate(defaultBranch, company?.baseCurrency ?? 'PHP'))}
            >
              New template
            </Button>
          )
        }
      />
      <ErrorAlert error={templates.error ?? toggle.error} />
      <Card flush>
        <DataTable<RecurringTemplate>
          loading={templates.isLoading}
          rows={templates.data ?? []}
          rowKey={(t) => t.id}
          onRowClick={canCreate ? (t) => setForm(templateForm(t)) : undefined}
          emptyMessage="No recurring templates yet."
          columns={[...COLUMNS, actions]}
        />
      </Card>
      <TemplateDialog form={form} onChange={setForm} onClose={() => setForm(null)} />
      <RunDialog template={running} onClose={() => setRunning(null)} />
      <HistoryDialog template={historyOf} onClose={() => setHistoryOf(null)} />
    </div>
  );
}
