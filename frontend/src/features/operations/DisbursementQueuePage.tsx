import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { statusMessage } from '@/components/ui/statusTones';
import { useState } from 'react';
import { opsApi } from '@/api/operations';
import type { Disbursement, DisbursementStatus } from '@/api/operations';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { CellStack } from '@/components/ui/CellStack';
import { RowActions } from '@/components/ui/RowActions';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { PageFooter } from '@/components/ui/Pager';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { moduleLabel, referenceText } from '@/utils/businessLabels';
import { formatDateTime, humanize } from '@/utils/format';
import './operations.css';

const TABS: readonly { id: DisbursementStatus; label: string }[] = [
  { id: 'SENT', label: 'To Acknowledge' },
  { id: 'ACKNOWLEDGED', label: 'Acknowledged' },
  { id: 'DV_ASSIGNED', label: 'DV Assigned' },
  { id: 'PAID', label: 'Paid' },
  { id: 'RETURNED', label: 'Returned' },
  { id: 'CANCELLED', label: 'Cancelled' },
];

/** Statuses after which the queue offers no action (paid, returned, cancelled by Disbursement). */
const CLOSED: readonly DisbursementStatus[] = ['PAID', 'RETURNED', 'CANCELLED'];

type DialogKind = 'dv' | 'return';

function ValueDialog({
  kind,
  request,
  busy,
  onClose,
  onSave,
}: Readonly<{
  kind: DialogKind;
  request: Disbursement;
  busy: boolean;
  onClose: () => void;
  onSave: (value: string) => void;
}>) {
  const [value, setValue] = useState('');
  const [error, setError] = useState<string>();
  const dv = kind === 'dv';
  const label = dv ? 'Disbursement Voucher No.' : 'Reason for Return';
  const save = () => {
    if (value.trim() === '') {
      setError(`${label} is required`);
      return;
    }
    onSave(value.trim());
  };
  return (
    <Modal
      title={dv ? `Assign DV Number to ${request.requestNo}` : `Return ${request.requestNo}`}
      open
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={busy} onClick={save}>
            {dv ? 'Assign DV Number' : 'Return Request'}
          </Button>
        </>
      }
    >
      <Field label={label} required error={error}>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={dv ? 40 : 200}
            value={value}
            onChange={(e) => {
              setValue(e.target.value);
              setError(undefined);
            }}
          />
        )}
      </Field>
    </Modal>
  );
}

function actionsOf(
  r: Disbursement,
  on: {
    acknowledge: (r: Disbursement) => void;
    dialog: (r: Disbursement, k: DialogKind) => void;
    paid: (r: Disbursement) => void;
  },
) {
  return (
    <RowActions
      record={r.requestNo}
      actions={[
        { label: 'Acknowledge', hidden: r.status !== 'SENT', onSelect: () => on.acknowledge(r) },
        {
          label: 'Assign DV Number',
          hidden: r.status !== 'SENT' && r.status !== 'ACKNOWLEDGED',
          onSelect: () => on.dialog(r, 'dv'),
        },
        { label: 'Mark Paid', hidden: r.status !== 'DV_ASSIGNED', onSelect: () => on.paid(r) },
        {
          label: 'Return to Sender',
          danger: true,
          hidden: CLOSED.includes(r.status),
          onSelect: () => on.dialog(r, 'return'),
        },
      ]}
    />
  );
}

/**
 * Disbursement queue (OQ02, RMTID.019, DBMID.001): the in-app stand-in for the Disbursement system.
 * Payment requests from remittance, refunds, the BIR 2307 report and incentive pass-on are
 * acknowledged, given their DV number, paid or returned to the sending team.
 */
export default function DisbursementQueuePage() {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [tab, setTab] = useState<DisbursementStatus>('SENT');
  const [page, setPage] = useState(0);
  const [dialog, setDialog] = useState<{ request: Disbursement; kind: DialogKind }>();
  const rows = useQuery({
    queryKey: ['ops', 'disbursements', companyId, tab, page],
    queryFn: () => opsApi.disbursements(companyId, tab, page),
    enabled: companyId > 0,
  });
  const act = useMutation({
    mutationFn: (fn: () => Promise<Disbursement>) => fn(),
    onSuccess: async (r) => {
      setDialog(undefined);
      await queryClient.invalidateQueries({ queryKey: ['ops'] });
      toast.success(statusMessage(r.requestNo, r.status));
    },
  });
  const on = {
    acknowledge: (r: Disbursement) => act.mutate(() => opsApi.acknowledge(r.id)),
    paid: (r: Disbursement) => act.mutate(() => opsApi.markPaid(r.id)),
    dialog: (r: Disbursement, kind: DialogKind) => setDialog({ request: r, kind }),
  };
  const columns: Column<Disbursement>[] = [
    {
      key: 'no',
      header: 'Request No.',
      render: (r) => (
        <>
          <strong>{r.requestNo}</strong>
          <div className="ops-muted">{humanize(r.type)}</div>
        </>
      ),
    },
    {
      key: 'payee',
      header: 'Payee',
      render: (r) => r.payeeName ?? r.payeeCode,
    },
    {
      key: 'src',
      header: 'From',
      render: (r) => (
        <CellStack
          main={moduleLabel(r.sourceModule)}
          sub={referenceText(r.sourceRef) || r.rootInvoiceNo}
        />
      ),
    },
    { key: 'amount', header: 'Amount', numeric: true, render: (r) => <Amount value={r.amount} /> },
    { key: 'dv', header: 'DV No.', render: (r) => r.dvNo ?? r.returnReason ?? '' },
    { key: 'sent', header: 'Sent', render: (r) => formatDateTime(r.sentAt) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
    { key: 'actions', header: '', kind: 'center', render: (r) => actionsOf(r, on) },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Operations"
        title="Disbursement Queue"
        description="Payment requests sent by the Operations teams."
      />
      <ErrorAlert error={rows.error ?? act.error} />
      <Card>
        <div className="stack">
          <Tabs
            tabs={TABS}
            active={tab}
            onChange={(t) => {
              setTab(t);
              setPage(0);
            }}
          />
          <DataTable
            caption="Payment requests"
            columns={columns}
            rows={rows.data?.content ?? []}
            rowKey={(r) => r.id}
            loading={rows.isLoading}
            emptyMessage="No items to display"
          />
          <PageFooter data={rows.data} noun="requests" onPage={setPage} />
        </div>
      </Card>
      {dialog !== undefined && (
        <ValueDialog
          kind={dialog.kind}
          request={dialog.request}
          busy={act.isPending}
          onClose={() => setDialog(undefined)}
          onSave={(value) =>
            act.mutate(() =>
              dialog.kind === 'dv'
                ? opsApi.assignDv(dialog.request.id, value)
                : opsApi.returnRequest(dialog.request.id, value),
            )
          }
        />
      )}
    </div>
  );
}
