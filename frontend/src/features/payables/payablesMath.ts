import type { InvoiceLine, PayableItem } from '@/api/payables';

/**
 * Client-side mirror of the backend payables arithmetic (InvoiceCalculator), used to preview
 * invoice totals and payment amounts before saving. The server recomputes everything.
 */

const CENTS = 100;
export const VAT_RATE = 0.12;

/** Rounds to centavos (half away from zero on the cent, like the server for positive values). */
export function round2(value: number): number {
  return Math.round((value + Number.EPSILON) * CENTS) / CENTS;
}

export interface LineTaxes {
  vat: number;
  wht: number;
  payable: number;
}

/** Input VAT (12 %), expanded withholding tax and payable of one invoice line. */
export function lineTaxes(net: number, vatApplicable: boolean, whtRatePercent: number): LineTaxes {
  const amount = Number.isFinite(net) ? net : 0;
  const vat = vatApplicable ? round2(amount * VAT_RATE) : 0;
  const wht = round2((amount * whtRatePercent) / CENTS);
  return { vat, wht, payable: round2(amount + vat - wht) };
}

export interface InvoiceTotals {
  net: number;
  vat: number;
  wht: number;
  payable: number;
}

/** Invoice totals as the sum of rounded lines. */
export function invoiceTotals(
  lines: Pick<InvoiceLine, 'netAmount'>[],
  vatApplicable: boolean,
  whtRatePercent: number,
): InvoiceTotals {
  return lines.reduce<InvoiceTotals>(
    (acc, l) => {
      const t = lineTaxes(l.netAmount, vatApplicable, whtRatePercent);
      return {
        net: round2(acc.net + (Number.isFinite(l.netAmount) ? l.netAmount : 0)),
        vat: round2(acc.vat + t.vat),
        wht: round2(acc.wht + t.wht),
        payable: round2(acc.payable + t.payable),
      };
    },
    { net: 0, vat: 0, wht: 0, payable: 0 },
  );
}

/** Due date = invoice date + supplier credit days (ISO dates). */
export function dueDate(invoiceDate: string, creditDays: number): string {
  if (invoiceDate === '') {
    return '';
  }
  const d = new Date(`${invoiceDate}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() + creditDays);
  return d.toISOString().slice(0, 10);
}

/** Selected amounts per open item, validated against what is still available. */
export type Selection = Record<number, number>;

export interface SelectionCheck {
  total: number;
  errors: string[];
}

export function checkSelection(items: PayableItem[], selection: Selection): SelectionCheck {
  const errors: string[] = [];
  let total = 0;
  items.forEach((item) => {
    const amount = selection[item.openItemId];
    if (amount === undefined) {
      return;
    }
    if (Number.isNaN(amount) || amount <= 0) {
      errors.push(`${item.documentNo}: amount must be positive`);
    } else if (amount > item.available) {
      errors.push(`${item.documentNo}: exceeds available ${item.available.toFixed(2)}`);
    }
    total += Number.isFinite(amount) ? amount : 0;
  });
  return { total: round2(total), errors };
}

/** Days between two ISO dates (to - from); negative when not yet due. */
export function daysBetween(from: string, to: string): number {
  const ms = Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`);
  return Math.round(ms / 86_400_000);
}

/** Share of the imprest still in the box, in percent (0-100). */
export function fundLevel(cash: number, imprest: number): number {
  if (imprest <= 0) {
    return 0;
  }
  return Math.max(0, Math.min(CENTS, Math.round((cash / imprest) * CENTS)));
}
