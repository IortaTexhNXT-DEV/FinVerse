import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, ChevronRight, Save, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import type { ProductDetail } from '@/api/catalog';
import { quotationsApi } from '@/api/quotations';
import type { Quotation } from '@/api/quotations';
import { ReferenceChip } from '@/components/broking/ReferenceChip';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { Tabs } from '@/components/ui/Tabs';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { DetailList } from '@/features/catalog/DetailList';
import { formatDate, today } from '@/utils/format';
import { LivePremium } from './PremiumBreakdown';
import {
  canSaveQuotation,
  formOfQuotation,
  newQuotationForm,
  QUOTATION_STEPS,
  quotationStepErrors,
  toQuotationInput,
} from './quotationForm';
import type { Preselection, QuotationForm, QuotationStep } from './quotationForm';
import { ClientStep, ItemsStep, TermsStep } from './QuotationSteps';
import type { QuotationStepProps } from './QuotationSteps';
import '@/styles/quotation.css';

function numberParam(value: string | null): number | undefined {
  return value === null || value === '' ? undefined : Number(value);
}

function preselection(params: URLSearchParams): Preselection {
  return {
    clientId: numberParam(params.get('client')),
    requestId: numberParam(params.get('request')),
    productCode: params.get('product') ?? undefined,
    marketSegment: params.get('segment') ?? undefined,
    sourceChannel: params.get('channel') ?? undefined,
  };
}

function ReviewStep({ form }: Readonly<{ form: QuotationForm }>) {
  return (
    <DetailList
      rows={[
        ['Client', form.clientName || '—'],
        ['Product', form.productCode],
        ['Insurer', form.insurerCode || 'To be advised'],
        ['Period', `${formatDate(form.periodFrom)} to ${formatDate(form.periodTo)}`],
        [
          'Valid until',
          form.validUntil === '' ? 'Configured validity' : formatDate(form.validUntil),
        ],
        ['Risk items', String(form.items.length)],
        ['Accounts (risk groups)', String(new Set(form.groups).size || 1)],
        ['Direct payment', form.directPayment ? 'Yes' : 'No'],
      ]}
    />
  );
}

function useQuotationSaver(setForm: (update: (f: QuotationForm) => QuotationForm) => void) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const persist = async (f: QuotationForm): Promise<Quotation> => {
    const body = toQuotationInput(f, companyId);
    const saved =
      f.id === undefined
        ? await quotationsApi.create(body)
        : await quotationsApi.update(f.id, body);
    setForm((cur) => ({ ...cur, id: saved.id, quotationNo: saved.quotationNo, arn: saved.arn }));
    queryClient.setQueryData(['quotation', saved.id], saved);
    await queryClient.invalidateQueries({ queryKey: ['quotations'] });
    return saved;
  };
  const save = useMutation({
    mutationFn: persist,
    onSuccess: (q) => toast.success(`${q.quotationNo} saved (version ${q.currentVersion})`),
  });
  const submit = useMutation({
    mutationFn: async (f: QuotationForm) => quotationsApi.submit((await persist(f)).id),
    onSuccess: (q) => {
      toast.success(`${q.quotationNo} submitted for review`);
      void navigate(`/quotations/${q.id}`);
    },
  });
  return { save, submit };
}

function StepBody({
  step,
  props,
  detail,
}: Readonly<{ step: QuotationStep; props: QuotationStepProps; detail?: ProductDetail }>) {
  switch (step) {
    case 'client':
      return <ClientStep {...props} />;
    case 'product':
      return <TermsStep {...props} product={detail?.product} />;
    case 'items':
      return <ItemsStep {...props} detail={detail} />;
    case 'premium':
      return (
        <p className="muted">
          The premium below is computed by the server with the catalog rates (Appendix A) and is
          stored with the version when you save.
        </p>
      );
    default:
      return <ReviewStep form={props.form} />;
  }
}

