import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Send } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams, useSearchParams } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import { PROPOSAL_ENTITY, proposalsApi } from '@/api/proposals';
import type { Proposal } from '@/api/proposals';
import { Attachments } from '@/components/attachments/Attachments';
import { ClientPicker } from '@/components/broking/ClientPicker';
import { LovSelect } from '@/components/broking/LovSelect';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { RiskItemsStep } from '@/features/accounts/RiskItemsStep';
import { SelectInput, TextInput } from '@/features/assets/FormControls';
import { alignGroups } from '@/features/quotations/quotationForm';
import { DocumentChecklist, InsurerChoices, SectionsCard } from './ProposalFormParts';
import { formOfProposal, newProposalForm, proposalErrors, toProposalInput } from './proposalForm';
import type { ProposalForm } from './proposalForm';
import '@/styles/quotation.css';

function HeaderCard({
  form,
  set,
  errors,
}: Readonly<{
  form: ProposalForm;
  set: (p: Partial<ProposalForm>) => void;
  errors: Record<string, string>;
}>) {
  const saved = form.id !== undefined;
  const products = useQuery({
    queryKey: ['catalog', 'products', { activeOnly: true }],
    queryFn: () => catalogApi.products({ activeOnly: true }),
  });
  return (
    <Card title="Client and product">
      <div className="form-grid">
        <Field label="Client or prospect" required error={errors.clientId}>
          {(id) => (
            <ClientPicker
              id={id}
              value={form.clientId}
              disabled={saved}
              onChange={(c) =>
                set({
                  clientId: c?.id,
                  marketSegment: form.marketSegment || (c?.marketSegment ?? ''),
                })
              }
            />
          )}
        </Field>
        <SelectInput
          label="Product line and risk code"
          required
          blank="Select"
          disabled={saved}
          error={errors.productCode}
          value={form.productCode}
          hint="Non-package risks always go to TSU; a package risk only when a TSU rule applies."
          options={(products.data ?? []).map((p) => ({
            value: p.code,
            label: `${p.code} – ${p.name}${p.packaged ? '' : ' (non-package)'}`,
          }))}
          onChange={(productCode) => set({ productCode, items: [], groups: [] })}
        />
        <Field label="Market segment">
          {(id) => (
            <LovSelect
              id={id}
              type="MARKET_SEGMENT"
              value={form.marketSegment}
              onChange={(marketSegment) => set({ marketSegment })}
            />
          )}
        </Field>
        <TextInput
          label="Period from"
          type="date"
          value={form.periodFrom}
          onChange={(periodFrom) => set({ periodFrom })}
        />
        <TextInput
          label="Period to"
          type="date"
          error={errors.periodTo}
          value={form.periodTo}
          onChange={(periodTo) => set({ periodTo })}
        />
      </div>
    </Card>
  );
}

function ItemsCard({
  form,
  set,
}: Readonly<{ form: ProposalForm; set: (p: Partial<ProposalForm>) => void }>) {
  const detail = useQuery({
    queryKey: ['catalog', 'product', form.productCode],
    queryFn: () => catalogApi.product(form.productCode),
    enabled: form.productCode !== '',
  });
  if (form.productCode === '') {
    return null;
  }
  return (
    <Card title="Risk items">
      <RiskItemsStep
        kind={detail.data?.riskItemKind ?? 'GENERIC'}
        motor={detail.data?.ratingMethod === 'MOTOR'}
        fleet
        items={form.items}
        onChange={(items) => set({ items, groups: alignGroups(form.groups, items.length) })}
      />
    </Card>
  );
}

function useProposalSaver(setForm: (u: (f: ProposalForm) => ProposalForm) => void) {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const persist = async (f: ProposalForm): Promise<Proposal> => {
    const body = toProposalInput(f, companyId);
    const saved =
      f.id === undefined ? await proposalsApi.create(body) : await proposalsApi.update(f.id, body);
    setForm((cur) => ({ ...cur, id: saved.id, prfNo: saved.prfNo }));
    queryClient.setQueryData(['proposal', saved.id], saved);
    await queryClient.invalidateQueries({ queryKey: ['proposals'] });
    await queryClient.invalidateQueries({ queryKey: ['proposal', saved.id, 'checklist'] });
    return saved;
  };
  const save = useMutation({
    mutationFn: persist,
    onSuccess: (p) => toast.success(`${p.prfNo} saved`),
  });
  const submit = useMutation({
    mutationFn: async (f: ProposalForm) => proposalsApi.submit((await persist(f)).id),
    onSuccess: (p) => {
      toast.success(`${p.prfNo} submitted for Marketing approval`);
      void navigate(`/proposals/${p.id}`);
    },
  });
  return { save, submit };
}

function Form({ initial }: Readonly<{ initial: ProposalForm }>) {
  const [form, setForm] = useState(initial);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const set = (patch: Partial<ProposalForm>) => setForm((f) => ({ ...f, ...patch }));
  const { save, submit } = useProposalSaver(setForm);
  const run = (action: typeof save) => {
    const found = proposalErrors(form);
    setErrors(found);
    if (Object.keys(found).length === 0) {
      action.mutate(form);
    }
  };
  const saved = form.id !== undefined;
  return (
    <div className="stack">
      <PageHeader
        backTo="/proposals"
        section="Non-Package Management"
        title={saved ? `PRF ${form.prfNo ?? ''}` : 'New Proposal Request'}
        description="Complete risk details, the requested insurers and the mandatory documents, then submit the PRF for Marketing approval."
        actions={
          <>
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
      <ErrorAlert error={save.error ?? submit.error} />
      <HeaderCard form={form} set={set} errors={errors} />
      <SectionsCard sections={form.sections} onChange={(sections) => set({ sections })} />
      <ItemsCard form={form} set={set} />
      <Card title="Insurers requested">
        <InsurerChoices selected={form.insurers} onChange={(insurers) => set({ insurers })} />
      </Card>
      {form.id === undefined ? (
        <p className="muted">
          Save the draft to attach the documents and see the mandatory-document checklist.
        </p>
      ) : (
        <>
          <DocumentChecklist proposalId={form.id} />
          <Attachments
            entityType={PROPOSAL_ENTITY}
            entityId={form.id}
            title="Documents"
            reference={form.prfNo}
          />
        </>
      )}
    </div>
  );
}

/**
 * Proposal Request Form (BRNB.005/007): client, product line, risk details in free-form sections
 * and items, requested insurers and attachments with the product's mandatory-document checklist.
 * Marketing edits a draft; TSU edits the PRF while it is in the TSU queue.
 */
export default function ProposalFormPage() {
  const params = useParams();
  const [search] = useSearchParams();
  const id = params.id === undefined ? undefined : Number(params.id);
  const existing = useQuery({
    queryKey: ['proposal', id],
    queryFn: () => proposalsApi.get(id ?? 0),
    enabled: id !== undefined,
  });
  if (id === undefined) {
    const client = search.get('client');
    return <Form initial={newProposalForm(client === null ? undefined : Number(client))} />;
  }
  if (existing.data === undefined) {
    return existing.error ? (
      <ErrorAlert error={existing.error} />
    ) : (
      <span className="spinner" aria-label="Loading" />
    );
  }
  return <Form key={existing.data.id} initial={formOfProposal(existing.data)} />;
}
