/**
 * A label naming the currency of its amounts: "Amount (<base currency>)".
 * The currency comes from the company master, never written into the screen.
 *
 * @param label label
 * @param currency currency code; the label alone when blank
 * @returns label with the currency
 */
export function inCurrency(label: string, currency: string): string {
  return currency === '' ? label : `${label} (${currency})`;
}
