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
