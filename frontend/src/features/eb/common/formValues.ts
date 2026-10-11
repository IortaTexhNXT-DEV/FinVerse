/** A typed text, or undefined when blank (optional fields of the API inputs). */
export function orNone(value: string | undefined): string | undefined {
  return value === undefined || value.trim() === '' ? undefined : value;
}
