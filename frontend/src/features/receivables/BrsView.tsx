import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Lock, Save } from 'lucide-react';
import { receivablesApi } from '@/api/receivables';
import type { BookEntry, StatementLine } from '@/api/receivables';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate } from '@/utils/format';
import { computeBrs } from './receivablesMath';

interface BrsViewProps {
  companyId: number;
  bank: string;
  asOf: string;
}

function BookItems({
  title,
  rows,
  credit,
}: Readonly<{ title: string; rows: BookEntry[]; credit: boolean }>) {
  return (
    <Card title={`${title} (${rows.length})`} flush>
      <DataTable<BookEntry>
        rows={rows}
        rowKey={(e) => e.id}
        caption={title}
        emptyMessage="None."
        columns={[
          { key: 'date', header: 'Doc. Date', render: (e) => formatDate(e.valueDate) },
          { key: 'no', header: 'Doc. No.', render: (e) => e.batchNo },
          { key: 'ref', header: 'Reference', render: (e) => e.reference ?? '' },
          { key: 'nar', header: 'Narration', render: (e) => e.narration ?? '' },
          {
            key: 'amt',
            header: 'Amount',
            numeric: true,
            render: (e) => <Amount value={credit ? e.credit : e.debit} />,
          },
        ]}
      />
    </Card>
  );
}

function BankItems({
  title,
  rows,
  credit,
}: Readonly<{ title: string; rows: StatementLine[]; credit: boolean }>) {
  return (
    <Card title={`${title} (${rows.length})`} flush>
      <DataTable<StatementLine>
        rows={rows}
        rowKey={(l) => l.id}
        caption={title}
        emptyMessage="None."
        columns={[
          { key: 'date', header: 'Value Date', render: (l) => formatDate(l.valueDate) },
          { key: 'ref', header: 'Reference', render: (l) => l.reference ?? '' },
          { key: 'desc', header: 'Description', render: (l) => l.description ?? '' },
          {
            key: 'amt',
            header: 'Amount',
            numeric: true,
            render: (l) => <Amount value={credit ? l.credit : l.debit} />,
          },
        ]}
      />
    </Card>
  );
}

/**
 * Bank Reconciliation Statement: book balance, the four groups of reconciling items, balance per
 * bank and the unexplained difference; saving records the reconciliation, finalizing locks it.
 */
export function BrsView({ companyId, bank, asOf }: Readonly<BrsViewProps>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const brs = useQuery({
    queryKey: ['brs', companyId, bank, asOf],
    queryFn: () => receivablesApi.brs(companyId, bank, asOf),
  });
  const action = useMutation({
    mutationFn: (run: () => Promise<string>) => run(),
    onSuccess: async (message) => {
      await queryClient.invalidateQueries({ queryKey: ['brs'] });
      await queryClient.invalidateQueries({ queryKey: ['reconciliations'] });
      toast.success(message);
    },
  });
  const data = brs.data;
  if (data === undefined) {
    return <ErrorAlert error={brs.error} />;
  }
  const f = data.figures;
  const check = computeBrs(
    f.bookBalance,
    f.bookDebitsNotInBank,
    f.bookCreditsNotInBank,
    f.bankDebitsNotInBook,
    f.bankCreditsNotInBook,
    f.statementBalance,
  );
  const rec = data.reconciliation;
  const finalized = rec?.status === 'FINALIZED';

  return (
    <div className="stack">
      <ErrorAlert error={action.error} />
      <div className="row">
        <span className="muted">
          {data.bank.code} - {data.bank.name} ({data.bank.currency}) as of {formatDate(data.asOf)}
        </span>
        {rec !== undefined && <StatusBadge status={finalized ? 'RECONCILED' : rec.status} />}
        <div className="spacer" />
        <Button
          variant="secondary"
          icon={<Save size={16} />}
          disabled={finalized}
          busy={action.isPending}
          onClick={() =>
            action.mutate(async () => {
              const saved = await receivablesApi.saveReconciliation(companyId, bank, asOf);
              return `Reconciliation saved, difference ${formatAmount(saved.difference)}`;
            })
          }
        >
          Save
        </Button>
        <Button
          variant="accent"
          icon={<Lock size={16} />}
          disabled={finalized || rec === undefined || check.difference !== 0 || !data.hasStatement}
          busy={action.isPending}
          onClick={() =>
            action.mutate(async () => {
              await receivablesApi.finalizeReconciliation(rec?.id ?? 0);
              return 'Reconciliation finalized';
            })
          }
        >
          Finalize
        </Button>
      </div>
      <div className="grid-4">
        <Kpi label="Balance as per book" value={formatAmount(f.bookBalance)} />
        <Kpi label="Balance per bank (computed)" value={formatAmount(check.computedBankBalance)} />
        <Kpi label="Balance per bank statement" value={formatAmount(f.statementBalance)} />
        <Kpi
          label="Unexplained difference"
          value={formatAmount(check.difference)}
          accent={check.difference !== 0}
        />
      </div>
      {!data.hasStatement && (
        <div className="alert warning" role="status">
          No bank statement has been imported up to this date.
        </div>
      )}
      <BookItems
        title="1. Book debits not accounted by bank (deposits in transit)"
        rows={data.bookDebits}
        credit={false}
      />
      <BookItems
        title="2. Book credits not accounted by bank (unpresented cheques)"
        rows={data.bookCredits}
        credit
      />
      <BankItems
        title="3. Bank debits not accounted in book"
        rows={data.bankDebits}
        credit={false}
      />
      <BankItems title="4. Bank credits not accounted in book" rows={data.bankCredits} credit />
    </div>
  );
}
