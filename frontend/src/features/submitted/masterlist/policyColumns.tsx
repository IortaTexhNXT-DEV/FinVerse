import { Link } from 'react-router-dom';
import type { PolicyRow } from '@/api/submitted';
import { BusinessTypeName, InsurerName, LovLabel } from '@/components/broking/LovLabel';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { formatAmount, formatDate } from '@/utils/format';
import { FlagChips } from '../common/SubmittedBits';
import { SBM_LOV, policyLink } from '../common/submittedCodes';

/** Columns of the masterlist (design section 12). */
export const policyColumns: Column<PolicyRow>[] = [
  {
    key: 'sbmNo',
    header: 'Masterlist No.',
    kind: 'code',
    render: (p) => <Link to={policyLink(p.id)}>{p.sbmNo}</Link>,
  },
  { key: 'assured', header: 'Assured', render: (p) => p.assuredName },
  {
    key: 'segment',
    header: 'Segment',
    defaultHidden: true,
    render: (p) => <LovLabel type={SBM_LOV.segment} code={p.segment} />,
  },
  {
    key: 'bt',
    header: 'Business Type',
    defaultHidden: true,
    render: (p) => <BusinessTypeName code={p.businessType} />,
  },
  { key: 'pn', header: 'PN No.', defaultHidden: true, kind: 'code', render: (p) => p.pnNo ?? '—' },
  { key: 'insurer', header: 'Insurer', render: (p) => <InsurerName code={p.insurerCode} /> },
  { key: 'expiry', header: 'Expiry', kind: 'date', render: (p) => formatDate(p.expiryDate) },
  {
    key: 'si',
    header: 'Sum Insured',
    kind: 'amount',
    render: (p) => (p.sumInsured === null ? '—' : formatAmount(p.sumInsured)),
  },
  {
    key: 'bucket',
    header: 'Bucket',
    defaultHidden: true,
    render: (p) => <LovLabel type={SBM_LOV.bucket} code={p.bucket} />,
  },
  { key: 'flags', header: 'Flags', render: (p) => <FlagChips flags={p.flags} /> },
  {
    key: 'status',
    header: 'Status',
    kind: 'status',
    render: (p) => <StatusBadge status={p.status} />,
  },
  {
    key: 'handler',
    header: 'Handler',
    defaultHidden: true,
    render: (p) => (p.handlerUsername ? <UserName login={p.handlerUsername} /> : '—'),
  },
];
