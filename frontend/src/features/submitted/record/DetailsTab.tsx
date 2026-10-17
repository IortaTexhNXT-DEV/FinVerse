import { useQuery } from '@tanstack/react-query';
import { submittedApi } from '@/api/submitted';
import type { HistoryRow, PolicyDetail } from '@/api/submitted';
import { InsurerName, LovLabel } from '@/components/broking/LovLabel';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { DefinitionGrid } from '@/components/ui/DefinitionGrid';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { UserName } from '@/components/ui/UserName';
import { formatAmount, formatDate, formatDateTime, humanize } from '@/utils/format';
import { SBM_LOV, SOURCE_LABELS } from '../common/submittedCodes';

const templateLabel = (t: string | null) => {
  if (t === null) {
    return null;
  }
  return t === 'FFY' ? 'Free First Year' : 'Generic';
};

const yesNo = (v: boolean) => (v ? 'Yes' : 'No');
const amount = (v: number | null) => (v === null ? null : formatAmount(v));

/** Details of a record: loan, assured, policy, risk, processing outcome and origin. */
export function DetailsTab({ detail }: Readonly<{ detail: PolicyDetail }>) {
  const { loan, assured, terms, risk, marks } = detail.data;
  const o = detail.outcome;
  return (
    <div className="stack">
      <Card title="Loan">
        <DefinitionGrid
          columns={2}
          collapseEmpty
          items={[
            { label: 'PN No.', value: loan.pnNo },
            { label: 'Loan Application No.', value: loan.loanApplicationNo },
            { label: 'CIF', value: loan.cif },
            { label: 'Borrower', value: loan.borrowerName },
            { label: 'Value Date', value: formatDate(loan.valueDate) },
            { label: 'Maturity Date', value: formatDate(loan.maturityDate) },
            { label: 'Referring Branch', value: loan.referringBranch },
            { label: 'Originating Unit', value: loan.originatingUnit },
            {
              label: 'Loan Status',
              value: <LovLabel type={SBM_LOV.loanStatus} code={o.loanStatus} />,
            },
            { label: 'Amortised', value: yesNo(o.amortised) },
          ]}
        />
      </Card>
      <Card title="Assured">
        <DefinitionGrid
          columns={2}
          items={[
            { label: 'Assured', value: assured.assuredName },
            { label: 'E-mail', value: assured.email },
            { label: 'Mobile', value: assured.mobile },
            { label: 'Telephone', value: assured.telephone },
            { label: 'Bank Counterpart', value: assured.bankCounterpartEmail },
            { label: 'Mailing Address', value: assured.mailingAddress, wide: true },
          ]}
        />
      </Card>
      <Card title="Policy and Risk">
        <DefinitionGrid
          columns={2}
          items={[
            { label: 'Insurer', value: <InsurerName code={terms.insurerCode} /> },
            { label: 'Policy No.', value: terms.policyNo },
            { label: 'Inception', value: formatDate(terms.inceptionDate) },
            { label: 'Expiry', value: formatDate(terms.expiryDate) },
            { label: 'Sum Insured', value: amount(terms.sumInsured) },
            { label: 'Total Premium', value: amount(terms.totalPremium) },
            { label: 'Unit', value: risk.unitDescription },
            { label: 'Plate No.', value: risk.plateNo },
            { label: 'Serial No.', value: risk.serialNo },
            { label: 'Motor No.', value: risk.motorNo },
            { label: 'Vehicle Type', value: risk.vehicleType },
            { label: 'Vehicle Year', value: risk.vehicleYear },
            { label: 'Occupancy', value: risk.occupancy },
            { label: 'Mortgagee', value: risk.mortgagee },
            { label: 'Property Location', value: risk.propertyLocation, wide: true },
          ]}
        />
      </Card>
      <Card title="Processing">
        <DefinitionGrid
          columns={2}
          items={[
            {
              label: 'Classification',
              value: detail.row.classification ? humanize(detail.row.classification) : null,
            },
            { label: 'Bucket', value: <LovLabel type={SBM_LOV.bucket} code={detail.row.bucket} /> },
            {
              label: 'Bucket Reason',
              value: <LovLabel type={SBM_LOV.reason} code={o.bucketReason} />,
            },
            { label: 'RA Template', value: templateLabel(o.raTemplate) },
            {
              label: 'Renewal Tag',
              value: detail.row.renewalTag ? humanize(detail.row.renewalTag) : null,
            },
            {
              label: 'Tagged By',
              value: o.renewalTaggedBy ? <UserName login={o.renewalTaggedBy} /> : null,
            },
            {
              label: 'Tag Reason',
              value: <LovLabel type={SBM_LOV.nonRenewal} code={o.renewalTagReason} />,
            },
            { label: 'Last Run', value: o.lastRunNo },
            { label: 'FFY', value: yesNo(marks.ffy) },
            { label: 'Employee Account', value: yesNo(marks.employeeAccount) },
            { label: 'No Touch', value: yesNo(marks.noTouch) },
            {
              label: 'Fallout Reason',
              value: <LovLabel type={SBM_LOV.reason} code={detail.row.falloutReason} />,
            },
          ]}
        />
      </Card>
      <Card title="Origin">
        <DefinitionGrid
          columns={2}
          items={[
            {
              label: 'Source',
              value: SOURCE_LABELS[detail.origin.sourceCode] ?? detail.origin.sourceCode,
            },
            { label: 'Date Received', value: formatDate(detail.origin.dateReceived) },
            { label: 'Migrated', value: yesNo(detail.origin.migrated) },
            { label: 'Legacy Reference', value: detail.origin.legacyRef },
            { label: 'Created By', value: <UserName login={detail.origin.createdBy} /> },
            { label: 'Created', value: formatDateTime(detail.origin.createdAt) },
          ]}
        />
      </Card>
    </div>
  );
}

/** Field changes of a record, newest first. */
export function HistoryTab({ policyId }: Readonly<{ policyId: number }>) {
  const history = useQuery({
    queryKey: ['submitted', 'history', policyId],
    queryFn: () => submittedApi.history(policyId),
  });
  return (
    <Card title="Changes" flush>
      <ErrorAlert error={history.error} />
      <DataTable<HistoryRow>
        loading={history.isLoading}
        rows={history.data ?? []}
        rowKey={(h) => `${h.at}-${h.field}`}
        emptyMessage="No changes recorded"
        columns={[
          {
            key: 'at',
            header: 'Date and Time',
            kind: 'datetime',
            render: (h) => formatDateTime(h.at),
          },
          { key: 'field', header: 'Field', render: (h) => h.field },
          { key: 'old', header: 'Old Value', render: (h) => h.oldValue ?? '—' },
          { key: 'new', header: 'New Value', render: (h) => h.newValue ?? '—' },
          { key: 'source', header: 'Source', render: (h) => humanize(h.source) },
          { key: 'ref', header: 'Reference', kind: 'code', render: (h) => h.reference ?? '—' },
          { key: 'by', header: 'By', render: (h) => <UserName login={h.by} /> },
        ]}
      />
    </Card>
  );
}
