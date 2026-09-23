import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { payablesApi } from '@/api/payables';
import type { PayableItem, PaymentCategory, PaymentMode } from '@/api/payables';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useDefaultBranchId } from '@/context/workspaceContext';
import { formatDate, humanize, today } from '@/utils/format';
import { checkSelection } from './payablesMath';
import type { Selection } from './payablesMath';
import { PAYEE_TYPES, usePayablesLookups } from './usePayablesLookups';

const MODES: PaymentMode[] = ['CHEQUE', 'BANK_TRANSFER', 'PDC'];
const CATEGORIES: ('' | PaymentCategory)[] = [
  '',
  'SUPPLIER',
  'COMMISSION',
  'CLAIM',
  'REINSURANCE',
  'PREMIUM_REFUND',
];

/** New payment voucher: pick the payee's open payables, the amounts, the mode and the bank. */
export default function PaymentEntryPage() {
  const branch = useDefaultBranchId();
  const { companyId, parties, activeBanks } = usePayablesLookups(PAYEE_TYPES);
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [partyCode, setPartyCode] = useState('');
  const [selection, setSelection] = useState<Selection>({});
  const [mode, setMode] = useState<PaymentMode>('CHEQUE');
  const [category, setCategory] = useState<'' | PaymentCategory>('');
  const [bankId, setBankId] = useState<number | undefined>(undefined);
  const [voucherDate, setVoucherDate] = useState(today());
  const [chequeDate, setChequeDate] = useState(today());
  const [narration, setNarration] = useState('');

  const items = useQuery({
    queryKey: ['payable-items', companyId, partyCode],
    queryFn: () => payablesApi.payableItems(companyId, partyCode),
    enabled: companyId > 0 && partyCode !== '',
  });
  const rows = items.data ?? [];
  const check = checkSelection(rows, selection);
  const bank = bankId ?? activeBanks[0]?.id;

  const save = useMutation({
    mutationFn: async () => {
      const created = await payablesApi.createVoucher({
        companyId,
        branchId: branch,
        partyCode,
        category: category === '' ? undefined : category,
        mode,
        bankAccountId: bank ?? 0,
        voucherDate,
        chequeDate: mode === 'PDC' ? chequeDate : undefined,
        narration,
        items: Object.entries(selection).map(([id, amount]) => ({
          openItemId: Number(id),
          amount,
        })),
      });
      return payablesApi.submitVoucher(created.id);
    },
    onSuccess: async (v) => {
      await queryClient.invalidateQueries({ queryKey: ['vouchers'] });
      // Open payables of the party: the voucher reserves (and on approval settles) them.
      await queryClient.invalidateQueries({ queryKey: ['payable-items'] });
      toast.success(`${v.voucherNo} submitted for approval`);
      void navigate('/payables/vouchers');
    },
  });
  const toggle = (item: PayableItem, on: boolean) =>
    setSelection(
      on
        ? { ...selection, [item.openItemId]: item.available }
        : Object.fromEntries(
            Object.entries(selection).filter(([k]) => Number(k) !== item.openItemId),
          ),
    );

  return (
    <div className="stack">
      <PageHeader
        section="Payables & Cash"
        title="New Payment"
        description="Select the payee's open items to settle. The accounting event follows the payee and documents (supplier, commission, claim, reinsurance, refund)."
      />
      <ErrorAlert error={save.error ?? items.error} />
      <Card title="Payee and bank">
        <div className="form-grid">
          <Field label="Payee" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={partyCode}
                onChange={(e) => {
                  setPartyCode(e.target.value);
                  setSelection({});
                }}
              >
                <option value="">Select…</option>
                {parties.map((p) => (
                  <option key={p.code} value={p.code}>
                    {p.code} – {p.name} ({humanize(p.partyType)})
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Category" hint="Blank = derived from payee and documents">
            {(id) => (
              <select
                id={id}
                className="select"
                value={category}
                onChange={(e) => setCategory(CATEGORIES.find((c) => c === e.target.value) ?? '')}
              >
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {c === '' ? 'Automatic' : humanize(c)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Mode" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={mode}
                onChange={(e) => setMode(MODES.find((m) => m === e.target.value) ?? 'CHEQUE')}
              >
                {MODES.map((m) => (
                  <option key={m} value={m}>
                    {humanize(m)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Bank account" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={bank ?? ''}
                onChange={(e) => setBankId(Number(e.target.value))}
              >
                {activeBanks.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.code} – {b.currency}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Payment date" required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={voucherDate}
                onChange={(e) => setVoucherDate(e.target.value)}
              />
            )}
          </Field>
          {mode === 'PDC' && (
            <Field label="Cheque date (post-dated)" required>
              {(id) => (
                <input
                  id={id}
                  className="input"
                  type="date"
                  value={chequeDate}
                  onChange={(e) => setChequeDate(e.target.value)}
                />
              )}
            </Field>
          )}
          <Field label="Narration">
            {(id) => (
              <input
                id={id}
                className="input"
                value={narration}
                onChange={(e) => setNarration(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Card>
      <Card title="Open payables" flush>
        <DataTable<PayableItem>
          loading={items.isFetching}
          rows={rows}
          rowKey={(i) => i.openItemId}
          emptyMessage={partyCode === '' ? 'Select a payee.' : 'No open payables.'}
          columns={[
            {
              key: 'x',
              header: 'Pay',
              render: (i) => (
                <input
                  type="checkbox"
                  aria-label={`Pay ${i.documentNo}`}
                  checked={selection[i.openItemId] !== undefined}
                  onChange={(e) => toggle(i, e.target.checked)}
                />
              ),
            },
            { key: 'n', header: 'Document', render: (i) => i.documentNo },
            { key: 't', header: 'Type', render: (i) => humanize(i.documentType) },
            { key: 'd', header: 'Due', render: (i) => formatDate(i.dueDate) },
            { key: 'c', header: 'Ccy', render: (i) => i.currency },
            {
              key: 'o',
              header: 'Outstanding',
              numeric: true,
              render: (i) => <Amount value={i.outstanding} />,
            },
            {
              key: 'v',
              header: 'Available',
              numeric: true,
              render: (i) => <Amount value={i.available} />,
            },
            {
              key: 'a',
              header: 'Amount to pay',
              render: (i) =>
                selection[i.openItemId] !== undefined && (
                  <input
                    className="input"
                    type="number"
                    step="0.01"
                    aria-label={`Amount for ${i.documentNo}`}
                    value={selection[i.openItemId]}
                    onChange={(e) =>
                      setSelection({ ...selection, [i.openItemId]: Number(e.target.value) })
                    }
                  />
                ),
            },
          ]}
        />
      </Card>
      {check.errors.map((e) => (
        <div key={e} className="alert danger" role="alert">
          {e}
        </div>
      ))}
      <div className="row">
        <Kpi label="Payment amount" accent value={<Amount value={check.total} />} />
        <div className="spacer" />
        <Button variant="secondary" onClick={() => void navigate('/payables/vouchers')}>
          Back
        </Button>
        <Button
          variant="accent"
          disabled={check.total <= 0 || check.errors.length > 0 || bank === undefined}
          busy={save.isPending}
          onClick={() => save.mutate()}
        >
          Save &amp; submit
        </Button>
      </div>
    </div>
  );
}
