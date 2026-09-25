import { useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate } from '@/utils/format';
import { frbsApi } from './api';
import type { ServiceFeeRecipient, ServiceFeeRule } from './api';
import { RecipientDialog, RuleDialog } from './SetupDialogs';
import './frbs.css';

const RULE_COLUMNS: Column<ServiceFeeRule>[] = [
  {
    key: 'segment',
    header: 'Segment',
    render: (r) => (
      <>
        <strong>{r.segment}</strong>
        <span className="cell-sub">{r.marketSegments.join(', ')}</span>
      </>
    ),
  },
  { key: 'rate', header: 'Rate', numeric: true, render: (r) => `${String(r.rate)}%` },
  {
    key: 'base',
    header: 'Base',
    render: (r) =>
      r.netOfWtax ? 'Fully paid commission, net of withholding tax' : 'Fully paid commission',
  },
  {
    key: 'dates',
    header: 'Effective',
    render: (r) =>
      `${formatDate(r.effectiveFrom)} – ${r.effectiveTo ? formatDate(r.effectiveTo) : 'open'}`,
  },
  {
    key: 'status',
    header: 'Status',
    render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
  },
];

const RECIPIENT_COLUMNS: Column<ServiceFeeRecipient>[] = [
  { key: 'unit', header: 'Sales Unit', render: (r) => <strong>{r.salesUnit}</strong> },
  {
    key: 'payee',
    header: 'Recipient',
    render: (r) => (
      <>
        {r.payeeName}
        <span className="cell-sub">{r.payeeCode}</span>
      </>
    ),
  },
  { key: 'cc', header: 'Cost Centre', render: (r) => r.costCenter ?? 'By rule' },
  {
    key: 'status',
    header: 'Status',
    render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
  },
];

/**
 * Service-fee rates and recipients (FRBS 2.10.0; values to confirm, AQ20): the rate of each
 * service-fee segment and the market segments it covers, and the payee and cost centre of each
 * sales unit. The GL team lead maintains them; runs already computed keep their rates.
 */
export default function ServiceFeeSetupPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [rule, setRule] = useState<ServiceFeeRule | 'new'>();
  const [recipient, setRecipient] = useState<ServiceFeeRecipient | 'new'>();
  const rules = useQuery({ queryKey: ['frbs', 'rules'], queryFn: frbsApi.rules });
  const recipients = useQuery({
    queryKey: ['frbs', 'recipients', companyId],
    queryFn: () => frbsApi.recipients(companyId),
    enabled: companyId > 0,
  });
  const maintain = can('SERVICE_FEE_APPROVE');
  const saved = (what: string) => {
    setRule(undefined);
    setRecipient(undefined);
    toast.success(`${what} saved`);
    void queryClient.invalidateQueries({ queryKey: ['frbs'] });
  };
  return (
    <div className="stack">
      <PageHeader
        backTo="/frbs/service-fee"
        section="Finance · Accounting Reports"
        title="Service Fee Rates and Recipients"
        description="Rates per service-fee segment and the payee and cost centre of each sales unit (to confirm with BDOI, AQ20)."
      />
      <ErrorAlert error={rules.error ?? recipients.error} />
      <Card
        title="Rates"
        flush
        actions={
          maintain && (
            <Button size="sm" icon={<Plus size={14} />} onClick={() => setRule('new')}>
              New Rate
            </Button>
          )
        }
      >
        <DataTable
          caption="Service-fee rates"
          columns={RULE_COLUMNS}
          rows={rules.data ?? []}
          rowKey={(r) => r.id}
          loading={rules.isLoading}
          onRowClick={maintain ? (r) => setRule(r) : undefined}
        />
      </Card>
      <Card
        title="Recipients"
        flush
        actions={
          maintain && (
            <Button size="sm" icon={<Plus size={14} />} onClick={() => setRecipient('new')}>
              New Recipient
            </Button>
          )
        }
      >
        <DataTable
          caption="Service-fee recipients"
          columns={RECIPIENT_COLUMNS}
          rows={recipients.data ?? []}
          rowKey={(r) => r.id}
          loading={recipients.isLoading}
          emptyMessage="No recipients: units are paid under their own code"
          onRowClick={maintain ? (r) => setRecipient(r) : undefined}
        />
      </Card>
      {rule !== undefined && (
        <RuleDialog
          rule={rule === 'new' ? undefined : rule}
          onClose={() => setRule(undefined)}
          onDone={() => saved('Rate')}
        />
      )}
      {recipient !== undefined && (
        <RecipientDialog
          recipient={recipient === 'new' ? undefined : recipient}
          onClose={() => setRecipient(undefined)}
          onDone={() => saved('Recipient')}
        />
      )}
    </div>
  );
}
