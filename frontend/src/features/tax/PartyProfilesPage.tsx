import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { taxApi } from '@/api/tax';
import type { PartyTaxProfile, PartyTaxProfileRequest, PayeeClass, VatTreatment } from '@/api/tax';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { useCompanyId } from '@/context/workspaceContext';
import { AuthorizeButton, CheckField, SelectField, TextField } from './MasterControls';
import { exemptionLabel, withholdingLabel } from './taxDisplay';
import { formatDate } from '@/utils/format';
import { awaitsOtherChecker } from '@/utils/makerChecker';
import { ConfigUploadButton } from '@/features/configpromo/ConfigUploadButton';

const PAYEE_CLASSES: readonly PayeeClass[] = ['CORPORATE', 'INDIVIDUAL'];
const VAT_TREATMENTS: readonly VatTreatment[] = ['REGULAR', 'ZERO_RATED', 'EXEMPT'];

type Form = Partial<PartyTaxProfileRequest> & { id?: number };

/**
 * Tax profiles of business partners: TIN and branch code, registered name (name parts for
 * individuals), VAT treatment, the default ATC of their income payments, the withholding status
 * (withholding agent, top withholding agent, government payor) and the tax exemption certificate.
 */
export default function PartyProfilesPage() {
  const companyId = useCompanyId();
  const { can, user } = useAuth();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Form | null>(null);
  const profiles = useQuery({
    queryKey: ['tax-profiles', companyId],
    queryFn: () => taxApi.profiles(companyId),
    enabled: companyId > 0,
  });
  const codes = useQuery({
    queryKey: ['tax-codes', companyId],
    queryFn: () => taxApi.codes(companyId),
    enabled: companyId > 0,
  });
  const atcs = (codes.data ?? []).filter((c) => c.taxType === 'EWT').map((c) => c.code);
  const refresh = () => queryClient.invalidateQueries({ queryKey: ['tax-profiles'] });
  const save = useMutation({
    mutationFn: (f: Form) => {
      const body = { ...f, companyId } as PartyTaxProfileRequest;
      return f.id === undefined ? taxApi.createProfile(body) : taxApi.updateProfile(f.id, body);
    },
    onSuccess: async (p) => {
      await refresh();
      setForm(null);
      toast.success(`Tax profile of ${p.partyCode} saved – pending authorization`);
    },
  });
  const authorize = useMutation({
    mutationFn: taxApi.authorizeProfile,
    onSuccess: async (p) => {
      await refresh();
      toast.success(`Tax profile of ${p.partyCode} authorized`);
    },
  });
  const set = (patch: Partial<Form>) => setForm((f) => ({ ...f, ...patch }));

  return (
    <div className="stack">
      <PageHeader
        section="Tax & Statutory"
        title="Party Tax Profiles"
        description="TIN, registered name, VAT treatment, default ATC, withholding status and tax exemption certificate of suppliers, agents, brokers and customers."
        actions={
          <>
            <ConfigUploadButton types={['CFG_PARTY_TAX_PROFILE']} />
            {can('TAX_MANAGE') && (
              <Button
                variant="accent"
                icon={<Plus size={16} />}
                onClick={() => setForm({ payeeClass: 'CORPORATE', vatTreatment: 'REGULAR' })}
              >
                New Profile
              </Button>
            )}
          </>
        }
      />
      <ErrorAlert error={profiles.error ?? authorize.error} />
      <Card flush>
        <DataTable<PartyTaxProfile>
          rows={profiles.data ?? []}
          loading={profiles.isLoading}
          rowKey={(p) => p.id}
          caption="Party tax profiles"
          onRowClick={can('TAX_MANAGE') ? (p) => setForm(p) : undefined}
          columns={[
            { key: 'c', header: 'Party', render: (p) => <strong>{p.partyCode}</strong> },
            { key: 'n', header: 'Registered Name', render: (p) => p.registeredName },
            { key: 't', header: 'TIN', render: (p) => `${p.tin}-${p.branchCode}` },
            { key: 'p', header: 'Payee', render: (p) => p.payeeClass },
            { key: 'v', header: 'VAT', render: (p) => p.vatTreatment },
            { key: 'a', header: 'Default ATC', render: (p) => p.defaultAtcCode ?? '' },
            { key: 'w', header: 'Withholding', render: (p) => withholdingLabel(p) },
            {
              key: 'e',
              header: 'Exemption Certificate',
              render: (p) => exemptionLabel(p, formatDate),
            },
            { key: 's', header: 'Status', render: (p) => <StatusBadge status={p.recordStatus} /> },
            {
              key: 'x',
              header: 'Actions',
              render: (p) =>
                awaitsOtherChecker(p, user?.username) && can('MASTER_AUTHORIZE') ? (
                  <AuthorizeButton
                    busy={authorize.isPending && authorize.variables === p.id}
                    onClick={() => authorize.mutate(p.id)}
                  />
                ) : null,
            },
          ]}
        />
      </Card>
      <Modal
        title={form?.id === undefined ? 'New tax profile' : `Edit ${form.partyCode ?? ''}`}
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
            <TextField
              label="Party code"
              required
              disabled={form.id !== undefined}
              value={form.partyCode}
              onChange={(v) => set({ partyCode: v })}
            />
            <TextField
              label="TIN"
              required
              hint="123-456-789-000"
              value={form.tin}
              onChange={(v) => set({ tin: v })}
            />
            <TextField
              label="Branch code"
              value={form.branchCode}
              onChange={(v) => set({ branchCode: v })}
            />
            <SelectField
              label="Payee class"
              required
              value={form.payeeClass}
              options={PAYEE_CLASSES}
              onChange={(v) => set({ payeeClass: v })}
            />
            <TextField
              label="Registered name"
              required
              value={form.registeredName}
              onChange={(v) => set({ registeredName: v })}
            />
            {form.payeeClass === 'INDIVIDUAL' && (
              <>
                <TextField
                  label="Last name"
                  required
                  value={form.lastName}
                  onChange={(v) => set({ lastName: v })}
                />
                <TextField
                  label="First name"
                  required
                  value={form.firstName}
                  onChange={(v) => set({ firstName: v })}
                />
                <TextField
                  label="Middle name"
                  value={form.middleName}
                  onChange={(v) => set({ middleName: v })}
                />
              </>
            )}
            <TextField
              label="Registered address"
              value={form.registeredAddress}
              onChange={(v) => set({ registeredAddress: v })}
            />
            <TextField
              label="ZIP code"
              value={form.zipCode}
              onChange={(v) => set({ zipCode: v })}
            />
            <SelectField
              label="VAT treatment"
              value={form.vatTreatment}
              options={VAT_TREATMENTS}
              onChange={(v) => set({ vatTreatment: v })}
            />
            <SelectField
              label="Default ATC"
              value={form.defaultAtcCode}
              options={atcs}
              allowEmpty
              onChange={(v) => set({ defaultAtcCode: v })}
            />
            <CheckField
              label="Withholding agent"
              checked={form.withholdingAgent}
              onChange={(v) => set({ withholdingAgent: v })}
            />
            <CheckField
              label="Top withholding agent"
              checked={form.topWithholdingAgent}
              onChange={(v) => set({ topWithholdingAgent: v })}
            />
            <CheckField
              label="Government payor (final VAT and final tax withheld)"
              checked={form.governmentPayor}
              onChange={(v) => set({ governmentPayor: v })}
            />
            <TextField
              label="Tax exemption certificate no."
              value={form.exemptionCertificateNo}
              onChange={(v) => set({ exemptionCertificateNo: v })}
            />
            <TextField
              label="Certificate valid from"
              type="date"
              value={form.exemptionValidFrom}
              onChange={(v) => set({ exemptionValidFrom: v })}
            />
            <TextField
              label="Certificate valid to"
              type="date"
              value={form.exemptionValidTo}
              onChange={(v) => set({ exemptionValidTo: v })}
            />
          </div>
        )}
      </Modal>
    </div>
  );
}
