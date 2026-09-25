import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Calculator } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, humanize, today } from '@/utils/format';
import { commissionApi } from './commissionApi';
import type { IncentiveRun, RunLine, Scheme } from './commissionApi';

const LINE_COLUMNS: Column<RunLine>[] = [
  { key: 'invoice', header: 'Invoice No.', render: (l) => <strong>{l.invoiceNo}</strong> },
  { key: 'unit', header: 'Sales Unit', render: (l) => l.salesUnit ?? '' },
  { key: 'line', header: 'Product Line', render: (l) => l.productLine ?? '' },
  {
    key: 'basic',
    header: 'Basic Premium',
    numeric: true,
    render: (l) => <Amount value={l.basicPremium} />,
  },
  {
    key: 'gross',
    header: 'Gross Premium',
    numeric: true,
    render: (l) => <Amount value={l.grossPremium} />,
  },
  {
    key: 'excluded',
    header: 'Excluded By',
    render: (l) => (l.exclusionReason ? humanize(l.exclusionReason) : ''),
  },
  {
    key: 'incentive',
    header: 'Incentive',
    numeric: true,
    render: (l) => <Amount value={l.incentive} />,
  },
];

function columnsOf(schemes: Scheme[]): Column<IncentiveRun>[] {
  const nameOf = (id: number) => schemes.find((s) => s.id === id)?.terms.name ?? String(id);
  return [
    { key: 'no', header: 'Run No.', render: (r) => <strong>{r.runNo}</strong> },
    { key: 'scheme', header: 'Scheme', render: (r) => nameOf(r.schemeId) },
    {
      key: 'period',
      header: 'Period',
      render: (r) => `${formatDate(r.periodFrom)} – ${formatDate(r.periodTo)}`,
    },
    {
      key: 'production',
      header: 'Eligible Production',
      numeric: true,
      render: (r) => <Amount value={r.totals.eligibleProduction} />,
    },
    { key: 'tier', header: 'Tier Applied', render: (r) => r.totals.tierApplied ?? '' },
    {
      key: 'incentive',
      header: 'Incentive',
      numeric: true,
      render: (r) => <Amount value={r.totals.incentive} />,
    },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
  ];
}

function ComputeDialog({
  schemes,
  busy,
  error,
  onClose,
  onCompute,
}: Readonly<{
  schemes: Scheme[];
  busy: boolean;
  error: unknown;
  onClose: () => void;
  onCompute: (schemeId: number, from: string, to: string) => void;
}>) {
  const active = schemes.filter((s) => s.terms.active);
  const [schemeId, setSchemeId] = useState('');
  const [from, setFrom] = useState(`${today().slice(0, 7)}-01`);
  const [to, setTo] = useState(today());
  return (
    <Modal
      title="Compute Incentive"
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={busy}
            disabled={schemeId === '' || from === '' || to === ''}
            onClick={() => onCompute(Number(schemeId), from, to)}
          >
            Compute
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <Field label="Scheme" required hint="Active schemes only">
          {(id) => (
            <select
              id={id}
              className="select"
              value={schemeId}
              onChange={(e) => setSchemeId(e.target.value)}
            >
              <option value="">Select…</option>
              {active.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.terms.name}
                </option>
              ))}
            </select>
          )}
        </Field>
        <div className="form-grid">
          <Field label="Booked From" required>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
              />
            )}
          </Field>
          <Field label="Booked To" required>
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={to}
                onChange={(e) => setTo(e.target.value)}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}

