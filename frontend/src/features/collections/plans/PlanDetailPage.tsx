import { useQuery } from '@tanstack/react-query';
import { CalendarClock, FileText, Layers, UserRound, Wallet } from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { ItemResultsDialog } from '@/components/broking/ItemResultsDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { billingApi } from '../billing/api';
import type { Installment, ItemResult, Plan } from './api';
import { plansApi } from './api';
import { SOURCE_LABELS } from './labels';
import { PromiseDialog } from './PlanDialogs';
import { PlanTabsCard } from './PlanTabs';
import { ReasonDialog, RecordLoading } from './Parts';
import { usePlanActions } from './usePlanActions';
import { usePromiseSave } from './usePromiseSave';

function Summary({ plan }: Readonly<{ plan: Plan }>) {
  return (
    <RecordSummary
      title={plan.assuredName}
      chips={
        <>
          <ReferenceChip label="Plan" value={plan.planNo} />
          <ReferenceChip label="ARN" value={plan.arn} />
          <StatusBadge status={plan.status} />
        </>
      }
      flags={<span className="tag">{SOURCE_LABELS[plan.source]}</span>}
      facts={[
        { icon: FileText, label: 'Invoice', value: plan.invoiceNo ?? 'All policy years' },
        { icon: Layers, label: 'Billing Frequency', value: humanize(plan.frequency) },
        {
          icon: Wallet,
          label: `Total / Paid (${plan.currency})`,
          value: `${formatAmount(plan.total)} / ${formatAmount(plan.paidTotal)}`,
        },
        { icon: CalendarClock, label: 'First Due', value: formatDate(plan.firstDue) },
        {
          icon: UserRound,
          label: 'Created By',
          value: `${plan.createdBy}, ${formatDateTime(plan.createdAt)}`,
        },
      ]}
    />
  );
}

/** Page actions of a live plan: refresh the allocation, cancel the plan. */
function PlanActions({
  refreshing,
  canCancel,
  onRefresh,
  onCancel,
}: Readonly<{
  refreshing: boolean;
  canCancel: boolean;
  onRefresh: () => void;
  onCancel: () => void;
}>) {
  return (
    <>
      <Button variant="secondary" busy={refreshing} onClick={onRefresh}>
        Refresh Allocation
      </Button>
      {canCancel && (
        <Button variant="danger" onClick={onCancel}>
          Cancel Plan
        </Button>
      )}
    </>
  );
}

function descriptionOf(plan: Plan): string {
  const remarks = plan.remarks === undefined ? '' : ` – ${plan.remarks}`;
  return `${SOURCE_LABELS[plan.source]} plan of ${plan.arn}${remarks}`;
}

/**
 * Installment plan record (BRCLXN.053/054/058): the plan's billing cycles with the payments
 * allocated from the ledger, the statement of account of each cycle and the promises to pay.
 */
export default function PlanDetailPage() {
  const id = Number(useParams().id);
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [cancelling, setCancelling] = useState(false);
  const [promiseFor, setPromiseFor] = useState<Installment>();
  const [results, setResults] = useState<ItemResult[]>();
  const plan = useQuery({
    queryKey: ['collections', 'plan', id],
    queryFn: () => plansApi.plan(id),
  });
  const statements = useQuery({
    queryKey: ['collections', 'statements', 'plan', id],
    queryFn: () => billingApi.forPlan(id),
  });
  const { refresh, cancel, bill } = usePlanActions(id, () => setCancelling(false));
  const promise = usePromiseSave(companyId, (r) => {
    setPromiseFor(undefined);
    setResults(r);
  });
  if (plan.data === undefined) {
    return <RecordLoading error={plan.error} />;
  }
  const p = plan.data;
  const live = p.status === 'ACTIVE';
  const canAct = live && (can('CLX_BILLING') || can('CLX_WORK'));
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections · Installment Plans"
        backTo="/collections/plans"
        title={p.planNo}
        description={descriptionOf(p)}
        actions={
          canAct ? (
            <PlanActions
              refreshing={refresh.isPending}
              canCancel={can('CLX_BILLING')}
              onRefresh={() => refresh.mutate()}
              onCancel={() => setCancelling(true)}
            />
          ) : undefined
        }
      />
      <ErrorAlert error={refresh.error ?? bill.error} />
      <Summary plan={p} />
      {p.cancelReason !== undefined && (
        <div className="alert warning">Cancelled: {p.cancelReason}</div>
      )}
      <PlanTabsCard
        plan={p}
        statements={statements.data ?? []}
        actions={{
          canBill: can('CLX_BILLING'),
          canPromise: live && can('CLX_WORK'),
          busy: bill.isPending,
          onBill: (seq) => bill.mutate(seq),
          onPromise: setPromiseFor,
        }}
      />
      {cancelling && (
        <ReasonDialog
          title="Cancel Installment Plan"
          confirmLabel="Cancel Plan"
          busy={cancel.isPending}
          error={cancel.error}
          onClose={() => setCancelling(false)}
          onConfirm={(reason) => cancel.mutate(reason)}
        />
      )}
      {promiseFor !== undefined && (
        <PromiseDialog
          initialInvoice={promiseFor.invoiceNo}
          installmentId={promiseFor.id}
          allowMany={false}
          busy={promise.isPending}
          error={promise.error}
          onClose={() => setPromiseFor(undefined)}
          onSave={(draft) => promise.mutate(draft)}
        />
      )}
      {results !== undefined && (
        <ItemResultsDialog
          title="Promises Recorded"
          results={results}
          onClose={() => setResults(undefined)}
        />
      )}
    </div>
  );
}
