import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Download, Save, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { PACKAGE_REQUEST_ENTITY, productMaintApi } from '@/api/productmaint';
import type { PackageRequest } from '@/api/productmaint';
import { Attachments } from '@/components/attachments/Attachments';
import { Button } from '@/components/ui/Button';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import {
  formOfRequest,
  newRequestForm,
  requestErrors,
  submissionGaps,
  toRequestInput,
} from './packageRequest';
import type { RequestForm } from './packageRequest';
import { RequestHeaderCard } from './RequestHeaderCard';
import { TermsEditor } from './TermsEditor';
import '@/styles/quotation.css';

function useRequestSaver(setForm: (u: (f: RequestForm) => RequestForm) => void) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const persist = async (f: RequestForm): Promise<PackageRequest> => {
    const body = toRequestInput(f, companyId);
    const saved =
      f.id === undefined
        ? await productMaintApi.create(body)
        : await productMaintApi.update(f.id, body);
    setForm((cur) => ({ ...cur, id: saved.id, requestNo: saved.requestNo }));
    queryClient.setQueryData(['package-request', saved.id], saved);
    await queryClient.invalidateQueries({ queryKey: ['package-requests'] });
    return saved;
  };
  const save = useMutation({
    mutationFn: persist,
    onSuccess: (p) => toast.success(`${p.requestNo} saved`),
  });
  const submit = useMutation({
    mutationFn: async (f: RequestForm) => productMaintApi.submit((await persist(f)).id),
    onSuccess: (p) => {
      toast.success(`${p.requestNo} submitted for Marketing approval`);
      void navigate(`/product-maintenance/requests/${p.id}`);
    },
  });
  return { save, submit };
}

function Form({ initial }: Readonly<{ initial: RequestForm }>) {
  const toast = useToast();
  const [form, setForm] = useState(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (patch: Partial<RequestForm>) => setForm((f) => ({ ...f, ...patch }));
  const { save, submit } = useRequestSaver(setForm);
  const prefill = useMutation({
    mutationFn: () => productMaintApi.prefill(form.productCode),
    onSuccess: (p) => {
      set({ terms: { ...p.terms, sections: form.terms.sections } });
      toast.success(`Terms of ${p.productCode} version ${p.versionNo} loaded`);
    },
  });
  const run = (action: typeof save) => {
    const found = requestErrors(form);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      action.mutate(form);
    }
  };
  const gaps = submissionGaps(form);
  const saved = form.id !== undefined;
  return (
    <div className="stack">
      <PageHeader
        backTo="/product-maintenance/requests"
        section="Product Maintenance · Package Requests"
        title={saved ? `Package Request ${form.requestNo ?? ''}` : 'New Package Request'}
        description="Describe the package, the requested terms and the insurers to approach, then submit the request for Marketing approval."
        actions={
          <>
            {form.type !== 'NEW' && form.productCode !== '' && (
              <Button
                variant="secondary"
                icon={<Download size={16} />}
                busy={prefill.isPending}
                onClick={() => prefill.mutate()}
              >
                Load Current Terms
              </Button>
            )}
            <Button
              variant="secondary"
              icon={<Save size={16} />}
              busy={save.isPending}
              onClick={() => run(save)}
            >
              Save Draft
            </Button>
            <Button
              variant="accent"
              icon={<Send size={16} />}
              busy={submit.isPending}
              onClick={() => run(submit)}
            >
              Submit for Approval
            </Button>
          </>
        }
      />
      <ErrorAlert error={save.error ?? submit.error ?? prefill.error} />
      {gaps.length > 0 && (
        <p className="muted" role="status">
          Before submitting, complete {gaps.join('; ')}.
        </p>
      )}
      <RequestHeaderCard form={form} set={set} errors={errors} />
      {form.type !== 'RETIRE' && (
        <TermsEditor
          terms={form.terms}
          onChange={(terms) => set({ terms })}
          errors={errors}
          withInsurers={form.negotiationRequired}
        />
      )}
      {form.id === undefined ? (
        <p className="muted">Save the draft to attach the supporting documents.</p>
      ) : (
        <Attachments
          entityType={PACKAGE_REQUEST_ENTITY}
          entityId={form.id}
          title="Documents"
          reference={form.requestNo}
        />
      )}
    </div>
  );
}

/**
 * Package Request Form (BRPM.008/011): type and scope, client or programme, line, cover type and
 * product, reason, requested terms by section and coverage, rate scheme and package term, target
 * insurers and documents. Marketing or TSU edit a draft; the TSU Team Lead completes it during the
 * review.
 */
export default function PackageRequestFormPage() {
  const params = useParams();
  const id = params.id === undefined ? undefined : Number(params.id);
  const existing = useQuery({
    queryKey: ['package-request', id],
    queryFn: () => productMaintApi.get(id ?? 0),
    enabled: id !== undefined,
  });
  if (id === undefined) {
    return <Form initial={newRequestForm()} />;
  }
  if (existing.data === undefined) {
    return existing.error ? (
      <ErrorAlert error={existing.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  return <Form key={existing.data.id} initial={formOfRequest(existing.data)} />;
}