function RunDialog({
  run,
  busy,
  error,
  mayPost,
  mayCancel,
  onClose,
  onPost,
  onCancel,
}: Readonly<{
  run: IncentiveRun;
  busy: boolean;
  error: unknown;
  mayPost: boolean;
  mayCancel: boolean;
  onClose: () => void;
  onPost: () => void;
  onCancel: () => void;
}>) {
  const [page, setPage] = useState(0);
  const lines = useQuery({
    queryKey: ['commission', 'run-lines', run.id, page],
    queryFn: () => commissionApi.runLines(run.id, page),
  });
  const computed = run.status === 'COMPUTED';
  const t = run.totals;
  return (
    <Modal
      title={`Incentive Run ${run.runNo}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Close
          </Button>
          {computed && mayCancel && (
            <Button variant="ghost" busy={busy} onClick={onCancel}>
              Cancel Run
            </Button>
          )}
          {computed && mayPost && t.incentive > 0 && (
            <Button busy={busy} onClick={onPost}>
              Post Incentive
            </Button>
          )}
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error ?? lines.error} />
        <p>
          {t.eligibleCount} eligible invoice(s), production {formatAmount(t.eligibleProduction)};{' '}
          {t.excludedCount} excluded ({formatAmount(t.excludedAmount)}). {t.tierApplied}. Incentive{' '}
          <strong>{formatAmount(t.incentive)}</strong>
          {t.passOn > 0 && `, passed on to the branches ${formatAmount(t.passOn)}`}.
        </p>
        {run.journalRefs && <p className="muted">Posted: {run.journalRefs}</p>}
        <DataTable
          caption="Invoices of the run"
          columns={LINE_COLUMNS}
          rows={lines.data?.content ?? []}
          rowKey={(l) => l.invoiceNo}
          loading={lines.isLoading}
          emptyMessage="No items to display"
        />
        <PageFooter data={lines.data} noun="invoices" onPage={setPage} />
      </div>
    </Modal>
  );
}

/**
 * Incentive runs (CMRID.003-006): a scheme computed on the production booked in a period, the
 * excluded invoices, then posted as incentive receivable and passed on to the branches.
 */
export default function IncentiveRunsPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [page, setPage] = useState(0);
  const [computing, setComputing] = useState(false);
  const [open, setOpen] = useState<IncentiveRun>();
  const schemes = useQuery({
    queryKey: ['commission', 'schemes', companyId],
    queryFn: () => commissionApi.schemes(companyId),
    enabled: companyId > 0,
  });
  const runs = useQuery({
    queryKey: ['commission', 'runs', companyId, page],
    queryFn: () => commissionApi.runs(companyId, undefined, page),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<IncentiveRun>) => fn(),
    onSuccess: async (r) => {
      setComputing(false);
      setOpen(r.status === 'COMPUTED' ? r : undefined);
      await queryClient.invalidateQueries({ queryKey: ['commission', 'runs'] });
      toast.success(`${r.runNo} ${humanize(r.status).toLowerCase()}`);
    },
  });
  return (
    <div className="stack">
      <PageHeader
        section="Commission Receivables"
        title="Incentive Runs"
        description="Incentive computed on the production of a period, reviewed and posted."
        actions={
          can('INCENTIVE_MANAGE') ? (
            <Button icon={<Calculator size={16} />} onClick={() => setComputing(true)}>
              Compute Incentive
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={runs.error ?? schemes.error} />
      <Card>
        <div className="stack">
          <DataTable
            caption="Incentive runs"
            columns={columnsOf(schemes.data ?? [])}
            rows={runs.data?.content ?? []}
            rowKey={(r) => r.id}
            loading={runs.isLoading}
            onRowClick={setOpen}
            emptyMessage="No items to display"
          />
          <PageFooter data={runs.data} noun="runs" onPage={setPage} />
        </div>
      </Card>
      {computing && (
        <ComputeDialog
          schemes={schemes.data ?? []}
          busy={act.isPending}
          error={act.error}
          onClose={() => setComputing(false)}
          onCompute={(schemeId, from, to) =>
            act.mutate(() => commissionApi.compute(schemeId, from, to))
          }
        />
      )}
      {open !== undefined && (
        <RunDialog
          run={open}
          busy={act.isPending}
          error={act.error}
          mayPost={can('COMMREC_APPROVE')}
          mayCancel={can('INCENTIVE_MANAGE')}
          onClose={() => setOpen(undefined)}
          onPost={() => act.mutate(() => commissionApi.postRun(open.id))}
          onCancel={() => act.mutate(() => commissionApi.cancelRun(open.id))}
        />
      )}
    </div>
  );
}
