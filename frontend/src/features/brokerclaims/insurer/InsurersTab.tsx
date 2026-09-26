import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { MessageSquarePlus, Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useToast } from '@/components/ui/toastContext';
import { formatDate, formatDateTime, humanize } from '@/utils/format';
import type { Claim } from '../record/api';
import type { InsurerLine, InsurerUpdate, NewInsurerLine, NewUpdate } from './api';
import { insurerApi } from './api';
import { AddInsurerDialog, AdjusterDialog, NumberDialog } from './InsurerDialogs';
import { UpdateDialog } from './UpdateDialog';

type Dialog =
  { kind: 'add' } | { kind: 'update' } | { kind: 'number' | 'adjuster'; line: InsurerLine };

function LineActions({
  line,
  canNumber,
  canAdjuster,
  onOpen,
}: Readonly<{
  line: InsurerLine;
  canNumber: boolean;
  canAdjuster: boolean;
  onOpen: (d: Dialog) => void;
}>) {
  return (
    <span className="row">
      {canNumber && (
        <Button size="sm" variant="ghost" onClick={() => onOpen({ kind: 'number', line })}>
          Add Claim No.
        </Button>
      )}
      {canAdjuster && (
        <Button size="sm" variant="ghost" onClick={() => onOpen({ kind: 'adjuster', line })}>
          Assign Adjuster
        </Button>
      )}
    </span>
  );
}

function UpdatesTable({ updates }: Readonly<{ updates: InsurerUpdate[] }>) {
  return (
    <DataTable<InsurerUpdate>
      caption="Insurer updates"
      rows={updates}
      rowKey={(u) => u.id}
      emptyMessage="No insurer update recorded yet."
      columns={[
        { key: 'd', header: 'Update Date', render: (u) => formatDate(u.updateDate) },
        { key: 's', header: 'Source', render: (u) => humanize(u.source) },
        { key: 'r', header: 'Reference', render: (u) => u.reference ?? '' },
        {
          key: 'm',
          header: 'Remarks',
          render: (u) => (
            <span>
              {u.remarks}
              {u.correctsUpdateId !== undefined && <span className="muted"> (correction)</span>}
              {u.attachmentIds.length > 0 && (
                <span className="muted"> · {u.attachmentIds.length} document(s)</span>
              )}
            </span>
          ),
        },
        {
          key: 'b',
          header: 'Recorded',
          render: (u) =>
            [u.recordedBy, formatDateTime(u.recordedAt), u.uploadRef].filter(Boolean).join(', '),
        },
      ]}
    />
  );
}

function LinesTable({
  rows,
  loading,
  canNumber,
  canAdjuster,
  onOpen,
}: Readonly<{
  rows: InsurerLine[];
  loading: boolean;
  canNumber: boolean;
  canAdjuster: boolean;
  onOpen: (d: Dialog) => void;
}>) {
  return (
    <DataTable<InsurerLine>
      caption="Insurer claim lines"
      loading={loading}
      rows={rows}
      rowKey={(l) => l.id}
      emptyMessage="No insurer on this claim yet."
      columns={[
        { key: 'i', header: 'Insurer', render: (l) => l.insurerName ?? l.insurerCode },
        { key: 's', header: 'Share %', numeric: true, render: (l) => l.sharePct ?? '' },
        {
          key: 'n',
          header: 'Insurer Claim No.',
          render: (l) => l.insurerClaimNo ?? 'Awaiting number',
        },
        {
          key: 'd',
          header: 'Reported to Insurer',
          render: (l) => formatDate(l.reportedToInsurerOn),
        },
        { key: 'a', header: 'Adjuster', render: (l) => l.adjusterLabel ?? '' },
        {
          key: 'x',
          header: '',
          render: (l) => (
            <LineActions line={l} canNumber={canNumber} canAdjuster={canAdjuster} onOpen={onOpen} />
          ),
        },
      ]}
    />
  );
}

