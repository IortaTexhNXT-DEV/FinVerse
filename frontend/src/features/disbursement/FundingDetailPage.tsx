import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { WorkAction } from '@/api/workflow';
import { useAuth } from '@/auth/authContext';
import { WorkflowPanel } from '@/components/broking/WorkflowPanel';
import { workflowKey } from '@/components/broking/workflowKey';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { disbursementApi } from './api';
import type { Bank, Funding } from './api';
import { EMPTY_FUNDING, bankLabel, fundingErrors, fundingFormOf, fundingInputOf } from './forms';
import type { FundingForm } from './forms';
import { TextDialog } from './VoucherDialogs';
import './disbursement.css';

const ENTITY = 'DisbursementFunding';

function FundingFields({
  form,
  banks,
  errors,
  disabled,
  onChange,
}: Readonly<{
  form: FundingForm;
  banks: Bank[];
  errors: Record<string, string>;
  disabled: boolean;
  onChange: (patch: Partial<FundingForm>) => void;
}>) {
  const bankSelect = (key: 'source' | 'target', label: string) => (
    <Field label={label} required error={errors[key]}>
      {(id) => (
        <select
          id={id}
          className="select"
          disabled={disabled}
          value={form[key]}
          onChange={(e) => onChange({ [key]: e.target.value })}
        >
          <option value="">Select…</option>
          {banks.map((b) => (
            <option key={b.id} value={b.id}>
              {bankLabel(banks, b.id)}
            </option>
          ))}
        </select>
      )}
    </Field>
  );
  return (
    <div className="dsb-form">
      {bankSelect('source', 'From Account')}
      {bankSelect('target', 'To Account')}
      <Field label="Amount" required error={errors.amount}>
        {(id) => (
          <input
            id={id}
            type="number"
            step="0.01"
            className="input"
            disabled={disabled}
            value={form.amount}
            onChange={(e) => onChange({ amount: e.target.value })}
          />
        )}
      </Field>
      <Field label="Currency" required>
        {(id) => (
          <input
            id={id}
            className="input"
            maxLength={3}
            disabled={disabled}
            value={form.currency}
            onChange={(e) => onChange({ currency: e.target.value.toUpperCase() })}
          />
        )}
      </Field>
      <Field label="Value Date" required error={errors.valueDate}>
        {(id) => (
          <input
            id={id}
            type="date"
            className="input"
            disabled={disabled}
            value={form.valueDate}
            onChange={(e) => onChange({ valueDate: e.target.value })}
          />
        )}
      </Field>
      <div className="dsb-form-wide">
        <Field label="Purpose" required error={errors.purpose}>
          {(id) => (
            <textarea
              id={id}
              className="textarea"
              rows={2}
              maxLength={500}
              disabled={disabled}
              value={form.purpose}
              onChange={(e) => onChange({ purpose: e.target.value })}
            />
          )}
        </Field>
      </div>
    </div>
  );
}

const ACTIONS: Record<string, string> = {
  submit: 'Submit for Verification',
  verify: 'Verify',
  approve: 'Approve',
};

/** The editable transfer card: saves a new request or the request still with the maker. */
function TransferCard({
  funding,
  onSaved,
}: Readonly<{ funding?: Funding; onSaved: (f: Funding) => void }>) {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const [form, setForm] = useState<FundingForm>(
    funding === undefined ? EMPTY_FUNDING : fundingFormOf(funding),
  );
  const [touched, setTouched] = useState(false);
  const banks = useQuery({
    queryKey: ['disbursement', 'banks', companyId],
    queryFn: () => disbursementApi.banks(companyId),
    enabled: companyId > 0,
  });
  const save = useMutation({
    mutationFn: () =>
      funding === undefined
        ? disbursementApi.createFunding(fundingInputOf(form, companyId))
        : disbursementApi.updateFunding(funding.id, fundingInputOf(form, companyId)),
    onSuccess: onSaved,
  });
  const errors = fundingErrors(form);
  const editable = can('DISB_FUNDING_REQUEST') && (funding?.stage ?? 'CREATED') === 'CREATED';
  const submit = () => {
    setTouched(true);
    if (Object.keys(errors).length === 0) {
      save.mutate();
    }
  };
  return (
    <Card
      title="Transfer"
      actions={
        editable ? (
          <Button icon={<Save size={16} />} busy={save.isPending} onClick={submit}>
            Save
          </Button>
        ) : undefined
      }
    >
      <div className="stack">
        <ErrorAlert error={save.error ?? banks.error} />
        <FundingFields
          form={form}
          banks={banks.data ?? []}
          errors={touched ? errors : {}}
          disabled={!editable}
          onChange={(patch) => setForm({ ...form, ...patch })}
        />
      </div>
    </Card>
  );
}

