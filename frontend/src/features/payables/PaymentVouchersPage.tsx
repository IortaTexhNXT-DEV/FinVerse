import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { saveFile } from '@/api/client';
import { payablesApi } from '@/api/payables';
import type { Voucher } from '@/api/payables';
import { reportApi } from '@/api/reports';
import type { ExportFormat } from '@/api/reports';
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
import { ExportButtons } from '@/features/reports/ExportButtons';
import { DOCUMENT_FORMATS } from '@/features/reports/exportFormats';
import { humanize, formatDate } from '@/utils/format';
import { DateReasonModal } from './DateReasonModal';
import { firstError, voucherActions } from './payablesActions';
import type { VoucherActionId } from './payablesActions';
import { usePayablesLookups } from './usePayablesLookups';

const STATUSES = ['', 'DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'VOIDED', 'CANCELLED'];
type Action = 'reject' | 'cancel' | 'void' | 'presented';

const ACTION_TITLES: Record<Action, string> = {
  reject: 'Reject voucher',
  cancel: 'Cancel voucher',
  void: 'Void cheque (reversal)',
  presented: 'Confirm cheque presentation',
};

function approvedMessage(v: Voucher): string {
  return v.chequeNo === undefined ? 'approved and posted' : `approved, cheque ${v.chequeNo}`;
}

/**
 * Payment vouchers: approval queue, posting, cheque presentation / void and the voucher form as PDF
 * or Word.
 */
export default function PaymentVouchersPage() {
  const { companyId, bankName } = usePayablesLookups();
  const { can } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [status, setStatus] = useState('PENDING_APPROVAL');
  const [partyCode, setPartyCode] = useState('');
  const [selected, setSelected] = useState<number | null>(null);
  const [action, setAction] = useState<Action | null>(null);

  const list = useQuery({
    queryKey: ['vouchers', companyId, status, partyCode],
    queryFn: () => payablesApi.vouchers({ companyId, status, partyCode, size: 50 }),
    enabled: companyId > 0,
  });
  const detail = useQuery({
    queryKey: ['voucher', selected],
    queryFn: () => payablesApi.voucher(selected ?? 0),
    enabled: selected !== null,
  });
  const done = async (v: Voucher, message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['vouchers'] });
    await queryClient.invalidateQueries({ queryKey: ['voucher', v.id] });
    // Approving, rejecting or voiding a voucher changes what New Payment may still pay.
    await queryClient.invalidateQueries({ queryKey: ['payable-items'] });
    setAction(null);
    toast.success(`${v.voucherNo} ${message}`);
  };
  const submit = useMutation({
    mutationFn: payablesApi.submitVoucher,
    onSuccess: (v) => done(v, 'submitted for approval'),
  });
  const approve = useMutation({
    mutationFn: payablesApi.approveVoucher,
    onSuccess: (v) => done(v, approvedMessage(v)),
  });
  const act = useMutation({
    mutationFn: ({
      v,
      kind,
      date,
      reason,
    }: {
      v: Voucher;
      kind: Action;
      date: string;
      reason: string;
    }) => {
      switch (kind) {
        case 'reject':
          return payablesApi.rejectVoucher(v.id, reason);
        case 'cancel':
          return payablesApi.cancelVoucher(v.id, reason);
        case 'void':
          return payablesApi.voidVoucher(v.id, date, reason);
        default:
          return payablesApi.presentedVoucher(v.id, date);
      }
    },
    onSuccess: (v) => done(v, humanize(v.status).toLowerCase()),
  });
  const print = useMutation({
    mutationFn: ({ v, format }: { v: Voucher; format: ExportFormat }) =>
      reportApi.export(
        'FIN-AP-VOUCHER',
        {
          companyId: String(companyId),
          voucherNo: v.voucherNo,
          fromDate: v.voucherDate,
          toDate: v.voucherDate,
          status: 'ALL',
        },
        format,
      ),
    onSuccess: ({ blob, fileName }) => saveFile(blob, fileName),
  });
  const v = detail.data;
  const handlers: Record<VoucherActionId, (x: Voucher) => void> = {
    submit: (x) => submit.mutate(x.id),
    approve: (x) => approve.mutate(x.id),
    reject: () => setAction('reject'),
    cancel: () => setAction('cancel'),
    presented: () => setAction('presented'),
    void: () => setAction('void'),
  };

  return (
    <div className="stack">
      <PageHeader
        section="Payables & Cash"
        title="Payment Vouchers"
        description="Pay open payables of suppliers, intermediaries, claimants, reinsurers and policyholders by cheque, transfer or PDC."
        actions={
          can('RECEIPT_PAYMENT_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => void navigate('/payables/vouchers/new')}
            >
              New Payment
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
                onChange={(e) => setStatus(e.target.value)}
              >
                {STATUSES.map((s) => (
                  <option key={s} value={s}>
                    {s === '' ? 'All' : humanize(s)}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Payee code">
            {(id) => (
              <input
                id={id}
                className="input"
                value={partyCode}
                onChange={(e) => setPartyCode(e.target.value.toUpperCase())}
              />
            )}
          </Field>
        </div>
      </Card>
      <ErrorAlert error={firstError(list.error, print.error)} />
      <Card flush>
        <DataTable<Voucher>
          loading={list.isLoading}
          rows={list.data?.content ?? []}
          rowKey={(r) => r.id}
          caption="Payment vouchers"
          onRowClick={(r) => setSelected(r.id)}
          columns={[
            { key: 'n', header: 'Voucher', render: (r) => <strong>{r.voucherNo}</strong> },
            { key: 'd', header: 'Date', render: (r) => formatDate(r.voucherDate) },
            { key: 'p', header: 'Payee', render: (r) => `${r.partyCode} ${r.payeeName}` },
            { key: 'c', header: 'Category', render: (r) => humanize(r.category) },
            { key: 'm', header: 'Mode', render: (r) => humanize(r.paymentMode) },
            { key: 'b', header: 'Bank', render: (r) => bankName(r.bankAccountId) },
            { key: 'q', header: 'Cheque', render: (r) => r.chequeNo ?? '' },
            {
              key: 'a',
              header: 'Amount',
              numeric: true,
              render: (r) => <Amount value={r.amount} />,
            },
            { key: 's', header: 'Status', render: (r) => <StatusBadge status={r.status} /> },
          ]}
        />
      </Card>
      <Modal
        title={v === undefined ? 'Payment voucher' : `${v.voucherNo} – ${v.payeeName}`}
        open={selected !== null}
        onClose={() => setSelected(null)}
        footer={
          v !== undefined && (
            <div className="row">
              <ExportButtons
                formats={DOCUMENT_FORMATS}
                variant="ghost"
                pending={print.isPending ? print.variables.format : undefined}
                onExport={(format) => print.mutate({ v, format })}
              />
              {voucherActions(v, can).map((b) => (
                <Button
                  key={b.id}
                  variant={b.variant}
                  busy={
                    (b.id === 'submit' && submit.isPending) ||
                    (b.id === 'approve' && approve.isPending)
                  }
                  onClick={() => handlers[b.id](v)}
                >
                  {b.label}
                </Button>
              ))}
            </div>
          )
        }
      >
        <ErrorAlert error={firstError(detail.error, submit.error, approve.error)} />
        {v !== undefined && <VoucherDetail voucher={v} bank={bankName(v.bankAccountId)} />}
      </Modal>
      <DateReasonModal
        title={action === null ? '' : ACTION_TITLES[action]}
        open={action !== null && v !== undefined}
        withDate={action === 'void' || action === 'presented'}
        withReason={action !== 'presented'}
        confirmLabel="Confirm"
        busy={act.isPending}
        error={act.error}
        onClose={() => setAction(null)}
        onConfirm={(val) =>
          v !== undefined &&
          action !== null &&
          act.mutate({ v, kind: action, date: val.date, reason: val.reason })
        }
      />
    </div>
  );
}

function VoucherDetail({ voucher, bank }: Readonly<{ voucher: Voucher; bank: string }>) {
  return (
    <div className="stack">
      <div className="grid-4">
        <div>
          <div className="muted">Mode / bank</div>
          {humanize(voucher.paymentMode)} · {bank}
        </div>
        <div>
          <div className="muted">Cheque</div>
          {voucher.chequeNo ?? '—'}{' '}
          {voucher.chequeDate !== undefined && `dated ${formatDate(voucher.chequeDate)}`}
        </div>
        <div>
          <div className="muted">Journal</div>
          {voucher.journalBatchNo ?? '—'}
        </div>
        <div>
          <div className="muted">Status</div>
          <StatusBadge status={voucher.status} />
          {voucher.statusReason !== undefined && (
            <div className="muted">{voucher.statusReason}</div>
          )}
        </div>
      </div>
      <DataTable
        rows={voucher.allocations}
        rowKey={(a) => a.openItemId}
        columns={[
          { key: 'n', header: 'Document', render: (a) => a.documentNo },
          { key: 't', header: 'Type', render: (a) => humanize(a.documentType) },
          { key: 'd', header: 'Date', render: (a) => formatDate(a.documentDate) },
          { key: 'u', header: 'Due', render: (a) => formatDate(a.dueDate) },
          { key: 'a', header: 'Paid', numeric: true, render: (a) => <Amount value={a.amount} /> },
        ]}
      />
      <div className="row">
        <div className="spacer" />
        <strong>
          Total <Amount value={voucher.amount} /> {voucher.currency}
        </strong>
      </div>
    </div>
  );
}
