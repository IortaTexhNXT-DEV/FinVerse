import {
  BadgeCheck,
  CalendarClock,
  CircleDollarSign,
  MessageSquareWarning,
  UserRound,
} from 'lucide-react';
import type { ValidationCheck, VersionDetail } from '@/api/productCatalog';
import { checksOf } from './validationOutcome';
import { KeyFacts } from '@/components/broking/RecordSummary';
import type { Fact } from '@/components/broking/RecordSummary';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import type { Column } from '@/components/ui/DataTable';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { UserName } from '@/components/ui/UserName';
import { useWorkspace } from '@/context/workspaceContext';
import { formatAmount, formatDateTime } from '@/utils/format';

const RESULT_LABEL: Record<ValidationCheck['result'], string> = {
  PASSED: 'Passed',
  FAILED: 'Failed',
  NOT_APPLICABLE: 'Not Applicable',
};

const COLUMNS: Column<ValidationCheck>[] = [
  { key: 'seq', header: '#', kind: 'center', width: '56px', render: (c) => String(c.seq) },
  { key: 'check', header: 'Check', render: (c) => c.label },
  {
    key: 'result',
    header: 'Result',
    kind: 'status',
    width: '168px',
    render: (c) => <StatusBadge status={c.result} label={RESULT_LABEL[c.result]} />,
  },
  { key: 'detail', header: 'Details', render: (c) => c.detail },
];

function premiumText(premium: number | undefined, returned: boolean, currency: string) {
  return premium === undefined || returned ? '' : `${currency} ${formatAmount(premium)}`;
}

/**
 * The outcome of the validation checkpoint of a package version (PMADD06): result, validator,
 * time, test premium and the return reason in the header card style, then the checks run with
 * their result and the figures compared.
 */
export function ValidationResultCard({ detail }: Readonly<{ detail: VersionDetail }>) {
  const { company } = useWorkspace();
  const s = detail.summary;
  const returned = s.validatedBy === undefined;
  const facts: Fact[] = [
    {
      icon: BadgeCheck,
      label: 'Result',
      value: returned ? (
        <StatusBadge status="RETURNED" label="Returned" />
      ) : (
        <StatusBadge status="PASSED" label="Passed" />
      ),
    },
    {
      icon: UserRound,
      label: returned ? 'Returned By' : 'Validated By',
      value: <UserName login={returned ? detail.returnedBy : s.validatedBy} />,
    },
    {
      icon: CalendarClock,
      label: returned ? 'Returned On' : 'Validated On',
      value: formatDateTime(returned ? detail.returnedAt : s.validatedAt),
    },
    {
      icon: CircleDollarSign,
      label: 'Test Premium',
      value: premiumText(detail.testPremium, returned, company?.baseCurrency ?? ''),
    },
  ];
  if (returned) {
    facts.push({
      icon: MessageSquareWarning,
      label: 'Return Reason',
      value: detail.returnedReason,
    });
  }
  const checks = checksOf(detail);
  return (
    <Card title="Validation" callout="validation">
      <div className="stack">
        <KeyFacts facts={facts} label="Validation result" />
        <DataTable
          caption="Validation checks"
          callout="validation-checks"
          columns={COLUMNS}
          rows={checks}
          rowKey={(c) => c.seq}
          emptyMessage="No checks recorded"
        />
      </div>
    </Card>
  );
}
