import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Eraser } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { formatDateTime, humanize } from '@/utils/format';
import { CodeSelect, TextField } from './CashFields';
import { cashieringApi } from './cashieringApi';
import type { Layout, MinimalBalanceRule } from './cashieringApi';

const TABS = [
  { id: 'layouts', label: 'Payment File Layouts' },
  { id: 'minimal', label: 'Minimal Balance' },
] as const;
type TabId = (typeof TABS)[number]['id'];

function LayoutDialog({ layout, onClose }: Readonly<{ layout: Layout; onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [kind, setKind] = useState(layout.kind);
  const [delimiter, setDelimiter] = useState(layout.delimiter ?? '');
  const [fields, setFields] = useState(layout.fields ?? '');
  const save = useMutation({
    mutationFn: () =>
      cashieringApi.changeLayout(
        layout.handlerCode,
        kind,
        delimiter || undefined,
        fields || undefined,
      ),
    onSuccess: async () => {
      toast.success(`Layout of ${humanize(layout.handlerCode)} saved`);
      await queryClient.invalidateQueries({ queryKey: ['cashiering', 'layouts'] });
      onClose();
    },
  });
  return (
    <Modal
      open
      title={`Layout of ${humanize(layout.handlerCode)}`}
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button busy={save.isPending} onClick={() => save.mutate()}>
            Save Layout
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error} />
        <CodeSelect
          label="File Kind"
          required
          value={kind}
          options={['AUTO', 'DELIMITED', 'FIXED_WIDTH']}
          onChange={setKind}
        />
        {kind === 'DELIMITED' && (
          <TextField
            label="Separator"
            required
            value={delimiter}
            onChange={setDelimiter}
            maxLength={1}
          />
        )}
        {kind === 'FIXED_WIDTH' && (
          <Field label="Fields (Header:start:length separated by ;, start is 1-based)" required>
            {(id) => (
              <textarea
                id={id}
                className="input"
                rows={8}
                value={fields}
                maxLength={1000}
                onChange={(e) => setFields(e.target.value)}
              />
            )}
          </Field>
        )}
      </div>
    </Modal>
  );
}

function Layouts() {
  const { can } = useAuth();
  const [editing, setEditing] = useState<Layout>();
  const list = useQuery({ queryKey: ['cashiering', 'layouts'], queryFn: cashieringApi.layouts });
  const columns: Column<Layout>[] = [
    {
      key: 'code',
      header: 'File',
      render: (l) => (
        <>
          <strong>{humanize(l.handlerCode)}</strong>
          <span className="cell-sub">{l.description ?? ''}</span>
        </>
      ),
    },
    { key: 'kind', header: 'Kind', render: (l) => humanize(l.kind) },
    {
      key: 'sep',
      header: 'Separator',
      render: (l) => (l.delimiter ? <code>{l.delimiter}</code> : ''),
    },
    {
      key: 'upd',
      header: 'Last Changed',
      render: (l) => (l.updatedBy ? `${l.updatedBy} · ${formatDateTime(l.updatedAt)}` : ''),
    },
    {
      key: 'edit',
      header: '',
      render: (l) =>
        can('CASH_APPROVE') && (
          <Button size="sm" variant="secondary" onClick={() => setEditing(l)}>
            Edit
          </Button>
        ),
    },
  ];
  return (
    <>
      <p className="muted">
        The bank and channel file layouts are still to be confirmed by BDOI (OQ03/OQ04): the parser
        reads the configured kind.
      </p>
      <ErrorAlert error={list.error} />
      <DataTable
        caption="Payment file layouts"
        columns={columns}
        rows={list.data ?? []}
        rowKey={(l) => l.handlerCode}
        loading={list.isLoading}
      />
      {editing && <LayoutDialog layout={editing} onClose={() => setEditing(undefined)} />}
    </>
  );
}

const RULE_COLUMNS: Column<MinimalBalanceRule>[] = [
  {
    key: 'kind',
    header: 'Balance',
    render: (r) => (
      <>
        <strong>{humanize(r.kind)}</strong>
        <span className="cell-sub">{r.description ?? ''}</span>
      </>
    ),
  },
  { key: 'max', header: 'Up To', numeric: true, render: (r) => <Amount value={r.maxAmount} /> },
  {
    key: 'excl',
    header: 'Excludes',
    render: (r) =>
      [
        r.excludeCwt && 'CWT accounts',
        r.excludeDst && 'DST',
        r.excludeWholePremium && 'Unpaid invoices',
      ]
        .filter(Boolean)
        .join(', '),
  },
  { key: 'action', header: 'Action', render: (r) => humanize(r.action) },
  {
    key: 'active',
    header: 'Status',
    render: (r) => <StatusBadge status={r.active ? 'ACTIVE' : 'INACTIVE'} />,
  },
];

function MinimalBalance() {
  const { can } = useAuth();
  const toast = useToast();
  const rules = useQuery({
    queryKey: ['cashiering', 'minimal-rules'],
    queryFn: cashieringApi.minimalRules,
  });
  const sweep = useMutation({
    mutationFn: cashieringApi.sweep,
    onSuccess: (s) =>
      toast.success(
        `Sweep done: ${s.premium} premium balance(s) reversed, ${s.excess} excess moved to overages`,
      ),
  });
  return (
    <>
      <div className="worklist-toolbar">
        <span className="muted">
          Balances at or below the limits are cleared by the MINIMAL_BALANCE_SWEEP job (CSHID.016).
        </span>
        <div className="worklist-actions">
          {can('CASH_APPROVE') && (
            <Button
              variant="accent"
              icon={<Eraser size={16} />}
              busy={sweep.isPending}
              onClick={() => sweep.mutate()}
            >
              Run Sweep Now
            </Button>
          )}
        </div>
      </div>
      <ErrorAlert error={rules.error ?? sweep.error} />
      <DataTable
        caption="Minimal balance rules"
        columns={RULE_COLUMNS}
        rows={rules.data ?? []}
        rowKey={(r) => r.kind}
        loading={rules.isLoading}
      />
    </>
  );
}

/**
 * Cashiering Setup: the payment file layouts (CSHID.008, OQ03/OQ04) and the minimal balance
 * rules with an on-demand sweep (CSHID.016, OQ11).
 */
export default function CashieringSetupPage() {
  const [tab, setTab] = useState<TabId>('layouts');
  return (
    <div className="stack">
      <PageHeader
        section="Cashiering"
        title="Cashiering Setup"
        description="Payment file layouts and minimal balance rules."
      />
      <Card flush>
        <Tabs tabs={TABS} active={tab} onChange={setTab} />
        {tab === 'layouts' ? <Layouts /> : <MinimalBalance />}
      </Card>
    </div>
  );
}
