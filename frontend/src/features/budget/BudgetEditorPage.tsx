import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Save, Send } from 'lucide-react';
import { useMemo, useState } from 'react';
import { useParams } from 'react-router-dom';
import { budgetApi } from '@/api/budget';
import type { Budget } from '@/api/budget';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useGlLookups } from '@/features/gl/useLookups';
import { formatAmount } from '@/utils/format';
import { BudgetGrid } from './BudgetGrid';
import type { GridRow } from './BudgetGrid';
import { BudgetTools } from './BudgetTools';
import { MONTHS, sum } from './budgetMath';
import { SpreadModal } from './SpreadModal';

let nextKey = 1;

function toRows(budget: Budget | undefined): GridRow[] {
  return (budget?.lines ?? []).map((l) => ({
    key: nextKey++,
    accountCode: l.accountCode,
    costCenter: l.costCenter ?? '',
    months: l.months.map(Number),
  }));
}

function emptyRow(): GridRow {
  return { key: nextKey++, accountCode: '', costCenter: '', months: Array<number>(MONTHS).fill(0) };
}

function isEditable(budget: Budget | undefined): boolean {
  return budget?.status === 'DRAFT' || budget?.status === 'REJECTED';
}

interface ActionsProps {
  budget: Budget;
  saving: boolean;
  submitting: boolean;
  onAdd: () => void;
  onSave: () => void;
  onSubmit: () => void;
}

function EditorActions({
  budget,
  saving,
  submitting,
  onAdd,
  onSave,
  onSubmit,
}: Readonly<ActionsProps>) {
  return (
    <>
      <StatusBadge status={budget.status} />
      {isEditable(budget) && (
        <>
          <Button variant="secondary" icon={<Plus size={16} />} onClick={onAdd}>
            Add Line
          </Button>
          <Button variant="accent" icon={<Save size={16} />} busy={saving} onClick={onSave}>
            Save
          </Button>
          <Button
            variant="secondary"
            icon={<Send size={16} />}
            busy={submitting}
            onClick={onSubmit}
          >
            Submit
          </Button>
        </>
      )}
    </>
  );
}

/** Budget grid editor with spreading, CSV import, copy from prior-year actuals and submission. */
export default function BudgetEditorPage() {
  const id = Number(useParams().id);
  const toast = useToast();
  const queryClient = useQueryClient();
  const { accounts, costCenters } = useGlLookups();
  const [rows, setRows] = useState<GridRow[] | undefined>();
  const [spreading, setSpreading] = useState<GridRow | undefined>();

  const budget = useQuery({ queryKey: ['budget', id], queryFn: () => budgetApi.get(id) });
  const current = budget.data;
  const initial = useMemo(() => toRows(current), [current]);
  const grid = rows ?? initial;
  const plAccounts = accounts.filter(
    (a) => a.postable && (a.accountClass === 'INCOME' || a.accountClass === 'EXPENSE'),
  );

  const updated = (b: Budget, message: string) => {
    setRows(toRows(b));
    queryClient.setQueryData(['budget', id], b);
    void queryClient.invalidateQueries({ queryKey: ['budgets'] });
    toast.success(message);
  };
  const save = useMutation({
    mutationFn: () =>
      budgetApi.saveLines(
        id,
        grid.map((r) => ({
          accountCode: r.accountCode,
          costCenter: r.costCenter || undefined,
          months: r.months,
        })),
      ),
    onSuccess: (b) => updated(b, 'Budget saved'),
  });
  const submit = useMutation({
    mutationFn: () => budgetApi.submit(id),
    onSuccess: async () => {
      await budget.refetch();
      toast.success('Budget submitted for approval');
    },
  });
  const title = current ? `Budget FY ${current.fiscalYear} v${current.versionNo}` : 'Budget';

  return (
    <div className="stack">
      <PageHeader
        section="Planning & Closing"
        title={title}
        description={current && `${current.name} · amounts in ${current.currency}`}
        actions={
          current && (
            <EditorActions
              budget={current}
              saving={save.isPending}
              submitting={submit.isPending}
              onAdd={() => setRows([...grid, emptyRow()])}
              onSave={() => save.mutate()}
              onSubmit={() => submit.mutate()}
            />
          )
        }
      />
      <ErrorAlert error={budget.error ?? save.error ?? submit.error} />
      {current?.rejectionReason && (
        <div className="alert warning">Rejected: {current.rejectionReason}</div>
      )}
      <Card
        title={`Budget lines · annual total ${formatAmount(sum(grid.flatMap((r) => r.months)))}`}
        flush
      >
        <BudgetGrid
          rows={grid}
          accounts={plAccounts}
          costCenters={costCenters}
          readOnly={!isEditable(current)}
          onChange={setRows}
          onSpread={setSpreading}
        />
      </Card>
      {current && isEditable(current) && <BudgetTools budget={current} onUpdated={updated} />}
      {spreading && (
        <SpreadModal
          open
          initialAnnual={sum(spreading.months)}
          onClose={() => setSpreading(undefined)}
          onApply={(months) => {
            setRows(grid.map((r) => (r.key === spreading.key ? { ...r, months } : r)));
            setSpreading(undefined);
          }}
        />
      )}
    </div>
  );
}
