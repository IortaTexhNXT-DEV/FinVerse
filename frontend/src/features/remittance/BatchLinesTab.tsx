import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Ban, RotateCcw } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime } from '@/utils/format';
import { remittanceApi } from './api';
import type { Batch, BatchLine } from './api';
import { ExcludeDialog } from './BatchDialogs';
import { isExcluded } from './remittanceLabels';
import './remittance.css';

function amountColumn(
  key: string,
  header: string,
  pick: (l: BatchLine) => number,
): Column<BatchLine> {
  return { key, header, numeric: true, render: (l) => <Amount value={pick(l)} /> };
}

const COLUMNS: Column<BatchLine>[] = [
  {
    key: 'inv',
    header: 'Invoice No.',
    render: (l) => (
      <span className={isExcluded(l) ? 'remit-excluded-row' : undefined}>
        <Link to={remittanceApi.invoiceLink(l.invoiceNo)} onClick={(e) => e.stopPropagation()}>
          {l.invoiceNo}
        </Link>
        <div className="remit-muted">{l.policyNo ?? l.arn}</div>
      </span>
    ),
  },
  { key: 'assured', header: 'Name of Assured', render: (l) => l.assuredName },
  { key: 'paidOn', header: 'Last Paid', render: (l) => formatDate(l.lastPaidOn) },
  amountColumn('paid', 'Paid AR', (l) => l.amounts.paidAr),
  amountColumn('comm', 'Commission', (l) => l.amounts.commission),
  amountColumn('vat', 'VAT', (l) => l.amounts.commissionVat),
  amountColumn('wtax', 'WTAX', (l) => l.amounts.wtax),
  amountColumn('inc', 'Incentive', (l) => l.amounts.incentive + l.amounts.incentiveVat),
  amountColumn('cpc2', 'CPC2', (l) => l.amounts.cpc2 + l.amounts.cpc2Vat),
  amountColumn('net', 'Net Due', (l) => l.amounts.netDue),
  {
    key: 'or',
    header: 'Insurer OR',
    render: (l) =>
      l.insurerOr ? (
        <>
          {l.insurerOr.orNo}
          <div>
            <StatusBadge status={l.insurerOr.status} />
          </div>
        </>
      ) : (
        ''
      ),
  },
];

/** The exclusions side panel: reason, user and restore (RMTID.002 addendum). */
function ExclusionsPanel({
  lines,
  canRestore,
  busy,
  onRestore,
}: Readonly<{
  lines: BatchLine[];
  canRestore: boolean;
  busy: boolean;
  onRestore: (invoiceNo: string) => void;
}>) {
  const excluded = lines.filter(isExcluded);
  return (
    <aside className="remit-panel" aria-label="Exclusions">
      <h3>Exclusions ({excluded.length})</h3>
      {excluded.length === 0 && <p className="remit-muted">No account is excluded.</p>}
      {excluded.map((l) => (
        <div key={l.invoiceNo} className="remit-exclusion">
          <strong>{l.invoiceNo}</strong>
          <span>{l.exclusion?.reason}</span>
          {l.exclusion?.comment !== undefined && (
            <span className="remit-muted">{l.exclusion.comment}</span>
          )}
          <span className="remit-muted">
            {l.exclusion?.excludedBy} · {formatDateTime(l.exclusion?.excludedAt)}
          </span>
          {canRestore && (
            <div>
              <Button
                size="sm"
                variant="secondary"
                icon={<RotateCcw size={14} />}
                busy={busy}
                onClick={() => onRestore(l.invoiceNo)}
              >
                Restore
              </Button>
            </div>
          )}
        </div>
      ))}
    </aside>
  );
}

/**
 * The accounts of a batch with their read-only amounts (RMTID.002 addendum): select accounts to
 * exclude them with a reason; the Exclusions panel shows who excluded what and restores it until
 * the batch is submitted.
 */
export function BatchLinesTab({
  batch,
  editable,
  onChanged,
}: Readonly<{ batch: Batch; editable: boolean; onChanged: (b: Batch) => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const selection = useRowSelection();
  const [text, setText] = useState('');
  const [excluding, setExcluding] = useState(false);
  const id = batch.summary.id;
  const done = async (b: Batch, message: string) => {
    selection.clear();
    setExcluding(false);
    onChanged(b);
    await queryClient.invalidateQueries({ queryKey: ['remittance'] });
    toast.success(message);
  };
  const exclude = useMutation({
    mutationFn: (v: { reason: string; comment?: string }) =>
      remittanceApi.exclude(id, selection.keys, v.reason, v.comment),
    onSuccess: (b) => done(b, 'Accounts excluded'),
  });
  const restore = useMutation({
    mutationFn: (invoiceNo: string) => remittanceApi.restore(id, invoiceNo),
    onSuccess: (b) => done(b, 'Account restored'),
  });
  const needle = text.toLowerCase();
  const shown = batch.lines.filter(
    (l) =>
      needle === '' ||
      l.invoiceNo.toLowerCase().includes(needle) ||
      l.assuredName.toLowerCase().includes(needle),
  );
  const kept = shown.filter((l) => !isExcluded(l));
  const columns = editable
    ? [
        selectionColumn(
          kept,
          (l) => l.invoiceNo,
          selection,
          (l) => l.invoiceNo,
        ),
        ...COLUMNS,
      ]
    : COLUMNS;
  return (
    <div className="remit-split">
      <div className="stack">
        <ErrorAlert error={restore.error} />
        <WorklistToolbar placeholder="Search Invoice No. or Assured" onSearch={setText}>
          {editable && (
            <Button
              variant="danger"
              icon={<Ban size={16} />}
              disabled={selection.keys.length === 0}
              onClick={() => setExcluding(true)}
            >
              Exclude Selected
            </Button>
          )}
        </WorklistToolbar>
        <DataTable
          caption="Accounts of the batch"
          columns={columns}
          rows={shown}
          rowKey={(l) => l.invoiceNo}
        />
      </div>
      <ExclusionsPanel
        lines={batch.lines}
        canRestore={editable}
        busy={restore.isPending}
        onRestore={(invoiceNo) => restore.mutate(invoiceNo)}
      />
      {excluding && (
        <ExcludeDialog
          count={selection.keys.length}
          busy={exclude.isPending}
          error={exclude.error}
          onClose={() => setExcluding(false)}
          onExclude={(reason, comment) => exclude.mutate({ reason, comment })}
        />
      )}
    </div>
  );
}
