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

/** "1 claim" / "3 claims": the claims of the expiring term in words; empty when unknown. */
export function claimsText(count: number | null): string {
  if (count === null) {
    return '';
  }
  return count === 1 ? '1 claim' : `${String(count)} claims`;
}

/**
 * The columns of a renewal list (FRS "Expiring list columns"). The list fits its card: related
 * values share a column, one under the other (Risk / Insurance Company, Status / Classification,
 * Gross Premium / Outstanding), instead of scrolling sideways.
 */
export function candidateColumns(showMoney: boolean): Column<CandidateRow>[] {
  const columns: Column<CandidateRow>[] = [
    {
      key: 'ref',
      header: 'Renewal Reference',
      kind: 'code',
      render: (r) => (
        <span className="rnw-ref">
          <span className="nowrap">{r.renewalRef}</span>
          <FlagChips row={r} />
        </span>
      ),
    },
    {
      key: 'client',
      header: 'Client',
      render: (r) => (
        <CellStack
          main={r.parties.clientName}
          sub={r.parties.clientCode ? <span className="nowrap">{r.parties.clientCode}</span> : ''}
        />
      ),
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
      header: 'Risk / Insurance Company',
      render: (r) => (
        <CellStack
          main={
            r.policy.productCode ? (
              <ProductName code={r.policy.productCode} />
            ) : (
              (r.policy.productName ?? '')
            )
          }
          sub={<InsurerName code={r.policy.insurerCode} />}
        />
      ),
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
      header: 'Status / Classification',
      kind: 'status',
      render: (r) => (
        <span className="rnw-ref">
          <StatusBadge status={r.stage} label={r.stageLabel} />
          <BucketPill bucket={r.bucket} />
        </span>
      ),
    },
    {
      key: 'disposition',
      header: 'Disposition',
      render: (r) => <CellStack main={dispositionLabel(r.disposition)} sub={r.remarks ?? ''} />,
    },
  ];
  if (showMoney) {
    columns.push({
      key: 'premium',
      header: 'Gross Premium / Outstanding',
      kind: 'amount',
      render: (r) => (
        <CellStack
          main={<Amount value={r.money.grossPremium} />}
          sub={
            r.money.outstanding === null ? (
              claimsText(r.money.claimCount)
            ) : (
              <>
                <Amount value={r.money.outstanding} />
                {r.money.claimCount === null ? '' : ` · ${claimsText(r.money.claimCount)}`}
              </>
            )
          }
        />
      ),
    });
  }
  return columns;
}
