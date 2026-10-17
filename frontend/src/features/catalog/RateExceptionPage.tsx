import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  BadgePercent,
  CalendarDays,
  CheckCircle2,
  FileText,
  Package,
  UserRound,
  XCircle,
} from 'lucide-react';
import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { productCatalogApi } from '@/api/productCatalog';
import type { RateExceptionDetail } from '@/api/productCatalog';
import { quotationsApi } from '@/api/quotations';
import { useAuth } from '@/auth/authContext';
import { ActionDialog } from '@/components/broking/ActionDialog';
import { RecordSummary } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, formatDateTime } from '@/utils/format';
import {
  exceptionStatus,
  exceptionSubject,
  mayDecide,
  requestedValue,
  schemeInForce,
} from './rateException';
import { LoadingPanel } from '@/components/ui/LoadingPanel';

/** The quotation (or account) the exception is for, opened from the record when it is found. */
function Transaction({ reference }: Readonly<{ reference: string }>) {
  const { can } = useAuth();
  const companyId = useCompanyId();
  const found = useQuery({
    queryKey: ['quotations', 'by-number', companyId, reference],
    queryFn: () => quotationsApi.search(companyId, { text: reference }, 0, 5),
    enabled: companyId > 0 && can('QUOTE_VIEW'),
  });
  const quotation = found.data?.content.find((q) => q.quotationNo === reference);
  if (quotation === undefined) {
    return <span className="mono">{reference}</span>;
  }
  return (
    <span className="cell-stack">
      <Link to={`/quotations/${quotation.id}`}>{reference}</Link>
      <span className="muted">
        {quotation.clientName}
        {quotation.grossPremium !== undefined && (
          <>
            {` · gross premium ${quotation.currency} `}
            <Amount value={quotation.grossPremium} />
          </>
        )}
      </span>
    </span>
  );
}

function Decision({ detail }: Readonly<{ detail: RateExceptionDetail }>) {
  const e = detail.exception;
  const status = exceptionStatus(e);
  if (status === 'PENDING_AUTHORIZATION') {
    return (
      <p className="muted">
        Waiting for an approver other than the requester. Approve lets the quotation be submitted
        with this rate; Reject keeps the scheme rate, and the requester is told why.
      </p>
    );
  }
  return (
    <dl className="detail-list">
      <dt>{status === 'APPROVED' ? 'Approved by' : 'Rejected by'}</dt>
      <dd>
        <UserName login={e.decidedBy ?? e.authorizedBy} withRole />
      </dd>
      <dt>On</dt>
      <dd>{e.decidedAt === undefined ? '—' : formatDateTime(e.decidedAt)}</dd>
      <dt>{status === 'APPROVED' ? 'Comment' : 'Reason'}</dt>
      <dd>{e.decisionComment ?? '—'}</dd>
    </dl>
  );
}

type Pending = 'approve' | 'reject' | null;

function exceptionFacts(d: RateExceptionDetail): Fact[] {
  const e = d.exception;
  return [
    { icon: Package, label: 'Package', value: `${e.productCode} – ${d.productName}` },
    {
      icon: BadgePercent,
      label: e.requestedVersionNo === undefined ? 'Requested rate' : 'Requested version',
      value: requestedValue(e),
    },
    { icon: BadgePercent, label: 'Scheme in force', value: schemeInForce(d) },
    { icon: CalendarDays, label: 'Valid until', value: formatDate(e.validUntil) },
    { icon: FileText, label: 'Quotation', value: <Transaction reference={e.transactionRef} /> },
    {
      icon: UserRound,
      label: 'Requested by',
      value: (
        <span className="cell-stack">
          <UserName login={e.requestedBy} />
          <span className="muted">{formatDateTime(e.requestedAt)}</span>
        </span>
      ),
    },
  ];
}

/**
 * A rate-scheme exception (BRPM.007, FR-PM-051, SCR-PM-22), opened from My Approvals or the
 * requester's notice: what is asked (rate or version, validity, reason) against the scheme in
 * force, for which quotation and by whom; an approver other than the requester approves it with an
 * optional comment or rejects it with a reason.
 */
export default function RateExceptionPage() {
  const reference = useParams().reference ?? '';
  const { user, can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [pending, setPending] = useState<Pending>(null);
  const detail = useQuery({
    queryKey: ['rate-exception', reference],
    queryFn: () => productCatalogApi.rateException(reference),
  });
  const decide = useMutation({
    mutationFn: ({ approve, comment }: { approve: boolean; comment?: string }) =>
      approve
        ? productCatalogApi.approveRateException(reference, comment)
        : productCatalogApi.rejectRateException(reference, comment ?? ''),
    onSuccess: async (e) => {
      setPending(null);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['rate-exception', reference] }),
        queryClient.invalidateQueries({ queryKey: ['rate-exceptions', e.transactionRef] }),
        queryClient.invalidateQueries({ queryKey: ['approvals'] }),
      ]);
      toast.success(
        `Rate exception ${e.referenceNo} ${e.recordStatus === 'ACTIVE' ? 'approved' : 'rejected'}`,
      );
    },
  });
  if (detail.data === undefined) {
    return detail.error ? <ErrorAlert error={detail.error} /> : <LoadingPanel />;
  }
  const d = detail.data;
  const e = d.exception;
  const decidable = mayDecide(e, user?.username, can);
  return (
    <div className="stack">
      <PageHeader
        backTo="/approvals"
        section="Product Maintenance · Rate Exception"
        title={e.referenceNo}
        description={`${exceptionSubject(e)} for ${e.transactionRef}`}
        actions={
          decidable && (
            <>
              <Button
                variant="secondary"
                icon={<XCircle size={16} />}
                onClick={() => setPending('reject')}
              >
                Reject
              </Button>
              <Button
                variant="accent"
                icon={<CheckCircle2 size={16} />}
                onClick={() => setPending('approve')}
              >
                Approve
              </Button>
            </>
          )
        }
      />
      <RecordSummary
        title={`${d.productName} – ${e.transactionRef}`}
        chips={
          <>
            <ReferenceChip label="Exception" value={e.referenceNo} />
            <StatusBadge status={exceptionStatus(e)} />
          </>
        }
        facts={exceptionFacts(d)}
      />
      <div className="grid-2">
        <Card title="Reason for the exception">
          <p className="message-body">{e.reason}</p>
        </Card>
        <Card title="Decision">
          <Decision detail={d} />
        </Card>
      </div>
      {pending !== null && (
        <ActionDialog
          title={pending === 'approve' ? 'Approve Rate Exception' : 'Reject Rate Exception'}
          record={`${e.referenceNo} · ${exceptionSubject(e)} · ${e.transactionRef}`}
          effect={
            pending === 'approve'
              ? 'The quotation can then be submitted with this rate; the requester is notified.'
              : 'The quotation keeps the scheme rate; the requester is notified with the reason.'
          }
          commentRequired={pending === 'reject'}
          commentLabel={pending === 'approve' ? 'Comment' : 'Reason for the rejection'}
          confirmLabel={pending === 'approve' ? 'Approve' : 'Reject'}
          busy={decide.isPending}
          error={decide.error}
          onConfirm={(note) =>
            decide.mutate({ approve: pending === 'approve', comment: note.comment })
          }
          onClose={() => setPending(null)}
        />
      )}
    </div>
  );
}
