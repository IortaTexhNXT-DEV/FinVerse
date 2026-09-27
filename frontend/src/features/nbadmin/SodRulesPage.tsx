import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Ban, Check, Plus, X } from 'lucide-react';
import { useState } from 'react';
import { nbadminApi } from '@/api/nbadmin';
import type { SodRule, SodRuleInput } from '@/api/nbadmin';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { UserName } from '@/components/ui/UserName';
import { formatDateTime } from '@/utils/format';
import { EMPTY_SOD_RULE, pendingText, ruleErrors } from './sodRules';

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
  const profile = (key: 'profileA' | 'profileB', label: string) => (
    <Field label={label} required error={errors[key]}>
      {(id) => (
        <select
          id={id}
          className="select"
          value={input[key]}
          onChange={(e) => setInput({ ...input, [key]: e.target.value })}
        >
          <option value="">Select a group profile…</option>
          {active.map((r) => (
            <option key={r.code} value={r.code}>
              {r.name}
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
        <ErrorAlert error={create.error ?? roles.error} />
        <div className="form-grid">
          {profile('profileA', 'Group Profile')}
          {profile('profileB', 'May Not Be Held With')}
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
    }: {
      rule: SodRule;
      action: 'deactivate' | 'authorize' | 'reject';
    }) => {
      if (action === 'deactivate') {
        return nbadminApi.deactivateSodRule(rule.id);
      }
      return action === 'authorize'
        ? nbadminApi.authorizeSodRule(rule.id)
        : nbadminApi.rejectSodRule(rule.id);
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
  const actions = (r: SodRule) => {
    const pending = r.pendingAction !== 'NONE' && r.status !== 'INACTIVE';
    if (pending && authorise && !mine(r)) {
      return (
        <div className="row">
          <Button
            size="sm"
            icon={<Check size={14} />}
            onClick={() => act.mutate({ rule: r, action: 'authorize' })}
          >
            Authorise
          </Button>
          <Button
            size="sm"
            variant="secondary"
            icon={<X size={14} />}
            onClick={() => act.mutate({ rule: r, action: 'reject' })}
          >
            Reject
          </Button>
        </div>
      );
    }
    if (!pending && r.status === 'ACTIVE' && maintain) {
      return (
        <Button
          size="sm"
          variant="secondary"
          icon={<Ban size={14} />}
          onClick={() => act.mutate({ rule: r, action: 'deactivate' })}
        >
          Deactivate
        </Button>
      );
    }
    return null;
  };
  return (
    <div className="stack">
      <PageHeader
        section="User Access"
        title="Separation of Duties"
        description="Group profiles one user may not hold together. Requests and bulk lines that break an active rule are refused."
        actions={
          maintain && (
            <Button icon={<Plus size={16} />} onClick={() => setAdding(true)}>
              New Rule
            </Button>
          )
        }
      />
      <ErrorAlert error={rules.error ?? act.error} />
      <Card flush>
        <DataTable<SodRule>
          caption="Separation-of-duties rules"
          loading={rules.isLoading}
          rows={rules.data ?? []}
          rowKey={(r) => r.id}
          emptyMessage="No separation-of-duties rule yet."
          columns={[
            {
              key: 'c',
              header: 'Rule',
              render: (r) => <strong className="mono">{r.ruleCode}</strong>,
            },
            { key: 'a', header: 'Group Profile', render: (r) => r.profileAName },
            { key: 'b', header: 'May Not Be Held With', render: (r) => r.profileBName },
            { key: 'd', header: 'Reason', render: (r) => r.description },
            {
              key: 's',
              header: 'Status',
              render: (r) => (
                <>
                  <StatusBadge status={r.status} />
                  {pendingText(r) && <span className="cell-sub">{pendingText(r)}</span>}
                </>
              ),
            },
            { key: 'm', header: 'Maker', render: (r) => <UserName login={r.maker} /> },
            {
              key: 'z',
              header: 'Authorised',
              render: (r) =>
                r.authorizedBy ? (
                  <>
                    <UserName login={r.authorizedBy} />
                    <span className="cell-sub">{formatDateTime(r.authorizedAt)}</span>
                  </>
                ) : (
                  ''
                ),
            },
            { key: 'x', header: 'Actions', render: actions },
          ]}
        />
      </Card>
      {adding && <NewRuleDialog onClose={() => setAdding(false)} />}
    </div>
  );
}
