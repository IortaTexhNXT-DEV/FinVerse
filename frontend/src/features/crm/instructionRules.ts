import type { InstructionRequest } from '@/api/clients';

export type InstructionErrors = Partial<Record<keyof InstructionRequest, string>>;

/** Field errors of a special instruction (type, text, effective dates in order). */
export function instructionErrors(r: InstructionRequest): InstructionErrors {
  const errors: InstructionErrors = {};
  if (r.type === '') {
    errors.type = 'Select the instruction type';
  }
  if (r.text.trim() === '') {
    errors.text = 'Enter the instruction';
  }
  if (r.effectiveFrom === '') {
    errors.effectiveFrom = 'Enter the first day';
  }
  if (r.effectiveTo !== undefined && r.effectiveTo !== '' && r.effectiveTo < r.effectiveFrom) {
    errors.effectiveTo = 'The end date must not be before the start date';
  }
  return errors;
}

/** Whether an instruction applies on a date (ISO yyyy-MM-dd). */
export function inForce(
  i: { active: boolean; effectiveFrom: string; effectiveTo?: string },
  date: string,
): boolean {
  return (
    i.active && i.effectiveFrom <= date && (i.effectiveTo === undefined || i.effectiveTo >= date)
  );
}
