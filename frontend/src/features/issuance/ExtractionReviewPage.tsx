import { useQuery } from '@tanstack/react-query';
import { Download } from 'lucide-react';
import { Link, useParams } from 'react-router-dom';
import { attachmentsApi } from '@/api/attachments';
import { issuanceApi } from '@/api/issuance';
import type { Review } from '@/api/issuance';
import { useAuth } from '@/auth/authContext';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { useFileDownload } from '@/components/broking/useFileDownload';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { ConfirmPolicyCard } from './ConfirmPolicyCard';

interface Row {
  field: string;
  extracted: string;
  account: string;
}

function rowsOf(r: Review): Row[] {
  const e = r.epolicy;
  const a = r.account;
  return [
    {
      field: 'Policy number(s)',
      extracted: e.extractedPolicyNumbers.join(', '),
      account: a.policyNumbers.join(', ') || `${a.termYears} expected`,
    },
    {
      field: 'Period start',
      extracted: formatDate(e.extractedPeriodFrom),
      account: formatDate(a.periodFrom),
    },
    {
      field: 'Period end',
      extracted: formatDate(e.extractedPeriodTo),
      account: formatDate(a.periodTo),
    },
    {
      field: 'Premium',
      extracted: formatAmount(e.extractedPremium),
      account: formatAmount(a.grossPremium),
    },
  ];
}

function Comparison({ review }: Readonly<{ review: Review }>) {
  return (
    <Card title="Extracted Values and Account" flush>
      <div className="table-wrap">
        <table className="table compare-table">
          <caption className="visually-hidden">Extracted values and account values</caption>
          <thead>
            <tr>
              <th>Field</th>
              <th>Extracted from the e-policy</th>
              <th>On the account</th>
            </tr>
          </thead>
          <tbody>
            {rowsOf(review).map((r) => (
              <tr key={r.field}>
                <td>{r.field}</td>
                <td
                  className={
                    r.extracted !== '' && r.extracted !== r.account ? 'differs' : undefined
                  }
                >
                  {r.extracted || <span className="muted">not found</span>}
                </td>
                <td>{r.account || '—'}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  );
}

/**
 * Extraction review (BRNB.074/104): the values read from the e-policy next to the account
 * values, the differences to check, and the confirmation that records the policy number(s) on
 * the account. Extraction reads the PDF text with the insurer's patterns; scanned policies are
 * completed by hand (OCR parked, Q24).
 */
export default function ExtractionReviewPage() {
  const id = Number(useParams().id);
  const { can } = useAuth();
  const download = useFileDownload();
  const review = useQuery({
    queryKey: ['issuance', 'review', id],
    queryFn: () => issuanceApi.review(id),
  });
  if (review.data === undefined) {
    return review.error ? (
      <ErrorAlert error={review.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const r = review.data;
  const open = r.epolicy.status === 'REVIEW' || r.epolicy.status === 'RECEIVED';
  return (
    <div className="stack">
      <PageHeader
        backTo="/issuance"
        section="Policy Issuance · Extraction Review"
        title={r.account.clientName}
        description={`${r.epolicy.fileName} · received ${formatDateTime(r.epolicy.createdAt)} by ${r.epolicy.createdBy} · matched by ${humanize(r.epolicy.matchMethod)}`}
        actions={
          <>
            <ReferenceChip label="ARN" value={r.account.arn} />
            <StatusBadge status={r.epolicy.status} />
            <Button
              variant="secondary"
              icon={<Download size={16} />}
              onClick={() => download.mutate(() => attachmentsApi.download(r.epolicy.attachmentId))}
            >
              Download E-policy
            </Button>
            <Link className="btn btn-secondary" to={`/placement/accounts/${r.account.arn}`}>
              Open Account
            </Link>
          </>
        }
      />
      <ErrorAlert error={download.error} />
      {r.epolicy.extractionNote !== undefined && (
        <div className="alert" role="status">
          Extraction: {r.epolicy.extractionNote}
        </div>
      )}
      {r.differences.length > 0 && (
        <div className="alert warning" role="alert">
          <strong>Check before confirming</strong>
          <ul className="field-errors">
            {r.differences.map((d) => (
              <li key={d}>{d}</li>
            ))}
          </ul>
        </div>
      )}
      <Comparison review={r} />
      {open && can('EPOLICY_MANAGE') && <ConfirmPolicyCard key={r.epolicy.id} review={r} />}
      {r.epolicy.status === 'CONFIRMED' && (
        <div className="alert success" role="status">
          Policy {r.epolicy.policyNumbers.join(', ')} confirmed by {r.epolicy.reviewedBy} on{' '}
          {formatDateTime(r.epolicy.reviewedAt)}; the account is{' '}
          {humanize(r.account.status).toLowerCase()}.
        </div>
      )}
      {r.epolicy.status === 'REJECTED' && (
        <div className="alert danger" role="status">
          Rejected by {r.epolicy.reviewedBy}: {humanize(r.epolicy.rejectReason ?? '')}
        </div>
      )}
    </div>
  );
}
