import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import type { RecordOriginKind } from '@/api/types';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { OriginBadge } from '@/components/ui/OriginBadge';
import { OriginFilter } from '@/components/ui/OriginFilter';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId, useDefaultBranchId, useWorkspace } from '@/context/workspaceContext';
import { CodeSelect, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { ReceiptKind, Series, SeriesBody } from './cashieringApi';
import { seriesNumberExample, seriesUsedPercent } from './cashieringLogic';
import './cashiering.css';
import { RowActions } from '@/components/ui/RowActions';

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
  year: string;
  numberFormat: string;
}

const STANDARD_FORMAT = '{PREFIX}-{YEAR}-{BRANCH}-{SEQ}';

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
    year: String(new Date().getFullYear()),
    numberFormat: STANDARD_FORMAT,
  });
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (k: keyof SeriesForm) => (v: string) => setF((x) => ({ ...x, [k]: v }));
  const save = () => {
    const e: Record<string, string> = {};
    if (f.prefix.trim() === '') e.prefix = 'Prefix is required';
    if (!(Number(f.toNo) >= Number(f.fromNo) && Number(f.fromNo) >= 1))
      e.toNo = 'The range must end at or after its start';
    if (f.numberFormat.trim() !== '' && !f.numberFormat.includes('{SEQ}'))
      e.numberFormat = 'The number format must hold the sequence {SEQ}';
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
        seriesYear: f.year === '' ? undefined : Number(f.year),
        numberFormat: f.numberFormat.trim(),
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
          <TextField
            label="Year"
            type="number"
            value={f.year}
            onChange={set('year')}
            placeholder="Every year when blank"
          />
          <TextField
            label="Number Format"
            value={f.numberFormat}
            onChange={set('numberFormat')}
            error={errors.numberFormat}
            maxLength={60}
            placeholder="Prefix and sequence when blank"
          />
        </div>
        <p className="muted">
          First number:{' '}
          <strong>
            {seriesNumberExample(f.numberFormat, {
              prefix: f.prefix.trim(),
              year: f.year === '' ? undefined : Number(f.year),
              branchCode: branches.find((b) => String(b.id) === f.branchId)?.code,
              fromNo: Number(f.fromNo || 1),
              toNo: Number(f.toNo || f.fromNo || 1),
            })}
          </strong>
        </p>
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
  const [origin, setOrigin] = useState<RecordOriginKind>();
  const list = useQuery({
    queryKey: ['cashiering', 'series', companyId, origin],
    queryFn: () => cashieringApi.series(companyId, origin),
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
          <strong>{s.prefix}</strong> <OriginBadge record={s} />
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
    { key: 'year', header: 'Year', render: (s) => s.seriesYear ?? 'Every year' },
    { key: 'next', header: 'Next Number', render: (s) => s.nextNumber ?? 'Depleted' },
    { key: 'gauge', header: 'Used', render: (s) => <Gauge s={s} /> },
    { key: 'status', header: 'Status', render: (s) => <StatusBadge status={s.recordStatus} /> },
    {
      key: 'actions',
      header: '',
      render: (s) => (
        <RowActions
          record={s.prefix}
          actions={[
            {
              label: 'Authorize',
              hidden: !(
                s.recordStatus === 'PENDING_AUTHORIZATION' &&
                (can('MASTER_AUTHORIZE') || manage)
              ),
              confirm: {
                title: 'Authorize Receipt Series',
                effect: 'The series becomes active for issuing receipts.',
              },
              onSelect: () => act.mutateAsync(() => cashieringApi.authorizeSeries(s.id)),
            },
            {
              label: 'Deactivate',
              danger: true,
              hidden: !(s.recordStatus === 'ACTIVE' && manage),
              confirm: {
                title: 'Deactivate Receipt Series',
                effect: 'No more receipts can be issued from this series.',
                destructive: true,
              },
              onSelect: () => act.mutateAsync(() => cashieringApi.deactivateSeries(s.id)),
            },
          ]}
        />
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
      <Card>
        <OriginFilter value={origin} onChange={setOrigin} />
      </Card>
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
