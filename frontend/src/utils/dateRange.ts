/**
 * Message for a reversed date range, or undefined when the range is valid or open. Dates are ISO
 * strings (yyyy-mm-dd, as date inputs deliver them), which compare correctly as text.
 */
export function dateRangeError(
  from: string | undefined,
  to: string | undefined,
  labels: { from: string; to: string } = { from: 'From date', to: 'To date' },
): string | undefined {
  if (from === undefined || to === undefined || from === '' || to === '') {
    return undefined;
  }
  return to < from ? `${labels.to} must not be before ${labels.from}` : undefined;
}
