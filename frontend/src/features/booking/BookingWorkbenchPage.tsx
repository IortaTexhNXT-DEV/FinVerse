import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { History, ListPlus, Upload } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { bookingApi } from '@/api/booking';
import type { BatchRun, WorkbenchCounts, WorkbenchRow, WorkbenchTab } from '@/api/booking';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { handedArns, rowLink, selectedArns, withHanded, WORKBENCH_TABS } from './bookingForm';
import { BookingDateDialog, ConfirmDialog, QueueEditDialog } from './WorkbenchDialogs';
import { WorkbenchFilters } from './WorkbenchFilters';
import { WorkbenchTable } from './WorkbenchTable';
import { WorkbenchToolbar } from './WorkbenchToolbar';
import type { WorkbenchDialog } from './WorkbenchToolbar';
import './booking.css';

const TILES: readonly { tab: WorkbenchTab; label: string; count: keyof WorkbenchCounts }[] = [
  { tab: 'READY', label: 'Ready to book', count: 'readyToBook' },
  { tab: 'QUEUED', label: 'Queued for batch', count: 'queued' },
  { tab: 'BOOKED', label: 'Booked today', count: 'bookedToday' },
  { tab: 'FAILED', label: 'Failed', count: 'failed' },
];

function Tiles({
  counts,
  active,
  onChoose,
}: Readonly<{
  counts: WorkbenchCounts | undefined;
  active: WorkbenchTab;
  onChoose: (tab: WorkbenchTab) => void;
}>) {
  return (
    <div className="grid-4 booking-tiles">
      {TILES.map((t) => {
        const classes = ['kpi', 'kpi-button'];
        if (t.tab === 'FAILED') {
          classes.push('kpi-alert');
        }
        if (t.tab === active) {
          classes.push('active');
        }
        return (
          <button
            key={t.tab}
            type="button"
            className={classes.join(' ')}
            aria-pressed={t.tab === active}
            onClick={() => onChoose(t.tab)}
          >
            <span className="kpi-label">{t.label}</span>
            <span className="kpi-value">{counts?.[t.count] ?? '–'}</span>
          </button>
        );
      })}
    </div>
  );
}

function HeaderLinks({ upload }: Readonly<{ upload: boolean }>) {
  return (
    <>
      <Link className="btn btn-secondary" to="/booking/batch-runs">
        <History size={16} aria-hidden="true" /> Batch Runs
      </Link>
      {upload && (
        <Link className="btn btn-secondary" to="/bulk/BOOKING_UPLOAD">
          <Upload size={16} aria-hidden="true" /> Upload Bookings
        </Link>
      )}
    </>
  );
}

/** The accounts sent by the Placement Workbench ("For Booking"), with a one-click enqueue. */
function HandedBanner({
  arns,
  canProcess,
  busy,
  onEnqueue,
  onDismiss,
}: Readonly<{
  arns: string[];
  canProcess: boolean;
  busy: boolean;
  onEnqueue: () => void;
  onDismiss: () => void;
}>) {
  return (
    <div className="alert booking-handed">
      <span>
        {String(arns.length)} account(s) sent from placement for booking: {arns.join(', ')}. They
        are selected on this page when listed.
      </span>
      <div className="row">
        {canProcess && (
          <Button variant="secondary" icon={<ListPlus size={16} />} busy={busy} onClick={onEnqueue}>
            Add All to Batch
          </Button>
        )}
        <Button variant="ghost" onClick={onDismiss}>
          Dismiss
        </Button>
      </div>
    </div>
  );
}

/** The ARNs handed over in `?arns=`, their dismissal and their enqueueing in one go. */
function useHanded(companyId: number) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [params, setParams] = useSearchParams();
  const arns = handedArns(params.get('arns'));
  const dismiss = () => setParams({}, { replace: true });
  const enqueue = useMutation({
    mutationFn: () => bookingApi.enqueue(companyId, arns),
    onSuccess: async (results) => {
      dismiss();
      await queryClient.invalidateQueries({ queryKey: ['booking'] });
      toast.success(
        `${String(results.filter((r) => r.queued).length)} account(s) added to the batch`,
      );
    },
  });
  return { arns, dismiss, enqueue };
}

