import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus, Save, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import type { Column } from '@/components/ui/DataTable';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { disbursementApi } from './api';
import type { AccountInput, Payee, PayeeAccount } from './api';
import { CREDIT_MODES } from './labels';
import { AccountDialog, AccountFields, PayeeFields } from './PayeeParts';
import {
  ACCOUNT_COLUMNS,
  EMPTY_ACCOUNT,
  EMPTY_PAYEE,
  accountErrors,
  payeeErrors,
  payeeFormOf,
  payeeInputOf,
} from './payeeForm';
import type { PayeeForm } from './payeeForm';
import './disbursement.css';

const ENTITY = 'DisbursementPayee';
const ACTIONS: Record<string, string> = {
  submit: 'Submit for Authorisation',
  authorize: 'Authorise',
  deactivate: 'Request Deactivation',
  reactivate: 'Request Reactivation',
};

function payeeDescription(p: Payee): string {
  const parts = [`Payee ${p.summary.payeeCode}`, `created by ${p.createdBy}`];
  if (p.updatedBy !== undefined) {
    parts.push(`last changed by ${p.updatedBy}`);
  }
  return parts.join(' · ');
}

function Accounts({ payee, onChanged }: Readonly<{ payee: Payee; onChanged: (p: Payee) => void }>) {
  const { can } = useAuth();
  const toast = useToast();
  const [adding, setAdding] = useState(false);
  const deactivate = useMutation({
    mutationFn: (accountId: number) =>
      disbursementApi.deactivateAccount(payee.summary.id, accountId),
    onSuccess: (p) => {
      onChanged(p);
      toast.success('Account deactivated');
    },
  });
  const maintain = can('DISB_PAYEE_MAINTAIN');
  const columns: Column<PayeeAccount>[] = maintain
    ? [
        ...ACCOUNT_COLUMNS,
        {
          key: 'act',
          header: 'Action',
          render: (a) =>
            a.active ? (
              <Button
                size="sm"
                variant="secondary"
                busy={deactivate.isPending}
                onClick={() => deactivate.mutate(a.id)}
              >
                Deactivate
              </Button>
            ) : null,
        },
      ]
    : ACCOUNT_COLUMNS;
  return (
    <Card
      title="Bank Accounts"
      actions={
        maintain ? (
          <Button variant="secondary" icon={<Plus size={16} />} onClick={() => setAdding(true)}>
            Add Account
          </Button>
        ) : undefined
      }
    >
      <ErrorAlert error={deactivate.error} />
      <DataTable
        caption="Bank accounts"
        columns={columns}
        rows={payee.accounts}
        rowKey={(a) => a.id}
        emptyMessage="No bank account; the payee is paid by check"
      />
      {adding && (
        <AccountDialog
          payee={payee}
          onClose={() => setAdding(false)}
          onDone={(p) => {
            setAdding(false);
            onChanged(p);
            toast.success('Account added; the payee goes back for authorisation');
          }}
        />
      )}
    </Card>
  );
}

function NewPayee() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const [params] = useSearchParams();
  const [form, setForm] = useState<PayeeForm>({
    ...EMPTY_PAYEE,
    payeeCode: params.get('code') ?? '',
    name: params.get('name') ?? '',
  });
  const [account, setAccount] = useState<AccountInput>(EMPTY_ACCOUNT);
  const [withAccount, setWithAccount] = useState(false);
  const [touched, setTouched] = useState(false);
  const errors = {
    ...payeeErrors(form, withAccount),
    ...(withAccount ? accountErrors(account) : {}),
  };
  const save = useMutation({
    mutationFn: () =>
      disbursementApi.createPayee(payeeInputOf(form, companyId, withAccount ? [account] : [])),
    onSuccess: (p) => {
      toast.success(`Payee ${p.summary.payeeCode} saved as draft`);
      void navigate(`/disbursement/payees/${p.summary.id}`, { replace: true });
    },
  });
  const submit = () => {
    setTouched(true);
    if (Object.keys(errors).length === 0) {
      save.mutate();
    }
  };
  const shown = touched ? errors : {};
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        backTo="/disbursement/payees"
        title="New Payee"
        description="The payee is saved as a draft and submitted for the team leader's authorisation."
        actions={
          <Button icon={<Save size={16} />} busy={save.isPending} onClick={submit}>
            Save Draft
          </Button>
        }
      />
      <ErrorAlert error={save.error} />
      <Card title="Payee">
        <PayeeFields
          form={form}
          errors={shown}
          creating
          disabled={false}
          onChange={(patch) => setForm({ ...form, ...patch })}
        />
      </Card>
      <Card title="Bank Account">
        <div className="stack">
          <label className="checkbox">
            <input
              type="checkbox"
              checked={withAccount}
              onChange={(e) => setWithAccount(e.target.checked)}
            />
            Add a bank account (needed for credit to account, telegraphic transfer and online
            banking)
          </label>
          {withAccount && <AccountFields value={account} errors={shown} onChange={setAccount} />}
        </div>
      </Card>
    </div>
  );
}

