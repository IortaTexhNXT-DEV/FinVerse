import { useMutation } from '@tanstack/react-query';
import { Plus, RotateCcw, Save, SplitSquareHorizontal, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { useToast } from '@/components/ui/toastContext';
import { disbursementApi } from './api';
import type { AllocationRow, Line, Side, Voucher } from './api';
import { entryTotals } from './labels';
import { DialogFooter } from './VoucherDialogs';
import './disbursement.css';

const READ_COLUMNS: Column<Line>[] = [
  { key: 'side', header: 'Side', render: (l) => (l.side === 'DEBIT' ? 'Debit' : 'Credit') },
  { key: 'account', header: 'Account', render: (l) => l.accountCode },
  { key: 'party', header: 'Party', render: (l) => l.partyCode ?? '' },
  { key: 'cc', header: 'Cost Centre', render: (l) => l.costCenter ?? '' },
  {
    key: 'debit',
    header: 'Debit',
    numeric: true,
    render: (l) => (l.side === 'DEBIT' ? <Amount value={l.amount} /> : ''),
  },
  {
    key: 'credit',
    header: 'Credit',
    numeric: true,
    render: (l) => (l.side === 'CREDIT' ? <Amount value={l.amount} /> : ''),
  },
  {
    key: 'origin',
    header: 'Origin',
    render: (l) => (l.origin === 'RULE' ? 'From rule' : <span className="tag">{l.origin}</span>),
  },
  { key: 'narration', header: 'Narration', render: (l) => l.narration ?? '' },
];

function Balance({ lines }: Readonly<{ lines: readonly Line[] }>) {
  const t = entryTotals(lines);
  return (
    <div className="dsb-balance" aria-live="polite">
      <span>
        Debits{' '}
        <strong>
          <Amount value={t.debit} />
        </strong>
      </span>
      <span>
        Credits{' '}
        <strong>
          <Amount value={t.credit} />
        </strong>
      </span>
      <span>{t.balanced ? 'Balanced' : 'Not balanced'}</span>
    </div>
  );
}

function LineEditor({
  line,
  onChange,
  onRemove,
}: Readonly<{ line: Line; onChange: (l: Line) => void; onRemove: () => void }>) {
  return (
    <tr>
      <td>
        <select
          aria-label="Side"
          className="select"
          value={line.side}
          onChange={(e) => onChange({ ...line, side: e.target.value as Side })}
        >
          <option value="DEBIT">Debit</option>
          <option value="CREDIT">Credit</option>
        </select>
      </td>
      <td>
        <input
          aria-label="Account"
          className="input"
          value={line.accountCode}
          onChange={(e) => onChange({ ...line, accountCode: e.target.value })}
        />
      </td>
      <td>
        <input
          aria-label="Party"
          className="input"
          value={line.partyCode ?? ''}
          onChange={(e) => onChange({ ...line, partyCode: e.target.value || undefined })}
        />
      </td>
      <td>
        <input
          aria-label="Cost centre"
          className="input"
          value={line.costCenter ?? ''}
          onChange={(e) => onChange({ ...line, costCenter: e.target.value || undefined })}
        />
      </td>
      <td>
        <input
          aria-label="Amount"
          type="number"
          step="0.01"
          min="0.01"
          className="input"
          value={line.amount}
          onChange={(e) => onChange({ ...line, amount: Number(e.target.value) })}
        />
      </td>
      <td>
        <Button
          variant="ghost"
          size="sm"
          aria-label="Remove line"
          icon={<Trash2 size={16} />}
          onClick={onRemove}
        />
      </td>
    </tr>
  );
}

/** The expense allocation (DIS 2.7.10): account, cost centre and amount rows adding up to the gross. */
function AllocationDialog({
  voucher,
  onClose,
  onDone,
}: Readonly<{ voucher: Voucher; onClose: () => void; onDone: (v: Voucher) => void }>) {
  const [rows, setRows] = useState<AllocationRow[]>([
    { accountCode: '', amount: voucher.summary.gross },
  ]);
  const allocate = useMutation({
    mutationFn: () => disbursementApi.allocate(voucher.summary.id, rows),
    onSuccess: onDone,
  });
  const total = rows.reduce((sum, r) => sum + Math.round(r.amount * 100), 0) / 100;
  const set = (i: number, patch: Partial<AllocationRow>) =>
    setRows(rows.map((r, j) => (j === i ? { ...r, ...patch } : r)));
  const complete =
    total === voucher.summary.gross && rows.every((r) => r.accountCode.trim() !== '');
  return (
    <Modal
      open
      title={`Allocate Expenses of ${voucher.summary.dvNo}`}
      onClose={onClose}
      footer={
        <DialogFooter
          label="Apply Allocation"
          busy={allocate.isPending}
          disabled={!complete}
          onClose={onClose}
          onConfirm={() => allocate.mutate()}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={allocate.error} />
        <p>
          The rows replace the debit of the disbursement type and must add up to the gross amount (
          <Amount value={voucher.summary.gross} />
          ). Allocated: <Amount value={total} />.
        </p>
        {rows.map((r, i) => (
          <div className="dsb-form" key={`row-${i}`}>
            <Field label={`Account ${i + 1}`} required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={r.accountCode}
                  onChange={(e) => set(i, { accountCode: e.target.value })}
                />
              )}
            </Field>
            <Field label="Cost Centre">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  value={r.costCenter ?? ''}
                  onChange={(e) => set(i, { costCenter: e.target.value || undefined })}
                />
              )}
            </Field>
            <Field label="Amount" required>
              {(id) => (
                <input
                  id={id}
                  type="number"
                  step="0.01"
                  className="input"
                  value={r.amount}
                  onChange={(e) => set(i, { amount: Number(e.target.value) })}
                />
              )}
            </Field>
          </div>
        ))}
        <div>
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={16} />}
            onClick={() => setRows([...rows, { accountCode: '', amount: 0 }])}
          >
            Add Row
          </Button>
        </div>
      </div>
    </Modal>
  );
}

