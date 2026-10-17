import { useQuery } from '@tanstack/react-query';
import { useState } from 'react';
import { catalogApi } from '@/api/catalog';
import { ClientPicker } from '@/components/broking/ClientPicker';
import { LovSelect } from '@/components/broking/LovSelect';
import { Card } from '@/components/ui/Card';
import { Field } from '@/components/ui/Field';
import { useBaseCurrency, useCompanyId } from '@/context/workspaceContext';
import { formatAmount } from '@/utils/format';
import { CodeSelect, InsurerField, TextField } from './CashFields';
import type { RecordForm } from './recordLogic';
import { currenciesOf, defaultBankAccount, isNonPremium, NON_PREMIUM_TYPES } from './recordLogic';
import type { FormSettings } from './recordsApi';
import { OtherIncomeSelect } from './OtherIncomeSelect';

/** What every section of the Create AR / OR form receives. */
export interface SectionProps {
  form: RecordForm;
  settings: FormSettings;
  set: <K extends keyof RecordForm>(key: K, value: RecordForm[K]) => void;
  update: (change: (form: RecordForm) => RecordForm) => void;
  error: (key: string) => string | undefined;
}

const AR_TYPES = ['PREMIUM', ...NON_PREMIUM_TYPES] as const;
const AR_TYPE_LABELS: Record<string, string> = {
  PREMIUM: 'Premium Payment',
  REFUND: 'Non-Premium Payment: Refund from Insurer',
  OTHER_EXPENSES: 'Non-Premium Payment: Other Expenses',
  AR_INSURANCE: 'Non-Premium Payment: AR Insurance',
};
const ENTRY_LABELS: Record<string, string> = {
  CLIENT: 'Client',
  INSURER: 'Insurer',
  OTHER: 'Other',
};
const TENDER_LABELS: Record<string, string> = {
  CASH: 'Cash',
  CHECK: 'Check',
  DIRECT_CREDIT: 'Direct Credit',
};
const kindLabel = (k: string) => (k === 'AR' ? 'Acknowledgement Receipt' : 'Official Receipt');

/** Receipt type, AR / OR type and receipting branch (FRS.CSH.02.01.02, 02.02.02). */
export function ReceiptSection({ form, settings, set, update, error }: Readonly<SectionProps>) {
  const isOr = form.receiptKind === 'OR';
  const branches = isOr ? settings.orBranches : settings.arBranches;
  const chooseArType = (type: string) =>
    update((f) => ({
      ...f,
      receiptType: type,
      entryType: (NON_PREMIUM_TYPES as readonly string[]).includes(type) ? 'INSURER' : f.entryType,
    }));
  return (
    <Card title="Receipt">
      <div className="form-grid">
        <CodeSelect
          label="Receipt Type"
          value={form.receiptKind}
          options={[form.receiptKind]}
          labelOf={kindLabel}
          onChange={() => undefined}
          required
        />
        {isOr ? (
          <Field label="Official Receipt Type" required error={error('receiptType')}>
            {(id) => (
              <LovSelect
                id={id}
                type="OR_TYPE"
                value={form.receiptType}
                onChange={(v) => set('receiptType', v)}
              />
            )}
          </Field>
        ) : (
          <CodeSelect
            label="Acknowledgement Receipt Type"
            value={form.receiptType}
            options={AR_TYPES}
            labelOf={(c) => AR_TYPE_LABELS[c] ?? c}
            onChange={chooseArType}
            error={error('receiptType')}
            required
          />
        )}
        <CodeSelect
          label="Receipting Branch"
          value={form.branchId}
          options={branches.map((b) => String(b.id))}
          labelOf={(id) => branches.find((b) => String(b.id) === id)?.name ?? id}
          empty="Select…"
          onChange={(v) => set('branchId', v)}
          error={error('branchId')}
          required
        />
        <Field label="Receipt Number">
          {() => <span className="muted">To be generated once posted</span>}
        </Field>
      </div>
    </Card>
  );
}

/** Entry type with the client or insurer and the payor name (FRS.CSH.02.01.02). */
export function PayorSection({ form, set, update, error }: Readonly<SectionProps>) {
  const companyId = useCompanyId();
  const [clientId, setClientId] = useState<number | undefined>(undefined);
  const insurers = useQuery({
    queryKey: ['catalog', 'insurers', companyId],
    queryFn: () => catalogApi.insurers(companyId),
    enabled: companyId > 0,
  });
  const nonPremium = isNonPremium(form);
  const insurerShown = form.entryType === 'INSURER' || nonPremium;
  const chooseInsurer = (code: string) => {
    const name = insurers.data?.find((i) => i.partyCode === code)?.name ?? '';
    update((f) => ({ ...f, insurerCode: code, insurerName: name, payorName: f.payorName || name }));
  };
  return (
    <Card title="Payor">
      <div className="form-grid">
        <CodeSelect
          label="Entry Type"
          value={form.entryType}
          options={nonPremium ? ['INSURER'] : ['CLIENT', 'INSURER', 'OTHER']}
          labelOf={(c) => ENTRY_LABELS[c] ?? c}
          onChange={(v) => set('entryType', v as RecordForm['entryType'])}
          error={error('entryType')}
          required
        />
        {form.entryType === 'CLIENT' && (
          <Field label="Client" required error={error('clientCode')}>
            {(id) => (
              <ClientPicker
                id={id}
                value={clientId}
                allowProspect={false}
                onChange={(c) => {
                  setClientId(c?.id);
                  update((f) => ({
                    ...f,
                    clientCode: c?.clientCode ?? c?.code ?? '',
                    clientName: c?.displayName ?? '',
                    payorName: f.payorName || (c?.displayName ?? ''),
                  }));
                }}
              />
            )}
          </Field>
        )}
        {form.entryType === 'CLIENT' && (
          <TextField
            label="Client Name"
            value={form.clientName}
            onChange={(v) => set('clientName', v)}
            maxLength={250}
          />
        )}
        {insurerShown && (
          <InsurerField
            value={form.insurerCode}
            empty="Select…"
            required
            error={error('insurerCode')}
            onChange={chooseInsurer}
          />
        )}
        {form.entryType === 'INSURER' && (
          <TextField
            label="Bank Name"
            value={form.insurerBank}
            onChange={(v) => set('insurerBank', v)}
            hint="From the insurer banking list"
          />
        )}
        <TextField
          label="Payor Name"
          value={form.payorName}
          onChange={(v) => set('payorName', v)}
          error={error('payorName')}
          maxLength={250}
          required
        />
      </div>
    </Card>
  );
}

