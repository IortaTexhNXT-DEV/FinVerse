import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import type { RowSelection } from '@/components/broking/rowSelection';
import { selectionColumn, useRowSelection } from '@/components/broking/rowSelection';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageFooter } from '@/components/ui/Pager';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { AcceptRequestDialog, ConfirmValidationDialog, RejectDialog } from './RequestDialogs';
import { REQUEST_COLUMNS, REVERSAL_COLUMNS, VALIDATION_COLUMNS } from './requestColumns';
import { requestsApi } from './requestsApi';

/** The three queues of the Cashiering requests screen (wave C1-C). */

const KEY = ['cashiering', 'requests'];

/** After a decision: close the dialog, clear the selection, reload the queues and confirm. */
function useDecided(selection: RowSelection, close: () => void) {
  const queryClient = useQueryClient();
  const toast = useToast();
  return async (message: string) => {
    close();
    selection.clear();
    await queryClient.invalidateQueries({ queryKey: KEY });
    toast.success(message);
  };
}

/** Collector requests: accept (assign the disposition) or reject the queued ones. */
export function CollectorRequestsPanel({
  companyId,
  open,
}: Readonly<{ companyId: number; open: boolean }>) {
  const { can } = useAuth();
  const navigate = useNavigate();
  const [query, setQuery] = useState('');
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<'ACCEPT' | 'REJECT'>();
  const selection = useRowSelection();
  const rows = useQuery({
    queryKey: [...KEY, 'collector', companyId, open, query, page],
    queryFn: () =>
      requestsApi.collectorRequests(
        companyId,
        open ? ['QUEUED'] : ['ACCEPTED', 'APPLIED', 'REJECTED'],
        query,
        page,
      ),
    enabled: companyId > 0,
  });
  const list = rows.data?.content ?? [];
  const selected = list.find((r) => selection.keys.length === 1 && selection.has(String(r.id)));
  const done = useDecided(selection, () => setDialog(undefined));
  const accept = useMutation({
    mutationFn: (body: Parameters<typeof requestsApi.accept>[1]) =>
      requestsApi.accept(selected?.id ?? 0, body),
    onSuccess: (d) =>
      done(`Disposition ${humanize(d.dispositionType)} assigned (${humanize(d.status)})`),
  });
  const reject = useMutation({
    mutationFn: (reason: string) => requestsApi.rejectRequest(selected?.id ?? 0, reason),
    onSuccess: (r) => done(`${r.requestNo} rejected`),
  });
  const selectable = open && can('CASH_DISPOSITION');
  const columns = selectable
    ? [
        selectionColumn(
          list,
          (r) => String(r.id),
          selection,
          (r) => r.requestNo,
        ),
        ...REQUEST_COLUMNS,
      ]
    : REQUEST_COLUMNS;
  return (
    <>
      <ErrorAlert error={rows.error} />
      <WorklistToolbar
        placeholder="Search Request No."
        initial={query}
        onSearch={(t) => {
          setQuery(t);
          setPage(0);
        }}
      >
        {selectable && (
          <>
            <Button
              variant="secondary"
              disabled={selected === undefined}
              onClick={() => setDialog('REJECT')}
            >
              Reject Request
            </Button>
            <Button
              variant="accent"
              disabled={selected === undefined}
              onClick={() => setDialog('ACCEPT')}
            >
              Accept Request
            </Button>
          </>
        )}
      </WorklistToolbar>
      <DataTable
        caption="Collector requests"
        columns={columns}
        rows={list}
        rowKey={(r) => r.id}
        loading={rows.isLoading}
        onRowClick={(r) => void navigate(`/cashiering/unapplied/${r.unappliedId}`)}
        emptyMessage="No collector requests to display"
      />
      <PageFooter data={rows.data} noun="requests" onPage={setPage} />
      {dialog === 'ACCEPT' && selected !== undefined && (
        <AcceptRequestDialog
          request={selected}
          busy={accept.isPending}
          error={accept.error}
          onClose={() => setDialog(undefined)}
          onAccept={(body) => accept.mutate(body)}
        />
      )}
      {dialog === 'REJECT' && selected !== undefined && (
        <RejectDialog
          title={`Reject ${selected.requestNo}`}
          busy={reject.isPending}
          error={reject.error}
          onClose={() => setDialog(undefined)}
          onReject={(reason) => reject.mutate(reason)}
        />
      )}
    </>
  );
}