function firstError(sources: readonly { error: unknown }[]): unknown {
  return sources.find((s) => s.error)?.error ?? null;
}

function summary(run: BatchRun): string {
  return `${run.runNo}: ${String(run.bookedCount)} booked, ${String(run.failedCount)} failed`;
}

/** The queue and batch commands of the workbench, each refreshing the lists when done. */
function useWorkbenchActions(companyId: number, selectedRows: WorkbenchRow[], reset: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const arns = selectedRows.map((r) => r.arn);
  const done = async (message: string) => {
    reset();
    await queryClient.invalidateQueries({ queryKey: ['booking'] });
    toast.success(message);
  };
  return {
    enqueue: useMutation({
      mutationFn: () => bookingApi.enqueue(companyId, arns),
      onSuccess: (results) =>
        done(`${String(results.filter((r) => r.queued).length)} account(s) added to the batch`),
    }),
    bookNow: useMutation({
      mutationFn: (date: string) => bookingApi.bookNow(companyId, arns, date),
      onSuccess: (run) => done(summary(run)),
    }),
    confirm: useMutation({
      mutationFn: (date: string) =>
        bookingApi.confirmBatch(
          companyId,
          selectedRows.map((r) => r.id),
          date,
        ),
      onSuccess: (run) => done(summary(run)),
    }),
    cancel: useMutation({
      mutationFn: () => bookingApi.cancelBatch(companyId),
      onSuccess: (r) => done(`Batch cancelled: ${String(r.removed)} account(s) removed`),
    }),
    edit: useMutation({
      mutationFn: (v: { id: number; date?: string; costCenter?: string }) =>
        bookingApi.editQueued(v.id, v.date, v.costCenter),
      onSuccess: (e) => done(`${e.arn} updated`),
    }),
    remove: useMutation({
      mutationFn: (row: WorkbenchRow) => bookingApi.removeQueued(row.id),
      onSuccess: (e) => done(`${e.arn} removed from the queue`),
    }),
  };
}

/**
 * Booking Workbench (BRNB.027/036/038/076/111): accounts ready to book (issued or booked directly),
 * the batch queue, booked accounts and failures. Select accounts to book them now or add them to
 * the end-of-day batch; queued accounts can be edited or removed, and the batch confirmed or
 * cancelled. Accounts sent by the Placement Workbench arrive as `?arns=` and are pre-selected.
 */