/** The open dialog of the tab with its save action. */
function DialogHost({
  dialog,
  claimId,
  companyId,
  lines,
  updates,
  onClose,
}: Readonly<{
  dialog: Dialog;
  claimId: number;
  companyId: number;
  lines: InsurerLine[];
  updates: InsurerUpdate[];
  onClose: () => void;
}>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const done = async (message: string) => {
    toast.success(message);
    onClose();
    await queryClient.invalidateQueries({ queryKey: ['broker-claims'] });
  };
  const add = useMutation({
    mutationFn: (v: { line: NewInsurerLine; confirm: boolean }) =>
      insurerApi.add(companyId, claimId, v.line, v.confirm),
    onSuccess: () => done('Insurer added'),
  });
  const number = useMutation({
    mutationFn: (v: {
      lineId: number;
      insurerClaimNo: string;
      reportedToInsurerOn?: string;
      confirmReuse: boolean;
    }) => insurerApi.number(companyId, claimId, v.lineId, v),
    onSuccess: () => done('Insurer claim number saved'),
  });
  const adjuster = useMutation({
    mutationFn: (v: { lineId: number; code: string }) =>
      insurerApi.adjuster(companyId, claimId, v.lineId, v.code),
    onSuccess: () => done('Adjuster assigned'),
  });
  const update = useMutation({
    mutationFn: (u: NewUpdate) => insurerApi.recordUpdate(companyId, claimId, u),
    onSuccess: () => done('Insurer update recorded'),
  });
  switch (dialog.kind) {
    case 'add':
      return (
        <AddInsurerDialog
          companyId={companyId}
          busy={add.isPending}
          error={add.error}
          onClose={onClose}
          onSave={(line, confirm) => add.mutate({ line, confirm })}
        />
      );
    case 'number':
      return (
        <NumberDialog
          line={dialog.line}
          busy={number.isPending}
          error={number.error}
          onClose={onClose}
          onSave={(v) => number.mutate({ lineId: dialog.line.id, ...v })}
        />
      );
    case 'adjuster':
      return (
        <AdjusterDialog
          line={dialog.line}
          busy={adjuster.isPending}
          error={adjuster.error}
          onClose={onClose}
          onSave={(code) => adjuster.mutate({ lineId: dialog.line.id, code })}
        />
      );
    default:
      return (
        <UpdateDialog
          claimId={claimId}
          lines={lines}
          updates={updates}
          busy={update.isPending}
          error={update.error}
          onClose={onClose}
          onSave={(u) => update.mutate(u)}
        />
      );
  }
}

/**
 * Insurers & Updates tab of a claim (BRCLM.018/041/043; FR-CL-021/022/031): one line per insurer
 * and insurer claim number with share, date reported and adjuster, and the insert-only timeline of
 * insurer updates.
 */
export function InsurersTab({ claim, companyId }: Readonly<{ claim: Claim; companyId: number }>) {
  const { can } = useAuth();
  const [dialog, setDialog] = useState<Dialog>();
  const lines = useQuery({
    queryKey: ['broker-claims', 'insurers', claim.id],
    queryFn: () => insurerApi.lines(companyId, claim.id),
  });
  const updates = useQuery({
    queryKey: ['broker-claims', 'updates', claim.id],
    queryFn: () => insurerApi.updates(companyId, claim.id),
  });
  const open = claim.progress.phase !== 'CLOSED';
  const canRecord = can('BCL_RECORD');
  return (
    <div className="stack">
      <div className="row">
        {open && canRecord && (
          <Button
            variant="secondary"
            icon={<Plus size={16} />}
            onClick={() => setDialog({ kind: 'add' })}
          >
            Add Insurer
          </Button>
        )}
        {canRecord && (
          <Button
            variant="secondary"
            icon={<MessageSquarePlus size={16} />}
            onClick={() => setDialog({ kind: 'update' })}
          >
            Record Insurer Update
          </Button>
        )}
      </div>
      <ErrorAlert error={lines.error ?? updates.error} />
      <LinesTable
        rows={lines.data ?? []}
        loading={lines.isLoading}
        canNumber={open && canRecord}
        canAdjuster={open && can('BCL_ADJUSTER_ASSIGN')}
        onOpen={setDialog}
      />
      <UpdatesTable updates={updates.data ?? []} />
      {dialog !== undefined && (
        <DialogHost
          dialog={dialog}
          claimId={claim.id}
          companyId={companyId}
          lines={lines.data ?? []}
          updates={updates.data ?? []}
          onClose={() => setDialog(undefined)}
        />
      )}
    </div>
  );
}
