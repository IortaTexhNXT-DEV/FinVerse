import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { humanize } from '@/utils/format';
import { optionalText } from './acsl';
import { acslApi } from './api';
import type { GlSlControl, SlSource } from './api';
import { FormDialog } from './FormDialog';
import type { DialogField } from './FormDialog';

const SOURCES: { value: SlSource; label: string }[] = [
  { value: 'PARTY_LEDGER', label: 'Party ledger (open items by party)' },
  { value: 'OPEN_ITEMS', label: 'Open items' },
  { value: 'OPS_LEDGER', label: 'Operations ledger (invoice components)' },
];

function fields(c: GlSlControl | null): DialogField[] {
  return [
    { key: 'accountCode', label: 'Control Account', required: true, initial: c?.accountCode },
    { key: 'source', label: 'Sub-ledger', required: true, options: SOURCES, initial: c?.source },
    {
      key: 'components',
      label: 'Ledger Components',
      hint: 'Comma-separated, for the operations ledger',
      initial: c?.components,
    },
    {
      key: 'documentTypes',
      label: 'Document Types',
      hint: 'Comma-separated',
      initial: c?.documentTypes,
    },
    { key: 'currency', label: 'Currency', initial: c?.currency },
    {
      key: 'active',
      label: 'Status',
      required: true,
      options: [
        { value: 'true', label: 'Active' },
        { value: 'false', label: 'Inactive' },
      ],
      initial: c ? String(c.active) : 'true',
    },
  ];
}

/**
 * The control accounts of the GL-SL reconciliation and the sub-ledger each one is compared with
 * (ACSL 2.16.0; the mapping is configuration, OQ07), kept by the ACSL reviewer.
 */
export function GlSlControlsCard() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<GlSlControl | 'new' | null>(null);
  const controls = useQuery({
    queryKey: ['acsl', 'controls', companyId],
    queryFn: () => acslApi.controls(companyId),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: (v: Record<string, string>) =>
      acslApi.configure(companyId, {
        accountCode: (v.accountCode ?? '').trim(),
        source: (v.source ?? 'PARTY_LEDGER') as SlSource,
        components: optionalText(v.components),
        documentTypes: optionalText(v.documentTypes),
        currency: optionalText(v.currency),
        active: v.active !== 'false',
      }),
    onSuccess: async (c) => {
      setEditing(null);
      await queryClient.invalidateQueries({ queryKey: ['acsl', 'controls'] });
      toast.success(`${c.accountCode} saved`);
    },
  });
  const manage = can('ACSL_REVIEW');
  const columns: Column<GlSlControl>[] = [
    { key: 'account', header: 'Control Account', render: (c) => <strong>{c.accountCode}</strong> },
    { key: 'source', header: 'Sub-ledger', render: (c) => humanize(c.source) },
    { key: 'components', header: 'Components', render: (c) => c.components ?? '—' },
    { key: 'types', header: 'Document Types', render: (c) => c.documentTypes ?? '—' },
    { key: 'currency', header: 'Currency', render: (c) => c.currency ?? 'All' },
    {
      key: 'status',
      header: 'Status',
      render: (c) => <StatusBadge status={c.active ? 'ACTIVE' : 'INACTIVE'} />,
    },
  ];
  const current = editing === 'new' ? null : editing;
  return (
    <Card
      title="Control Accounts"
      flush
      actions={
        manage && (
          <Button
            variant="secondary"
            size="sm"
            icon={<Plus size={14} />}
            onClick={() => setEditing('new')}
          >
            Add Control Account
          </Button>
        )
      }
    >
      <ErrorAlert error={controls.error} />
      <DataTable
        caption="GL-SL control accounts"
        columns={columns}
        rows={controls.data ?? []}
        rowKey={(c) => c.accountCode}
        loading={controls.isLoading}
        emptyMessage="No items to display"
        onRowClick={manage ? (c) => setEditing(c) : undefined}
      />
      {editing && (
        <FormDialog
          title={current ? `Control Account ${current.accountCode}` : 'Add Control Account'}
          confirmLabel="Save"
          fields={fields(current)}
          busy={save.isPending}
          error={save.error}
          onConfirm={(v) => save.mutate(v)}
          onClose={() => setEditing(null)}
        />
      )}
    </Card>
  );
}
