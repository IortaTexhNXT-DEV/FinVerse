import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Fragment, useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { payRequestApi } from './api';
import type { PayRequest, RefundLineView, ValidationView } from './api';

const LINE_COLUMNS: Column<RefundLineView>[] = [
  { key: 'no', header: 'Item', render: (l) => l.lineNo },
  {
    key: 'ar',
    header: 'AR No.',
    render: (l) => (
      <>
        <strong>{l.arNo}</strong>
        {l.cancelledPolicy && (
          <span className="tag-list">
            <span className="tag">Cancelled Policy</span>
          </span>
        )}
      </>
    ),
  },
  {
    key: 'invoice',
    header: 'Invoice / Root',
    render: (l) => (
      <>
        {l.invoiceNo ?? '—'}
        <span className="cell-sub">{l.rootInvoiceNo ?? ''}</span>
      </>
    ),
  },
  {
    key: 'client',
    header: 'Assured / Client',
    render: (l) => (
      <>
        {l.assuredName}
        <span className="cell-sub">{l.clientCode}</span>
      </>
    ),
  },
  { key: 'amount', header: 'Amount', numeric: true, render: (l) => <Amount value={l.amount} /> },
  { key: 'reason', header: 'Reason', render: (l) => humanize(l.reasonCode) },
  { key: 'unit', header: 'Branch / Unit', render: (l) => l.branchUnit ?? '—' },
  {
    key: 'cat',
    header: 'Category A / B',
    render: (l) => `${l.categoryA ?? '—'} / ${l.categoryB ?? '—'}`,
  },
];

function Facts({ items }: Readonly<{ items: [string, string | undefined][] }>) {
  return (
    <dl className="detail-list">
      {items.map(([label, value]) => (
        <Fragment key={label}>
          <dt>{label}</dt>
          <dd>{value === undefined || value === '' ? '—' : value}</dd>
        </Fragment>
      ))}
    </dl>
  );
}

/** The form of the request: accounts of a refund, facts of a cash advance or check cancellation. */
export function DetailsTab({ request: r }: Readonly<{ request: PayRequest }>) {
  return (
    <div className="stack">
      <Card title="Request">
        <Facts
          items={[
            ['Reference', r.content.referenceText],
            ['Segment', r.content.segment],
            ['Requesting Unit', r.content.requestingUnit],
            ['Purpose', r.content.purpose],
            ['Type', r.content.rfpType ? humanize(r.content.rfpType) : undefined],
            ['Mode of Payment', humanize(r.payee.mode)],
            ['Account No.', r.payee.accountNo],
            ['Account / Check Name', r.payee.accountName],
            ['CA / SA on Client Record', r.payoutRecorded ? 'Recorded' : undefined],
            [
              'Check to Cancel',
              r.target.requestNo ? `${r.target.requestNo} · DV ${r.target.dvNo ?? ''}` : undefined,
            ],
            ['Check No.', r.target.checkNo],
            [
              'Cancellation Reason',
              r.target.reasonCode ? humanize(r.target.reasonCode) : undefined,
            ],
          ]}
        />
      </Card>
      {r.kind === 'REFUND' && (
        <Card title="Accounts to Refund" flush>
          <DataTable
            caption="Refund accounts"
            columns={LINE_COLUMNS}
            rows={r.lines}
            rowKey={(l) => l.lineNo}
          />
        </Card>
      )}
    </div>
  );
}

/** Where the payment stands in Disbursement (MKT 1.20.0). */
export function DisbursementTab({ request: r }: Readonly<{ request: PayRequest }>) {
  const t = r.track;
  return (
    <Card title="Disbursement">
      <Facts
        items={[
          ['Payment Request', t.requestNo],
          ['Gateway Status', t.status ? humanize(t.status) : undefined],
          ['DV No.', t.dvNo],
          ['DV Stage', t.dvStatus ? humanize(t.dvStatus) : undefined],
          ['Instrument Status', t.instrumentStatus ? humanize(t.instrumentStatus) : undefined],
          ['Disbursed', t.disbursedAt ? formatDateTime(t.disbursedAt) : undefined],
          ['Message', t.message],
          ['Hand-off to Disbursement', t.handoffRef],
        ]}
      />
    </Card>
  );
}

