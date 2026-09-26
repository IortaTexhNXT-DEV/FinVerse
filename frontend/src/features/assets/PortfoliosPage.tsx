import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { investmentsApi } from '@/api/investments';
import type { Classification, Portfolio, PortfolioInput } from '@/api/investments';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { humanize } from '@/utils/format';
import { SelectInput, TextInput } from './FormControls';
import { enumOptions } from './options';
import { useAssetLookups } from './useAssetLookups';
import { awaitsOtherChecker } from '@/utils/makerChecker';

type PortfolioForm = Partial<PortfolioInput> & { id?: number };

const CLASSIFICATIONS = enumOptions(['AMORTIZED_COST', 'FVOCI', 'FVPL']);
const ACCOUNT_FIELDS: { key: keyof PortfolioInput; label: string }[] = [
  { key: 'investmentAccount', label: 'Investment account' },
  { key: 'accruedInterestAccount', label: 'Accrued interest account' },
  { key: 'interestIncomeAccount', label: 'Interest / dividend income account' },
  { key: 'realizedGainAccount', label: 'Realized gain / loss account' },
];

/** Investment portfolios: PFRS 9 classification and GL accounts (maker-checker). */
export default function PortfoliosPage() {
  const { companyId, portfolios, postableAccounts } = useAssetLookups();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<PortfolioForm | null>(null);
  const accounts = postableAccounts.map((a) => ({ value: a.code, label: `${a.code} – ${a.name}` }));
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['investment-portfolios'] });
  const save = useMutation({
    mutationFn: (f: PortfolioForm) => {
      const body = { ...f, companyId } as PortfolioInput;
      return f.id === undefined
        ? investmentsApi.createPortfolio(body)
        : investmentsApi.updatePortfolio(f.id, body);
    },
    onSuccess: async (p) => {
      await refresh();
      setForm(null);
      toast.success(`Portfolio ${p.code} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: (id: number) => investmentsApi.authorizePortfolio(id),
    onSuccess: async (p) => {
      await refresh();
      toast.success(`Portfolio ${p.code} authorized`);
    },
  });
  const set = (patch: PortfolioForm) => form && setForm({ ...form, ...patch });
  const fairValued = form?.classification !== undefined && form.classification !== 'AMORTIZED_COST';

  return (
    <div className="stack">
      <PageHeader
        section="Assets & Investments"
        title="Investment Portfolios"
        description="Each portfolio fixes the PFRS 9 classification and the GL accounts its holdings post to. FVOCI changes go to the equity reserve, FVPL changes to profit or loss."
        actions={
          can('MASTER_MAINTAIN') && (
            <Button
              variant="accent"
              icon={<Plus size={16} />}
              onClick={() => setForm({ classification: 'AMORTIZED_COST' })}
            >
              New Portfolio
            </Button>
          )
        }
      />
      <ErrorAlert error={authorize.error} />
      <Card flush>
        <DataTable<Portfolio>
          rows={portfolios}
          rowKey={(p) => p.id}
          onRowClick={can('MASTER_MAINTAIN') ? (p) => setForm(p) : undefined}
          columns={[
            { key: 'c', header: 'Code', render: (p) => <strong>{p.code}</strong> },
            { key: 'n', header: 'Name', render: (p) => p.name },
            { key: 'k', header: 'Classification', render: (p) => humanize(p.classification) },
            { key: 'i', header: 'Investment', render: (p) => p.investmentAccount },
            { key: 'a', header: 'Accrued Int.', render: (p) => p.accruedInterestAccount },
            { key: 'y', header: 'Income', render: (p) => p.interestIncomeAccount },
            { key: 'g', header: 'Realized', render: (p) => p.realizedGainAccount },
            { key: 'f', header: 'Fair Value', render: (p) => p.fairValueAccount ?? '' },
            { key: 's', header: 'Status', render: (p) => <StatusBadge status={p.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (p) =>
                awaitsOtherChecker(p, user?.username) &&
                can('MASTER_AUTHORIZE') && (
                  <Button
                    size="sm"
                    variant="secondary"
                    onClick={(e) => {
                      e.stopPropagation();
                      authorize.mutate(p.id);
                    }}
                  >
                    Authorize
                  </Button>
                ),
            },
          ]}
        />
      </Card>
      <Modal
        title={form?.id === undefined ? 'New portfolio' : `Edit ${form.code ?? ''}`}
        open={form !== null}
        onClose={() => setForm(null)}
        footer={
          <Button variant="accent" busy={save.isPending} onClick={() => form && save.mutate(form)}>
            Save for Authorization
          </Button>
        }
      >
        <ErrorAlert error={save.error} />
        {form !== null && (
          <div className="form-grid">
            <TextInput
              label="Code"
              required
              upper
              disabled={form.id !== undefined}
              value={form.code}
              onChange={(code) => set({ code })}
            />
            <TextInput label="Name" required value={form.name} onChange={(name) => set({ name })} />
            <SelectInput
              label="Classification"
              required
              disabled={form.id !== undefined}
              value={form.classification}
              options={CLASSIFICATIONS}
              onChange={(v) => set({ classification: v as Classification })}
            />
            {ACCOUNT_FIELDS.map((f) => (
              <SelectInput
                key={f.key}
                label={f.label}
                required
                blank="Select"
                value={form[f.key] as string | undefined}
                options={accounts}
                onChange={(v) => set({ [f.key]: v })}
              />
            ))}
            {fairValued && (
              <SelectInput
                label={
                  form.classification === 'FVOCI' ? 'FVOCI reserve (equity)' : 'FVPL gain / loss'
                }
                required
                blank="Select"
                value={form.fairValueAccount}
                options={accounts}
                onChange={(fairValueAccount) => set({ fairValueAccount })}
              />
            )}
          </div>
        )}
      </Modal>
    </div>
  );
}