function describe(f: Funding): string {
  const parts = [`Requested by ${f.createdBy}`];
  if (f.verifiedBy !== undefined) {
    parts.push(`verified by ${f.verifiedBy}`);
  }
  if (f.journalNo !== undefined) {
    parts.push(`journal ${f.journalNo}`);
  }
  return parts.join(' · ');
}

function ExistingFunding({ id }: Readonly<{ id: number }>) {
  const toast = useToast();
  const queryClient = useQueryClient();
  const [approving, setApproving] = useState(false);
  const key = ['disbursement', 'funding', id];
  const funding = useQuery({ queryKey: key, queryFn: () => disbursementApi.funding(id) });
  const done = async (f: Funding, message: string) => {
    queryClient.setQueryData(key, f);
    await queryClient.invalidateQueries({ queryKey: workflowKey(ENTITY, id) });
    toast.success(`${f.fundingNo}: ${message}`);
  };
  const act = useMutation({
    mutationFn: ({ action, value }: { action: string; value?: string }) =>
      disbursementApi.fundingAction(
        id,
        action as 'submit' | 'verify' | 'approve',
        action === 'approve' ? { bobReference: value === '' ? undefined : value } : {},
      ),
    onSuccess: async (f, { action }) => {
      setApproving(false);
      await done(f, `${ACTIONS[action] ?? action} done`);
    },
  });
  const f = funding.data;
  if (f === undefined) {
    return funding.error ? (
      <ErrorAlert error={funding.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  const businessActions = (available: WorkAction[]) =>
    available
      .filter((a) => ACTIONS[a.action] !== undefined)
      .map((a) => (
        <Button
          key={a.action}
          size="sm"
          busy={act.isPending}
          onClick={() =>
            a.action === 'approve' ? setApproving(true) : act.mutate({ action: a.action })
          }
        >
          {ACTIONS[a.action]}
        </Button>
      ));
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        backTo="/disbursement/funding"
        title={f.fundingNo}
        description={describe(f)}
        actions={<StatusBadge status={f.stage} />}
      />
      <WorkflowPanel
        entityType={ENTITY}
        entityId={f.id}
        renderBusinessActions={businessActions}
        onChanged={() => void funding.refetch()}
      />
      <ErrorAlert error={act.error} />
      <TransferCard
        key={`${f.stage}-${f.amount}`}
        funding={f}
        onSaved={(n) => void done(n, 'saved')}
      />
      {approving && (
        <TextDialog
          title={`Approve ${f.fundingNo}`}
          label="Approve"
          fieldLabel="BOB Reference"
          hint={
            f.stage === 'FOR_APPROVAL_2'
              ? 'The second approval posts the transfer entry.'
              : undefined
          }
          required={false}
          busy={act.isPending}
          error={act.error}
          onClose={() => setApproving(false)}
          onConfirm={(value) => act.mutate({ action: 'approve', value })}
        />
      )}
    </div>
  );
}

function NewFunding() {
  const navigate = useNavigate();
  const toast = useToast();
  return (
    <div className="stack">
      <PageHeader
        section="Finance · Disbursement"
        backTo="/disbursement/funding"
        title="New Funding Request"
        description="Transfer between two BDOIR bank accounts."
      />
      <TransferCard
        onSaved={(f) => {
          toast.success(`${f.fundingNo} saved`);
          void navigate(`/disbursement/funding/${f.id}`, { replace: true });
        }}
      />
    </div>
  );
}

/**
 * A funding request (DIS 2.17.0-2.17.5): the source and target accounts, amount and value date,
 * edited by the maker, then verified and approved twice (DISB_FUNDING, four eyes at each step);
 * the second approval records the BOB reference and posts the transfer entry.
 */
export default function FundingDetailPage() {
  const { id } = useParams();
  return id === undefined || id === 'new' ? <NewFunding /> : <ExistingFunding id={Number(id)} />;
}