/** Payment type, currency, bank account, paid amount and the taxes of an OR (FRS.CSH.02.01.02). */
export function PaymentSection({ form, settings, set, update, error }: Readonly<SectionProps>) {
  const base = useBaseCurrency();
  const isOr = form.receiptKind === 'OR';
  const currencies = currenciesOf(settings.bankAccounts);
  const chooseCurrency = (currency: string) =>
    update((f) => ({
      ...f,
      currency,
      bankAccount: defaultBankAccount(settings.bankAccounts, currency),
    }));
  return (
    <Card title="Payment">
      <div className="form-grid">
        <CodeSelect
          label="Payment Type"
          value={form.tenderType}
          options={isOr ? ['CASH', 'CHECK', 'DIRECT_CREDIT'] : ['CASH', 'CHECK']}
          labelOf={(c) => TENDER_LABELS[c] ?? c}
          onChange={(v) => set('tenderType', v as RecordForm['tenderType'])}
          error={error('tenderType')}
          required
        />
        <CodeSelect
          label="Currency"
          value={form.currency}
          options={currencies.length > 0 ? currencies : [base]}
          labelOf={(c) => c}
          onChange={chooseCurrency}
          error={error('currency')}
          required
        />
        {form.currency !== base && (
          <Field label="Rate of the Day">
            {() => <span className="num">{formatAmount(settings.foreignRate)}</span>}
          </Field>
        )}
        <CodeSelect
          label="Post to Bank Account"
          value={form.bankAccount}
          options={settings.bankAccounts.map((b) => b.code)}
          labelOf={(c) => settings.bankAccounts.find((b) => b.code === c)?.name ?? c}
          empty="Select…"
          onChange={(v) => set('bankAccount', v)}
          error={error('bankAccount')}
          required
        />
        <TextField
          label="Paid Amount"
          type="number"
          value={form.amount}
          onChange={(v) => set('amount', v)}
          error={error('amount')}
          hint="At most 1,000,000,000.00"
          required
        />
        {isOr && (
          <OrTaxFields form={form} settings={settings} set={set} update={update} error={error} />
        )}
      </div>
    </Card>
  );
}

function OrTaxFields({ form, set, update, error }: Readonly<SectionProps>) {
  return (
    <>
      <TextField label="VAT" type="number" value={form.vat} onChange={(v) => set('vat', v)} />
      <TextField
        label="Withholding Tax"
        type="number"
        value={form.wtax}
        onChange={(v) => set('wtax', v)}
      />
      <TextField
        label="BIR 2307 Certificate Reference"
        value={form.certificateRef}
        onChange={(v) => set('certificateRef', v)}
        error={error('certificateRef')}
      />
      <OtherIncomeSelect form={form} update={update} />
    </>
  );
}

/** Check details, receipt issuance date and remarks (FRS.CSH.02.01.02). */
export function CheckSection({ form, settings, set, error }: Readonly<SectionProps>) {
  const check = form.tenderType === 'CHECK';
  const holding = settings.latestCheckDate
    ? `Holding period of ${settings.holdingDays} working days`
    : undefined;
  return (
    <Card title="Check and Remarks">
      <div className="form-grid">
        {check && (
          <TextField
            label="Check Number"
            value={form.checkNo}
            onChange={(v) => set('checkNo', v)}
            error={error('checkNo')}
            maxLength={40}
            required
          />
        )}
        {check && (
          <TextField
            label="Check Date"
            type="date"
            value={form.checkDate}
            onChange={(v) => set('checkDate', v)}
            error={error('checkDate')}
            hint={holding}
            required
          />
        )}
        {check && (
          <TextField
            label="Bank of the Check"
            value={form.checkBank}
            onChange={(v) => set('checkBank', v)}
          />
        )}
        {form.receiptKind === 'AR' && (
          <TextField
            label="Receipt Issuance Date"
            type="date"
            value={form.receiptDate}
            onChange={(v) => set('receiptDate', v)}
          />
        )}
        <TextField
          label="Remarks"
          value={form.remarks}
          onChange={(v) => set('remarks', v)}
          error={error('remarks')}
          maxLength={200}
          required={settings.remarksRequired}
        />
      </div>
    </Card>
  );
}
