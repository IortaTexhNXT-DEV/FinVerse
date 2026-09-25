import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { FileUp, Plus } from 'lucide-react';
import { useState } from 'react';
import { receivablesApi } from '@/api/receivables';
import type { BankAccount } from '@/api/receivables';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { LayoutFields } from './LayoutFields';
import { frbsSetupApi } from './frbsSetupApi';
import type { StatementLayout } from './frbsSetupApi';
import { layoutProblems } from './setupForms';

const NEW_LAYOUT: StatementLayout = {
  bankAccountCode: '',
  name: '',
  dateColumn: 'Date',
  descriptionColumn: 'Description',
  referenceColumn: 'Check No',
  amountColumn: 'Amount',
  balanceColumn: 'Balance',
  datePattern: 'MM/dd/yyyy',
  chequeNumberFirst: true,
};

/**
 * Spreadsheet bank statements (FRBS 3.3.1 / 3.3.2): the column layout of each bank account's
 * export, the cheque number and amount matching rule, and the import that runs the automatic
 * reconciliation at once.
 */
export default function StatementLayoutsPage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<StatementLayout | null>(null);
  const [checked, setChecked] = useState(false);
  const [importing, setImporting] = useState(false);

  const layouts = useQuery({
    queryKey: ['statement-layouts', companyId],
    queryFn: () => frbsSetupApi.layouts(companyId),
    enabled: companyId > 0,
  });
  const banks = useQuery({
    queryKey: ['bank-accounts', companyId],
    queryFn: () => receivablesApi.bankAccounts(companyId),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (body: StatementLayout) => frbsSetupApi.saveLayout(companyId, body),
    onSuccess: async (l) => {
      setForm(null);
      await queryClient.invalidateQueries({ queryKey: ['statement-layouts'] });
      toast.success(`Layout of ${l.bankAccountCode} saved`);
    },
  });
  const problems = form === null ? {} : layoutProblems(form);
  const submit = () => {
    setChecked(true);
    if (form !== null && Object.keys(problems).length === 0) {
      save.mutate(form);
    }
  };
  const open = (l: StatementLayout | null) => {
    setChecked(false);
    save.reset();
    setForm(l ?? { ...NEW_LAYOUT, bankAccountCode: banks.data?.[0]?.code ?? '' });
  };

  return (
    <div className="stack">
      <PageHeader
        section="Setup"
        title="Bank Statement Layouts"
        description="Map the columns of each bank's spreadsheet export, then import statements to reconcile them automatically."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<FileUp size={16} />}
              onClick={() => setImporting(true)}
            >
              Import Statement
            </Button>
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => open(null)}>
              Add Layout
            </Button>
          </>
        }
      />
      <ErrorAlert error={layouts.error ?? banks.error} />
      <Card flush>
        <DataTable<StatementLayout>
          loading={layouts.isLoading}
          rows={layouts.data ?? []}
          rowKey={(l) => l.bankAccountCode}
          caption="Statement layouts"
          emptyMessage="No layout: statements use the standard columns date, description, reference, debit, credit, balance"
          onRowClick={open}
          columns={[
            {
              key: 'b',
              header: 'Bank Account',
              render: (l) => <strong>{l.bankAccountCode}</strong>,
            },
            { key: 'n', header: 'Layout', render: (l) => l.name },
            {
              key: 'd',
              header: 'Date Column',
              render: (l) => `${l.dateColumn} (${l.datePattern})`,
            },
            {
              key: 'a',
              header: 'Amounts',
              render: (l) => l.amountColumn ?? `${l.debitColumn ?? ''} / ${l.creditColumn ?? ''}`,
            },
            { key: 'r', header: 'Cheque No.', render: (l) => l.referenceColumn ?? '' },
            {
              key: 'm',
              header: 'Cheque Matching',
              render: (l) => <StatusBadge status={l.chequeNumberFirst ? 'ACTIVE' : 'INACTIVE'} />,
            },
          ]}
        />
      </Card>
      <Modal
        title="Statement layout"
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <>
            <Button variant="secondary" onClick={() => setForm(null)}>
              Cancel
            </Button>
            <Button variant="accent" busy={save.isPending} onClick={submit}>
              Save Layout
            </Button>
          </>
        }
      >
        {form !== null && (
          <div className="stack">
            <ErrorAlert error={save.error} />
            <LayoutFields
              form={form}
              banks={banks.data ?? []}
              errors={checked ? problems : {}}
              onChange={(patch) => setForm((f) => (f === null ? f : { ...f, ...patch }))}
            />
          </div>
        )}
      </Modal>
      <ImportStatementDialog
        open={importing}
        companyId={companyId}
        banks={banks.data ?? []}
        onClose={() => setImporting(false)}
      />
    </div>
  );
}

function ImportStatementDialog({
  open,
  companyId,
  banks,
  onClose,
}: Readonly<{ open: boolean; companyId: number; banks: BankAccount[]; onClose: () => void }>) {
  const toast = useToast();
  const [bank, setBank] = useState('');
  const [file, setFile] = useState<File | null>(null);
  const account = bank || (banks[0]?.code ?? '');
  const run = useMutation({
    mutationFn: () =>
      file === null
        ? Promise.reject(new Error('Choose the statement file'))
        : frbsSetupApi.importStatement(companyId, account, file),
    onSuccess: (r) => {
      toast.success(
        `Statement ${r.statement.statementRef}: ${r.statement.lineCount} line(s), ${r.matched} matched automatically`,
      );
      onClose();
    },
  });
  return (
    <Modal
      title="Import bank statement"
      open={open}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={run.isPending}
            disabled={file === null || account === ''}
            onClick={() => run.mutate()}
          >
            Import and Match
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={run.error} />
        <Field label="Bank account" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={account}
              onChange={(e) => setBank(e.target.value)}
            >
              {banks.map((b) => (
                <option key={b.code} value={b.code}>
                  {b.code} – {b.name}
                </option>
              ))}
            </select>
          )}
        </Field>
        <Field
          label="Statement file"
          required
          hint="Excel (.xlsx), OpenDocument (.ods) or CSV in the layout of the bank account."
        >
          {(id) => (
            <input
              id={id}
              type="file"
              className="input"
              accept=".xlsx,.ods,.csv"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}