function ExistingPayee({ id }: Readonly<{ id: number }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const key = ['disbursement', 'payee', id];
  const payee = useQuery({ queryKey: key, queryFn: () => disbursementApi.payee(id) });
  const [form, setForm] = useState<PayeeForm>();
  const refresh = async (p?: Payee) => {
    if (p !== undefined) {
      queryClient.setQueryData(key, p);
      setForm(payeeFormOf(p));
    }
    await queryClient.invalidateQueries({ queryKey: workflowKey(ENTITY, id) });
    await queryClient.invalidateQueries({ queryKey: ['disbursement', 'payees'] });
  };
  const save = useMutation({
    mutationFn: (f: PayeeForm) => disbursementApi.updatePayee(id, payeeInputOf(f, companyId)),
    onSuccess: async (p) => {
      await refresh(p);
      toast.success('Payee saved');
    },
  });
  const act = useMutation({
    mutationFn: (action: string) => disbursementApi.payeeAction(id, action),
    onSuccess: async (p, action) => {
      await refresh(p);
      toast.success(`${ACTIONS[action] ?? action} done`);
    },
  });
  const remove = useMutation({
    mutationFn: () => disbursementApi.deletePayee(id),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['disbursement', 'payees'] });
      toast.success('Payee deleted');
      void navigate('/disbursement/payees');
    },
  });
  if (payee.data === undefined) {
    return payee.error ? (
      <ErrorAlert error={payee.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const p = payee.data;
  const current = form ?? payeeFormOf(p);
  const editable =
    can('DISB_PAYEE_MAINTAIN') && (p.summary.stage === 'DRAFT' || p.summary.stage === 'ACTIVE');
  const hasCredit = p.accounts.some((a) => a.active && CREDIT_MODES.includes(a.mode));
  const errors = payeeErrors(current, hasCredit);
  const businessActions = (available: WorkAction[]) =>
    available
      .filter((a) => ACTIONS[a.action] !== undefined)
      .map((a) => (
        <Button key={a.action} size="sm" busy={act.isPending} onClick={() => act.mutate(a.action)}>
          {ACTIONS[a.action]}
        </Button>
      ));
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        backTo="/disbursement/payees"
        title={p.summary.name}
        description={payeeDescription(p)}
        actions={
          <div className="dsb-actions">
            <StatusBadge status={p.summary.stage} />
            {can('DISB_PAYEE_MAINTAIN') && !p.summary.used && (
              <Button
                variant="secondary"
                icon={<Trash2 size={16} />}
                busy={remove.isPending}
                onClick={() => remove.mutate()}
              >
                Delete Payee
              </Button>
            )}
          </div>
        }
      />
      <WorkflowPanel
        entityType={ENTITY}
        entityId={id}
        renderBusinessActions={businessActions}
        onChanged={() => void payee.refetch()}
      />
      <ErrorAlert error={save.error ?? act.error ?? remove.error} />
      <Card
        title="Payee"
        actions={
          editable ? (
            <Button
              icon={<Save size={16} />}
              busy={save.isPending}
              disabled={Object.keys(errors).length > 0}
              onClick={() => save.mutate(current)}
            >
              {p.summary.stage === 'ACTIVE' ? 'Save and Send for Authorisation' : 'Save'}
            </Button>
          ) : undefined
        }
      >
        <PayeeFields
          form={current}
          errors={errors}
          creating={false}
          disabled={!editable}
          onChange={(patch) => setForm({ ...current, ...patch })}
        />
      </Card>
      <Accounts payee={p} onChanged={(n) => void refresh(n)} />
    </div>
  );
}

/**
 * A payee (DIS 2.2.2-2.2.7): its details, bank accounts (masked for other units) and the
 * DISB_PAYEE maker-checker (submit, authorise, deactivate, reactivate); a payee never used by a
 * voucher can be deleted.
 */
export default function PayeePage() {
  const { id } = useParams();
  return id === undefined || id === 'new' ? <NewPayee /> : <ExistingPayee id={Number(id)} />;
}