/** Refund validations: confirm with the unapplied item holding the premium, or reject. */
export function ValidationsPanel({
  companyId,
  open,
}: Readonly<{ companyId: number; open: boolean }>) {
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<'CONFIRM' | 'REJECT'>();
  const selection = useRowSelection();
  const rows = useQuery({
    queryKey: [...KEY, 'validations', companyId, open, page],
    queryFn: () =>
      requestsApi.validations(companyId, open ? ['OPEN'] : ['CONFIRMED', 'REJECTED'], page),
    enabled: companyId > 0,
  });
  const list = rows.data?.content ?? [];
  const selected = list.find((t) => selection.keys.length === 1 && selection.has(String(t.id)));
  const done = useDecided(selection, () => setDialog(undefined));
  const confirm = useMutation({
    mutationFn: (v: { item: number; arNo?: string; remarks?: string }) =>
      requestsApi.confirm(selected?.id ?? 0, v.item, v.arNo, v.remarks),
    onSuccess: (t) => done(`${t.taskNo} confirmed`),
  });
  const reject = useMutation({
    mutationFn: (reason: string) => requestsApi.rejectValidation(selected?.id ?? 0, reason),
    onSuccess: (t) => done(`${t.taskNo} rejected`),
  });
  const selectable = open && can('CASH_DISPOSITION');
  const columns = selectable
    ? [
        selectionColumn(
          list,
          (t) => String(t.id),
          selection,
          (t) => t.taskNo,
        ),
        ...VALIDATION_COLUMNS,
      ]
    : VALIDATION_COLUMNS;
  return (
    <>
      <ErrorAlert error={rows.error} />
      {selectable && (
        <div className="worklist-toolbar">
          <div className="worklist-actions">
            <Button
              variant="secondary"
              disabled={selected === undefined}
              onClick={() => setDialog('REJECT')}
            >
              Reject Validation
            </Button>
            <Button
              variant="accent"
              disabled={selected === undefined}
              onClick={() => setDialog('CONFIRM')}
            >
              Confirm Validation
            </Button>
          </div>
        </div>
      )}
      <DataTable
        caption="Refund validations"
        columns={columns}
        rows={list}
        rowKey={(t) => t.id}
        loading={rows.isLoading}
        emptyMessage="No refund validations to display"
      />
      <PageFooter data={rows.data} noun="validations" onPage={setPage} />
      {dialog === 'CONFIRM' && selected !== undefined && (
        <ConfirmValidationDialog
          task={selected}
          busy={confirm.isPending}
          error={confirm.error}
          onClose={() => setDialog(undefined)}
          onConfirm={(item, arNo, remarks) => confirm.mutate({ item, arNo, remarks })}
        />
      )}
      {dialog === 'REJECT' && selected !== undefined && (
        <RejectDialog
          title={`Reject ${selected.taskNo}`}
          busy={reject.isPending}
          error={reject.error}
          onClose={() => setDialog(undefined)}
          onReject={(reason) => reject.mutate(reason)}
        />
      )}
    </>
  );
}

/** Payment reversals requested by ACSL: approve (four eyes) or reject. */
export function ReversalsPanel({
  companyId,
  open,
}: Readonly<{ companyId: number; open: boolean }>) {
  const { can } = useAuth();
  const [page, setPage] = useState(0);
  const [rejecting, setRejecting] = useState(false);
  const selection = useRowSelection();
  const rows = useQuery({
    queryKey: [...KEY, 'reversals', companyId, open, page],
    queryFn: () =>
      requestsApi.reversals(companyId, open ? ['SUBMITTED'] : ['APPROVED', 'REJECTED'], page),
    enabled: companyId > 0,
  });
  const list = rows.data?.content ?? [];
  const selected = list.find((r) => selection.keys.length === 1 && selection.has(String(r.id)));
  const done = useDecided(selection, () => setRejecting(false));
  const approve = useMutation({
    mutationFn: () => requestsApi.approveReversal(selected?.id ?? 0),
    onSuccess: (r) => done(`${r.requestNo} approved: payment back to unapplied`),
  });
  const reject = useMutation({
    mutationFn: (reason: string) => requestsApi.rejectReversal(selected?.id ?? 0, reason),
    onSuccess: (r) => done(`${r.requestNo} rejected`),
  });
  const selectable = open && can('CASH_APPROVE');
  const columns = selectable
    ? [
        selectionColumn(
          list,
          (r) => String(r.id),
          selection,
          (r) => r.requestNo,
        ),
        ...REVERSAL_COLUMNS,
      ]
    : REVERSAL_COLUMNS;
  return (
    <>
      <ErrorAlert error={rows.error ?? approve.error} />
      {selectable && (
        <div className="worklist-toolbar">
          <div className="worklist-actions">
            <Button
              variant="secondary"
              disabled={selected === undefined}
              onClick={() => setRejecting(true)}
            >
              Reject Reversal
            </Button>
            <Button
              variant="accent"
              busy={approve.isPending}
              disabled={selected === undefined}
              onClick={() => approve.mutate()}
            >
              Approve Reversal
            </Button>
          </div>
        </div>
      )}
      <DataTable
        caption="Payment reversals"
        columns={columns}
        rows={list}
        rowKey={(r) => r.id}
        loading={rows.isLoading}
        emptyMessage="No payment reversals to display"
      />
      <PageFooter data={rows.data} noun="reversals" onPage={setPage} />
      {rejecting && selected !== undefined && (
        <RejectDialog
          title={`Reject ${selected.requestNo}`}
          busy={reject.isPending}
          error={reject.error}
          onClose={() => setRejecting(false)}
          onReject={(reason) => reject.mutate(reason)}
        />
      )}
    </>
  );
}
