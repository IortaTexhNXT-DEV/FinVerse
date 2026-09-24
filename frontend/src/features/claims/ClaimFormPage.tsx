import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Save, Search } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { claimsApi } from '@/api/claims';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { DateField, TextField } from '@/features/underwriting/FormFields';
import { today } from '@/utils/format';
import { newClaimForm, toClaimInput, validateClaim } from './claimForm';
import type { ClaimForm } from './claimForm';
import { PolicyCoverCard } from './PolicyCoverCard';
import { LossDetailsCard, PartiesCard } from './ClaimFormSections';
import { useClaimLookups } from './useClaimLookups';

interface Lookup {
  policyNo: string;
  lossDate: string;
}

/**
 * First notification of loss: look the policy up and check its cover at the date of loss, record
 * the loss details and involved parties, and optionally submit initial loss / expense reserves for
 * approval.
 */
export default function ClaimFormPage() {
  const lookups = useClaimLookups();
  const navigate = useNavigate();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<ClaimForm>(() => newClaimForm(today()));
  const [lookup, setLookup] = useState<Lookup | null>(null);
  const [showErrors, setShowErrors] = useState(false);
  const set = (patch: Partial<ClaimForm>) => setForm((f) => ({ ...f, ...patch }));

  const cover = useQuery({
    queryKey: ['policy-cover', lookups.companyId, lookup],
    queryFn: () =>
      claimsApi.policyCover(lookups.companyId, lookup?.policyNo ?? '', lookup?.lossDate),
    enabled: lookup !== null && lookups.companyId > 0,
    retry: false,
  });
  const found = lookup?.policyNo === form.policyNo && lookup.lossDate === form.lossDate;
  const policy = found ? cover.data : undefined;
  const errors = validateClaim(form, policy, today());

  const register = useMutation({
    mutationFn: () => {
      if (policy === undefined) {
        throw new Error('Look up the policy first');
      }
      return claimsApi.register(toClaimInput(form, lookups.companyId, policy));
    },
    onSuccess: async (claim) => {
      toast.success(`Claim ${claim.claimNo} registered`);
      await queryClient.invalidateQueries({ queryKey: ['claims'] });
      void navigate(`/claims/${String(claim.id)}`);
    },
  });
  const submit = () => {
    setShowErrors(true);
    if (errors.length === 0) {
      register.mutate();
    }
  };

  return (
    <div className="stack">
      <PageHeader
        section="Claims"
        title="Notify claim"
        description="First notification of loss against a policy in force at the date of loss."
        actions={
          <Button
            variant="accent"
            icon={<Save size={16} />}
            busy={register.isPending}
            onClick={submit}
          >
            Register Claim
          </Button>
        }
      />
      <Card title="Policy and date of loss">
        <div className="form-grid">
          <TextField
            label="Policy no."
            required
            value={form.policyNo}
            onChange={(v) => set({ policyNo: v.trim() })}
          />
          <DateField
            label="Date of loss"
            required
            value={form.lossDate}
            onChange={(v) => set({ lossDate: v })}
          />
          <div className="field" style={{ alignSelf: 'end' }}>
            <Button
              variant="secondary"
              icon={<Search size={16} />}
              disabled={form.policyNo === '' || form.lossDate === ''}
              busy={cover.isFetching}
              onClick={() => setLookup({ policyNo: form.policyNo, lossDate: form.lossDate })}
            >
              Check Cover
            </Button>
          </div>
        </div>
        {found && <ErrorAlert error={cover.error} />}
      </Card>
      {policy !== undefined && <PolicyCoverCard cover={policy} />}
      <LossDetailsCard form={form} policy={policy} onChange={set} />
      <PartiesCard form={form} lookups={lookups} onChange={set} />
      {showErrors && errors.length > 0 && (
        <div className="alert warning" role="alert">
          {errors.join(' · ')}
        </div>
      )}
      <ErrorAlert error={register.error} />
    </div>
  );
}
