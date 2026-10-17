import type { MisEntity, MisField, MisFieldInput } from '@/api/misFields';
import { MIS_SOURCES } from '@/api/misFields';
import { humanize } from '@/utils/format';

/** The core entities of the MIS fields, in the order of the screen. */
export const MIS_ENTITIES: { value: MisEntity; label: string }[] = [
  { value: 'ACCOUNT', label: 'Policy / Account' },
  { value: 'PRODUCT', label: 'Product' },
  { value: 'INSURER', label: 'Insurer' },
  { value: 'CLAIM', label: 'Claim' },
  { value: 'USER', label: 'User' },
  { value: 'TRANSACTION', label: 'Transaction' },
];

/** The data elements of an entity as options. */
export function sourceOptions(entity: MisEntity): { value: string; label: string }[] {
  return MIS_SOURCES[entity].map((s) => ({ value: s, label: humanize(s) }));
}

/** A new field, or the field to change. */
export function inputOf(field?: MisField): MisFieldInput {
  if (field === undefined) {
    return { code: '', label: '', entity: 'ACCOUNT', source: 'POLICY_NUMBER', lovType: null };
  }
  return {
    code: field.code,
    label: field.label,
    entity: field.entity,
    source: field.source,
    lovType: field.lovType,
  };
}

/** The errors of the form, by field. */
export function fieldErrors(input: MisFieldInput): Partial<Record<'code' | 'label', string>> {
  const errors: Partial<Record<'code' | 'label', string>> = {};
  if (!/^[A-Z][A-Z0-9_]{1,39}$/.test(input.code)) {
    errors.code = 'Enter a code of capital letters, digits and underscores';
  }
  if (input.label.trim() === '') {
    errors.label = 'Enter the label of the field';
  }
  return errors;
}
