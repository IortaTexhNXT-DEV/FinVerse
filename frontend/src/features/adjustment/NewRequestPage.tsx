import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft, ArrowRight, Calculator, Save, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { adjustmentApi } from './api';
import type { EndorsementRequest, Recompute, RequestInput } from './api';
import { InvoicePicker } from './InvoicePicker';
import { RecomputePreview } from './RecomputePreview';
import { RequestFormFields } from './RequestFormFields';
import { EMPTY_FORM, formErrors, formOf, isValid, toInput } from './requestForm';
import type { RequestForm } from './requestForm';
import './adjustment.css';

const STEPS = ['Invoices', 'Request', 'Recompute & Submit'] as const;

function Steps({ current }: Readonly<{ current: number }>) {
  return (
    <ol className="adj-steps" aria-label="Steps">
      {STEPS.map((label, i) => (
        <li key={label} className="adj-step" aria-current={i === current ? 'step' : undefined}>
          <span className="adj-step-number">{i + 1}</span>
          {label}
        </li>
      ))}
    </ol>
  );
}

/** Saves the requests (one per invoice, or the change of an existing one), then submits them. */
function useSave(existing: EndorsementRequest | undefined, input: RequestInput) {
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: async (submit: boolean) => {
      const saved = existing
        ? [await adjustmentApi.update(existing.id, input)]
        : await adjustmentApi.create(input);
      if (submit) {
        await Promise.all(
          saved.map((r) =>
            r.stage === 'RETURNED' ? adjustmentApi.resubmit(r.id) : adjustmentApi.submit(r.id),
          ),
        );
      }
      return saved;
    },
    onSuccess: async (saved, submit) => {
      await queryClient.invalidateQueries({ queryKey: ['adjustment'] });
      const numbers = saved.map((r) => r.requestNo).join(', ');
      toast.success(`${numbers} ${submit ? 'submitted for validation' : 'saved as draft'}`);
      const first = saved[0];
      const target =
        saved.length === 1 && first ? `/adjustment/requests/${String(first.id)}` : '/adjustment';
      void navigate(target);
    },
  });
}

interface FooterProps {
  step: number;
  firstStep: number;
  canContinue: boolean;
  ready: boolean;
  busy: boolean;
  onStep: (step: number) => void;
  onNext: () => void;
  onSave: (submit: boolean) => void;
}

function WizardFooter(p: Readonly<FooterProps>) {
  return (
    <div className="adj-footer">
      {p.step > p.firstStep && (
        <Button
          variant="secondary"
          icon={<ArrowLeft size={16} />}
          onClick={() => p.onStep(p.step - 1)}
        >
          Back
        </Button>
      )}
      {p.step === 0 && (
        <Button icon={<ArrowRight size={16} />} disabled={!p.canContinue} onClick={p.onNext}>
          Next
        </Button>
      )}
      {p.step === 1 && (
        <Button icon={<Calculator size={16} />} onClick={p.onNext}>
          Recompute
        </Button>
      )}
      {p.step === 2 && (
        <>
          <Button
            variant="secondary"
            icon={<Save size={16} />}
            busy={p.busy}
            disabled={!p.ready}
            onClick={() => p.onSave(false)}
          >
            Save Draft
          </Button>
          <Button
            variant="accent"
            icon={<Send size={16} />}
            busy={p.busy}
            disabled={!p.ready}
            onClick={() => p.onSave(true)}
          >
            Submit for Validation
          </Button>
        </>
      )}
    </div>
  );
}

interface WizardProps {
  existing?: EndorsementRequest;
  initialInvoices: string[];
}

function PreviewStep({
  preview,
  form,
  onChange,
}: Readonly<{
  preview: { data?: Recompute; isLoading: boolean };
  form: RequestForm;
  onChange: (form: RequestForm) => void;
}>) {
  if (preview.data === undefined) {
    return preview.isLoading ? <span className="spinner" aria-label="Loading" /> : null;
  }
  return <RecomputePreview recompute={preview.data} form={form} onChange={onChange} />;
}

/** The three steps: invoices, request, recompute preview with save or submit. */
function Wizard({ existing, initialInvoices }: Readonly<WizardProps>) {
  const firstStep = existing ? 1 : 0;
  const [step, setStep] = useState(initialInvoices.length > 0 ? 1 : 0);
  const [invoices, setInvoices] = useState<string[]>(initialInvoices);
  const [form, setForm] = useState<RequestForm>(existing ? formOf(existing) : EMPTY_FORM);
  const [touched, setTouched] = useState(false);
  const errors = formErrors(form);
  const input = toInput(form, invoices);
  const preview = useQuery({
    queryKey: ['adjustment', 'preview', input],
    queryFn: () => adjustmentApi.preview(input),
    enabled: step === 2 && isValid(errors),
    retry: false,
  });
  const save = useSave(existing, input);
  const next = () => {
    setTouched(true);
    const ok = step === 0 ? invoices.length > 0 : isValid(errors);
    if (ok) {
      setStep(step + 1);
    }
  };
  return (
    <div className="stack">
      <Steps current={step} />
      <ErrorAlert error={save.error ?? preview.error} />
      {step === 0 && (
        <Card title="Booked Invoices">
          <InvoicePicker selected={invoices} onChange={setInvoices} />
        </Card>
      )}
      {step === 1 && (
        <Card title={`Request on ${invoices.join(', ')}`}>
          <RequestFormFields form={form} errors={touched ? errors : {}} onChange={setForm} />
        </Card>
      )}
      {step === 2 && <PreviewStep preview={preview} form={form} onChange={setForm} />}
      <WizardFooter
        step={step}
        firstStep={firstStep}
        canContinue={invoices.length > 0}
        ready={preview.data !== undefined}
        busy={save.isPending}
        onStep={setStep}
        onNext={next}
        onSave={(submit) => save.mutate(submit)}
      />
    </div>
  );
}

function invoicesOf(existing: EndorsementRequest | undefined, invoice: string | null): string[] {
  if (existing) {
    return [existing.invoice.invoiceNo];
  }
  return invoice ? [invoice] : [];
}

/**
 * New endorsement request wizard (ADJID.001-004/008/014/023/028, MKTID.008): choose one or more
 * booked invoices, enter the request (the form adapts to the type), review the recompute before /
 * after and per insurer with the service invoice impact, justify a duplicate or an
 * over-adjustment, then save as draft or submit. Also changes a draft or returned request.
 */
export default function NewRequestPage() {
  const { id } = useParams();
  const [params] = useSearchParams();
  const editing = id !== undefined;
  const existing = useQuery({
    queryKey: ['adjustment', 'request', Number(id)],
    queryFn: () => adjustmentApi.get(Number(id)),
    enabled: editing,
  });
  const requestNo = existing.data?.requestNo;
  const title = editing ? `Change ${requestNo ?? 'Request'}` : 'New Endorsement Request';
  return (
    <div className="stack">
      <PageHeader
        section="Client & Policy · Adjustment"
        backTo={editing ? `/adjustment/requests/${id}` : '/adjustment'}
        title={title}
        description="Raise an endorsement or cancellation on booked invoices; one request is created per invoice."
      />
      <ErrorAlert error={existing.error} />
      {editing && existing.data === undefined ? (
        <span className="spinner" aria-label="Loading" />
      ) : (
        <Wizard
          existing={existing.data}
          initialInvoices={invoicesOf(existing.data, params.get('invoice'))}
        />
      )}
    </div>
  );
}
