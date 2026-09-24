import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
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
import { formatDate, humanize, today } from '@/utils/format';
import { AutoBookRuleDialog, IncentiveRuleDialog, ServiceInvoiceTypeDialog } from './SetupDialogs';

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
          { key: 'segment', header: 'Market segment', render: (r) => r.marketSegment ?? ANY },
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

function IncentiveTab({ companyId, canEdit }: Readonly<{ companyId: number; canEdit: boolean }>) {
  const [editing, setEditing] = useState<IncentiveRule | null>(null);
  const rules = useQuery({
    queryKey: ['booking', 'setup', 'incentive', companyId],
    queryFn: () => bookingApi.incentiveRules(companyId),
  });
  const saved = useSaved('Incentive rule saved', () => setEditing(null));
  const save = useMutation({
    mutationFn: (rule: IncentiveRule) => bookingApi.saveIncentiveRule(companyId, rule),
    onSuccess: saved,
  });
  return (
    <Card
      title="Incentive rules"
      flush
      actions={
        canEdit && (
          <Button
            icon={<Plus size={16} />}
            onClick={() => setEditing({ periodFrom: today(), active: true, description: '' })}
          >
            New Rule
          </Button>
        )
      }
    >
      <ErrorAlert error={rules.error} />
      <DataTable<IncentiveRule>
        caption="Incentive rules"
        loading={rules.isLoading}
        rows={rules.data ?? []}
        rowKey={(r) => r.id ?? 0}
        onRowClick={canEdit ? setEditing : undefined}
        emptyMessage="No incentive rule (the qualification rules are pending, Q33)."
        columns={[
          { key: 'product', header: 'Product', render: (r) => r.productCode ?? ANY },
          { key: 'segment', header: 'Segment', render: (r) => r.marketSegment ?? ANY },
          { key: 'channel', header: 'Channel', render: (r) => r.sourceChannel ?? ANY },
          {
            key: 'period',
            header: 'Booked',
            render: (r) =>
              `${formatDate(r.periodFrom)} – ${r.periodTo ? formatDate(r.periodTo) : 'open'}`,
          },
          { key: 'description', header: 'Description', render: (r) => r.description },
          { key: 'active', header: 'Status', render: (r) => onOff(r.active) },
        ]}
      />
      {editing && (
        <IncentiveRuleDialog
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
            render: (t) => t.ownerUsername ?? t.ownerPermission ?? '',
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
        description="Which accounts are booked automatically, which bookings are incentive eligible and which service invoices are issued."
      />
      <Tabs tabs={TABS} active={tab} onChange={setTab} />
      {tab === 'auto' && <AutoBookTab companyId={companyId} canEdit={canEdit} />}
      {tab === 'incentive' && <IncentiveTab companyId={companyId} canEdit={canEdit} />}
      {tab === 'types' && <TypesTab canEdit={canEdit} />}
    </div>
  );
}
