import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, ChevronRight, Save, Send } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { accountsApi, ACCOUNT_ENTITY } from '@/api/accounts';
import type { Account } from '@/api/accounts';
import { catalogApi } from '@/api/catalog';
import type { ProductDetail } from '@/api/catalog';
import { Attachments } from '@/components/attachments/Attachments';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, today } from '@/utils/format';
import { AccountCheckPanel } from './AccountCheckPanel';
import {
  canSave,
  draftOfAccount,
  duplicateArns,
  newDraft,
  stepProblems,
  toAccountInput,
  WIZARD_STEPS,
} from './accountForm';
import type { AccountDraft, WizardStep } from './accountForm';
import { PremiumSummary } from './PremiumSummary';
import { RiskItemsStep } from './RiskItemsStep';
import { useAutosave } from './useAutosave';
import { ClientStep, ContactStep, PeriodStep, ProductStep } from './WizardSteps';
import type { StepProps } from './WizardSteps';

function DuplicateFallout({ arns }: Readonly<{ arns: string[] }>) {
  return (
    <div className="alert danger" role="alert">
      These risks are already on {arns.length === 1 ? 'account' : 'accounts'}{' '}
      {arns.map((arn) => (
        <Link key={arn} to={`/accounts/by-arn/${arn}`} style={{ marginRight: 'var(--space-2)' }}>
          {arn}
        </Link>
      ))}
      — open the existing account instead of creating a new one.
    </div>
  );
}

function Stepper({
  step,
  onStep,
}: Readonly<{ step: WizardStep; onStep: (s: WizardStep) => void }>) {
  const current = WIZARD_STEPS.findIndex((s) => s.id === step);
  return (
    <div className="tabs" role="tablist" aria-label="Wizard steps">
      {WIZARD_STEPS.map((s, i) => (
        <button
          key={s.id}
          type="button"
          role="tab"
          className="tab"
          aria-selected={s.id === step}
          disabled={i > current + 1}
          onClick={() => onStep(s.id)}
        >
          {i + 1}. {s.label}
        </button>
      ))}
    </div>
  );
}

function SaveBar({
  draft,
  savedAt,
  busy,
  onSave,
}: Readonly<{ draft: AccountDraft; savedAt?: string; busy: boolean; onSave: () => void }>) {
  return (
    <>
      {draft.arn && <ReferenceChip label="ARN" value={draft.arn} />}
      {savedAt && <span className="muted">Saved {formatDateTime(savedAt)}</span>}
      <Button
        variant="secondary"
        icon={<Save size={16} />}
        disabled={!canSave(draft)}
        busy={busy}
        onClick={onSave}
      >
        Save Draft
      </Button>
    </>
  );
}

function StepBody({
  step,
  props,
  detail,
}: Readonly<{ step: WizardStep; props: StepProps; detail?: ProductDetail }>) {
  const { draft, set } = props;
  switch (step) {
    case 'client':
      return <ClientStep {...props} />;
    case 'product':
      return <ProductStep {...props} />;
    case 'period':
      return <PeriodStep {...props} />;
    case 'items':
      return (
        <RiskItemsStep
          kind={detail?.riskItemKind ?? 'GENERIC'}
          motor={detail?.ratingMethod === 'MOTOR'}
          fleet={detail?.product.fleetCapable === true}
          items={draft.items}
          onChange={(items) => set({ items })}
        />
      );
    case 'contact':
      return <ContactStep {...props} />;
    default:
      return draft.id === undefined ? (
        <p className="muted">Choose the client and product to save the draft.</p>
      ) : (
        <AccountCheckPanel accountId={draft.id} />
      );
  }
}

function Feedback({ error, problems }: Readonly<{ error: unknown; problems: string[] }>) {
  const dupes = duplicateArns(error);
  return (
    <>
      {dupes.length > 0 ? <DuplicateFallout arns={dupes} /> : <ErrorAlert error={error} />}
      {problems.length > 0 && (
        <div className="alert warning" role="alert">
          {problems.join(' ')}
        </div>
      )}
    </>
  );
}

interface FooterProps {
  step: WizardStep;
  onGo: (step: WizardStep) => void;
  canSubmit: boolean;
  submitting: boolean;
  resubmission: boolean;
  onSubmit: () => void;
}

function WizardFooter({
  step,
  onGo,
  canSubmit,
  submitting,
  resubmission,
  onSubmit,
}: Readonly<FooterProps>) {
  const index = WIZARD_STEPS.findIndex((s) => s.id === step);
  const next = WIZARD_STEPS[index + 1]?.id;
  const previous = WIZARD_STEPS[index - 1]?.id;
  return (
    <div className="row">
      {previous && (
        <Button variant="secondary" icon={<ChevronLeft size={16} />} onClick={() => onGo(previous)}>
          Back
        </Button>
      )}
      <div className="spacer" />
      {next && (
        <Button variant="accent" icon={<ChevronRight size={16} />} onClick={() => onGo(next)}>
          Next
        </Button>
      )}
      {step === 'review' && (
        <Button
          variant="accent"
          icon={<Send size={16} />}
          disabled={!canSubmit}
          busy={submitting}
          onClick={onSubmit}
        >
          {resubmission ? 'Resubmit to Processing' : 'Submit to Processing'}
        </Button>
      )}
    </div>
  );
}

