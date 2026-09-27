import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Calculator, Save } from 'lucide-react';
import { useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { underwritingApi } from '@/api/underwriting';
import type { PolicyInput } from '@/api/underwriting';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useDefaultBranchId, useWorkspace } from '@/context/workspaceContext';
import { today } from '@/utils/format';
import { PolicyHeaderFields } from './PolicyHeaderFields';
import { fromPolicy, newPolicy, normalize, validatePolicy } from './policyForm';
import { PremiumSummary } from './PremiumSummary';
import { RiskEditor } from './RiskEditor';
import { useUwLookups } from './useUwLookups';

interface EditorProps {
  id: number | undefined;
  title: string;
  initial: PolicyInput;
}

function PolicyEditor({ id, title, initial }: Readonly<EditorProps>) {
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const lookups = useUwLookups();
  const [form, setForm] = useState<PolicyInput>(initial);
  const [errors, setErrors] = useState<string[]>([]);

  const preview = useMutation({ mutationFn: (f: PolicyInput) => underwritingApi.preview(f) });
  const save = useMutation({
    mutationFn: (f: PolicyInput) =>
      id === undefined ? underwritingApi.createPolicy(f) : underwritingApi.updatePolicy(id, f),
    onSuccess: async (policy) => {
      await queryClient.invalidateQueries({ queryKey: ['policies'] });
      await queryClient.invalidateQueries({ queryKey: ['policy', policy.id] });
      toast.success(`${policy.policyNo} saved as draft`);
      await navigate(`/underwriting/policies/${String(policy.id)}`);
    },
  });

  const product = lookups.products.find((p) => p.id === form.productId);
  const run = (action: (f: PolicyInput) => void) => {
    const problems = validatePolicy(form);
    setErrors(problems);
    if (problems.length === 0) {
      action(normalize(form));
    }
  };

  return (
    <div className="stack">
      <PageHeader
        section="Underwriting · Policies"
        title={title}
        description="Enter the terms and risks, preview the premium, then save the draft and submit it for approval."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<Calculator size={16} />}
              busy={preview.isPending}
              onClick={() => run((f) => preview.mutate(f))}
            >
              Preview Premium
            </Button>
            <Button
              variant="accent"
              icon={<Save size={16} />}
              busy={save.isPending}
              onClick={() => run((f) => save.mutate(f))}
            >
              Save Draft
            </Button>
          </>
        }
      />
      {errors.length > 0 && (
        <div className="alert danger" role="alert">
          {errors.map((e) => (
            <div key={e}>{e}</div>
          ))}
        </div>
      )}
      <ErrorAlert error={save.error ?? preview.error} />
      <Card title="Policy terms">
        <PolicyHeaderFields
          form={form}
          editingExisting={id !== undefined}
          onChange={(patch) => setForm({ ...form, ...patch })}
        />
      </Card>
      <Card title="Risks">
        <RiskEditor
          risks={form.risks}
          marine={product?.businessLine === 'MARINE'}
          onChange={(risks) => setForm({ ...form, risks })}
        />
      </Card>
      {preview.data !== undefined && (
        <Card title="Premium preview" flush>
          <PremiumSummary premium={preview.data} currency={form.currency} />
        </Card>
      )}
    </div>
  );
}

/** Create or edit a draft policy with its risks; live premium preview from the server. */
export default function PolicyFormPage() {
  const params = useParams();
  const id = params.id === undefined ? undefined : Number(params.id);
  const { company, branches } = useWorkspace();
  const defaultBranch = useDefaultBranchId();
  const existing = useQuery({
    queryKey: ['policy', id],
    queryFn: () => underwritingApi.policy(id ?? 0),
    enabled: id !== undefined,
  });

  if (id !== undefined) {
    if (existing.data === undefined) {
      return existing.error ? (
        <ErrorAlert error={existing.error} />
      ) : (
        <span className="spinner" aria-label="Loading" />
      );
    }
    return (
      <PolicyEditor
        key={existing.data.id}
        id={id}
        title={`Edit ${existing.data.policyNo}`}
        initial={fromPolicy(existing.data)}
      />
    );
  }
  if (company === undefined || branches.length === 0) {
    return <span className="spinner" aria-label="Loading" />;
  }
  return (
    <PolicyEditor
      id={undefined}
      title="New policy"
      initial={newPolicy(company.id, defaultBranch, today())}
    />
  );
}
