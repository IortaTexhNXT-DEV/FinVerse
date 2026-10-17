import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Plus } from 'lucide-react';
import { useState } from 'react';
import { otherChargesApi } from '@/api/otherCharges';
import type { OtherCharge, OtherChargeInput } from '@/api/otherCharges';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { DataTable } from '@/components/ui/DataTable';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Modal } from '@/components/ui/Modal';
import { Notice } from '@/components/ui/Notice';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { useToast } from '@/components/ui/toastContext';
import { LineLabel } from '@/components/broking/LovLabel';
import { useCompanyId } from '@/context/workspaceContext';
import { NumberInput, SelectInput, TextInput } from '@/features/assets/FormControls';
import { formatDate, today } from '@/utils/format';
import { RecordActions } from './RecordActions';
import {
  CHARGE_BASES,
  CHARGE_VAT_TREATMENTS,
  chargeProblems,
  chargeValueLabel,
  chargeVatLabel,
} from './otherCharges';

const KEY = ['catalog', 'rates', 'other-charges'] as const;

/**
 * Other charges billed with the premium besides the taxes (template PM-04 Charges): CTPL COCAF /
 * LTO authentication, notarial, policy or documentation fees, per line or product, as an amount
 * or a rate of the net premium, with the VAT treatment and the GL account. The premium calculator
 * adds them only while the parameter Bill other charges is on.
 */
export function OtherChargesTab() {
  const { can } = useAuth();
  const [adding, setAdding] = useState(false);
  const view = useQuery({ queryKey: KEY, queryFn: otherChargesApi.list });
  return (
    <>
      <ErrorAlert error={view.error} />
      {view.data && !view.data.enabled && (
        <Notice tone="info">
          Other charges are not billed yet: the System Administrator switches on the parameter "Bill
          the other charges" once they are confirmed. The rows below apply from then on.
        </Notice>
      )}
      {can('MASTER_MAINTAIN') && (
        <div className="toolbar">
          <Button variant="accent" icon={<Plus size={16} />} onClick={() => setAdding(true)}>
            New Charge
          </Button>
        </div>
      )}
      <DataTable<OtherCharge>
        loading={view.isLoading}
        rows={view.data?.charges ?? []}
        rowKey={(r) => r.id}
        caption="Other charges"
        columns={[
          { key: 'c', header: 'Charge', render: (r) => <strong>{r.name}</strong> },
          {
            key: 'l',
            header: 'Applies to',
            render: (r) => <ChargeScope lineCode={r.lineCode} productCode={r.productCode} />,
          },
          { key: 'v', header: 'Amount or Rate', render: (r) => chargeValueLabel(r.basis, r.value) },
          { key: 'vat', header: 'VAT', render: (r) => chargeVatLabel(r.vatTreatment) },
          { key: 'gl', header: 'GL Account', render: (r) => r.glAccountCode },
          { key: 'f', header: 'From', render: (r) => formatDate(r.effectiveFrom) },
          { key: 't', header: 'To', render: (r) => formatDate(r.effectiveTo) || 'Open' },
          { key: 's', header: 'Status', render: (r) => <StatusBadge status={r.recordStatus} /> },
          {
            key: 'x',
            header: <span className="visually-hidden">Actions</span>,
            width: '64px',
            render: (r) => (
              <RecordActions
                kind="OTHER_CHARGE"
                record={r}
                label={`${r.name} from ${formatDate(r.effectiveFrom)}`}
                refresh={[KEY]}
              />
            ),
          },
        ]}
      />
      {adding && <NewChargeModal onClose={() => setAdding(false)} />}
    </>
  );
}

/** What a charge applies to: a product, a line or every product. */
function ChargeScope({
  lineCode,
  productCode,
}: Readonly<{ lineCode?: string; productCode?: string }>) {
  if (productCode) {
    return <>{productCode}</>;
  }
  return lineCode ? <LineLabel code={lineCode} /> : <>All products</>;
}

function blankToUndefined(value: string | undefined): string | undefined {
  return value?.trim() ? value.trim() : undefined;
}

function NewChargeModal({ onClose }: Readonly<{ onClose: () => void }>) {
  const companyId = useCompanyId();
  const toast = useToast();
  const queryClient = useQueryClient();
  const [form, setForm] = useState<Partial<OtherChargeInput>>({
    basis: 'AMOUNT',
    vatTreatment: 'VATABLE',
    effectiveFrom: today(),
  });
  const [shown, setShown] = useState(false);
  const problems = chargeProblems(form);
  const set = (patch: Partial<OtherChargeInput>) => setForm((f) => ({ ...f, ...patch }));
  const save = useMutation({
    mutationFn: () =>
      otherChargesApi.create({
        ...(form as OtherChargeInput),
        companyId,
        lineCode: blankToUndefined(form.lineCode),
        productCode: blankToUndefined(form.productCode),
        effectiveTo: blankToUndefined(form.effectiveTo),
      }),
    onSuccess: async (c) => {
      await queryClient.invalidateQueries({ queryKey: KEY });
      toast.success(`${c.name} saved – pending authorization`);
      onClose();
    },
  });
  const error = (field: string) => (shown ? problems[field] : undefined);
  return (
    <Modal
      title="New other charge"
      open
      onClose={onClose}
      footer={
        <Button
          variant="accent"
          busy={save.isPending}
          onClick={() => {
            setShown(true);
            if (Object.keys(problems).length === 0) {
              save.mutate();
            }
          }}
        >
          Save for Authorization
        </Button>
      }
    >
      <ErrorAlert error={save.error} />
      <div className="form-grid">
        <TextInput
          label="Charge code"
          required
          upper
          hint="e.g. DOC_FEE"
          error={error('chargeCode')}
          value={form.chargeCode}
          onChange={(chargeCode) => set({ chargeCode })}
        />
        <TextInput
          label="Name"
          required
          error={error('name')}
          value={form.name}
          onChange={(name) => set({ name })}
        />
        <TextInput
          label="Product line"
          hint="Blank for every line"
          upper
          value={form.lineCode}
          onChange={(lineCode) => set({ lineCode })}
        />
        <TextInput
          label="Product"
          hint="Blank for every product of the line"
          upper
          value={form.productCode}
          onChange={(productCode) => set({ productCode })}
        />
        <SelectInput
          label="Basis"
          required
          value={form.basis}
          options={CHARGE_BASES}
          onChange={(v) => set({ basis: v as OtherChargeInput['basis'] })}
        />
        <NumberInput
          label={form.basis === 'RATE' ? 'Rate %' : 'Amount'}
          required
          error={error('value')}
          value={form.value}
          onChange={(value) => set({ value })}
        />
        <SelectInput
          label="VAT treatment"
          required
          value={form.vatTreatment}
          options={CHARGE_VAT_TREATMENTS}
          onChange={(v) => set({ vatTreatment: v as OtherChargeInput['vatTreatment'] })}
        />
        <TextInput
          label="GL account"
          required
          error={error('glAccountCode')}
          value={form.glAccountCode}
          onChange={(glAccountCode) => set({ glAccountCode })}
        />
        <TextInput
          label="Effective from"
          type="date"
          required
          error={error('effectiveFrom')}
          value={form.effectiveFrom}
          onChange={(effectiveFrom) => set({ effectiveFrom })}
        />
        <TextInput
          label="Effective to"
          type="date"
          error={error('effectiveTo')}
          value={form.effectiveTo}
          onChange={(effectiveTo) => set({ effectiveTo })}
        />
      </div>
    </Modal>
  );
}
