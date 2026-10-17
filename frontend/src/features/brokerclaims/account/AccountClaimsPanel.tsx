import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDate } from '@/utils/format';
import { claimsHomeApi } from '../home/api';
import type { ClaimExperienceLine } from '../home/api';
import { claimLink, experienceSummary } from './accountClaimsLogic';

const COLUMNS: Column<ClaimExperienceLine>[] = [
  {
    key: 'claimNo',
    header: 'Claim No.',
    render: (c) => <Link to={claimLink(c.claimId)}>{c.claimNo}</Link>,
  },
  { key: 'policyYear', header: 'Policy Year', render: (c) => c.policyYear, numeric: true },
  { key: 'lossDate', header: 'Date of Loss', render: (c) => formatDate(c.lossDate) },
  { key: 'status', header: 'Status', render: (c) => c.statusLabel ?? '—' },
  { key: 'phase', header: 'Phase', render: (c) => <StatusBadge status={c.phase} /> },
  { key: 'currency', header: 'Currency', render: (c) => c.currency },
  { key: 'paid', header: 'Paid', render: (c) => <Amount value={c.paid} />, numeric: true },
  {
    key: 'outstanding',
    header: 'Outstanding',
    render: (c) => <Amount value={c.outstanding} />,
    numeric: true,
  },
];

/**
 * Claims tab of the account page (CLAIMS_BROKING_DESIGN 11 and 14, wave CL2): the account's claims
 * from Claims Handling, read-only, with the paid and outstanding amounts and a link to each claim
 * record. Shown to holders of BCL_VIEW.
 */
export function AccountClaimsPanel({ arn }: Readonly<{ arn: string }>) {
  const experience = useQuery({
    queryKey: ['broker-claims', 'experience', arn],
    queryFn: () => claimsHomeApi.experience(arn),
  });
  return (
    <Card
      title="Claims"
      flush
      actions={
        experience.data !== undefined && (
          <span className="muted">{experienceSummary(experience.data)}</span>
        )
      }
    >
      <ErrorAlert error={experience.error} />
      <DataTable
        caption={`Claims of ${arn}`}
        columns={COLUMNS}
        rows={experience.data?.claims ?? []}
        rowKey={(c) => c.claimId}
        loading={experience.isLoading}
        emptyMessage="No claim recorded on this account"
      />
    </Card>
  );
}
