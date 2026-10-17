import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Lock, ShieldCheck } from 'lucide-react';
import { useState } from 'react';
import { closingApi } from '@/api/closing';
import type { ClosingBalance } from '@/api/closing';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatDateTime } from '@/utils/format';
import { ChecklistView } from './ChecklistView';
import { closeControlsApi } from './closeControlsApi';
import type { VerifiedYearEndClose } from './closeControlsApi';
import { PeriodSelectors } from './PeriodSelectors';
import type { usePeriodPicker } from './usePeriodPicker';
import { UserName } from '@/components/ui/UserName';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { Notice } from '@/components/ui/Notice';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { LineLabel } from '@/components/broking/LovLabel';

/** Income and expense balances the closing journal will transfer to retained earnings. */
function ClosingPreview({ companyId, yearId }: Readonly<{ companyId: number; yearId: number }>) {
  const { branches } = useWorkspace();
  const preview = useQuery({
    queryKey: ['year-preview', companyId, yearId],
    queryFn: () => closingApi.yearPreview(companyId, yearId),
    enabled: companyId > 0 && yearId > 0,
  });
  const net = -(preview.data ?? []).reduce((acc, b) => acc + b.netDebit, 0);
  const branch = (id: number) => branches.find((x) => x.id === id)?.code ?? String(id);
  return (
    <Card title={`Closing entries preview · net result ${formatAmount(net)}`} flush>
      <ErrorAlert error={preview.error} />
      <DataTable<ClosingBalance>
        loading={preview.isLoading}
        rows={preview.data ?? []}
        rowKey={(b) =>
          `${b.branchId}-${b.accountCode}-${b.costCenter ?? ''}-${b.businessLine ?? ''}`
        }
        emptyMessage="No income or expense balances to close."
        columns={[
          { key: 'b', header: 'Branch', render: (b) => branch(b.branchId) },
          { key: 'a', header: 'Account', render: (b) => b.accountCode },
          { key: 'c', header: 'Cost Centre', render: (b) => b.costCenter ?? '' },
          {
            key: 'l',
            header: 'Line of Business',
            render: (b) => (b.businessLine ? <LineLabel code={b.businessLine} /> : ''),
          },
          {
            key: 'n',
            header: 'Balance (Dr +)',
            numeric: true,
            render: (b) => <Amount value={b.netDebit} />,
          },
        ]}
      />
      <p className="muted" style={{ padding: '0 16px' }}>
        Balance sheet balances are cumulative and carry forward automatically; no opening balance
        journal is needed.
      </p>
    </Card>
  );
}

function verificationText(record: VerifiedYearEndClose): string {
  if (record.verifiedAt === undefined) {
    return 'not run yet.';
  }
  const outcome = record.verified === true ? 'both zero' : 'check the books';
  return `nominal accounts ${formatAmount(record.nominalBalance ?? 0)}, trial balance difference ${formatAmount(record.tbDifference ?? 0)} — ${outcome} (${formatDateTime(record.verifiedAt)}).`;
}

function CloseRecord({
  record,
  onVerify,
  verifying,
}: Readonly<{ record: VerifiedYearEndClose; onVerify?: () => void; verifying: boolean }>) {
  const verified = record.verified === true;
  return (
    <div className="stack">
      <Card title={`FY ${record.yearCode} Close`}>
        <DefinitionGrid
          columns={2}
          items={[
            { label: 'Closed by', value: <UserName login={record.closedBy} /> },
            { label: 'Closed on', value: formatDateTime(record.closedAt) },
            { label: 'Net result', value: formatAmount(record.netResult) },
            { label: 'Transferred to', value: record.retainedEarningsAccount },
            { label: 'Closing journals', value: record.closingBatches },
          ]}
        />
      </Card>
      <Notice
        tone={verified ? 'success' : 'warning'}
        title="Post-close verification"
        actions={
          onVerify !== undefined && (
            <Button
              size="sm"
              variant="secondary"
              icon={<ShieldCheck size={14} />}
              busy={verifying}
              onClick={onVerify}
            >
              Verify Again
            </Button>
          )
        }
      >
        {verificationText(record)}
      </Notice>
    </div>
  );
}

/** The close record with the verification and, for the GL team, "Verify Again". */
function VerifiedRecord({
  companyId,
  yearId,
  record,
}: Readonly<{ companyId: number; yearId: number; record: VerifiedYearEndClose }>) {
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const verify = useMutation({
    mutationFn: () => closeControlsApi.verifyYear(companyId, yearId),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries({ queryKey: ['year-close', yearId] });
      toast.success(
        `FY ${r.yearCode} verified: ${r.verified === true ? 'balanced' : 'differences found'}`,
      );
    },
  });
  const allowed = can('YEAR_END_CLOSE') || can('GL_CLOSE_SCHEDULE');
  return (
    <>
      <ErrorAlert error={verify.error} />
      <CloseRecord
        record={record}
        verifying={verify.isPending}
        onVerify={allowed ? () => verify.mutate() : undefined}
      />
    </>
  );
}

/**
 * Year-end close: pre-close checklist, preview of the transfer to retained earnings, the close
 * itself (closing journal per branch, fiscal year locked, next year opened) and the post-close
 * verification that nominal balances and the trial balance difference are zero (FRBS 2.7.1).
 */
export function YearEndPanel({ picker }: Readonly<{ picker: ReturnType<typeof usePeriodPicker> }>) {
  const { companyId } = picker;
  const yearId = picker.year?.id ?? 0;
  const yearCode = picker.year?.yearCode ?? 0;
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const closer = can('YEAR_END_CLOSE');

  const checklist = useQuery({
    queryKey: ['year-checklist', companyId, yearId],
    queryFn: () => closingApi.yearChecklist(companyId, yearId),
    enabled: companyId > 0 && yearId > 0,
  });
  const record = useQuery({
    queryKey: ['year-close', yearId],
    queryFn: () => closingApi.closeRecord(yearId),
    enabled: yearId > 0,
  });
  const close = useMutation({
    mutationFn: () => closingApi.closeYear(companyId, yearId),
    onSuccess: async (r) => {
      await queryClient.invalidateQueries();
      toast.success(`FY ${r.yearCode} closed, net result ${formatAmount(r.netResult)}`);
    },
  });
  const [confirming, setConfirming] = useState(false);

  return (
    <div className="stack">
      <div className="form-grid">
        <PeriodSelectors picker={picker} withPeriod={false} />
        {closer && (
          <Button
            variant="accent"
            icon={<Lock size={16} />}
            busy={close.isPending}
            disabled={checklist.data?.ready !== true}
            onClick={() => setConfirming(true)}
            style={{ alignSelf: 'end' }}
          >
            Close Fiscal Year
          </Button>
        )}
      </div>
      <ErrorAlert error={checklist.error ?? close.error} />
      {confirming && (
        <ConfirmDialog
          title="Close Fiscal Year"
          record={`FY ${String(yearCode)}`}
          effect="The nominal accounts are closed to retained earnings and the year is locked. This cannot be undone."
          confirmLabel="Close Fiscal Year"
          destructive
          busy={close.isPending}
          onClose={() => setConfirming(false)}
          onConfirm={() => {
            setConfirming(false);
            close.mutate();
          }}
        />
      )}
      {record.data && <VerifiedRecord companyId={companyId} yearId={yearId} record={record.data} />}
      <Card title="Pre-close checklist" flush>
        <ChecklistView checklist={checklist.data} loading={checklist.isLoading} />
      </Card>
      {closer && <ClosingPreview companyId={companyId} yearId={yearId} />}
    </div>
  );
}
