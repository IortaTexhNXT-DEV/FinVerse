import { useQuery } from '@tanstack/react-query';
import { CalendarRange, FilePlus2, Landmark, ShieldCheck, UserRound, Wallet } from 'lucide-react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { RecordSummary } from '@/components/broking/RecordSummary';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate, humanize } from '@/utils/format';
import { CLAIMS_SECTION } from '../ClaimsPlaceholder';
import type { CoverDetail } from './api';
import { coverApi } from './api';
import { CoverSearch } from './CoverSearch';
import { CoverTabs } from './CoverTabs';

function CoverSummary({ cover }: Readonly<{ cover: CoverDetail }>) {
  const h = cover.header;
  return (
    <RecordSummary
      title={h.assuredName}
      chips={
        <>
          <ReferenceChip label="ARN" value={h.arn} />
          <StatusBadge status={h.status} />
        </>
      }
      flags={
        cover.paymentArrangement === 'DIRECT_TO_INSURER' ? (
          <span className="tag">Direct Payment</span>
        ) : undefined
      }
      facts={[
        {
          icon: ShieldCheck,
          label: 'Product / Insurer',
          value: `${h.productCode} · ${h.insurerCode ?? ''}`,
        },
        {
          icon: CalendarRange,
          label: 'Period',
          value: `${formatDate(h.periodFrom)} – ${formatDate(h.periodTo)} (${cover.termYears} year(s))`,
        },
        {
          icon: Wallet,
          label: `Sum Insured (${h.currency ?? ''})`,
          value: formatAmount(cover.sumInsured),
        },
        { icon: Landmark, label: 'Policy No.', value: h.policyNumbers.join(', ') || 'Pending' },
        {
          icon: UserRound,
          label: 'Marketing Team / AO',
          value: `${cover.salesTeam ?? '—'} · ${cover.accountOfficer ?? '—'}`,
        },
        { icon: ShieldCheck, label: 'Line', value: humanize(h.lineCode ?? '') },
      ]}
    />
  );
}

/**
 * Cover Lookup (BRCLM.002/003/042; FR-CL-010): any cover of the company, read-only - account,
 * policy years and numbers, items and locations, endorsements, invoices with their payment and
 * remittance status, the claims of the cover and the insurer location references. Record Claim is
 * the only action.
 */
export default function CoverLookupPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const arn = params.get('arn') ?? '';
  const cover = useQuery({
    queryKey: ['broker-claims', 'cover', companyId, arn],
    queryFn: () => coverApi.cover(companyId, arn),
    enabled: arn !== '',
  });
  return (
    <div className="stack">
      <PageHeader
        section={CLAIMS_SECTION}
        title="Cover Lookup"
        description="Check the policy coverages of any cover: account, policy years, locations, endorsements, invoices with their premium status, claims and insurer location references. Nothing can be changed here."
        backTo={arn === '' ? undefined : '/claims-handling/covers'}
        actions={
          arn !== '' && can('BCL_RECORD') ? (
            <Button
              variant="accent"
              icon={<FilePlus2 size={16} />}
              onClick={() => void navigate(`/claims-handling/new?arn=${encodeURIComponent(arn)}`)}
            >
              Record Claim
            </Button>
          ) : undefined
        }
      />
      {arn === '' ? (
        <CoverSearch onSelect={(c) => setParams({ arn: c.arn })} />
      ) : (
        <>
          <ErrorAlert error={cover.error} />
          {cover.isLoading && <span className="spinner" aria-label="Loading" />}
          {cover.data && <CoverSummary cover={cover.data} />}
          {cover.data && <CoverTabs cover={cover.data} />}
        </>
      )}
    </div>
  );
}
