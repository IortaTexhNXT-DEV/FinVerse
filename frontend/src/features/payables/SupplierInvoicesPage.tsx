import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { payablesApi } from '@/api/payables';
import type { Invoice } from '@/api/payables';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { DateReasonModal } from './DateReasonModal';
import { Pager } from './Pager';
import { invoiceActions, invoiceListStart } from './payablesActions';
import type { InvoiceActionId } from './payablesActions';

const STATUSES = ['', 'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'CANCELLED'];
type Action = 'reject' | 'cancel';

/** Supplier invoices: search, approval queue (maker-checker) and invoice detail. */
export default function SupplierInvoicesPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  // Drill-down from other screens (tax worksheets): ?invoice=<id> opens that invoice.
  const [params] = useSearchParams();
  const start = invoiceListStart(params.get('invoice'));
  const [status, setStatus] = useState(start.status);
  const [partyCode, setPartyCode] = useState('');
  const [page, setPage] = useState(0);
  const [selected, setSelected] = useState<number | null>(start.selected);
  const [action, setAction] = useState<Action | null>(null);

  const list = useQuery({
    queryKey: ['invoices', companyId, status, partyCode, page],
    queryFn: () => payablesApi.invoices({ companyId, status, partyCode, page, size: 25 }),
    enabled: companyId > 0,
  });
  const detail = useQuery({
    queryKey: ['invoice', selected],
    queryFn: () => payablesApi.invoice(selected ?? 0),
    enabled: selected !== null,
  });
  const done = async (i: Invoice, message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['invoices'] });
    await queryClient.invalidateQueries({ queryKey: ['invoice', i.id] });
    // An approved (or cancelled) invoice opens (or closes) a payable offered by New Payment.
    await queryClient.invalidateQueries({ queryKey: ['payable-items'] });
    setAction(null);
    toast.success(`${i.documentNo} ${message}`);
  };
  const submit = useMutation({
    mutationFn: payablesApi.submitInvoice,
    onSuccess: (i) => done(i, 'submitted for approval'),
  });
  const approve = useMutation({
    mutationFn: payablesApi.approveInvoice,
    onSuccess: (i) => done(i, `approved and posted (${i.journalBatchNos ?? ''})`),
  });
  const reasoned = useMutation({
    mutationFn: ({ id, kind, reason }: { id: number; kind: Action; reason: string }) =>
      kind === 'reject'
        ? payablesApi.rejectInvoice(id, reason)
        : payablesApi.cancelInvoice(id, reason),
    onSuccess: (i) => done(i, i.status === 'CANCELLED' ? 'cancelled' : 'returned to maker'),
  });
  const inv = detail.data;
  const handlers: Record<InvoiceActionId, (x: Invoice) => void> = {
    submit: (x) => submit.mutate(x.id),
    approve: (x) => approve.mutate(x.id),
    reject: () => setAction('reject'),
    cancel: () => setAction('cancel'),
  };

  return (
    <div className="stack">
      <PageHeader
        section="Payables & Cash"
        title="Supplier Invoices"
        description="Accounts payable: capture, approve and post supplier invoices with input VAT and withholding tax."
        actions={
          can('RECEIPT_PAYMENT_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => void navigate('/payables/invoices/new')}
            >
              New Invoice
            </Button>
          )
        }
      />
      <Card>
        <div className="form-grid">
          <Field label="Status">
            {(id) => (
              <select
                id={id}
                className="select"
                value={status}
                onChange={(e) => {
                  setStatus(e.target.value);
                  setPage(0);
                }}
              >
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s === '' ? 'All' : s.replace('_', ' ')}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Supplier code">
            {(id) => (
              <input
                id={id}
                className="input"
                value={partyCode}
                onChange={(e) => {
                  setPartyCode(e.target.value.toUpperCase());
                  setPage(0);
                }}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={list.error} />
      <Card flush>
        <DataTable<Invoice>
          loading={list.isLoading}
          rows={list.data?.content ?? []}
          rowKey={(i) => i.id}
          caption="Supplier invoices"
          onRowClick={(i) => setSelected(i.id)}
          columns={[
            { key: 'd', header: 'Document', render: (i) => <strong>{i.documentNo}</strong> },
            { key: 'p', header: 'Supplier', render: (i) => i.partyCode },
            { key: 'n', header: 'Supplier Inv. No.', render: (i) => i.supplierInvoiceNo },
            { key: 'id', header: 'Date', render: (i) => formatDate(i.invoiceDate) },
            { key: 'du', header: 'Due', render: (i) => formatDate(i.dueDate) },
            {
              key: 'a',
              header: 'Payable',
              numeric: true,
              render: (i) => <Amount value={i.payableAmount} />,
            },
            { key: 'm', header: 'Maker', render: (i) => i.createdBy },
            { key: 's', header: 'Status', render: (i) => <StatusBadge status={i.status} /> },
          ]}
        />
        <Pager data={list.data} noun="invoices" onPage={setPage} />
      </Card>
      <Modal
        title={inv === undefined ? 'Invoice' : `${inv.documentNo} – ${inv.partyCode}`}
        open={selected !== null}
        onClose={() => setSelected(null)}
        footer={
          inv !== undefined && (
            <div className="row">
              {invoiceActions(inv, can).map((b) => (
                <Button
                  key={b.id}
                  variant={b.variant}
                  busy={
                    (b.id === 'submit' && submit.isPending) ||
                    (b.id === 'approve' && approve.isPending)
                  }
                  onClick={() => handlers[b.id](inv)}
                >
                  {b.label}
                </Button>
              ))}
            </div>
          )
        }
      >
        <ErrorAlert error={detail.error ?? submit.error ?? approve.error} />
        {inv !== undefined && <InvoiceDetail invoice={inv} />}
      </Modal>
      <DateReasonModal
        title={action === 'reject' ? 'Reject invoice' : 'Cancel invoice'}
        open={action !== null && inv !== undefined}
        withReason
        confirmLabel={action === 'reject' ? 'Reject' : 'Cancel invoice'}
        busy={reasoned.isPending}
        error={reasoned.error}
        onClose={() => setAction(null)}
        onConfirm={(v) =>
          inv !== undefined &&
          action !== null &&
          reasoned.mutate({ id: inv.id, kind: action, reason: v.reason })
        }
      />
    </div>
  );
}

function InvoiceDetail({ invoice }: Readonly<{ invoice: Invoice }>) {
  return (
    <div className="stack">
      <div className="grid-4">
        <div>
          <div className="muted">Supplier invoice</div>
          {invoice.supplierInvoiceNo}
        </div>
        <div>
          <div className="muted">Invoice / due date</div>
          {formatDate(invoice.invoiceDate)} / {formatDate(invoice.dueDate)}
        </div>
        <div>
          <div className="muted">Status</div>
          <StatusBadge status={invoice.status} />
          {invoice.statusReason !== undefined && (
            <div className="muted">{invoice.statusReason}</div>
          )}
        </div>
        <div>
          <div className="muted">Journals</div>
          {invoice.journalBatchNos ?? '—'}
        </div>
      </div>
      <DataTable
        rows={invoice.lines}
        rowKey={(l) => l.lineNo ?? 0}
        columns={[
          { key: 'a', header: 'Account', render: (l) => l.expenseAccountCode },
          { key: 'c', header: 'Cost Centre', render: (l) => l.costCenter ?? '' },
          { key: 'd', header: 'Description', render: (l) => l.description },
          { key: 'n', header: 'Net', numeric: true, render: (l) => <Amount value={l.netAmount} /> },
          { key: 'v', header: 'VAT', numeric: true, render: (l) => <Amount value={l.vatAmount} /> },
          { key: 'w', header: 'EWT', numeric: true, render: (l) => <Amount value={l.whtAmount} /> },
        ]}
      />
      <div className="row">
        <div className="spacer" />
        <span>
          Net <Amount value={invoice.netAmount} /> · VAT <Amount value={invoice.vatAmount} /> · EWT{' '}
          <Amount value={invoice.whtAmount} /> · <strong>Payable</strong>{' '}
          <Amount value={invoice.payableAmount} /> {invoice.currency}
        </span>
      </div>
    </div>
  );
}
