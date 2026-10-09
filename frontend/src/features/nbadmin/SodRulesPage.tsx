import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Ban, Check, Plus, X } from 'lucide-react';
import { useState } from 'react';
import { nbadminApi } from '@/api/nbadmin';
import type { SodRule, SodRuleInput, SodRuleKind } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { RowActions } from '@/components/ui/RowActions';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { EMPTY_SOD_RULE, pendingText, RULE_KINDS, ruleErrors } from './sodRules';
import { permissionLabel } from '@/utils/permissionLabel';
import { CellStack } from '@/components/ui/CellStack';
import { ConfigUploadButton } from '@/features/configpromo/ConfigUploadButton';

function NewRuleDialog({ onClose }: Readonly<{ onClose: () => void }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [input, setInput] = useState<SodRuleInput>(EMPTY_SOD_RULE);
  const [tried, setTried] = useState(false);
  const roles = useQuery({ queryKey: ['nbadmin', 'roles'], queryFn: nbadminApi.roles });
  const active = (roles.data ?? []).filter((r) => r.active);
  const errors = tried ? ruleErrors(input) : {};
  const create = useMutation({
    mutationFn: () => nbadminApi.createSodRule({ ...input, description: input.description.trim() }),
    onSuccess: async (rule) => {
      toast.success(`Rule ${rule.ruleCode} sent for authorisation`);
      await queryClient.invalidateQueries({ queryKey: ['nbadmin', 'sod-rules'] });
      onClose();
    },
  });
  const matrix = useQuery({
    queryKey: ['nbadmin', 'access-matrix'],
    queryFn: nbadminApi.matrix,
    enabled: input.kind === 'PERMISSIONS',
  });
  const choices =
    input.kind === 'PERMISSIONS'
      ? (matrix.data?.permissions ?? []).map((p) => ({
          value: p.permission,
          label: permissionLabel(p.permission),
        }))
      : active.map((r) => ({ value: r.code, label: r.name }));
  const pick = (key: 'profileA' | 'profileB', label: string) => (
    <Field label={label} required error={errors[key]}>
      {(id) => (
        <select
          id={id}
          className="select"
          value={input[key]}
          onChange={(e) => setInput({ ...input, [key]: e.target.value })}
        >
          <option value="">Select…</option>
          {choices.map((c) => (
            <option key={c.value} value={c.value}>
              {c.label}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
  return (
    <Modal
      open
      title="New Separation-of-Duties Rule"
      onClose={onClose}
      footer={
        <>
          <Button variant="secondary" onClick={onClose}>
            Cancel
          </Button>
          <Button
            variant="accent"
            busy={create.isPending}
            onClick={() => {
              setTried(true);
              if (Object.keys(ruleErrors(input)).length === 0) {
                create.mutate();
              }
            }}
          >
            Send for Authorisation
          </Button>
        </>
      }
    >
      <div className="stack">
        <ErrorAlert
          error={create.error ?? roles.error}
          title="Cannot send the rule for authorisation"
        />
        <Field label="Rule Type" required>
          {(id) => (
            <select
              id={id}
              className="select"
              value={input.kind}
              onChange={(e) =>
                setInput({
                  ...EMPTY_SOD_RULE,
                  description: input.description,
                  kind: e.target.value as SodRuleKind,
                })
              }
            >
              {RULE_KINDS.map((k) => (
                <option key={k.value} value={k.value}>
                  {k.label}
                </option>
              ))}
            </select>
          )}
        </Field>
        <div className="form-grid">
          {pick('profileA', input.kind === 'PERMISSIONS' ? 'Permission' : 'Group Profile')}
          {pick('profileB', 'May Not Be Held With')}
        </div>
        <Field label="Reason" required error={errors.description}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={3}
              maxLength={500}
              value={input.description}
              onChange={(e) => setInput({ ...input, description: e.target.value })}
            />
          )}
        </Field>
      </div>
    </Modal>
  );
}

/**
 * Separation of Duties (BRD-11 permissions matrix; V1065): the pairs of group profiles one user
 * may not hold together. The Business Administrator adds and deactivates rules; Information
 * Security authorises each change (maker-checker). Every user request and bulk line is checked
 * against the active rules.
 */
export default function SodRulesPage() {
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [adding, setAdding] = useState(false);
  const rules = useQuery({ queryKey: ['nbadmin', 'sod-rules'], queryFn: nbadminApi.sodRules });
  const maintain = can('UAM_SOD_MAINTAIN');
  const authorise = can('UAM_SOD_AUTHORIZE');
  const act = useMutation({
    mutationFn: ({
      rule,
      action,
      reason,
    }: {
      rule: SodRule;
      action: 'deactivate' | 'authorize' | 'reject';
      reason?: string;
    }) => {
      if (action === 'deactivate') {
        return nbadminApi.deactivateSodRule(rule.id);
      }
      return action === 'authorize'
        ? nbadminApi.authorizeSodRule(rule.id)
        : nbadminApi.rejectSodRule(rule.id, reason ?? '');
    },
    onSuccess: async (rule, { action }) => {
      const done = {
        deactivate: 'deactivation sent for authorisation',
        authorize: 'authorised',
        reject: 'rejected',
      }[action];
      toast.success(`Rule ${rule.ruleCode} ${done}`);
      await queryClient.invalidateQueries({ queryKey: ['nbadmin', 'sod-rules'] });
    },
  });
  const mine = (r: SodRule) =>
    user !== null && r.maker.toLowerCase() === user.username.toLowerCase();
  // The actions of a rule in its row action menu (screen standard), each confirmed in a dialog.
  const actions = (r: SodRule) => {
    const pending = r.pendingAction !== 'NONE' && r.status !== 'INACTIVE';
    const pair = `${r.profileAName} and ${r.profileBName}`;
    const change = pendingText(r) || 'The change';
    const decide = pending && authorise && !mine(r);
    return (
      <RowActions
        record={`Rule ${r.ruleCode}`}
        actions={[
          {
            label: 'Authorise',
            icon: <Check size={14} />,
            hidden: !decide,
            confirm: {
              title: `Authorise Rule ${r.ruleCode}`,
              record: pair,
              effect: `${change} takes effect: requests and bulk lines are checked against the active rules.`,
            },
            onSelect: () => act.mutateAsync({ rule: r, action: 'authorize' }),
          },
          {
            label: 'Reject',
            icon: <X size={14} />,
            hidden: !decide,
            danger: true,
            confirm: {
              title: `Reject Rule ${r.ruleCode}`,
              record: pair,
              effect: 'The pending change is rejected and the rule stays as it was.',
              reason: 'required',
              destructive: true,
            },
            onSelect: (reason) => act.mutateAsync({ rule: r, action: 'reject', reason }),
          },
          {
            label: 'Deactivate',
            icon: <Ban size={14} />,
            hidden: pending || r.status !== 'ACTIVE' || !maintain,
            danger: true,
            confirm: {
              title: `Deactivate Rule ${r.ruleCode}`,
              record: pair,
              effect:
                'The deactivation is sent to Information Security; the rule stays active until it is authorised.',
              destructive: true,
            },
            onSelect: () => act.mutateAsync({ rule: r, action: 'deactivate' }),
          },
        ]}
      />
    );
  };
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="Separation of Duties"
        description="Group profiles one user may not hold together. Requests and bulk lines that break an active rule are refused."
        actions={
          <>
            <ConfigUploadButton types={['CFG_SOD_RULE']} />
            {maintain && (
              <Button icon={<Plus size={16} />} onClick={() => setAdding(true)}>
                New Rule
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={rules.error} />
      <ErrorAlert error={act.error} title="Cannot change the rule" />
      <Card flush>
        <DataTable<SodRule>
          caption="Separation-of-duties rules"
          loading={rules.isLoading}
          rows={rules.data ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No separation-of-duties rule yet."
          columns={[
            { key: 'c', header: 'Rule', kind: 'code', render: (r) => r.ruleCode },
            {
              key: 't',
              header: 'Type',
              render: (r) => (r.kind === 'PERMISSIONS' ? 'Permissions' : 'Group profiles'),
            },
            { key: 'a', header: 'Group Profile / Permission', render: (r) => r.profileAName },
            { key: 'b', header: 'May Not Be Held With', render: (r) => r.profileBName },
            { key: 'd', header: 'Reason', render: (r) => r.description },
            {
              key: 's',
              header: 'Status',
              kind: 'status',
              render: (r) => (
                <CellStack main={<StatusBadge status={r.status} />} sub={pendingText(r)} />
              ),
            },
            { key: 'm', header: 'Maker', render: (r) => <UserName login={r.maker} /> },
            {
              key: 'z',
              header: 'Authorised',
              render: (r) =>
                r.authorizedBy ? (
                  <CellStack
                    main={<UserName login={r.authorizedBy} />}
                    sub={formatDateTime(r.authorizedAt)}
                  />
                ) : (
                  ''
                ),
            },
            { key: 'x', header: '', kind: 'actions', render: actions },
          ]}
        />
      </Card>
      {adding && <NewRuleDialog onClose={() => setAdding(false)} />}
    </div>
  );
}
