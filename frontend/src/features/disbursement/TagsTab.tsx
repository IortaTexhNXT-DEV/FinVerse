import { useMutation } from '@tanstack/react-query';
import { Receipt, Stamp } from 'lucide-react';
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
import { useToast } from '@/components/ui/toastContext';
import { formatDate, humanize, today } from '@/utils/format';
import { disbursementApi } from './api';
import type { Tag, Voucher } from './api';
import { DialogFooter } from './VoucherDialogs';
import './disbursement.css';

const COLUMNS: Column<Tag>[] = [
  { key: 'kind', header: 'Tag', render: (t) => (t.kind === 'OR_AR' ? 'OR / AR' : 'CWT') },
  { key: 'dir', header: 'Received / Released', render: (t) => humanize(t.direction ?? '') },
  { key: 'no', header: 'Number', render: (t) => t.docNo ?? '' },
  {
    key: 'period',
    header: 'Date / Period',
    render: (t) =>
      t.kind === 'CWT'
        ? `${formatDate(t.periodFrom)} - ${formatDate(t.periodTo)}`
        : formatDate(t.docDate),
  },
  {
    key: 'on',
    header: 'Received / Released On',
    render: (t) => formatDate(t.receivedOn ?? t.releasedOn),
  },
  { key: 'amount', header: 'Amount', numeric: true, render: (t) => <Amount value={t.amount} /> },
  { key: 'by', header: 'Tagged By', render: (t) => t.taggedBy },
];

type Kind = 'receipt' | 'cwt';

interface TagForm {
  number: string;
  date: string;
  on: string;
  from: string;
  to: string;
  amount: string;
  direction: 'RECEIVED' | 'RELEASED';
}

function TagDialog({
  kind,
  voucher,
  onClose,
  onDone,
}: Readonly<{ kind: Kind; voucher: Voucher; onClose: () => void; onDone: (v: Voucher) => void }>) {
  const [form, setForm] = useState<TagForm>({
    number: '',
    date: today(),
    on: today(),
    from: '',
    to: '',
    amount: '',
    direction: 'RECEIVED',
  });
  const set = (patch: Partial<TagForm>) => setForm({ ...form, ...patch });
  const save = useMutation({
    mutationFn: () =>
      kind === 'receipt'
        ? disbursementApi.tagReceipt(voucher.summary.id, {
            receiptNo: form.number,
            receiptDate: form.date,
            receivedOn: form.on,
            amount: form.amount === '' ? undefined : Number(form.amount),
          })
        : disbursementApi.tagCwt(voucher.summary.id, {
            direction: form.direction,
            certificateNo: form.number,
            periodFrom: form.from,
            periodTo: form.to,
            on: form.on,
            amount: Number(form.amount),
          }),
    onSuccess: onDone,
  });
  const incomplete =
    form.number.trim() === '' ||
    (kind === 'cwt' && (form.from === '' || form.to === '' || Number(form.amount) <= 0));
  return (
    <Modal
      open
      title={kind === 'receipt' ? 'Tag OR / AR Received' : 'Tag CWT Certificate'}
      onClose={onClose}
      footer={
        <DialogFooter
          label="Save Tag"
          busy={save.isPending}
          disabled={incomplete}
          onClose={onClose}
          onConfirm={() => save.mutate()}
        />
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <div className="dsb-form">
          {kind === 'cwt' && (
            <Field label="Received / Released" required>
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={form.direction}
                  onChange={(e) => set({ direction: e.target.value as TagForm['direction'] })}
                >
                  <option value="RECEIVED">Received from insurer</option>
                  <option value="RELEASED">Released to supplier</option>
                </select>
              )}
            </Field>
          )}
          <Field label={kind === 'receipt' ? 'OR / AR No.' : 'Certificate No.'} required>
            {(id) => (
              <input
                id={id}
                className="input"
                value={form.number}
                onChange={(e) => set({ number: e.target.value })}
              />
            )}
          </Field>
          {kind === 'receipt' ? (
            <Field label="Receipt Date" required>
              {(id) => (
                <input
                  id={id}
                  type="date"
                  className="input"
                  value={form.date}
                  onChange={(e) => set({ date: e.target.value })}
                />
              )}
            </Field>
          ) : (
            <>
              <Field label="Period From" required>
                {(id) => (
                  <input
                    id={id}
                    type="date"
                    className="input"
                    value={form.from}
                    onChange={(e) => set({ from: e.target.value })}
                  />
                )}
              </Field>
              <Field label="Period To" required>
                {(id) => (
                  <input
                    id={id}
                    type="date"
                    className="input"
                    value={form.to}
                    onChange={(e) => set({ to: e.target.value })}
                  />
                )}
              </Field>
            </>
          )}
          <Field
            label={kind === 'cwt' && form.direction === 'RELEASED' ? 'Released On' : 'Received On'}
            required
          >
            {(id) => (
              <input
                id={id}
                type="date"
                className="input"
                value={form.on}
                onChange={(e) => set({ on: e.target.value })}
              />
            )}
          </Field>
          <Field label="Amount" required={kind === 'cwt'}>
            {(id) => (
              <input
                id={id}
                type="number"
                step="0.01"
                className="input"
                value={form.amount}
                onChange={(e) => set({ amount: e.target.value })}
              />
            )}
          </Field>
        </div>
      </div>
    </Modal>
  );
}

/**
 * OR / AR and CWT tags of an approved voucher (DIS 2.10.0-2.11.2): the receipt received from the
 * payee, and the CWT certificates received from insurers or released to suppliers.
 */
export function TagsTab({
  voucher,
  onSaved,
}: Readonly<{ voucher: Voucher; onSaved: (v: Voucher) => void }>) {
  const { can } = useAuth();
  const toast = useToast();
  const [kind, setKind] = useState<Kind>();
  const taggable = voucher.summary.stage === 'APPROVED' && can('DISB_TAG');
  return (
    <Card
      title="OR / AR and CWT"
      actions={
        taggable ? (
          <div className="dsb-actions">
            <Button
              variant="secondary"
              icon={<Receipt size={16} />}
              onClick={() => setKind('receipt')}
            >
              Tag OR / AR
            </Button>
            <Button variant="secondary" icon={<Stamp size={16} />} onClick={() => setKind('cwt')}>
              Tag CWT
            </Button>
          </div>
        ) : undefined
      }
    >
      <DataTable
        caption="Tags"
        columns={COLUMNS}
        rows={voucher.tags}
        rowKey={(t) => t.id}
        emptyMessage="No OR / AR or CWT tagged yet"
      />
      {kind !== undefined && (
        <TagDialog
          kind={kind}
          voucher={voucher}
          onClose={() => setKind(undefined)}
          onDone={(v) => {
            setKind(undefined);
            onSaved(v);
            toast.success('Tag saved');
          }}
        />
      )}
    </Card>
  );
}
