import type { DueState, TaxDocument, TaxForm, Worksheet, WorksheetKind } from '@/api/tax';

/** Badge tone (CSS class of `.badge`) and text of a calendar due state. */
export function dueBadge(state: DueState, daysToDue: number): { tone: string; text: string } {
  switch (state) {
    case 'PAID':
      return { tone: 'success', text: 'Paid' };
    case 'OVERDUE':
      return { tone: 'danger', text: `Overdue ${Math.abs(daysToDue)} d` };
    case 'DUE_SOON':
      return { tone: 'warning', text: `Due in ${daysToDue} d` };
    case 'REMINDER':
      return { tone: 'neutral', text: 'Outside FinVerse' };
    default:
      return { tone: 'neutral', text: `Due in ${daysToDue} d` };
  }
}

/**
 * Frontend route of the source document of a worksheet row. Supplier invoices have no page of
 * their own: the invoice list opens with that invoice's detail. Policies, endorsements and
 * commissions carry the policy id and open the policy.
 */
export function sourceLink(doc: TaxDocument): string {
  return doc.sourceType === 'SUPPLIER_INVOICE'
    ? `/payables/invoices?invoice=${doc.sourceId}`
    : `/underwriting/policies/${doc.sourceId}`;
}

/** Amount of a worksheet summary line (0 when absent). */
export function lineAmount(worksheet: Worksheet | undefined, code: string): number {
  return worksheet?.lines.find((l) => l.code === code)?.amount ?? 0;
}

/** Headline KPIs of a worksheet: label and amount, in display order. */
export function worksheetKpis(worksheet: Worksheet): { label: string; value: number }[] {
  const f = worksheet.figures;
  switch (worksheet.kind) {
    case 'VAT':
      return [
        { label: 'Output VAT', value: f.taxDue },
        { label: 'Input VAT and carry-over', value: f.taxCredits },
        { label: 'VAT payable', value: f.amountPayable },
        { label: 'Excess credit', value: f.excessCredit },
      ];
    case 'EWT':
      return [
        { label: 'Income payments', value: f.taxBase },
        { label: 'Tax withheld', value: f.taxDue },
        { label: 'Remitted monthly', value: f.taxCredits },
        { label: 'Tax still due', value: f.amountPayable },
      ];
    default:
      return [
        { label: 'Premiums', value: f.taxBase },
        { label: 'Tax due', value: f.taxDue },
        { label: 'Documents', value: worksheet.documents.length },
      ];
  }
}

/** Authorized, tracked forms computed by a worksheet (for "Create return"). */
export function formsFor(forms: TaxForm[], kind: WorksheetKind): TaxForm[] {
  return forms.filter((f) => f.worksheet === kind && f.trackFiling && f.recordStatus === 'ACTIVE');
}

/** Whether a form is filed per quarter (its periods are picked as quarters). */
export function isQuarterly(form: TaxForm): boolean {
  return form.frequency === 'QUARTERLY';
}

/** Sum of the ledger reconciliation differences (0 when fully reconciled). */
export function unreconciled(worksheet: Worksheet): number {
  return worksheet.controls.reduce((sum, c) => sum + Math.abs(c.difference), 0);
}