function WizardFooter({
  step,
  onGo,
  canSubmit,
  submitting,
  onSubmit,
}: Readonly<{
  step: QuotationStep;
  onGo: (s: QuotationStep) => void;
  canSubmit: boolean;
  submitting: boolean;
  onSubmit: () => void;
}>) {
  const index = QUOTATION_STEPS.findIndex((s) => s.id === step);
  const next = QUOTATION_STEPS[index + 1]?.id;
  const previous = QUOTATION_STEPS[index - 1]?.id;
  return (
    <div className="row">
      {previous && (
        <Button variant="secondary" icon={<ChevronLeft size={16} />} onClick={() => onGo(previous)}>
          Back
        </Button>
      )}
      <div className="spacer" />
      {next && (
        <Button variant="primary" icon={<ChevronRight size={16} />} onClick={() => onGo(next)}>
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
          Submit for Review
        </Button>
      )}
    </div>
  );
}

const PREMIUM_STEPS = new Set<QuotationStep>(['items', 'premium', 'review']);

function Wizard({ initial }: Readonly<{ initial: QuotationForm }>) {
  const [form, setForm] = useState(initial);
  const [step, setStep] = useState<QuotationStep>(
    initial.clientId === undefined ? 'client' : 'product',
  );
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (patch: Partial<QuotationForm>) => setForm((f) => ({ ...f, ...patch }));
  const companyId = useCompanyId();
  const detail = useQuery({
    queryKey: ['catalog', 'product', form.productCode],
    queryFn: () => catalogApi.product(form.productCode),
    enabled: form.productCode !== '',
  });
  const { save, submit } = useQuotationSaver(setForm);
  const index = QUOTATION_STEPS.findIndex((s) => s.id === step);
  const go = (target: QuotationStep) => {
    const forward = QUOTATION_STEPS.findIndex((s) => s.id === target) > index;
    const blocking = forward ? quotationStepErrors(step, form) : {};
    setErrors(blocking);
    if (Object.keys(blocking).length === 0) {
      setStep(target);
    }
  };
  const saved = form.id !== undefined;
  const props = { form, set, errors, saved };
  return (
    <div className="stack">
      <PageHeader
        backTo="/quotations"
        section="Quotation / Proposal"
        title={saved ? `Quotation ${form.quotationNo ?? ''}` : 'New Quotation'}
        description="Client or prospect, product and terms, risk items, then the premium computed live with the rates in force."
        actions={
          <>
            {form.arn && <ReferenceChip label="ARN" value={form.arn} />}
            <Button
              variant="secondary"
              icon={<Save size={16} />}
              disabled={!canSaveQuotation(form)}
              busy={save.isPending}
              onClick={() => save.mutate(form)}
            >
              Save Draft
            </Button>
          </>
        }
      />
      <Tabs
        tabs={QUOTATION_STEPS.map((s, i) => ({ id: s.id, label: `${i + 1}. ${s.label}` }))}
        active={step}
        onChange={go}
      />
      <ErrorAlert error={save.error ?? submit.error} />
      <Card title={QUOTATION_STEPS[index]?.label}>
        <StepBody step={step} props={props} detail={detail.data} />
      </Card>
      {PREMIUM_STEPS.has(step) && <LivePremium input={toQuotationInput(form, companyId)} />}
      <WizardFooter
        step={step}
        onGo={go}
        canSubmit={canSaveQuotation(form)}
        submitting={submit.isPending}
        onSubmit={() => submit.mutate(form)}
      />
    </div>
  );
}

/**
 * New quotation wizard (BRNB.043/063): client or prospect, product and terms, risk items with
 * their risk groups, live premium (Appendix A) and review. With an id, continues a draft; a
 * change after submission opens the next version (BRNB.020). The client, product or request can
 * be preselected from the client page or the request inbox.
 */
export default function QuotationWizardPage() {
  const params = useParams();
  const [search] = useSearchParams();
  const id = params.id === undefined ? undefined : Number(params.id);
  const existing = useQuery({
    queryKey: ['quotation', id],
    queryFn: () => quotationsApi.get(id ?? 0),
    enabled: id !== undefined,
  });
  if (id === undefined) {
    return <Wizard initial={newQuotationForm(today(), preselection(search))} />;
  }
  if (existing.data === undefined) {
    return existing.error ? (
      <ErrorAlert error={existing.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  return <Wizard key={existing.data.id} initial={formOfQuotation(existing.data)} />;
}
