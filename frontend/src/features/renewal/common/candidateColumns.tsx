import type { CandidateRow } from '@/api/renewal';
import { Amount } from '@/components/ui/Amount';
import { InsurerName, ProductName, SalesUnitName } from '@/components/broking/LovLabel';
import { CellStack } from '@/components/ui/CellStack';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatDate } from '@/utils/format';
import { BucketPill, FlagChips } from './RenewalBits';
import { dispositionLabel } from './renewalCodes';

/** The columns of a renewal list (FRS "Expiring list columns"). */
export function candidateColumns(showMoney: boolean): Column<CandidateRow>[] {
  const columns: Column<CandidateRow>[] = [
    {
      key: 'ref',
      header: 'Renewal Reference',
      kind: 'code',
      render: (r) => (
        <span className="rnw-ref">
          {r.renewalRef}
          <FlagChips row={r} />
        </span>
      ),
    },
    {
      key: 'client',
      header: 'Client',
      render: (r) => <CellStack main={r.parties.clientName} sub={r.parties.clientCode ?? ''} />,
    },
    {
      key: 'policy',
      header: 'Expiring Policy / Invoice',
      render: (r) => (
        <CellStack
          main={r.policy.policyNo ?? r.policy.sourceRef ?? ''}
          sub={r.policy.expiringInvoiceNo ?? r.policy.expiringArn ?? ''}
        />
      ),
    },
    {
      key: 'risk',
      header: 'Risk',
      render: (r) =>
        r.policy.productCode ? (
          <ProductName code={r.policy.productCode} withCode />
        ) : (
          (r.policy.productName ?? '')
        ),
    },
    {
      key: 'insurer',
      header: 'Insurance Company',
      render: (r) => <InsurerName code={r.policy.insurerCode} />,
    },
    {
      key: 'officer',
      header: 'Unit / Officer',
      render: (r) => (
        <CellStack
          main={<SalesUnitName code={r.parties.ownerUnit} />}
          sub={<UserName login={r.parties.assignedAo ?? r.parties.accountOfficer ?? ''} />}
        />
      ),
    },
    {
      key: 'expiry',
      header: 'Expiry',
      kind: 'date',
      render: (r) => (
        <CellStack main={formatDate(r.expiry)} sub={`${String(r.daysToExpiry)} days`} />
      ),
    },
    {
      key: 'stage',
      header: 'Status',
      kind: 'status',
      render: (r) => <StatusBadge status={r.stage} label={r.stageLabel} />,
    },
    {
      key: 'disposition',
      header: 'Disposition',
      render: (r) => <CellStack main={dispositionLabel(r.disposition)} sub={r.remarks ?? ''} />,
    },
    {
      key: 'bucket',
      header: 'Classification',
      kind: 'status',
      render: (r) => <BucketPill bucket={r.bucket} />,
    },
  ];
  if (showMoney) {
    columns.push(
      {
        key: 'premium',
        header: 'Gross Premium',
        kind: 'amount',
        render: (r) => <Amount value={r.money.grossPremium} />,
      },
      {
        key: 'outstanding',
        header: 'Outstanding',
        kind: 'amount',
        render: (r) => (r.money.outstanding === null ? '' : <Amount value={r.money.outstanding} />),
      },
      {
        key: 'claims',
        header: 'Claims',
        render: (r) => r.money.claimStatus ?? '',
      },
    );
  }
  return columns;
}
