import type { CandidateDetail } from '@/api/renewal';
import {
  BranchName,
  LovLabel,
  LineLabel,
  ProductName,
  SalesUnitName,
} from '@/components/broking/LovLabel';
import { Amount } from '@/components/ui/Amount';
import { Card } from '@/components/ui/Card';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { UserName } from '@/components/ui/UserName';
import { formatDate, formatDateTime } from '@/utils/format';
import { closedAsLabel } from '../common/presentation';
import { RenewalInsurer, RenewalProduct, RenewalUnit } from '../common/RenewalBits';
import { RNW_LOV, dispositionLabel } from '../common/renewalCodes';

type Props = Readonly<{ detail: CandidateDetail }>;

function PolicyCard({ detail }: Props) {
  const { row, lifecycle } = detail;
  const p = row.policy;
  const pkg = lifecycle.legacyPackage;
  return (
    <Card title="Expiring account">
      <DefinitionGrid
        collapseEmpty
        items={[
          { label: 'Source', value: p.source === 'LEGACY' ? 'Migrated policy' : 'Booked account' },
          { label: 'ARN', value: p.expiringArn },
          { label: 'Invoice', value: p.expiringInvoiceNo },
          { label: 'Policy No.', value: p.policyNo },
          { label: 'Cover No.', value: p.coverNo },
          {
            label: 'Product',
            value: <RenewalProduct row={row} />,
          },
          { label: 'Line', value: p.lineCode ? <LineLabel code={p.lineCode} /> : null },
          {
            label: 'Insurance Company',
            value: p.insurerCode ? <RenewalInsurer row={row} /> : null,
          },
          { label: 'Inception', value: formatDate(p.inception) },
          { label: 'Expiry', value: formatDate(row.expiry) },
          { label: 'PN No.', value: p.pnNos },
          { label: 'Mortgaged', value: p.mortgaged ? `Yes – ${p.mortgageeBank ?? ''}` : 'No' },
          {
            label: 'Legacy package',
            value: pkg.legacyCode ? (
              <>
                {[pkg.legacyCode, pkg.legacyVersion].filter(Boolean).join(' ')}
                {pkg.productCode ? (
                  <>
                    {', renews on '}
                    <ProductName code={pkg.productCode} />
                  </>
                ) : (
                  ', not mapped'
                )}
              </>
            ) : null,
          },
        ]}
      />
    </Card>
  );
}

function officer(login: string | null) {
  return login ? <UserName login={login} /> : null;
}

function unit(code: string | null) {
  return code ? <SalesUnitName code={code} /> : null;
}

function PartiesCard({ detail }: Props) {
  const q = detail.row.parties;
  return (
    <Card title="Client and officers">
      <DefinitionGrid
        collapseEmpty
        items={[
          {
            label: 'Client',
            value: (
              <span className="cell-stack">
                <span>{q.clientName}</span>
                {q.clientCode && <span className="muted">{q.clientCode}</span>}
              </span>
            ),
          },
          { label: 'Assured', value: q.assuredName },
          { label: 'Segment', value: q.segment },
          { label: 'Business Origin', value: q.businessOrigin },
          { label: 'Account Type', value: q.accountType },
          { label: 'Branch', value: q.branchCode ? <BranchName code={q.branchCode} /> : null },
          { label: 'Region', value: unit(q.regionCode) },
          { label: 'Department', value: unit(q.departmentCode) },
          { label: 'Owner Unit', value: q.ownerUnit ? <RenewalUnit row={detail.row} /> : null },
          { label: 'Unit Head', value: officer(q.unitHead) },
          { label: 'Account Officer', value: officer(q.assignedAo) },
          { label: 'Processing Officer', value: officer(q.assignedPo) },
        ]}
      />
    </Card>
  );
}

function AmountsCard({ detail }: Props) {
  const m = detail.row.money;
  return (
    <Card title="Amounts">
      <DefinitionGrid
        collapseEmpty
        items={[
          { label: 'Currency', value: m.currency },
          { label: 'Basic Premium', value: <Amount value={m.basicPremium} /> },
          { label: 'Gross Premium', value: <Amount value={m.grossPremium} /> },
          { label: 'Sum Insured', value: <Amount value={m.sumInsured} /> },
          { label: 'Premium Rate', value: m.premiumRate },
          { label: 'Commission Rate', value: m.commissionRate },
          {
            label: 'Outstanding',
            value: m.outstanding === null ? null : <Amount value={m.outstanding} />,
          },
          { label: 'Claims', value: m.claimStatus },
        ]}
      />
    </Card>
  );
}

function proposalText(p: CandidateDetail['lifecycle']['proposal']): string | null {
  if (!p.disposition) return null;
  const how = p.automation === 'AUTO' ? 'automatic' : 'proposal';
  return `${dispositionLabel(p.disposition)} (${how}, version ${String(p.matrixVersion ?? '')})`;
}

function RenewalCard({ detail }: Props) {
  const { row, lifecycle } = detail;
  const links = lifecycle.links;
  return (
    <Card title="Renewal">
      <DefinitionGrid
        collapseEmpty
        items={[
          { label: 'Disposition', value: dispositionLabel(row.disposition) },
          {
            label: 'Reason',
            value: row.reason ? (
              <LovLabel type={RNW_LOV.nonRenewalReason} code={row.reason} />
            ) : null,
          },
          { label: 'Proposed by the matrix', value: proposalText(lifecycle.proposal) },
          { label: 'Renewal ARN', value: links.renewalArn },
          { label: 'Quotation', value: links.quotationRef },
          { label: 'Proposal', value: links.proposalRef },
          { label: 'Renewed Invoice', value: links.renewedInvoiceNo },
          {
            label: 'Initiated',
            value: lifecycle.initiatedAt ? (
              <>
                {formatDateTime(lifecycle.initiatedAt)}
                {lifecycle.initiatedBy && (
                  <>
                    {' by '}
                    <UserName login={lifecycle.initiatedBy} />
                  </>
                )}
              </>
            ) : null,
          },
          {
            label: 'Closed',
            value: links.closedAt
              ? `${formatDateTime(links.closedAt)} (${closedAsLabel(links.closedAs)})`
              : null,
          },
        ]}
      />
    </Card>
  );
}

/** Details: the expiring account, the parties, the amounts and the renewal. */
export function DetailsTab({ detail }: Props) {
  return (
    <div className="rnw-grid">
      <PolicyCard detail={detail} />
      <PartiesCard detail={detail} />
      <AmountsCard detail={detail} />
      <RenewalCard detail={detail} />
    </div>
  );
}
