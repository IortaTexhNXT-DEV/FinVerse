import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { BadgeCheck, Building2, CalendarRange, Landmark, UserRound } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { accountsApi } from '@/api/accounts';
import type { Account } from '@/api/accounts';
import { bookingApi, ACCOUNT_ENTITY } from '@/api/booking';
import type { BookingPreview, BookRequest, InvoiceDraft } from '@/api/booking';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { formatAmount, formatDate, today } from '@/utils/format';
import { JournalLines, PremiumTables, SummaryFact } from './BookingParts';

interface Options {
  bookingDate: string;
  costCenter: string;
  cwt2Percent: boolean;
}

function requestOf(arn: string, o: Options): BookRequest {
  return {
    arn,
    bookingDate: o.bookingDate,
    costCenter: o.costCenter.trim() || undefined,
    cwt2Percent: o.cwt2Percent ? true : undefined,
  };
}

function OptionsForm({
  value,
  onChange,
}: Readonly<{ value: Options; onChange: (o: Options) => void }>) {
  const future = value.bookingDate === '' || value.bookingDate > today();
  return (
    <div className="form-grid">
      <Field
        label="Booking date"
        required
        error={future ? 'Enter a booking date that is not in the future' : undefined}
      >
        {(id) => (
          <input
            id={id}
            className="input"
            type="date"
            max={today()}
            value={value.bookingDate}
            onChange={(e) => onChange({ ...value, bookingDate: e.target.value })}
          />
        )}
      </Field>
      <Field label="Cost center" hint="Blank: the account's cost center (sales organisation).">
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={20}
            value={value.costCenter}
            onChange={(e) => onChange({ ...value, costCenter: e.target.value.toUpperCase() })}
          />
        )}
      </Field>
      <label className="checkbox">
        <input
          type="checkbox"
          checked={value.cwt2Percent}
          onChange={(e) => onChange({ ...value, cwt2Percent: e.target.checked })}
        />
        Client withholds 2% creditable tax (CWT)
      </label>
    </div>
  );
}

function YearsTable({ invoices }: Readonly<{ invoices: InvoiceDraft[] }>) {
  return (
    <DataTable<InvoiceDraft>
      caption="Policy years"
      rows={invoices}
      rowKey={(i) => i.transactionNo}
      columns={[
        { key: 'year', header: 'Policy Year', render: (i) => i.policyYear },
        { key: 'policy', header: 'Policy No.', render: (i) => i.policyNo ?? '' },
        {
          key: 'period',
          header: 'Period',
          render: (i) => `${formatDate(i.inceptionDate)} – ${formatDate(i.expiryDate)}`,
        },
        {
          key: 'gross',
          header: 'Gross Premium',
          numeric: true,
          render: (i) => formatAmount(i.premium.total),
        },
        {
          key: 'when',
          header: 'Booked',
          render: (i) => <StatusBadge status={i.policyYear === 1 ? 'NOW' : 'SCHEDULED'} />,
        },
      ]}
    />
  );
}

function AccountSummary({
  arn,
  account: a,
}: Readonly<{ arn: string; account: Account | undefined }>) {
  if (a === undefined) {
    return null;
  }
  return (
    <Card>
      <div className="summary-card">
        <SummaryFact icon={BadgeCheck} label="Proposal No. (ARN)">
          <ReferenceChip value={arn} />
        </SummaryFact>
        <SummaryFact icon={UserRound} label="Client">
          {a.clientName} <span className="muted">{a.clientCode}</span>
        </SummaryFact>
        <SummaryFact icon={Building2} label="Insurer">
          {a.insurerCode} {a.insurerBranch}
        </SummaryFact>
        <SummaryFact icon={CalendarRange} label="Period">
          {formatDate(a.periodFrom)} – {formatDate(a.periodTo)}
        </SummaryFact>
        <SummaryFact icon={Landmark} label="Product">
          {a.productCode} · {a.lineCode}
        </SummaryFact>
        <SummaryFact icon={BadgeCheck} label="Status">
          <StatusBadge status={a.status} />
        </SummaryFact>
      </div>
    </Card>
  );
}

function InvoicePreview({ preview }: Readonly<{ preview: BookingPreview }>) {
  const first = preview.invoices[0];
  if (first === undefined) {
    return null;
  }
  const flags = first.flags;
  return (
    <>
      <Card title="Invoice">
        <div className="stack">
          <div className="record-facts">
            <span className="muted">Cost center</span> <strong>{first.facts.costCenter}</strong>
            {flags.directPayment && <StatusBadge status="DIRECT_PAYMENT" />}
            {flags.cwt2Percent && <StatusBadge status="CWT_2_PERCENT" />}
            {flags.incentiveEligible && <StatusBadge status="INCENTIVE_ELIGIBLE" />}
            <StatusBadge status={flags.businessType} />
          </div>
          <PremiumTables premium={first.premium} commission={first.commission} />
          {preview.invoices.length > 1 && <YearsTable invoices={preview.invoices} />}
        </div>
      </Card>
      <Card title="Journal preview">
        <JournalLines lines={preview.journal} />
      </Card>
    </>
  );
}

/**
 * Pre-booking confirmation (BRNB.027/036): the invoice the account will be booked with - premium
 * components, commission, withholding tax, flags and, for a multi-year account, the scheduled policy
 * years - and the journal the accounting rules will post. Nothing is posted until Book Account.
 */
export default function BookingConfirmPage() {
  const arn = decodeURIComponent(useParams().arn ?? '');
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [options, setOptions] = useState<Options>({
    bookingDate: today(),
    costCenter: '',
    cwt2Percent: false,
  });
  const [applied, setApplied] = useState<Options>(options);
  const account = useQuery({
    queryKey: ['account', 'arn', arn],
    queryFn: () => accountsApi.byArn(arn),
  });
  const preview = useQuery({
    queryKey: ['booking', 'preview', arn, applied],
    queryFn: () => bookingApi.preview(requestOf(arn, applied)),
    retry: false,
  });
  const book = useMutation({
    mutationFn: () => bookingApi.book(requestOf(arn, applied)),
    onSuccess: async (result) => {
      await queryClient.invalidateQueries({ queryKey: ['booking'] });
      toast.success(`${arn} booked as ${result.invoiceNo}`);
      void navigate(`/booking/invoices/${String(result.id)}`);
    },
  });
  const changed = JSON.stringify(options) !== JSON.stringify(applied);
  return (
    <div className="stack">
      <PageHeader
        section="Booking · Pre-booking Confirmation"
        backTo="/booking"
        title={account.data?.clientName ?? arn}
        description="Check the invoice and the journal, then book the account."
        actions={
          <>
            <Button variant="secondary" onClick={() => void navigate('/booking')}>
              Cancel
            </Button>
            <Button
              busy={book.isPending}
              disabled={preview.data === undefined || changed}
              onClick={() => book.mutate()}
            >
              Book Account
            </Button>
          </>
        }
      />
      <ErrorAlert error={account.error ?? book.error} />
      <AccountSummary arn={arn} account={account.data} />
      {account.data && (
        <WorkflowPanel entityType={ACCOUNT_ENTITY} entityId={account.data.id} showHistory={false} />
      )}
      <Card
        title="Booking options"
        actions={
          <Button variant="secondary" disabled={!changed} onClick={() => setApplied(options)}>
            Update Preview
          </Button>
        }
      >
        <OptionsForm value={options} onChange={setOptions} />
      </Card>
      <ErrorAlert error={preview.error} />
      {preview.isLoading && <span className="spinner" aria-label="Loading" />}
      {preview.data && <InvoicePreview preview={preview.data} />}
    </div>
  );
}