function ResultDialog({
  request,
  task,
  onClose,
}: Readonly<{ request: PayRequest; task: ValidationView; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [confirmed, setConfirmed] = useState(true);
  const [newArNo, setNewArNo] = useState('');
  const [remarks, setRemarks] = useState('');
  const save = useMutation({
    mutationFn: () =>
      payRequestApi.recordValidation(request.id, task.id, {
        confirmed,
        newArNo: newArNo.trim() || undefined,
        remarks: remarks.trim() || undefined,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['payrequest'] });
      await queryClient.invalidateQueries({ queryKey: ['workflow'] });
      toast.success(`${humanize(task.validator)} result recorded`);
      onClose();
    },
  });
  return (
    <Modal
      open
      title={`Record ${humanize(task.validator)} Result`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button variant="accent" busy={save.isPending} onClick={() => save.mutate()}>
            Record Result
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <Field label="Result" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={confirmed ? 'yes' : 'no'}
              onChange={(e) => setConfirmed(e.target.value === 'yes')}
            >
              <option value="yes">Confirmed</option>
              <option value="no">Rejected</option>
            </select>
          )}
        </Field>
        {task.validator === 'CASHIERING' && (
          <Field label="New AR No." hint="AR issued when the payment went back to unapplied">
            {(id) => (
              <input
                id={id}
                className="input"
                value={newArNo}
                onChange={(e) => setNewArNo(e.target.value)}
              />
            )}
          </Field>
        )}
        <Field label="Remarks">
          {(id) => (
            <input
              id={id}
              className="input"
              value={remarks}
              onChange={(e) => setRemarks(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * ACSL and Cashiering validations of a refund of a cancelled policy (MKT 1.11.0): each task with
 * its ticket (ACSL case or hand-off) and result; a handed-over result is entered here.
 */
export function ValidationTab({ request }: Readonly<{ request: PayRequest }>) {
  const { can } = useAuth();
  const [task, setTask] = useState<ValidationView | null>(null);
  const tasks = useQuery({
    queryKey: ['payrequest', 'validations', request.id],
    queryFn: () => payRequestApi.validations(request.id),
  });
  const columns: Column<ValidationView>[] = [
    { key: 'round', header: 'Round', render: (v) => v.roundNo },
    { key: 'line', header: 'Item', render: (v) => v.lineNo },
    { key: 'validator', header: 'Validator', render: (v) => humanize(v.validator) },
    {
      key: 'ticket',
      header: 'Ticket',
      render: (v) => (
        <>
          {v.ticketRef ?? '—'}
          <span className="cell-sub">{v.message ?? ''}</span>
        </>
      ),
    },
    { key: 'status', header: 'Status', render: (v) => <StatusBadge status={v.status} /> },
    {
      key: 'result',
      header: 'Result',
      render: (v) => (
        <>
          {v.remarks ?? '—'}
          <span className="cell-sub">{v.newArNo ? `New AR ${v.newArNo}` : ''}</span>
        </>
      ),
    },
    {
      key: 'act',
      header: '',
      render: (v) =>
        (v.status === 'DEFERRED' || v.status === 'OPEN') &&
        can('PRQ_ASSIGN') &&
        request.stage === 'FOR_VALIDATION' ? (
          <Button size="sm" variant="secondary" onClick={() => setTask(v)}>
            Record Result
          </Button>
        ) : null,
    },
  ];
  return (
    <Card title="Validations" flush>
      <ErrorAlert error={tasks.error} />
      <DataTable
        caption="Validation tasks"
        columns={columns}
        rows={tasks.data ?? []}
        rowKey={(v) => v.id}
        loading={tasks.isLoading}
        emptyMessage="No validation needed for this request"
      />
      {task && <ResultDialog request={request} task={task} onClose={() => setTask(null)} />}
    </Card>
  );
}
