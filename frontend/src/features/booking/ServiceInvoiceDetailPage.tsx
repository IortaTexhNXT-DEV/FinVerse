import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  Building2,
  CalendarDays,
  Download,
  FileText,
  Mail,
  Receipt,
  Undo2,
  UserRound,
} from 'lucide-react';
import { useState } from 'react';
import { useParams } from 'react-router-dom';
import { bookingApi, SERVICE_INVOICE_ENTITY } from '@/api/booking';
import type { ServiceInvoice } from '@/api/booking';
import { saveFile } from '@/api/client';
import { useAuth } from '@/auth/authContext';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { SentMessages } from '@/components/broking/SentMessages';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate } from '@/utils/format';
import { SummaryFact } from './BookingParts';

function CreditDialog({ si, onClose }: Readonly<{ si: ServiceInvoice; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [commission, setCommission] = useState('');
  const [vat, setVat] = useState('');
  const [reason, setReason] = useState('');
  const credit = useMutation({
    mutationFn: () =>
      bookingApi.credit(si.id, {
        commission: commission === '' ? undefined : Number(commission),
        vatOnCommission: vat === '' ? undefined : Number(vat),
        reason: reason.trim(),
      }),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: ['booking'] });
      toast.success(`Credit ${c.siNo} issued`);
      onClose();
    },
  });
  const missing = reason.trim() === '';
  return (
    <Modal
      open
      title={`Credit ${si.siNo}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={credit.isPending} disabled={missing} onClick={() => credit.mutate()}>
            Issue Credit
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={credit.error} />
        <Field label="Commission to credit" hint="Blank: all the commission still open.">
          {(id) => (
            <input
              id={id}
              className="input num"
              type="number"
              step="0.01"
              min="0"
              value={commission}
              onChange={(e) => setCommission(e.target.value)}
            />
          )}
        </Field>
        <Field label="VAT to credit" hint="Blank: all the VAT still open.">
          {(id) => (
            <input
              id={id}
              className="input num"
              type="number"
              step="0.01"
              min="0"
              value={vat}
              onChange={(e) => setVat(e.target.value)}
            />
          )}
        </Field>
        <Field
          label="Reason"
          required
          error={missing ? 'Give the reason of the credit' : undefined}
        >
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              maxLength={500}
              value={reason}
              onChange={(e) => setReason(e.target.value)}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

function Lines({ si }: Readonly<{ si: ServiceInvoice }>) {
  return (
    <dl className="detail-list">
      <dt>Brokerage commission</dt>
      <dd>{formatAmount(si.commission)}</dd>
      <dt>VAT on commission</dt>
      <dd>{formatAmount(si.vatOnCommission)}</dd>
      <dt>Less: withholding tax</dt>
      <dd>{formatAmount(-si.wtaxAmount)}</dd>
      <dt>Net amount</dt>
      <dd>
        <strong>
          {si.currency} {formatAmount(si.netAmount)}
        </strong>
      </dd>
    </dl>
  );
}

function Facts({ si: s }: Readonly<{ si: ServiceInvoice }>) {
  return (
    <Card>
      <div className="stack">
        <div className="record-facts">
          <ReferenceChip label="Service invoice" value={s.siNo} />
          {s.invoiceNo && <ReferenceChip label="Invoice" value={s.invoiceNo} />}
          {s.arn && <ReferenceChip label="ARN" value={s.arn} />}
          <StatusBadge status={s.kind} />
          <StatusBadge status={s.dispatchStatus} />
        </div>
        <div className="summary-card">
          <SummaryFact icon={Building2} label="Recipient">
            {s.recipientName} <span className="muted">{s.recipientCode}</span>
          </SummaryFact>
          <SummaryFact icon={Mail} label="Billing e-mail">
            {s.recipientEmail ?? 'None'}
          </SummaryFact>
          <SummaryFact icon={CalendarDays} label="Issue date">
            {formatDate(s.issueDate)}
          </SummaryFact>
          <SummaryFact icon={FileText} label="Template">
            {s.templateCode} v{s.templateVersion}
          </SummaryFact>
          <SummaryFact icon={UserRound} label="Owner">
            {s.ownerUsername ?? s.ownerPermission ?? ''}
          </SummaryFact>
          <SummaryFact icon={Receipt} label="Credit of">
            {s.creditOf ?? '-'}
          </SummaryFact>
        </div>
        {s.dispatchError && <div className="alert warning">E-mail: {s.dispatchError}</div>}
      </div>
    </Card>
  );
}

/**
 * A service invoice or credit (BRNB.100/100b): recipient, lines, template version, e-mail
 * outcome and send log; download the PDF as issued, send it again or credit it.
 */
export default function ServiceInvoiceDetailPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [crediting, setCrediting] = useState(false);
  const si = useQuery({
    queryKey: ['booking', 'service-invoice', id],
    queryFn: () => bookingApi.serviceInvoice(id),
  });
  const resend = useMutation({
    mutationFn: () => bookingApi.resend(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['booking'] });
      toast.success('Service invoice sent again');
    },
  });
  const download = useMutation({
    mutationFn: () => bookingApi.serviceInvoicePdf(id),
    onSuccess: (file) => saveFile(file.blob, file.fileName),
  });
  if (si.data === undefined) {
    return si.error ? (
      <ErrorAlert error={si.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const s = si.data;
  return (
    <div className="stack">
      <PageHeader
        section="Booking · Service Invoice"
        backTo="/booking/service-invoices"
        title={s.siNo}
        description={`${s.kind === 'CREDIT' ? 'Credit' : 'Service invoice'} · ${s.typeCode}`}
        actions={
          <>
            <Button
              variant="secondary"
              icon={<Download size={16} />}
              busy={download.isPending}
              onClick={() => download.mutate()}
            >
              Download PDF
            </Button>
            <Button
              variant="secondary"
              icon={<Mail size={16} />}
              busy={resend.isPending}
              onClick={() => resend.mutate()}
            >
              Send via Email
            </Button>
            {s.kind === 'INVOICE' && can('BOOKING_ADJUST') && (
              <Button icon={<Undo2 size={16} />} onClick={() => setCrediting(true)}>
                Credit
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={resend.error ?? download.error} />
      <Facts si={s} />
      <Card title="Lines">
        <Lines si={s} />
      </Card>
      <Card title="E-mails" flush>
        <SentMessages entityType={SERVICE_INVOICE_ENTITY} entityId={s.id} />
      </Card>
      {crediting && <CreditDialog si={s} onClose={() => setCrediting(false)} />}
    </div>
  );
}
