import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { useAuth } from '@/auth/authContext';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDate, humanize } from '@/utils/format';
import type { Rule, RuleInput, RuleMatch } from './api';
import { escalationsApi } from './api';
import { RuleDialog } from './EscalationDialogs';
import { describeRule } from './labels';

const MATCH_COLUMNS: Column<RuleMatch>[] = [
  { key: 'inv', header: 'Invoice No.', render: (m) => <strong>{m.invoiceNo}</strong> },
  { key: 'arn', header: 'ARN', render: (m) => m.arn },
  { key: 'assured', header: 'Name of Assured', render: (m) => m.assuredName },
  { key: 'booked', header: 'Booked', render: (m) => formatDate(m.bookingDate) },
  { key: 'inception', header: 'Inception', render: (m) => formatDate(m.inceptionDate) },
  { key: 'bal', header: 'Outstanding', numeric: true, render: (m) => <Amount value={m.balance} /> },
];

/** The accounts a rule escalates today (preview). */
function PreviewDialog({ rule, onClose }: Readonly<{ rule: Rule; onClose: () => void }>) {
  const matches = useQuery({
    queryKey: ['collections', 'rule-matches', rule.id],
    queryFn: () => escalationsApi.matches(rule.id),
  });
  return (
    <Modal
      title={`Accounts Meeting ${rule.code}`}
      open
      onClose={onClose}
      footer={
        <Button variant="secondary" onClick={onClose}>
          Close
        </Button>
      }
    >
      <div className="stack">
        <p className="muted">
          {describeRule(rule)} – as of today, before authorization and deduplication.
        </p>
        <ErrorAlert error={matches.error} />
        <DataTable
          caption="Accounts meeting the rule"
          columns={MATCH_COLUMNS}
          rows={matches.data ?? []}
          rowKey={(m) => m.invoiceNo}
          loading={matches.isLoading}
          emptyMessage="No account meets the rule today"
        />
      </div>
    </Modal>
  );
}

/**
 * Escalation rules (BRCLXN.049, CQ14): what escalates an account (aging, commitments, broken
 * promises, overdue installments, amount), to whom and within which SLA. Changes apply once
 * another user authorizes them.
 */
export default function EscalationRulesPage() {
  const companyId = useCompanyId();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [editing, setEditing] = useState<Rule | 'new'>();
  const [previewing, setPreviewing] = useState<Rule>();
  const rules = useQuery({
    queryKey: ['collections', 'rules', companyId],
    queryFn: () => escalationsApi.rules(companyId),
    enabled: companyId > 0,
  });
  const done = async (message: string) => {
    await queryClient.invalidateQueries({ queryKey: ['collections', 'rules'] });
    toast.success(message);
  };
  const save = useMutation({
    mutationFn: (input: RuleInput) =>
      editing === 'new' || editing === undefined
        ? escalationsApi.createRule(input)
        : escalationsApi.updateRule(editing.id, input),
    onSuccess: async (r) => {
      setEditing(undefined);
      await done(`${r.code} saved; it applies once authorized`);
    },
  });
  const decide = useMutation({
    mutationFn: (v: { rule: Rule; authorize: boolean }) =>
      v.authorize
        ? escalationsApi.authorizeRule(v.rule.id)
        : escalationsApi.deactivateRule(v.rule.id),
    onSuccess: (r) => done(`${r.code}: ${humanize(r.recordStatus)}`),
  });
  const columns: Column<Rule>[] = [
    {
      key: 'code',
      header: 'Rule',
      render: (r) => (
        <>
          <strong>{r.code}</strong>
          <div className="muted">{r.name}</div>
        </>
      ),
    },
    { key: 'what', header: 'Escalates When', render: (r) => describeRule(r) },
    { key: 'segment', header: 'Segment', render: (r) => r.segment ?? 'All' },
    { key: 'reason', header: 'Reason', render: (r) => humanize(r.reasonCode) },
    { key: 'sla', header: 'SLA (h)', numeric: true, render: (r) => r.slaHours },
    { key: 'from', header: 'Effective', render: (r) => formatDate(r.effectiveFrom) },
    { key: 'status', header: 'Status', render: (r) => <StatusBadge status={r.recordStatus} /> },
    {
      key: 'actions',
      header: 'Actions',
      render: (r) => (
        <span className="row">
          {(can('CLX_SETUP') || can('MASTER_AUTHORIZE')) && (
            <Button size="sm" variant="ghost" onClick={() => setPreviewing(r)}>
              Preview
            </Button>
          )}
          {can('CLX_SETUP') && r.recordStatus !== 'INACTIVE' && (
            <Button size="sm" variant="secondary" onClick={() => setEditing(r)}>
              Change
            </Button>
          )}
          {can('MASTER_AUTHORIZE') &&
            r.recordStatus === 'PENDING_AUTHORIZATION' &&
            r.maker !== user?.username && (
              <Button
                size="sm"
                busy={decide.isPending}
                onClick={() => decide.mutate({ rule: r, authorize: true })}
              >
                Authorize
              </Button>
            )}
          {can('CLX_SETUP') && r.recordStatus === 'ACTIVE' && (
            <Button
              size="sm"
              variant="danger"
              onClick={() => decide.mutate({ rule: r, authorize: false })}
            >
              Deactivate
            </Button>
          )}
        </span>
      ),
    },
  ];
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Collections"
        title="Escalation Rules"
        description="When an account is escalated automatically, to whom, and how long the receiving level has to act."
        actions={
          can('CLX_SETUP') ? (
            <Button variant="accent" icon={<Plus size={16} />} onClick={() => setEditing('new')}>
              New Rule
            </Button>
          ) : undefined
        }
      />
      <ErrorAlert error={rules.error ?? decide.error} />
      <Card flush>
        <DataTable
          caption="Escalation rules"
          columns={columns}
          rows={rules.data ?? []}
          rowKey={(r) => r.id}
          loading={rules.isLoading}
          emptyMessage="No escalation rules"
        />
      </Card>
      {editing !== undefined && (
        <RuleDialog
          rule={editing === 'new' ? undefined : editing}
          companyId={companyId}
          busy={save.isPending}
          error={save.error}
          onClose={() => setEditing(undefined)}
          onSave={(input) => save.mutate(input)}
        />
      )}
      {previewing !== undefined && (
        <PreviewDialog rule={previewing} onClose={() => setPreviewing(undefined)} />
      )}
    </div>
  );
}
