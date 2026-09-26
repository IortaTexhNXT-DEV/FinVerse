import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Upload } from 'lucide-react';
import { useState } from 'react';
import { receivablesApi } from '@/api/receivables';
import type { BankStatement, StatementLine } from '@/api/receivables';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate } from '@/utils/format';
import { csvPreview } from './receivablesMath';
import { bankOptions, useReceivablesLookups } from './useReceivablesLookups';

/**
 * Bank statement import (CSV: date, description, reference, debit, credit, balance - see
 * docs/samples/bank-statement-sample.csv) and inquiry of the imported statement lines.
 */
export default function BankStatementsPage() {
  const toast = useToast();
  const queryClient = useQueryClient();
  const { companyId, bankAccounts } = useReceivablesLookups();
  const [bank, setBank] = useState('');
  const [fileName, setFileName] = useState('');
  const [content, setContent] = useState('');
  const [reference, setReference] = useState('');
  const [opening, setOpening] = useState('');
  const [selected, setSelected] = useState<number | null>(null);

  const statements = useQuery({
    queryKey: ['statements', companyId],
    queryFn: () => receivablesApi.statements(companyId),
    enabled: companyId > 0,
  });
  const lines = useQuery({
    queryKey: ['statement-lines', selected],
    queryFn: () => receivablesApi.statementLines(selected ?? 0),
    enabled: selected !== null,
  });
  const upload = useMutation({
    mutationFn: () =>
      receivablesApi.importStatement({
        companyId,
        bankAccountCode: bank,
        statementRef: reference || undefined,
        fileName,
        content,
        openingBalance: opening === '' ? undefined : Number(opening),
      }),
    onSuccess: async (s) => {
      setContent('');
      setFileName('');
      setReference('');
      setSelected(s.id);
      await queryClient.invalidateQueries({ queryKey: ['statements'] });
      toast.success(`${s.statementRef}: ${s.lineCount} lines imported`);
    },
  });
  const preview = content === '' ? null : csvPreview(content);
  const readFile = async (file: File | undefined) => {
    if (file !== undefined) {
      setFileName(file.name);
      setContent(await file.text());
    }
  };

  return (
    <div className="stack">
      <PageHeader
        section="Receivables & Banking"
        title="Bank Statements"
        description="Import the bank's statement (CSV) for a GL bank account before reconciling it."
      />
      <ErrorAlert error={upload.error ?? statements.error} />
      <Card
        title="Import statement"
        actions={
          <Button
            variant="accent"
            icon={<Upload size={16} />}
            busy={upload.isPending}
            disabled={bank === '' || content === ''}
            onClick={() => upload.mutate()}
          >
            Import
          </Button>
        }
      >
        <div className="form-grid">
          <Field label="Bank account" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={bank}
                onChange={(e) => setBank(e.target.value)}
              >
                <option value="">Select...</option>
                {bankOptions(bankAccounts).map((o) => (
                  <option key={o.value} value={o.value}>
                    {o.label}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field
            label="CSV file"
            required
            hint="date, description, reference, debit, credit, balance"
          >
            {(id) => (
              <input
                id={id}
                type="file"
                accept=".csv,text/csv"
                className="input"
                onChange={(e) => void readFile(e.target.files?.[0])}
              />
            )}
          </Field>
          <Field label="Statement reference" hint="Defaults to the file name">
            {(id) => (
              <input
                id={id}
                className="input"
                value={reference}
                onChange={(e) => setReference(e.target.value)}
              />
            )}
          </Field>
          <Field label="Opening balance" hint="Derived from the first line when blank">
            {(id) => (
              <input
                id={id}
                type="number"
                step="0.01"
                className="input num"
                value={opening}
                onChange={(e) => setOpening(e.target.value)}
              />
            )}
          </Field>
        </div>
        {preview !== null && (
          <p className="muted">
            {fileName}: {preview.lines} lines · debits {formatAmount(preview.debit)} · credits{' '}
            {formatAmount(preview.credit)}
          </p>
        )}
      </Card>
      <Card title="Imported statements" flush>
        <DataTable<BankStatement>
          loading={statements.isLoading}
          rows={statements.data ?? []}
          rowKey={(s) => s.id}
          onRowClick={(s) => setSelected(s.id)}
          caption="Statements"
          columns={[
            { key: 'bank', header: 'Bank', render: (s) => s.bankAccountCode },
            { key: 'ref', header: 'Reference', render: (s) => <strong>{s.statementRef}</strong> },
            { key: 'from', header: 'From', render: (s) => formatDate(s.periodFrom) },
            { key: 'to', header: 'To', render: (s) => formatDate(s.periodTo) },
            {
              key: 'open',
              header: 'Opening',
              numeric: true,
              render: (s) => <Amount value={s.openingBalance} />,
            },
            {
              key: 'dr',
              header: 'Debits',
              numeric: true,
              render: (s) => <Amount value={s.totalDebit} />,
            },
            {
              key: 'cr',
              header: 'Credits',
              numeric: true,
              render: (s) => <Amount value={s.totalCredit} />,
            },
            {
              key: 'close',
              header: 'Closing',
              numeric: true,
              render: (s) => <Amount value={s.closingBalance} />,
            },
            { key: 'n', header: 'Lines', numeric: true, render: (s) => s.lineCount },
            { key: 'by', header: 'Imported by', render: (s) => s.createdBy },
          ]}
        />
      </Card>
      {selected !== null && (
        <Card title="Statement lines" flush>
          <DataTable<StatementLine>
            loading={lines.isLoading}
            rows={lines.data ?? []}
            rowKey={(l) => l.id}
            caption="Statement lines"
            columns={[
              { key: 'no', header: '#', numeric: true, render: (l) => l.lineNo },
              { key: 'date', header: 'Value Date', render: (l) => formatDate(l.valueDate) },
              { key: 'desc', header: 'Description', render: (l) => l.description ?? '' },
              { key: 'ref', header: 'Reference', render: (l) => l.reference ?? '' },
              {
                key: 'dr',
                header: 'Debit',
                numeric: true,
                render: (l) => <Amount value={l.debit || null} />,
              },
              {
                key: 'cr',
                header: 'Credit',
                numeric: true,
                render: (l) => <Amount value={l.credit || null} />,
              },
              {
                key: 'bal',
                header: 'Balance',
                numeric: true,
                render: (l) => <Amount value={l.balance} />,
              },
              {
                key: 'm',
                header: 'Reconciled',
                render: (l) => (
                  <StatusBadge status={l.matchId === undefined ? 'UNMATCHED' : 'RECONCILED'} />
                ),
              },
            ]}
          />
        </Card>
      )}
    </div>
  );
}
