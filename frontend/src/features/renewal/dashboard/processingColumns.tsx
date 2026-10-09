import { Link } from 'react-router-dom';
import type { ProcessingRow } from '@/api/renewalDashboard';
import type { Column, ColumnKind } from '@/components/ui/DataTable';
import { UserName } from '@/components/ui/UserName';
import { clientShortName } from '@/context/clientNames';
import { formatAmount, formatDate } from '@/utils/format';

type Key = keyof ProcessingRow;

const value = (key: Key, header: string, kind?: ColumnKind): Column<ProcessingRow> => ({
  key,
  header,
  kind,
  render: (r) => {
    const v = r[key];
    if (kind === 'date') {
      return formatDate(typeof v === 'string' ? v : null);
    }
    if (kind === 'amount') {
      return typeof v === 'number' ? formatAmount(v) : '';
    }
    return typeof v === 'string' ? v : '';
  },
});

const count = (key: Key, header: string): Column<ProcessingRow> => ({
  key,
  header,
  kind: 'amount',
  render: (r) => {
    const v = r[key];
    return typeof v === 'number' ? v : '';
  },
});

const user = (
  key: 'unitHead' | 'accountOfficer' | 'processor',
  header: string,
): Column<ProcessingRow> => ({
  key,
  header,
  render: (r) => {
    const v = r[key];
    return v === null ? '' : <UserName login={v} />;
  },
});

function head(): Column<ProcessingRow>[] {
  return [
    {
      key: 'arn',
      header: 'Account Reference Number',
      kind: 'code',
      render: (r) =>
        r.renewalRef === null ? (
          r.arn
        ) : (
          <Link to={`/renewal/candidates/${encodeURIComponent(r.renewalRef)}`}>{r.arn}</Link>
        ),
    },
    value('businessType', 'Business Type'),
    value('branch', `${clientShortName()} Branch`),
    user('unitHead', 'Unit Head'),
    user('accountOfficer', 'Account Officer'),
    value('assured', "Assured's Name"),
    value('insurer', 'Insurer'),
    value('inceptionDate', 'Inception Date', 'date'),
    value('expiryDate', 'Expiry Date', 'date'),
  ];
}

function policy(): Column<ProcessingRow>[] {
  return [
    value('department', 'Department'),
    value('assured', "Assured's Name"),
    value('arn', 'Reference No.', 'code'),
    value('insurer', 'Insurer'),
    value('riskCode', 'Risk Code', 'code'),
    value('inceptionDate', 'Inception Date', 'date'),
    value('expiryDate', 'Expiry Date', 'date'),
    value('placementDate', 'Processed Date', 'date'),
    value('bookingDate', 'Booking Date', 'date'),
    user('unitHead', 'Unit Head'),
    user('accountOfficer', 'Account Officer'),
    user('processor', 'Processor'),
    value('sumInsured', 'Sum Insured', 'amount'),
    value('premium', 'Basic Premium', 'amount'),
    value('commission', 'Commission', 'amount'),
    value('policyNumber', 'Policy Number', 'code'),
    value('segment', 'Market Segment'),
    { key: 'month', header: 'Month', render: (r) => (r.inceptionDate ?? '').substring(0, 7) },
  ];
}

/** The columns of the drill-down of a card of the Processing dashboard (FRRN.003.02.01 to .06). */
export function processingColumns(card: string): Column<ProcessingRow>[] {
  const posted = value('datePosted', 'Date Posted', 'date');
  const processor = user('processor', 'Processor');
  const line = value('insuranceLine', 'Insurance Line');
  switch (card) {
    case 'FOR_PLACEMENT':
      return [...head(), posted, processor, line, count('ageing', 'Aging (Days)')];
    case 'FOR_BOOKING':
      return [
        ...head(),
        value('placementDate', 'Placement Date', 'date'),
        processor,
        line,
        count('placementAgeing', 'Aging (Days)'),
      ];
    case 'WITH_POLICY':
    case 'WITHOUT_POLICY':
      return [
        ...policy(),
        value('policyStatus', 'Policy Status'),
        count('placementAgeing', 'Placement Aging'),
        value('transmittalDate', 'Policy Delivery Date', 'date'),
        count('deliveryAgeing', 'Policy Delivery Aging (No. of Days)'),
        value('tatStatus', 'TAT Status'),
        value('placementIssue', 'Placement Issue'),
        value('resolutionDate', 'Placement Issue Resolution Date', 'date'),
      ];
    case 'FOR_RELEASING':
    case 'RELEASED':
    case 'UNRELEASED':
      return [
        ...policy(),
        value('transmittalStatus', 'Policy Transmittal Status'),
        value('policyReceivedDate', 'Policy Received Date', 'date'),
        value('transmittalDate', 'Policy Transmittal Date', 'date'),
        count('deliveryAgeing', 'Policy Delivery Aging (No. of Days)'),
        value('tatStatus', 'TAT Status'),
        value('reasonForReject', 'Reason for Reject'),
      ];
    case 'RETURNED':
      return [
        ...head(),
        posted,
        processor,
        line,
        value('returnedDate', 'Returned Date', 'date'),
        value('returnReason', 'Reason for Return'),
      ];
    default:
      return [...head(), posted, processor, line];
  }
}
