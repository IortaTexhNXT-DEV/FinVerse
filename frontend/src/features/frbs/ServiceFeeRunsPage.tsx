import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Calculator, Settings2 } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
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
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, today } from '@/utils/format';
import { frbsApi } from './api';
import type { ServiceFeeRun, StageCounts } from './api';
import { RUN_TABS, periodErrors, tabOf } from './serviceFee';
import type { RunTab } from './serviceFee';
import './frbs.css';

const COLUMNS: Column<ServiceFeeRun>[] = [
  {
    key: 'run',
    header: 'Run No.',
    render: (r) => (
      <>
        <strong>{r.runNo}</strong>
        <span className="cell-sub">Computed by {r.createdBy}</span>
      </>
    ),
  },
  {
    key: 'period',
    header: 'Fully Paid',
    render: (r) => `${formatDate(r.periodFrom)} – ${formatDate(r.periodTo)}`,
  },
  { key: 'count', header: 'Invoices', numeric: true, render: (r) => r.invoiceCount },
  {
    key: 'fee',
    header: 'Service Fee',
    numeric: true,
    render: (r) => <Amount value={r.feeTotal} />,
  },
  { key: 'approved', header: 'Approved By', render: (r) => r.approvedBy ?? '—' },
  { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.stage} /> },
];

function tabsWithCounts(counts: StageCounts | undefined) {
  return RUN_TABS.map((t) => {
    const count = t.id === 'ALL' ? undefined : counts?.[t.id];
    return { id: t.id, label: count ? `${t.label} (${String(count)})` : t.label };
  });
}

function ComputeDialog({
  onDone,
  onClose,
}: Readonly<{ onDone: (run: ServiceFeeRun) => void; onClose: () => void }>) {
  const companyId = useCompanyId();
  const now = today();
  const [from, setFrom] = useState(`${now.slice(0, 8)}01`);
  const [to, setTo] = useState(now);
  const [checked, setChecked] = useState(false);
  const errors = checked ? periodErrors(from, to, now) : {};
  const compute = useMutation({
    mutationFn: () => frbsApi.compute(companyId, from, to),
    onSuccess: onDone,
  });
  return (
    <Modal
      open
      title="Compute Service Fee"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            busy={compute.isPending}
            onClick={() => {
              setChecked(true);
              if (Object.keys(periodErrors(from, to, now)).length === 0) {
                compute.mutate();
              }
            }}
          >
            Compute Service Fee
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={compute.error} />
        <p className="frbs-muted">
          The fee is computed on the commission of the invoices fully paid in the period, net of the
          insurer&apos;s withholding tax, at the rate of their segment. An invoice is paid once.
        </p>
        <div className="frbs-form">
          <Field label="Fully Paid From" required error={errors.from}>
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
          <Field label="Fully Paid To" required error={errors.to}>
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

/**
 * Service Fee Runs (FRBS 2.10.0-2.10.2): the runs by stage, from computation and approval to the
 * release and liquidation of the fee by the units; the GL officer computes a run for a period.
 */
export default function ServiceFeeRunsPage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const { can } = useAuth();
  const [params, setParams] = useSearchParams();
  const tab = tabOf(params.get('stage'));
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [computing, setComputing] = useState(false);
  const stage = tab === 'ALL' ? undefined : tab;
  const list = useQuery({
    queryKey: ['frbs', 'runs', companyId, stage, q, page],
    queryFn: () => frbsApi.runs(companyId, { stage, q, page }),
    enabled: companyId > 0,
  });
  const counts = useQuery({
    queryKey: ['frbs', 'counts', companyId],
    queryFn: () => frbsApi.counts(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Accounting Reports"
        title="Service Fee Runs"
        description="The referrers' share of fully paid commission: computed, approved, paid through Disbursement, then released and liquidated by the units."
        actions={
          <>
            <Link className="btn btn-secondary" to="/frbs/service-fee/setup">
              <Settings2 size={16} aria-hidden="true" /> Rates and Recipients
            </Link>
            {can('SERVICE_FEE_MANAGE') && (
              <Button icon={<Calculator size={16} />} onClick={() => setComputing(true)}>
                Compute Service Fee
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={list.error} />
      <Card flush>
        <div className="work-tabs">
          <Tabs<RunTab>
            tabs={tabsWithCounts(counts.data)}
            active={tab}
            onChange={(next) => {
              setPage(0);
              setParams(next === 'ALL' ? {} : { stage: next });
            }}
          />
        </div>
        <WorklistToolbar
          placeholder="Search Run No."
          onSearch={(text) => {
            setQ(text);
            setPage(0);
          }}
        />
        <DataTable
          caption="Service-fee runs"
          columns={COLUMNS}
          rows={list.data?.content ?? []}
          rowKey={(r) => r.id}
          loading={list.isLoading}
          emptyMessage="No items to display"
          onRowClick={(r) => void navigate(`/frbs/service-fee/runs/${String(r.id)}`)}
        />
        <PageFooter data={list.data} noun="runs" onPage={setPage} />
      </Card>
      {computing && (
        <ComputeDialog
          onClose={() => setComputing(false)}
          onDone={(run) => {
            setComputing(false);
            toast.success(`${run.runNo} computed: ${String(run.invoiceCount)} invoice(s)`);
            void queryClient.invalidateQueries({ queryKey: ['frbs'] });
            void navigate(`/frbs/service-fee/runs/${String(run.id)}`);
          }}
        />
      )}
    </div>
  );
}
