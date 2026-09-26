import { Download, Upload } from 'lucide-react';
import { useState } from 'react';
import { saveFile } from '@/api/client';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';
import { formatAmount } from '@/utils/format';
import type { Amounts, RemittanceType, Settlement } from './api';
import { TYPE_LABELS } from './remittanceLabels';
import './remittance.css';

/**
 * Totals strip of a batch (RMTID.002 addendum: amounts are read-only), with the CPC2 incentive
 * (DIS 3.29.2) and, once approved, the deductions and the amount due (ACSL 2.9.2).
 */
export function TotalsStrip({
  totals,
  currency,
  settlement,
}: Readonly<{ totals: Amounts; currency: string; settlement?: Settlement }>) {
  const deducted = settlement !== undefined && settlement.deductionAmount > 0;
  const items: { label: string; value: number; strong?: boolean }[] = [
    { label: 'Paid AR', value: totals.paidAr },
    { label: 'Realized Commission', value: totals.commission },
    { label: 'VAT', value: totals.commissionVat },
    { label: 'WTAX', value: totals.wtax },
    { label: 'DTIP', value: totals.dtip },
    { label: 'Incentive with VAT', value: totals.incentive + totals.incentiveVat },
    { label: 'CPC2 with VAT', value: totals.cpc2 + totals.cpc2Vat },
    { label: 'Net Due', value: totals.netDue },
    { label: `Payable (${currency})`, value: totals.payable, strong: !deducted },
  ];
  if (deducted) {
    items.push(
      { label: 'Deductions', value: settlement.deductionAmount },
      { label: `Amount Due (${currency})`, value: settlement.amountDue, strong: true },
    );
  }
  return (
    <div className="remit-totals" aria-label="Batch totals">
      {items.map((i) => (
        <div
          key={i.label}
          className={i.strong === true ? 'remit-total remit-total-strong' : 'remit-total'}
        >
          <span className="remit-total-label">{i.label}</span>
          <span className="remit-total-value num">{formatAmount(i.value)}</span>
        </div>
      ))}
    </div>
  );
}

/** Remittance type as a flag chip (BDO UX: flags apart from the status pill). */
export function TypeChip({ type }: Readonly<{ type: RemittanceType }>) {
  return <span className="tag">{TYPE_LABELS[type]}</span>;
}

/** Downloads a CSV template of an upload. */
export function TemplateButton({ name, content }: Readonly<{ name: string; content: string }>) {
  return (
    <Button
      variant="ghost"
      icon={<Download size={16} />}
      onClick={() => saveFile(new Blob([content], { type: 'text/csv' }), name)}
    >
      Download Template
    </Button>
  );
}

/** A file picker with its upload button (manual transport of the feeds). */
export function UploadForm({
  label,
  busy,
  onUpload,
}: Readonly<{ label: string; busy: boolean; onUpload: (file: File) => void }>) {
  const [file, setFile] = useState<File>();
  const [error, setError] = useState<string>();
  return (
    <div className="remit-form">
      <Field label={label} required error={error} hint="CSV, semicolon or tab separated text">
        {(id) => (
          <input
            id={id}
            className="input"
            type="file"
            accept=".csv,.txt,text/csv,text/plain"
            onChange={(e) => {
              setFile(e.target.files?.[0]);
              setError(undefined);
            }}
          />
        )}
      </Field>
      <div>
        <Button
          icon={<Upload size={16} />}
          busy={busy}
          onClick={() => {
            if (file === undefined) {
              setError('Choose a file');
              return;
            }
            onUpload(file);
          }}
        >
          Upload File
        </Button>
      </div>
    </div>
  );
}
