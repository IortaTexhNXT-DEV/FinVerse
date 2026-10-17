/** Splits a list of e-mail addresses typed with commas, semicolons or spaces. */
export function splitEmails(text: string): string[] {
  return text
    .split(/[\s,;]+/)
    .map((e) => e.trim())
    .filter((e) => e !== '');
}

/** Placement channels of an insurer with their labels. */
export const PLACEMENT_CHANNELS = [{ value: 'EMAIL', label: 'E-mail' }];

/** The label of an insurer's placement channel. */
export function placementChannelLabel(code: string | undefined): string {
  return PLACEMENT_CHANNELS.find((c) => c.value === code)?.label ?? code ?? '';
}

/** Tax statuses of an insurer with their labels (blank: the taxes of the product line). */
export const INSURER_TAX_STATUSES = [
  { value: 'VAT_REGISTERED', label: 'VAT-registered (VAT on premium)' },
  { value: 'NON_VAT', label: 'Not VAT-registered (premium tax)' },
];

/** The label of an insurer's tax status. */
export function insurerTaxStatusLabel(code: string | null | undefined): string {
  return INSURER_TAX_STATUSES.find((s) => s.value === code)?.label ?? 'As the product line';
}
