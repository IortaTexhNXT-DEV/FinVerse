import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId, useDefaultBranchId, useWorkspace } from '@/context/workspaceContext';
import { CodeSelect, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { ReceiptKind, Series, SeriesBody } from './cashieringApi';
import { seriesUsedPercent } from './cashieringLogic';
import './cashiering.css';

function Gauge({ s }: Readonly<{ s: Series }>) {
  const used = seriesUsedPercent(s);
  return (
    <span className="stack">
      <span
        className={s.low ? 'csh-gauge csh-gauge-low' : 'csh-gauge'}
        role="meter"
        aria-valuenow={used}
        aria-valuemin={0}
        aria-valuemax={100}
        aria-label={`${used}% used`}
      >
        <span className="csh-gauge-fill" style={{ width: `${used}%`, display: 'block' }} />
      </span>
      <span className="cell-sub">
        {s.remaining.toLocaleString()} left{s.low ? ' · low' : ''}
      </span>
    </span>
  );
}

interface SeriesForm {
  kind: ReceiptKind;
  prefix: string;
  fromNo: string;
  toNo: string;
  atpNo: string;
  warnAt: string;
  branchId: string;
}

function SeriesDialog({
  onSave,
  onClose,
  busy,
  error,
}: Readonly<{
  onSave: (f: Omit<SeriesBody, 'companyId'>) => void;
  onClose: () => void;
  busy: boolean;
  error: unknown;
}>) {
  const { branches } = useWorkspace();
  const branchId = useDefaultBranchId();
  const [f, setF] = useState<SeriesForm>({
    kind: 'AR',
    prefix: '',
    fromNo: '1',
    toNo: '',
    atpNo: '',
    warnAt: '50',
    branchId: String(branchId),
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (k: keyof SeriesForm) => (v: string) => setF((x) => ({ ...x, [k]: v }));
  const save = () => {
    const e: Record<string, string> = {};
    if (f.prefix.trim() === '') e.prefix = 'Prefix is required';
    if (!(Number(f.toNo) >= Number(f.fromNo) && Number(f.fromNo) >= 1))
      e.toNo = 'The range must end at or after its start';
    setErrors(e);
    if (Object.keys(e).length === 0) {
      onSave({
        kind: f.kind,
        prefix: f.prefix.trim(),
        fromNo: Number(f.fromNo),
        toNo: Number(f.toNo),
        atpNo: f.atpNo || undefined,
        warnAt: Number(f.warnAt || 0),
        branchId: Number(f.branchId),
      });
    }
  };
  return (
    <Modal
      open
      title="New Receipt Series"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            Save Series
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={error} />
        <div className="form-grid">
          <CodeSelect
            label="Kind"
            required
            value={f.kind}
            options={['AR', 'OR']}
            labelOf={(c) => c}
            onChange={set('kind')}
          />
          <CodeSelect
            label="Branch"
            required
            value={f.branchId}
            options={branches.map((b) => String(b.id))}
            labelOf={(id) => branches.find((b) => String(b.id) === id)?.name ?? id}
            onChange={set('branchId')}
          />
          <TextField
            label="Prefix"
            required
            value={f.prefix}
            onChange={set('prefix')}
            error={errors.prefix}
            maxLength={20}
            placeholder="AR-HO-"
          />
          <TextField
            label="From No."
            type="number"
            required
            value={f.fromNo}
            onChange={set('fromNo')}
          />
          <TextField
            label="To No."
            type="number"
            required
            value={f.toNo}
            onChange={set('toNo')}
            error={errors.toNo}
          />
          <TextField label="BIR ATP No." value={f.atpNo} onChange={set('atpNo')} maxLength={40} />
          <TextField
            label="Warn When Remaining At"
            type="number"
            value={f.warnAt}
            onChange={set('warnAt')}
          />
        </div>
      </div>
    </Modal>
  );
}

/**
 * Receipt Series (CSHID.006/011): the AR and OR number ranges per branch with their BIR ATP, the
 * remaining count and the RECEIPT_SERIES_LOW alert. A new series is active once authorized.
 */
export default function ReceiptSeriesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const { branches } = useWorkspace();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [adding, setAdding] = useState(false);
  const list = useQuery({
    queryKey: ['cashiering', 'series', companyId],
    queryFn: () => cashieringApi.series(companyId),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<Series>) => fn(),
    onSuccess: async (s) => {
      setAdding(false);
      toast.success(`Series ${s.prefix} saved`);
      await queryClient.invalidateQueries({ queryKey: ['cashiering', 'series'] });
    },
  });
  const manage = can('CASH_SERIES_MANAGE');
  const columns: Column<Series>[] = [
    {
      key: 'prefix',
      header: 'Series',
      render: (s) => (
        <>
          <strong>{s.prefix}</strong>
          <span className="cell-sub">
            {s.kind} · ATP {s.atpNo ?? '—'}
          </span>
        </>
      ),
    },
    {
      key: 'branch',
      header: 'Branch',
      render: (s) => branches.find((b) => b.id === s.branchId)?.name ?? s.branchId,
    },
    {
      key: 'range',
      header: 'Range',
      render: (s) => `${s.fromNo.toLocaleString()} – ${s.toNo.toLocaleString()}`,
    },
    { key: 'next', header: 'Next No.', render: (s) => s.nextNo.toLocaleString() },
    { key: 'gauge', header: 'Used', render: (s) => <Gauge s={s} /> },
    { key: 'status', header: 'Status', render: (s) => <StatusBadge status={s.recordStatus} /> },
    {
      key: 'actions',
      header: '',
      render: (s) => (
        <div className="row">
          {s.recordStatus === 'PENDING_AUTHORIZATION' && (can('MASTER_AUTHORIZE') || manage) && (
            <Button size="sm" onClick={() => act.mutate(() => cashieringApi.authorizeSeries(s.id))}>
              Authorize
            </Button>
          )}
          {s.recordStatus === 'ACTIVE' && manage && (
            <Button
              size="sm"
              variant="ghost"
              onClick={() => act.mutate(() => cashieringApi.deactivateSeries(s.id))}
            >
              Deactivate
            </Button>
          )}
        </div>
      ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Receipt Series"
        description="AR and OR number ranges per branch, with the numbers left before the series runs out."
        actions={
          manage && (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setAdding(true)}>
              New Series
            </Button>
          )
        }
      />
      <ErrorAlert error={list.error ?? act.error} />
      <Card flush>
        <DataTable
          caption="Receipt series"
          columns={columns}
          rows={list.data ?? []}
          rowKey={(s) => s.id}
          loading={list.isLoading}
          emptyMessage="No receipt series yet"
        />
      </Card>
      {adding && (
        <SeriesDialog
          busy={act.isPending}
          error={act.error}
          onClose={() => setAdding(false)}
          onSave={(f) => act.mutate(() => cashieringApi.createSeries({ ...f, companyId }))}
        />
      )}
    </div>
  );
}
