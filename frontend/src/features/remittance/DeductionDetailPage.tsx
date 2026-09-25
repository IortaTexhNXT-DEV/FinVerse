import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Building2, FileCheck2, Pencil, Scale, UserRound } from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { Attachments } from '@/components/attachments/Attachments';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { DeductionDialog } from './DeductionDialog';
import { appliedPercent, formOf, inputOf, isConfirmedByInsurer } from './deductionForm';
import type { DeductionForm } from './deductionForm';
import { deductionsApi } from './deductionsApi';
import type { Deduction, DeductionApplication } from './deductionsApi';
import { joinParts } from './remittanceLabels';
import './remittance.css';

const ENTITY = 'RemittanceDeduction';
const TABS = [
  { id: 'batches', label: 'Remittance Batches' },
  { id: 'documents', label: 'Insurer Confirmation' },
] as const;
type TabId = (typeof TABS)[number]['id'];
const ACTIONS: Record<string, string> = { submit: 'Submit for Confirmation', confirm: 'Confirm' };

const APPLICATION_COLUMNS: Column<DeductionApplication>[] = [
  {
    key: 'batch',
    header: 'Batch',
    render: (a) => (
      <Link to={`/remittance/batches/${a.batchId}`} onClick={(e) => e.stopPropagation()}>
        {a.sendCycle > 1 ? `${a.batchNo}/R${a.sendCycle}` : a.batchNo}
      </Link>
    ),
  },
  { key: 'on', header: 'Applied On', render: (a) => formatDateTime(a.createdAt) },
  { key: 'jv', header: 'Journal', render: (a) => a.journalBatchNo ?? '' },
  { key: 'amt', header: 'Amount', numeric: true, render: (a) => <Amount value={a.amount} /> },
  {
    key: 'st',
    header: 'Status',
    render: (a) => <StatusBadge status={a.reversed ? 'REVERSED' : 'POSTED'} />,
  },
];

function Summary({ d }: Readonly<{ d: Deduction }>) {
  return (
    <RecordSummary
      title={`${d.insurerCode} · ${humanize(d.sourceType)}`}
      chips={
        <>
          <ReferenceChip label="Deduction" value={d.deductionNo} />
          <StatusBadge status={d.stage} />
        </>
      }
      flags={<span className="tag">{d.currency}</span>}
      facts={[
        { icon: Building2, label: 'Insurer', value: d.insurerCode },
        {
          icon: Scale,
          label: 'Amount / Applied / Remaining',
          value: `${formatAmount(d.amount)} / ${formatAmount(d.appliedAmount)} (${appliedPercent(d)}%) / ${formatAmount(d.remaining)}`,
        },
        {
          icon: FileCheck2,
          label: 'Insurer Confirmation',
          value: isConfirmedByInsurer(d)
            ? `${d.confirmationRef ?? ''} · ${formatDate(d.confirmationDate)}`
            : 'Not recorded',
        },
        {
          icon: UserRound,
          label: 'Prepared / Confirmed By',
          value: `${d.createdBy} / ${d.confirmedBy ?? '—'}`,
        },
      ]}
    />
  );
}

/** Save, submit and confirm, each refreshing the deduction, its workflow and the list. */
function useDeductionActions(id: number, done: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const companyId = useCompanyId();
  const after = async (d: Deduction, message: string) => {
    done();
    queryClient.setQueryData(['remittance', 'deduction', id], d);
    await queryClient.invalidateQueries({ queryKey: workflowKey(ENTITY, id) });
    await queryClient.invalidateQueries({ queryKey: ['remittance', 'deductions'] });
    toast.success(message);
  };
  return {
    save: useMutation({
      mutationFn: (form: DeductionForm) => deductionsApi.update(id, inputOf(companyId, form)),
      onSuccess: (d) => after(d, `${d.deductionNo} saved`),
    }),
    act: useMutation({
      mutationFn: (v: { action: string; comment?: string }) =>
        v.action === 'confirm'
          ? deductionsApi.confirm(id, v.comment)
          : deductionsApi.submit(id, v.comment),
      onSuccess: (d) => after(d, `${d.deductionNo}: ${humanize(d.stage).toLowerCase()}`),
    }),
  };
}

/**
 * Remittance deduction (ACSL 2.9.2): the insurer, source and amount, the insurer's written
 * confirmation and its documents, the REM_DEDUCTION workflow (submit, confirm by another user,
 * return, cancel) and the batches that deducted it.
 */
export default function DeductionDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('batches');
  const [editing, setEditing] = useState(false);
  const [asking, setAsking] = useState<WorkAction>();
  const deduction = useQuery({
    queryKey: ['remittance', 'deduction', id],
    queryFn: () => deductionsApi.get(id),
  });
  const applications = useQuery({
    queryKey: ['remittance', 'deduction', id, 'applications'],
    queryFn: () => deductionsApi.applications(id),
  });
  const { save, act } = useDeductionActions(id, () => {
    setEditing(false);
    setAsking(undefined);
  });
  if (deduction.data === undefined) {
    return deduction.error ? (
      <ErrorAlert error={deduction.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const d = deduction.data;
  return (
    <div className="stack">
      <PageHeader
        section="Remittance · Deductions"
        backTo="/remittance/deductions"
        title={d.deductionNo}
        description={joinParts([
          d.sourceRef,
          d.invoiceNo,
          `created ${formatDateTime(d.createdAt)}`,
        ])}
        actions={
          d.stage === 'DRAFT' && can('ACSL_PROCESS') ? (
            <Button
              variant="secondary"
              icon={<Pencil size={16} />}
              onClick={() => setEditing(true)}
            >
              Edit Deduction
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={act.error} />
      <Summary d={d} />
      <WorkflowPanel
        entityType={ENTITY}
        entityId={id}
        renderBusinessActions={(actions) =>
          actions
            .filter((a) => ACTIONS[a.action] !== undefined)
            .map((a) => (
              <Button key={a.action} size="sm" busy={act.isPending} onClick={() => setAsking(a)}>
                {ACTIONS[a.action]}
              </Button>
            ))
        }
        onChanged={() => void deduction.refetch()}
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'batches' ? (
        <Card title="Deducted From">
          <ErrorAlert error={applications.error} />
          <DataTable
            caption="Batches that deducted this amount"
            columns={APPLICATION_COLUMNS}
            rows={applications.data ?? []}
            rowKey={(a) => a.id}
            loading={applications.isLoading}
            emptyMessage="Not yet deducted from a remittance batch"
          />
        </Card>
      ) : (
        <Attachments entityType={ENTITY} entityId={id} reference={d.deductionNo} />
      )}
      {asking !== undefined && (
        <ActionDialog
          title={`${ACTIONS[asking.action] ?? asking.label} ${d.deductionNo}`}
          confirmLabel={ACTIONS[asking.action] ?? asking.label}
          busy={act.isPending}
          error={act.error}
          onClose={() => setAsking(undefined)}
          onConfirm={(note) => act.mutate({ action: asking.action, comment: note.comment })}
        />
      )}
      {editing && (
        <DeductionDialog
          title={`Edit ${d.deductionNo}`}
          initial={formOf(d)}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(false)}
          onSave={(form) => save.mutate(form)}
        />
      )}
    </div>
  );
}