/** Saves the draft (create, then update) and submits it. */
function useAccountSaver(
  setDraft: (update: (d: AccountDraft) => AccountDraft) => void,
  resubmission: boolean,
) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [savedAt, setSavedAt] = useState<string>();
  const persist = async (d: AccountDraft): Promise<Account> => {
    const body = toAccountInput(d, companyId);
    const account =
      d.id === undefined ? await accountsApi.create(body) : await accountsApi.update(d.id, body);
    setDraft((cur) => ({ ...cur, id: account.id, arn: account.arn }));
    setSavedAt(new Date().toISOString());
    queryClient.setQueryData(['account', account.id], account);
    await queryClient.invalidateQueries({ queryKey: ['account', account.id, 'check'] });
    return account;
  };
  const save = useMutation({ mutationFn: persist });
  const submit = useMutation({
    mutationFn: async (d: AccountDraft) => {
      const account = await persist(d);
      return resubmission ? accountsApi.resubmit(account.id) : accountsApi.submit(account.id);
    },
    onSuccess: async (a) => {
      await queryClient.invalidateQueries({ queryKey: ['accounts'] });
      toast.success(`${a.arn} submitted to Processing`);
      void navigate(`/accounts/${a.id}`);
    },
  });
  return { save, submit, savedAt };
}

function Wizard({
  initial,
  resubmission,
}: Readonly<{ initial: AccountDraft; resubmission: boolean }>) {
  const [draft, setDraft] = useState(initial);
  const [step, setStep] = useState<WizardStep>(initial.id === undefined ? 'client' : 'items');
  const [problems, setProblems] = useState<string[]>([]);
  const set = (patch: Partial<AccountDraft>) => setDraft((d) => ({ ...d, ...patch }));
  const product = useQuery({
    queryKey: ['catalog', 'product', draft.productCode],
    queryFn: () => catalogApi.product(draft.productCode),
    enabled: draft.productCode !== '',
  });
  const { save, submit, savedAt } = useAccountSaver(setDraft, resubmission);
  const markSaved = useAutosave(draft, canSave(draft) && !save.isPending, (d) =>
    save.mutateAsync(d),
  );
  const saveNow = () => save.mutate(draft, { onSuccess: () => markSaved(draft) });
  const go = (target: WizardStep) => {
    const forward =
      WIZARD_STEPS.findIndex((s) => s.id === target) > WIZARD_STEPS.findIndex((s) => s.id === step);
    const blocking = forward ? stepProblems(step, draft) : [];
    setProblems(blocking);
    if (blocking.length === 0) {
      setStep(target);
      if (target === 'review' && canSave(draft)) {
        saveNow();
      }
    }
  };
  const saved = draft.id !== undefined;
  const stepProps = { draft, set, product: product.data?.product, saved };

  return (
    <div className="stack">
      <PageHeader
        section="Accounts & Placement"
        title={saved ? `Account ${draft.arn ?? ''}` : 'New account'}
        description={
          draft.clientName || 'Enter the account step by step; the draft is saved automatically.'
        }
        actions={<SaveBar draft={draft} savedAt={savedAt} busy={save.isPending} onSave={saveNow} />}
      />
      <Stepper step={step} onStep={go} />
      <Feedback error={save.error ?? submit.error} problems={problems} />
      <Card title={WIZARD_STEPS.find((s) => s.id === step)?.label}>
        <StepBody step={step} props={stepProps} detail={product.data} />
      </Card>
      {step === 'review' && draft.id !== undefined && (
        <>
          <PremiumSummary accountId={draft.id} />
          <Attachments
            entityType={ACCOUNT_ENTITY}
            entityId={draft.id}
            title="Documents"
            reference={draft.arn}
          />
        </>
      )}
      <WizardFooter
        step={step}
        onGo={go}
        canSubmit={saved}
        submitting={submit.isPending}
        resubmission={resubmission}
        onSubmit={() => submit.mutate(draft)}
      />
    </div>
  );
}

/**
 * New account in six steps (BRNB.051): client, product, period and payment, risk items,
 * contact and premium, then review with the completeness check, documents and submission.
 * The draft is saved every 30 seconds once the client and product are chosen; risks already on
 * a live account are refused with the existing ARN. With an id, continues a draft or a
 * returned account.
 */
export default function AccountWizardPage() {
  const params = useParams();
  const id = params.id === undefined ? undefined : Number(params.id);
  const existing = useQuery({
    queryKey: ['account', id],
    queryFn: () => accountsApi.get(id ?? 0),
    enabled: id !== undefined,
  });
  if (id === undefined) {
    return <Wizard initial={newDraft(today())} resubmission={false} />;
  }
  if (existing.data === undefined) {
    return existing.error ? (
      <ErrorAlert error={existing.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  return (
    <Wizard
      key={existing.data.id}
      initial={draftOfAccount(existing.data)}
      resubmission={existing.data.status === 'RETURNED_TO_MARKETING'}
    />
  );
}