export default function BookingWorkbenchPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const hand = useHanded(companyId);
  const handed = hand.arns;
  const [tab, setTab] = useState<WorkbenchTab>('READY');
  const [query, setQuery] = useState('');
  const [line, setLine] = useState('');
  const [page, setPage] = useState(0);
  const [selection, setSelection] = useState<Set<number>>(new Set());
  const [dialog, setDialog] = useState<WorkbenchDialog | null>(null);
  const [editing, setEditing] = useState<WorkbenchRow | null>(null);
  const [filtersOpen, setFiltersOpen] = useState(false);

  const counts = useQuery({
    queryKey: ['booking', 'counts', companyId],
    queryFn: () => bookingApi.counts(companyId),
    enabled: companyId > 0,
  });
  const rows = useQuery({
    queryKey: ['booking', 'workbench', companyId, tab, query, line, page],
    queryFn: () => bookingApi.workbench(companyId, tab, query, line, page),
    enabled: companyId > 0,
  });
  const content = rows.data?.content ?? [];
  const selected = tab === 'READY' ? withHanded(content, selection, handed) : selection;
  const selectedRows = content.filter((r) => selected.has(r.id));
  const dismissHanded = hand.dismiss;
  const actions = useWorkbenchActions(companyId, selectedRows, () => {
    setSelection(new Set());
    setDialog(null);
    setEditing(null);
    dismissHanded();
  });
  const enqueueHanded = hand.enqueue;
  const choose = (next: WorkbenchTab) => {
    setTab(next);
    setPage(0);
    setSelection(new Set());
  };
  const count = selectedArns(content, selected).length;

  return (
    <div className="stack">
      <PageHeader
        section="Booking"
        title="Booking Workbench"
        description="Book issued accounts individually or in batches; follow the queue, the booked accounts and the failures."
        actions={<HeaderLinks upload={can('BOOKING_PROCESS') && can('BULK_PROCESS')} />}
      />
      <ErrorAlert
        error={firstError([counts, rows, actions.remove, actions.enqueue, enqueueHanded])}
      />
      {handed.length > 0 && (
        <HandedBanner
          arns={handed}
          canProcess={can('BOOKING_PROCESS')}
          busy={enqueueHanded.isPending}
          onEnqueue={() => enqueueHanded.mutate()}
          onDismiss={dismissHanded}
        />
      )}
      <Tiles counts={counts.data} active={tab} onChoose={choose} />
      <Card>
        <div className="stack">
          <Tabs tabs={WORKBENCH_TABS} active={tab} onChange={choose} />
          <WorkbenchToolbar
            tab={tab}
            selected={count}
            canProcess={can('BOOKING_PROCESS')}
            filtersOpen={filtersOpen}
            enqueueing={actions.enqueue.isPending}
            onSearch={(text) => {
              setQuery(text);
              setPage(0);
            }}
            onToggleFilters={() => setFiltersOpen(!filtersOpen)}
            onEnqueue={() => actions.enqueue.mutate()}
            onDialog={setDialog}
          />
          {filtersOpen && (
            <WorkbenchFilters
              line={line}
              onLine={(value) => {
                setLine(value);
                setPage(0);
              }}
            />
          )}
          <WorkbenchTable
            tab={tab}
            rows={content}
            loading={rows.isLoading}
            selection={selected}
            onSelect={(next) => {
              setSelection(next);
              dismissHanded();
            }}
            onEdit={setEditing}
            onRemove={(row) => actions.remove.mutate(row)}
            onOpen={(row) => void navigate(rowLink(row))}
          />
          <PageFooter data={rows.data} noun="accounts" onPage={setPage} />
        </div>
      </Card>
      {dialog === 'bookNow' && (
        <BookingDateDialog
          title="Book Now"
          confirmLabel="Book Now"
          intro={`Book ${String(count)} account(s) now. Each account is booked on its own: one failure does not stop the others.`}
          busy={actions.bookNow.isPending}
          error={actions.bookNow.error}
          onConfirm={(date) => actions.bookNow.mutate(date)}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === 'confirmBatch' && (
        <BookingDateDialog
          title="Confirm Batch"
          confirmLabel="Confirm Batch"
          intro={`Book the ${String(count)} selected queued account(s). Accounts with their own booking date keep it.`}
          busy={actions.confirm.isPending}
          error={actions.confirm.error}
          onConfirm={(date) => actions.confirm.mutate(date)}
          onClose={() => setDialog(null)}
        />
      )}
      {dialog === 'cancelBatch' && (
        <ConfirmDialog
          title="Cancel Batch"
          confirmLabel="Cancel Batch"
          message="Remove every queued account from the batch? The accounts return to Ready to Book."
          busy={actions.cancel.isPending}
          error={actions.cancel.error}
          onConfirm={() => actions.cancel.mutate()}
          onClose={() => setDialog(null)}
        />
      )}
      {editing && (
        <QueueEditDialog
          row={editing}
          busy={actions.edit.isPending}
          error={actions.edit.error}
          onSave={(date, costCenter) => actions.edit.mutate({ id: editing.id, date, costCenter })}
          onClose={() => setEditing(null)}
        />
      )}
    </div>
  );
}
