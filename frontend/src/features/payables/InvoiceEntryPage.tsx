import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Trash2 } from 'lucide-react';
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { payablesApi } from '@/api/payables';
import type { InvoiceLine } from '@/api/payables';
import { Amount } from '@/components/ui/Amount';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Field } from '@/components/ui/Field';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { useToast } from '@/components/ui/toastContext';
import { useWorkspace } from '@/context/workspaceContext';
import { useGlLookups } from '@/features/gl/useLookups';
import { formatAmount, today } from '@/utils/format';
import { dueDate, invoiceTotals, lineTaxes } from './payablesMath';
import { usePayablesLookups } from './usePayablesLookups';

const emptyLine = (): InvoiceLine => ({
  expenseAccountCode: '',
  costCenter: '',
  description: '',
  netAmount: 0,
});

/** Captures a supplier invoice with expense lines; VAT, EWT and the payable are previewed live. */
export default function InvoiceEntryPage() {
  const { branches, branchId } = useWorkspace();
  const { companyId, parties } = usePayablesLookups();
  const { postableAccounts, costCenters } = useGlLookups();
  const toast = useToast();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [partyCode, setPartyCode] = useState('');
  const [supplierNo, setSupplierNo] = useState('');
  const [invoiceDate, setInvoiceDate] = useState(today());
  const [due, setDue] = useState('');
  const [vat, setVat] = useState(true);
  const [narration, setNarration] = useState('');
  const [branch, setBranch] = useState<number | undefined>(branchId);
  const [lines, setLines] = useState<InvoiceLine[]>([emptyLine()]);
  const party = parties.find((p) => p.code === partyCode);
  const whtRate = party?.withholdingTaxRate ?? 0;
  const totals = invoiceTotals(lines, vat, whtRate);
  const effectiveBranch = branch ?? branches.find((b) => b.headOffice)?.id ?? branches[0]?.id;

  const save = useMutation({
    mutationFn: async (andSubmit: boolean) => {
      const created = await payablesApi.createInvoice({
        companyId,
        branchId: effectiveBranch ?? 0,
        partyCode,
        supplierInvoiceNo: supplierNo,
        invoiceDate,
        dueDate: due === '' ? undefined : due,
        vatApplicable: vat,
        narration,
        lines: lines.map((l) => ({
          ...l,
          costCenter: l.costCenter === '' ? undefined : l.costCenter,
        })),
      });
      return andSubmit ? payablesApi.submitInvoice(created.id) : created;
    },
    onSuccess: async (inv) => {
      await queryClient.invalidateQueries({ queryKey: ['invoices'] });
      toast.success(`${inv.documentNo} saved (${inv.status.replace('_', ' ').toLowerCase()})`);
      void navigate('/payables/invoices');
    },
  });
  const setLine = (index: number, patch: Partial<InvoiceLine>) =>
    setLines(lines.map((l, i) => (i === index ? { ...l, ...patch } : l)));
  const ready =
    partyCode !== '' &&
    supplierNo !== '' &&
    totals.payable > 0 &&
    lines.every((l) => l.expenseAccountCode !== '' && l.description !== '');

  return (
    <div className="stack">
      <PageHeader
        section="Payables & Cash"
        title="New Supplier Invoice"
        description="Amounts are entered net of VAT. Input VAT 12 % and the supplier's expanded withholding tax are computed per line."
      />
      <ErrorAlert error={save.error} />
      <Card title="Invoice">
        <div className="form-grid">
          <Field label="Supplier" required>
            {(id) => (
              <select
                id={id}
                className="select"
                value={partyCode}
                onChange={(e) => setPartyCode(e.target.value)}
              >
                <option value="">Select…</option>
                {parties.map((p) => (
                  <option key={p.code} value={p.code}>
                    {p.code} – {p.name}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="Supplier invoice no." required>
            {(id) => (
              <input
                id={id}
                className="input"
                value={supplierNo}
                onChange={(e) => setSupplierNo(e.target.value)}
              />
            )}
          </Field>
          <Field label="Invoice date" required>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={invoiceDate}
                onChange={(e) => setInvoiceDate(e.target.value)}
              />
            )}
          </Field>
          <Field label="Due date" hint={`Default ${dueDate(invoiceDate, party?.creditDays ?? 0)}`}>
            {(id) => (
              <input
                id={id}
                className="input"
                type="date"
                value={due}
                onChange={(e) => setDue(e.target.value)}
              />
            )}
          </Field>
          <Field label="Branch">
            {(id) => (
              <select
                id={id}
                className="select"
                value={effectiveBranch ?? ''}
                onChange={(e) => setBranch(Number(e.target.value))}
              >
                {branches.map((b) => (
                  <option key={b.id} value={b.id}>
                    {b.code} – {b.name}
                  </option>
                ))}
              </select>
            )}
          </Field>
          <Field label="VAT registered supplier (12 % input VAT)">
            {(id) => (
              <input
                id={id}
                type="checkbox"
                checked={vat}
                onChange={(e) => setVat(e.target.checked)}
              />
            )}
          </Field>
          <Field label="Narration">
            {(id) => (
              <input
                id={id}
                className="input"
                value={narration}
                onChange={(e) => setNarration(e.target.value)}
              />
            )}
          </Field>
        </div>
      </Card>
      <Card
        title="Expense lines"
        actions={
          <Button
            size="sm"
            variant="secondary"
            icon={<Plus size={14} />}
            onClick={() => setLines([...lines, emptyLine()])}
          >
            Add line
          </Button>
        }
      >
        <div className="stack">
          {lines.map((line, index) => {
            const taxes = lineTaxes(line.netAmount, vat, whtRate);
            return (
              <div className="form-grid" key={index}>
                <Field label={`Account (line ${index + 1})`} required>
                  {(id) => (
                    <select
                      id={id}
                      className="select"
                      value={line.expenseAccountCode}
                      onChange={(e) => setLine(index, { expenseAccountCode: e.target.value })}
                    >
                      <option value="">Select…</option>
                      {postableAccounts
                        .filter((a) => !a.controlAccount)
                        .map((a) => (
                          <option key={a.code} value={a.code}>
                            {a.code} – {a.name}
                          </option>
                        ))}
                    </select>
                  )}
                </Field>
                <Field label="Cost centre">
                  {(id) => (
                    <select
                      id={id}
                      className="select"
                      value={line.costCenter ?? ''}
                      onChange={(e) => setLine(index, { costCenter: e.target.value })}
                    >
                      <option value="">—</option>
                      {costCenters.map((c) => (
                        <option key={c.code} value={c.code}>
                          {c.code} – {c.name}
                        </option>
                      ))}
                    </select>
                  )}
                </Field>
                <Field label="Description" required>
                  {(id) => (
                    <input
                      id={id}
                      className="input"
                      value={line.description}
                      onChange={(e) => setLine(index, { description: e.target.value })}
                    />
                  )}
                </Field>
                <Field
                  label="Net amount"
                  required
                  hint={`VAT ${formatAmount(taxes.vat)} · EWT ${formatAmount(taxes.wht)}`}
                >
                  {(id) => (
                    <input
                      id={id}
                      className="input"
                      type="number"
                      step="0.01"
                      min="0"
                      value={line.netAmount === 0 ? '' : line.netAmount}
                      onChange={(e) => setLine(index, { netAmount: Number(e.target.value) })}
                    />
                  )}
                </Field>
                <Button
                  size="sm"
                  variant="ghost"
                  aria-label={`Remove line ${index + 1}`}
                  icon={<Trash2 size={14} />}
                  disabled={lines.length === 1}
                  style={{ alignSelf: 'end' }}
                  onClick={() => setLines(lines.filter((_, i) => i !== index))}
                />
              </div>
            );
          })}
        </div>
      </Card>
      <div className="grid-4">
        <Kpi label="Net" value={<Amount value={totals.net} />} />
        <Kpi label="Input VAT 12 %" value={<Amount value={totals.vat} />} />
        <Kpi label={`EWT ${whtRate} %`} value={<Amount value={totals.wht} />} />
        <Kpi label="Payable to supplier" accent value={<Amount value={totals.payable} />} />
      </div>
      <div className="row">
        <div className="spacer" />
        <Button variant="secondary" onClick={() => void navigate('/payables/invoices')}>
          Back
        </Button>
        <Button
          variant="secondary"
          disabled={!ready}
          busy={save.isPending}
          onClick={() => save.mutate(false)}
        >
          Save draft
        </Button>
        <Button
          variant="accent"
          disabled={!ready}
          busy={save.isPending}
          onClick={() => save.mutate(true)}
        >
          Save &amp; submit
        </Button>
      </div>
    </div>
  );
}
