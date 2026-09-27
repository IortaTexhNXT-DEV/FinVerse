import { PeriodCell } from '@/components/ui/PeriodCell';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { bookingApi } from '@/api/booking';
import type { AutoBookRule, IncentiveRule, ServiceInvoiceType } from '@/api/booking';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';
import { AutoBookRuleDialog, ServiceInvoiceTypeDialog } from './SetupDialogs';
import { UserName } from '@/components/ui/UserName';
import { LovLabel } from '@/components/broking/LovLabel';

type TabId = 'auto' | 'incentive' | 'types';

const TABS: readonly { id: TabId; label: string }[] = [
  { id: 'auto', label: 'Auto-book Rules' },
  { id: 'incentive', label: 'Incentive Rules' },
  { id: 'types', label: 'Service Invoice Types' },
];

const ANY = 'Any';

function onOff(on: boolean) {
  return <StatusBadge status={on ? 'ACTIVE' : 'INACTIVE'} />;
}

function useSaved(message: string, close: () => void) {
  const toast = useToast();
  const queryClient = useQueryClient();
  return async () => {
    await queryClient.invalidateQueries({ queryKey: ['booking', 'setup'] });
    toast.success(message);
    close();
  };
}

function AutoBookTab({ companyId, canEdit }: Readonly<{ companyId: number; canEdit: boolean }>) {
  const [editing, setEditing] = useState<AutoBookRule | null>(null);
  const rules = useQuery({
    queryKey: ['booking', 'setup', 'auto', companyId],
    queryFn: () => bookingApi.autoBookRules(companyId),
  });
  const saved = useSaved('Auto-book rule saved', () => setEditing(null));
  const save = useMutation({
    mutationFn: (rule: AutoBookRule) => bookingApi.saveAutoBookRule(companyId, rule),
    onSuccess: saved,
  });
  return (
    <Card
      title="Auto-book rules"
      flush
      actions={
        canEdit && (
          <Button
            icon={<Plus size={16} />}
            onClick={() => setEditing({ enabled: true, description: '' })}
          >
            New Rule
          </Button>
        )
      }
    >
      <ErrorAlert error={rules.error} />
      <DataTable<AutoBookRule>
        caption="Auto-book rules"
        loading={rules.isLoading}
        rows={rules.data ?? []}
        rowKey={(r) => r.id ?? 0}
        onRowClick={canEdit ? setEditing : undefined}
        emptyMessage="No auto-book rule: every issued account waits in Ready to Book."
        columns={[
          { key: 'product', header: 'Product', render: (r) => r.productCode ?? ANY },
          {
            key: 'segment',
            header: 'Market Segment',
            render: (r) => <LovLabel type="MARKET_SEGMENT" code={r.marketSegment} empty={ANY} />,
          },
          { key: 'description', header: 'Description', render: (r) => r.description },
          { key: 'enabled', header: 'Status', render: (r) => onOff(r.enabled) },
        ]}
      />
      {editing && (
        <AutoBookRuleDialog
          value={editing}
          busy={save.isPending}
          error={save.error}
          onSave={(r) => save.mutate(r)}
          onClose={() => setEditing(null)}
        />
      )}
    </Card>
  );
}

/**
 * Incentive rules of booking, read only: they are frozen and maintained in Product Maintenance >
 * Incentive Criteria (the server refuses changes with INCENTIVE_RULES_FROZEN), so the screen
 * offers no New Rule or edit.
 */
