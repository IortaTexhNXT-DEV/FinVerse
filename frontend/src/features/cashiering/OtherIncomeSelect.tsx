import { useQuery } from '@tanstack/react-query';
import { api, toQuery } from '@/api/client';
import { useCompanyId } from '@/context/workspaceContext';
import { formatAmount, formatDate } from '@/utils/format';
import { CodeSelect, TextField } from './CashFields';
import type { RecordForm } from './recordLogic';
import type { OtherIncomeItem } from './otherIncomeLogic';
import { otherIncomeParty } from './otherIncomeLogic';

/**
 * The SOA or fee invoice of an OR of other income: the Valid for Collection SOAs of the insurer for
 * an incentive, the open fee invoices of the client for a fee; choosing one fills the paid amount
 * with its outstanding amount. Other OR types take a free reference.
 */
export function OtherIncomeSelect({
  form,
  update,
}: Readonly<{ form: RecordForm; update: (change: (f: RecordForm) => RecordForm) => void }>) {
  const companyId = useCompanyId();
  const party = otherIncomeParty(form);
  const items = useQuery({
    queryKey: ['cashiering', 'other-income', companyId, form.receiptType, party],
    queryFn: () =>
      api.get<OtherIncomeItem[]>(
        `/cashiering/other-income-items${toQuery({ companyId, orType: form.receiptType, party })}`,
      ),
    enabled: companyId > 0 && party !== '',
  });
  if (party === '') {
    return (
      <TextField
        label="SOA or Fee Invoice"
        value={form.otherIncomeRef}
        onChange={(v) => update((f) => ({ ...f, otherIncomeRef: v }))}
      />
    );
  }
  const open = items.data ?? [];
  return (
    <CodeSelect
      label={form.receiptType === 'INCENTIVE' ? 'Valid for Collection SOA' : 'Fee Policy Invoice'}
      value={form.otherIncomeRef}
      options={open.map((i) => i.reference)}
      empty={open.length === 0 ? 'No open item' : 'Select…'}
      labelOf={(ref) => {
        const i = open.find((x) => x.reference === ref);
        return i
          ? `${i.reference} · ${i.description} · ${formatDate(i.date)} · ${formatAmount(i.outstanding)}`
          : ref;
      }}
      onChange={(ref) => {
        const chosen = open.find((x) => x.reference === ref);
        update((f) => ({
          ...f,
          otherIncomeRef: ref,
          amount: chosen ? String(chosen.outstanding) : f.amount,
        }));
      }}
    />
  );
}
