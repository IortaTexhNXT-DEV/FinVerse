import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BookOpen, FileDown, Plus } from 'lucide-react';
import { useState } from 'react';
import { payablesApi } from '@/api/payables';
import type { BankAccount, BankAccountRequest } from '@/api/payables';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, today } from '@/utils/format';
import { PaymentFileModal } from './PaymentFileModal';
import { usePayablesLookups } from './usePayablesLookups';

type Form = Partial<BankAccountRequest> & { id?: number };

const TEXT_FIELDS: { key: keyof BankAccountRequest; label: string; required?: boolean }[] = [
  { key: 'code', label: 'Code', required: true },
  { key: 'name', label: 'Name', required: true },
  { key: 'bankName', label: 'Bank', required: true },
  { key: 'accountNo', label: 'Account number', required: true },
  { key: 'currency', label: 'Currency', required: true },
  { key: 'glAccountCode', label: 'GL account (bank)', required: true },
  { key: 'pdcClearingAccountCode', label: 'PDC issued clearing account' },
  { key: 'bankPartyCode', label: 'Bank party code' },
];

/** Company bank accounts (house banks), their cheque books and the bank payment file. */
export default function BankAccountsPage() {
  const { companyId, banks } = usePayablesLookups();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Form | null>(null);
  const [books, setBooks] = useState<BankAccount | null>(null);
  const [fileOpen, setFileOpen] = useState(false);
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['bank-accounts'] });

  const save = useMutation({
    mutationFn: (f: Form) => {
      const body = { ...f, companyId, notificationFormat: f.notificationFormat ?? 'FIXED_WIDTH' };
      return f.id === undefined
        ? payablesApi.createBankAccount(body as BankAccountRequest)
        : payablesApi.updateBankAccount(f.id, body as BankAccountRequest);
    },
    onSuccess: async (b) => {
      await refresh();
      setForm(null);
      toast.success(`Bank account ${b.code} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: payablesApi.authorizeBankAccount,
    onSuccess: async (b) => {
      await refresh();
      toast.success(`Bank account ${b.code} authorized`);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Payables & Cash"
        title="Bank Accounts"
        description="House bank accounts mapped to bank GL accounts, with cheque books and PDC clearing accounts."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<FileDown size={16} />}
              onClick={() => setFileOpen(true)}
            >
              Payment file
            </Button>
            {can('MASTER_MAINTAIN') && (
              <Button
                variant="accent"
                icon={<Plus size={16} />}
                onClick={() => setForm({ currency: 'PHP', notificationFormat: 'FIXED_WIDTH' })}
              >
                New bank account
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={authorize.error} />
      <Card flush>
        <DataTable<BankAccount>
          rows={banks}
          rowKey={(b) => b.id}
          caption="Bank accounts"
          onRowClick={can('MASTER_MAINTAIN') ? (b) => setForm(b) : undefined}
          columns={[
            { key: 'c', header: 'Code', render: (b) => <strong>{b.code}</strong> },
            { key: 'n', header: 'Name', render: (b) => b.name },
            { key: 'b', header: 'Bank', render: (b) => b.bankName },
            { key: 'a', header: 'Account no.', render: (b) => b.accountNo },
            { key: 'y', header: 'Ccy', render: (b) => b.currency },
            { key: 'g', header: 'GL', render: (b) => b.glAccountCode },
            { key: 'p', header: 'PDC clearing', render: (b) => b.pdcClearingAccountCode ?? '' },
            { key: 's', header: 'Status', render: (b) => <StatusBadge status={b.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (b) => (
                <div className="row">
                  <Button
                    size="sm"
                    variant="ghost"
                    icon={<BookOpen size={14} />}
                    onClick={(e) => {
                      e.stopPropagation();
                      setBooks(b);
                    }}
                  >
                    Cheque books
                  </Button>
                  {b.recordStatus === 'PENDING_AUTHORIZATION' && can('MASTER_AUTHORIZE') && (
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={(e) => {
                        e.stopPropagation();
                        authorize.mutate(b.id);
                      }}
                    >
                      Authorize
                    </Button>
                  )}
                </div>
              ),
            },
          ]}
        />
      </Card>
      <Modal
        title={form?.id === undefined ? 'New bank account' : `Edit ${form.code ?? ''}`}
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button variant="accent" busy={save.isPending} onClick={() => form && save.mutate(form)}>
            Save for authorization
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        {form !== null && (
          <div className="form-grid">
            {TEXT_FIELDS.map((f) => (
              <Field key={f.key} label={f.label} required={f.required}>
                {(id) => (
                  <input
                    id={id}
                    className="input"
                    disabled={f.key === 'code' && form.id !== undefined}
                    value={String(form[f.key] ?? '')}
                    onChange={(e) => setForm({ ...form, [f.key]: e.target.value })}
                  />
                )}
              </Field>
            ))}
            <Field label="Payment file layout">
              {(id) => (
                <select
                  id={id}
                  className="select"
                  value={form.notificationFormat ?? 'FIXED_WIDTH'}
                  onChange={(e) =>
                    setForm({
                      ...form,
                      notificationFormat: e.target.value === 'CSV' ? 'CSV' : 'FIXED_WIDTH',
                    })
                  }
                >
                  <option value="FIXED_WIDTH">Fixed width</option>
                  <option value="CSV">CSV</option>
                </select>
              )}
            </Field>
          </div>
        )}
      </Modal>
      {books !== null && <ChequeBooksModal bank={books} onClose={() => setBooks(null)} />}
      <PaymentFileModal open={fileOpen} banks={banks} onClose={() => setFileOpen(false)} />
    </div>
  );
}

function ChequeBooksModal({ bank, onClose }: Readonly<{ bank: BankAccount; onClose: () => void }>) {
  const { can } = useAuth();
  const queryClient = useQueryClient();
  const [range, setRange] = useState({ firstNo: '', lastNo: '' });
  const books = useQuery({
    queryKey: ['cheque-books', bank.id],
    queryFn: () => payablesApi.chequeBooks(bank.id),
  });
  const add = useMutation({
    mutationFn: () =>
      payablesApi.addChequeBook(bank.id, {
        firstNo: Number(range.firstNo),
        lastNo: Number(range.lastNo),
        receivedOn: today(),
      }),
    onSuccess: async () => {
      setRange({ firstNo: '', lastNo: '' });
      await queryClient.invalidateQueries({ queryKey: ['cheque-books', bank.id] });
    },
  });
  return (
    <Modal title={`Cheque books – ${bank.code}`} open onClose={onClose}>
      <div className="stack">
        <ErrorAlert error={add.error} />
        <DataTable
          rows={books.data ?? []}
          loading={books.isLoading}
          rowKey={(b) => b.id}
          columns={[
            { key: 'r', header: 'Range', render: (b) => `${b.firstNo} – ${b.lastNo}` },
            { key: 'n', header: 'Next leaf', render: (b) => b.nextNo },
            { key: 'l', header: 'Remaining', numeric: true, render: (b) => b.remaining },
            { key: 'd', header: 'Received', render: (b) => formatDate(b.receivedOn) },
            { key: 's', header: 'Status', render: (b) => <StatusBadge status={b.status} /> },
          ]}
        />
        {can('MASTER_MAINTAIN') && (
          <div className="form-grid">
            <Field label="First leaf">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  inputMode="numeric"
                  value={range.firstNo}
                  onChange={(e) => setRange({ ...range, firstNo: e.target.value })}
                />
              )}
            </Field>
            <Field label="Last leaf">
              {(id) => (
                <input
                  id={id}
                  className="input"
                  inputMode="numeric"
                  value={range.lastNo}
                  onChange={(e) => setRange({ ...range, lastNo: e.target.value })}
                />
              )}
            </Field>
            <Button
              style={{ alignSelf: 'end' }}
              disabled={range.firstNo === '' || range.lastNo === ''}
              busy={add.isPending}
              onClick={() => add.mutate()}
            >
              Add cheque book
            </Button>
          </div>
        )}
      </div>
    </Modal>
  );
}
