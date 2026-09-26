import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ReceiptText } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize, today } from '@/utils/format';
import { DialogFooter, InputField } from '../plans/Parts';
import type { Statement } from './api';
import { billingApi } from './api';
import type { StatementTab } from './labels';
import { STATEMENT_TABS, cycleText, periodError, statusesOfTab } from './labels';

const COLUMNS: Column<Statement>[] = [
  {
    key: 'no',
    header: 'SOA No.',
    render: (s) => (
      <>
        <strong>{s.soaNo}</strong>
        <div className="muted">Cycle {s.cycleSeq}</div>
      </>
    ),
  },
  { key: 'arn', header: 'ARN', render: (s) => s.arn },
  { key: 'assured', header: 'Name of Assured', render: (s) => s.assuredName },
  { key: 'freq', header: 'Frequency', render: (s) => humanize(s.frequency) },
  { key: 'cycle', header: 'Billing Cycle', render: (s) => cycleText(s) },
  { key: 'due', header: 'Due Date', render: (s) => formatDate(s.dueDate) },
  { key: 'bal', header: 'Amount Due', numeric: true, render: (s) => <Amount value={s.balance} /> },
  { key: 'status', header: 'Status', render: (s) => <StatusBadge status={s.status} /> },
];

/** The billing run: the statements of every billing cycle falling due in a period. */
function BillingRunDialog({
  busy,
  error,
  onClose,
  onRun,
}: Readonly<{
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onRun: (from: string, to: string) => void;
}>) {
  const [from, setFrom] = useState(today());
  const [to, setTo] = useState(today());
  const [problem, setProblem] = useState<string>();
  const run = () => {
    const found = periodError(from, to);
    setProblem(found);
    if (found === undefined) {
      onRun(from, to);
    }
  };
  return (
    <Modal
      title="Billing Run"
      open
      onClose={onClose}
      footer={
        <DialogFooter label="Generate Statements" busy={busy} onClose={onClose} onConfirm={run} />
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <p className="muted">
          One statement of account for each billing cycle of the live plans falling due in the
          period and not billed yet.
        </p>
        <div className="grid-2">
          <InputField label="Due From" type="date" value={from} onChange={setFrom} required />
          <InputField
            label="Due To"
            type="date"
            value={to}
            onChange={setTo}
            required
            error={problem}
          />
        </div>
      </div>
    </Modal>
  );
}

/**
 * Statements of account (BRCLXN.058/060): one per billing cycle of a multi-year or installment
 * plan, to send or sent; the billing run generates the statements of the cycles due in a period.
 */
export default function BillingStatementsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<StatementTab>('GENERATED');
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [running, setRunning] = useState(false);
  const rows = useQuery({
    queryKey: ['collections', 'statements', companyId, tab, query, page],
    queryFn: () => billingApi.statements(companyId, statusesOfTab(tab), query, page),
    enabled: companyId > 0,
  });
  const run = useMutation({
    mutationFn: (v: { from: string; to: string }) =>
      billingApi.generateDue(companyId, v.from, v.to),
    onSuccess: async (generated) => {
      setRunning(false);
      await queryClient.invalidateQueries({ queryKey: ['collections', 'statements'] });
      toast.success(`${generated.length} statement(s) of account generated`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        title="Billing Statements"
        description="Statements of account per billing cycle of multi-year and installment accounts. Billing is monitoring only: it creates no receivable."
        actions={
          can('CLX_BILLING') ? (
            <Button
              variant="accent"
              icon={<ReceiptText size={16} />}
              onClick={() => setRunning(true)}
            >
              Billing Run
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={rows.error} />
      <Card flush>
        <div>
          <Tabs
            tabs={STATEMENT_TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <WorklistToolbar
            placeholder="Search SOA No., ARN or Client"
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
          />
          <DataTable
            caption="Statements of account"
            columns={COLUMNS}
            rows={rows.data?.content ?? []}
            rowKey={(s) => s.id}
            loading={rows.isLoading}
            emptyMessage="No statements of account to display"
            onRowClick={(s) => void navigate(`/collections/billing/${s.id}`)}
          />
          <PageFooter data={rows.data} noun="statements" onPage={setPage} />
        </div>
      </Card>
      {running && (
        <BillingRunDialog
          busy={run.isPending}
          error={run.error}
          onClose={() => setRunning(false)}
          onRun={(from, to) => run.mutate({ from, to })}
        />
      )}
    </div>
  );
}
