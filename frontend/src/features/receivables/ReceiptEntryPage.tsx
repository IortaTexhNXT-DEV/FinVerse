import { useMutation, useQuery } from '@tanstack/react-query';
import { Save } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { PAYER_PARTY_TYPES, receivablesApi } from '@/api/receivables';
import type { OpenItem } from '@/api/receivables';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { AllocationTable } from './AllocationTable';
import { ReceiptFormFields } from './ReceiptFormFields';
import { allocationErrors, allocationTotal } from './receivablesMath';
import type { ReceiptForm } from './receiptForm';
import {
  bankCurrency,
  canSave,
  emptyReceiptForm,
  hasParty,
  showsAllocations,
  toPdcInput,
  toReceiptInput,
} from './receiptForm';
import { useReceivablesLookups } from './useReceivablesLookups';

interface AllocationCardProps {
  pdc: boolean;
  errors: string[];
  items: OpenItem[];
  loading: boolean;
  amount: number;
  allocations: Record<number, number>;
  onChange: (allocations: Record<number, number>) => void;
}

function AllocationCard(props: Readonly<AllocationCardProps>) {
  const { pdc, errors, allocations } = props;
  return (
    <Card
      title={pdc ? 'Debit note covered by the cheque' : 'Apply to debit notes'}
      actions={<span className="muted">Total {allocationTotal(allocations).toFixed(2)}</span>}
    >
      {errors.length > 0 && (
        <div className="alert danger" role="alert">
          {errors.join('; ')}
        </div>
      )}
      <AllocationTable
        items={props.items}
        amount={props.amount}
        allocations={allocations}
        onChange={props.onChange}
        loading={props.loading}
      />
    </Card>
  );
}

/** Fields whose change invalidates the selected allocations. */
const RESET_ALLOCATIONS: (keyof ReceiptForm)[] = ['payerType', 'partyCode', 'bankAccountCode'];

/**
 * Entry of an official receipt with allocation to the payer's open debit notes (manual or FIFO);
 * a post-dated cheque is registered in the PDC register instead and banked at maturity.
 */
export default function ReceiptEntryPage() {
  const navigate = useNavigate();
  const toast = useToast();
  const { companyId, branchId, bankAccounts } = useReceivablesLookups();
  const [form, setForm] = useState<ReceiptForm>(emptyReceiptForm);
  const [allocations, setAllocations] = useState<Record<number, number>>({});
  const set = (patch: Partial<ReceiptForm>) => {
    setForm((f) => ({ ...f, ...patch }));
    if (RESET_ALLOCATIONS.some((k) => k in patch)) {
      setAllocations({});
    }
  };
  const currency = bankCurrency(bankAccounts, form.bankAccountCode);
  const ctx = { companyId, branchId, currency };
  const pdc = form.mode === 'PDC';
  const showAllocations = showsAllocations(form);

  const parties = useQuery({
    queryKey: ['parties', companyId, form.payerType],
    queryFn: () => receivablesApi.parties(companyId, PAYER_PARTY_TYPES[form.payerType]),
    enabled: companyId > 0 && hasParty(form),
  });
  const items = useQuery({
    queryKey: ['open-items', companyId, form.partyCode, currency],
    queryFn: () => receivablesApi.openItems(companyId, form.partyCode, currency),
    enabled: showAllocations,
  });
  const errors =
    form.method === 'MANUAL' ? allocationErrors(items.data ?? [], allocations, form.amount) : [];

  const save = useMutation({
    mutationFn: async () => {
      if (pdc) {
        const registered = await receivablesApi.registerPdc(toPdcInput(form, ctx, allocations));
        return { path: '/receivables/pdcs', label: registered.pdcNo };
      }
      const receipt = await receivablesApi.createReceipt(toReceiptInput(form, ctx, allocations));
      return {
        path: `/receivables/receipts/${receipt.summary.id}`,
        label: receipt.summary.receiptNo,
      };
    },
    onSuccess: async (result) => {
      toast.success(`${result.label} saved`);
      await navigate(result.path);
    },
  });

  return (
    <div className="stack">
      <PageHeader
        section="Receivables & Banking"
        title="New Official Receipt"
        description="Record money received and apply it to the payer's debit notes; the rest stays on account."
        actions={
          <Button
            variant="accent"
            icon={<Save size={16} />}
            busy={save.isPending}
            disabled={!canSave(form, errors.length)}
            onClick={() => save.mutate()}
          >
            {pdc ? 'Register PDC' : 'Save for approval'}
          </Button>
        }
      />
      <ErrorAlert error={save.error} />
      <Card title="Receipt">
        <ReceiptFormFields
          form={form}
          set={set}
          parties={parties.data ?? []}
          bankAccounts={bankAccounts}
          currency={currency}
        />
      </Card>
      {showAllocations && (
        <AllocationCard
          pdc={pdc}
          errors={errors}
          items={items.data ?? []}
          loading={items.isLoading}
          amount={form.amount}
          allocations={allocations}
          onChange={setAllocations}
        />
      )}
    </div>
  );
}