function IncentiveTab({ companyId }: Readonly<{ companyId: number }>) {
  const rules = useQuery({
    queryKey: ['booking', 'setup', 'incentive', companyId],
    queryFn: () => bookingApi.incentiveRules(companyId),
  });
  return (
    <Card
      title="Incentive Rules"
      flush
      actions={
        <Link className="btn btn-secondary btn-sm" to="/catalog/incentives">
          Open Incentive Criteria
        </Link>
      }
    >
      <p className="card-note muted">Maintained in Product Maintenance › Incentive Criteria.</p>
      <ErrorAlert error={rules.error} />
      <DataTable<IncentiveRule>
        caption="Incentive rules"
        loading={rules.isLoading}
        rows={rules.data ?? []}
        rowKey={(r) => r.id ?? 0}
        emptyMessage="No incentive rules"
        columns={[
          { key: 'product', header: 'Product', render: (r) => r.productCode ?? ANY },
          {
            key: 'segment',
            header: 'Segment',
            render: (r) => <LovLabel type="MARKET_SEGMENT" code={r.marketSegment} empty={ANY} />,
          },
          {
            key: 'channel',
            header: 'Channel',
            render: (r) => <LovLabel type="SOURCE_CHANNEL" code={r.sourceChannel} empty={ANY} />,
          },
          {
            key: 'period',
            header: 'Booked',
            kind: 'period',
            render: (r) => <PeriodCell from={r.periodFrom} to={r.periodTo} />,
          },
          { key: 'description', header: 'Description', render: (r) => r.description },
          { key: 'active', header: 'Status', kind: 'status', render: (r) => onOff(r.active) },
        ]}
      />
    </Card>
  );
}

const NEW_TYPE: ServiceInvoiceType = {
  code: '',
  name: '',
  recipient: 'INSURER',
  trigger: 'MANUAL',
  templateCode: 'SERVICE_INVOICE_NOTE',
  active: true,
};

function TypesTab({ canEdit }: Readonly<{ canEdit: boolean }>) {
  const [editing, setEditing] = useState<ServiceInvoiceType | null>(null);
  const types = useQuery({
    queryKey: ['booking', 'setup', 'types'],
    queryFn: bookingApi.serviceInvoiceTypes,
  });
  const saved = useSaved('Service invoice type saved', () => setEditing(null));
  const save = useMutation({ mutationFn: bookingApi.saveServiceInvoiceType, onSuccess: saved });
  return (
    <Card
      title="Service invoice types"
      flush
      actions={
        canEdit && (
          <Button icon={<Plus size={16} />} onClick={() => setEditing(NEW_TYPE)}>
            New Type
          </Button>
        )
      }
    >
      <ErrorAlert error={types.error} />
      <DataTable<ServiceInvoiceType>
        caption="Service invoice types"
        loading={types.isLoading}
        rows={types.data ?? []}
        rowKey={(t) => t.code}
        onRowClick={canEdit ? setEditing : undefined}
        columns={[
          { key: 'code', header: 'Code', render: (t) => t.code },
          { key: 'name', header: 'Name', render: (t) => t.name },
          { key: 'recipient', header: 'Recipient', render: (t) => humanize(t.recipient) },
          { key: 'trigger', header: 'Trigger', render: (t) => humanize(t.trigger) },
          {
            key: 'owner',
            header: 'Owner',
            render: (t) =>
              t.ownerUsername ? <UserName login={t.ownerUsername} /> : (t.ownerPermission ?? ''),
          },
          { key: 'template', header: 'Template', render: (t) => t.templateCode },
          { key: 'active', header: 'Status', render: (t) => onOff(t.active) },
        ]}
      />
      {editing && (
        <ServiceInvoiceTypeDialog
          value={editing}
          busy={save.isPending}
          error={save.error}
          onSave={(t) => save.mutate(t)}
          onClose={() => setEditing(null)}
        />
      )}
    </Card>
  );
}

/**
 * Booking setup: auto-book rules (BRNB.076), incentive eligibility rules (BRNB.107) and service
 * invoice types with their trigger, owner and template (BRNB.100). Business Administrators
 * maintain them; Processing and Adjustment can read them.
 */
export default function BookingSetupPage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [tab, setTab] = useState<TabId>('auto');
  const canEdit = can('MASTER_MAINTAIN');
  return (
    <div className="stack">
      <PageHeader
        section="Booking"
        title="Booking Setup"
        description="Which accounts are booked automatically, which bookings are incentive eligible."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'auto' && <AutoBookTab companyId={companyId} canEdit={canEdit} />}
      {tab === 'incentive' && <IncentiveTab companyId={companyId} />}
      {tab === 'types' && <TypesTab canEdit={canEdit} />}
    </div>
  );
}