/**
 * The proforma entry of a voucher (DIS 2.7.6, 2.7.10): built from the accounting rule, editable
 * line by line by the processor while the voucher is in process (lines that differ from the rule
 * are marked Edited for the approver), rebuilt from the rule, or built from an expense allocation.
 */
export function ProformaTab({
  voucher,
  editable,
  onSaved,
}: Readonly<{ voucher: Voucher; editable: boolean; onSaved: (v: Voucher) => void }>) {
  const toast = useToast();
  const [lines, setLines] = useState<Line[]>(voucher.lines);
  const [allocating, setAllocating] = useState(false);
  const save = useMutation({
    mutationFn: () => disbursementApi.proforma(voucher.summary.id, lines),
    onSuccess: (v) => {
      setLines(v.lines);
      onSaved(v);
      toast.success(v.summary.proformaEdited ? 'Edited entry saved' : 'Entry saved');
    },
  });
  const reset = useMutation({
    mutationFn: () => disbursementApi.resetProforma(voucher.summary.id),
    onSuccess: (v) => {
      setLines(v.lines);
      onSaved(v);
      toast.success('Entry rebuilt from the rule');
    },
  });
  const title = voucher.summary.proformaEdited ? 'Accounting Entry (Edited)' : 'Accounting Entry';
  if (!editable) {
    return (
      <Card title={title}>
        <div className="stack">
          <DataTable
            caption="Proforma entry"
            columns={READ_COLUMNS}
            rows={voucher.lines}
            rowKey={(l) => `${l.side}-${l.accountCode}-${l.amount}-${l.component ?? ''}`}
            emptyMessage="The rule could not build the entry yet; complete the details."
          />
          <Balance lines={voucher.lines} />
        </div>
      </Card>
    );
  }
  return (
    <Card
      title={title}
      actions={
        <div className="dsb-actions">
          <Button
            variant="secondary"
            icon={<SplitSquareHorizontal size={16} />}
            onClick={() => setAllocating(true)}
          >
            Allocate Expenses
          </Button>
          <Button
            variant="secondary"
            icon={<RotateCcw size={16} />}
            busy={reset.isPending}
            onClick={() => reset.mutate()}
          >
            Rebuild from Rule
          </Button>
          <Button
            icon={<Save size={16} />}
            busy={save.isPending}
            disabled={!entryTotals(lines).balanced}
            onClick={() => save.mutate()}
          >
            Save Entry
          </Button>
        </div>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error ?? reset.error} />
        <table className="table dsb-lines">
          <caption className="visually-hidden">Editable proforma entry</caption>
          <thead>
            <tr>
              <th>Side</th>
              <th>Account</th>
              <th>Party</th>
              <th>Cost Centre</th>
              <th>Amount</th>
              <th aria-label="Remove" />
            </tr>
          </thead>
          <tbody>
            {lines.map((l, i) => (
              <LineEditor
                key={`line-${i}`}
                line={l}
                onChange={(next) => setLines(lines.map((x, j) => (j === i ? next : x)))}
                onRemove={() => setLines(lines.filter((_, j) => j !== i))}
              />
            ))}
          </tbody>
        </table>
        <div>
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={16} />}
            onClick={() => setLines([...lines, { side: 'DEBIT', accountCode: '', amount: 0 }])}
          >
            Add Line
          </Button>
        </div>
        <Balance lines={lines} />
      </div>
      {allocating && (
        <AllocationDialog
          voucher={voucher}
          onClose={() => setAllocating(false)}
          onDone={(v) => {
            setAllocating(false);
            setLines(v.lines);
            onSaved(v);
            toast.success('Expense allocation applied');
          }}
        />
      )}
    </Card>
  );
}
