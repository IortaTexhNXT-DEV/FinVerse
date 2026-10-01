import { humanize } from '@/utils/format';
import { ROLE_OPTIONS, roleLabel, taggingOwnerLabel } from './presentation';

/**
 * Names, descriptions and choices of the Collections Setup screen: the parameters by name with a business
 * description, the attributes of the disposition values as labels and drop-downs. The codes stay the stored
 * values; they are never shown. Pure functions, tested with Vitest.
 */

export interface Choice {
  code: string;
  label: string;
}

interface ParameterText {
  label: string;
  description: string;
  values?: Readonly<Record<string, string>>;
}

const PARAMETERS: Readonly<Record<string, ParameterText>> = {
  CLX_AGING_BASIS: {
    label: 'Aging Basis',
    description:
      'The date the aging of an account counts from: the booking date or the inception date',
    values: { BOOKING: 'Booking date', INCEPTION: 'Inception date' },
  },
  CLX_AGING_BRACKETS: {
    label: 'Aging Brackets',
    description: 'Aging brackets of the worklist, the home chart and the reports, in days',
  },
  CLX_EDIT_LOCK_MINUTES: {
    label: 'Edit Lock (Minutes)',
    description: 'Minutes after which the "<user> is editing" lock of a collection account expires',
  },
  CLX_EXPORT_MAX_ROWS: {
    label: 'Export Row Limit',
    description: 'Maximum rows of one export of the worklist to Collections Files',
  },
  CLX_INVOICE_NO_PATTERN: {
    label: 'Invoice Number Format',
    description:
      'Format of the invoice number of "For application to invoice": a legacy invoice I followed by eight digits, or BI-…',
  },
  CLX_MIN_BALANCE_THRESHOLD: {
    label: 'Minimal Balance Threshold',
    description: 'Net outstanding PR above which an invoice enters the collection worklist',
  },
  CLX_PROMISE_GRACE_DAYS: {
    label: 'Promise Grace Days',
    description: 'Days after the promised date before an unpaid promise to pay is broken',
  },
};

/** The name of a Collections parameter ("Minimal Balance Threshold", never the key). */
export function parameterLabel(key: string): string {
  return PARAMETERS[key]?.label ?? humanize(key.replace(/^CLX_/, ''));
}

/** What a parameter does, in business words. */
export function parameterDescription(key: string, stored: string): string {
  return PARAMETERS[key]?.description ?? stored;
}

/** The value of a parameter as the screen shows it ("Booking date" for BOOKING). */
export function parameterValueText(key: string, value: string): string {
  return PARAMETERS[key]?.values?.[value] ?? value;
}

/** The choices of a parameter with a fixed set of values; undefined for a free value. */
export function parameterChoices(key: string): Choice[] | undefined {
  const values = PARAMETERS[key]?.values;
  return values === undefined
    ? undefined
    : Object.entries(values).map(([code, label]) => ({ code, label }));
}

/** The list a disposition value belongs to. */
const LIST_LABELS: Readonly<Record<string, string>> = {
  CLX_PR_DISPOSITION: 'PR Dispositions',
  CLX_UPP_DISPOSITION: 'Unapplied Payment Dispositions',
};

/** The attributes of the disposition values, by name. */
const ATTRIBUTE_LABELS: Readonly<Record<string, string>> = {
  category: 'Category',
  tagging_owner: 'Tagging Owner',
  ops_action: 'Operations Action',
  allowed_roles: 'Reserved To',
  requires_invoice: 'Requires Invoice',
  cashiering_action: 'Cashiering Action',
};

/** The name of a list of disposition values ("PR Dispositions"). */
export function listLabel(list: string): string {
  return LIST_LABELS[list] ?? humanize(list.replace(/^CLX_/, ''));
}

/** The name of an attribute of the disposition values ("Reserved To" for the allowed roles). */
export function attributeLabel(attribute: string): string {
  return ATTRIBUTE_LABELS[attribute] ?? humanize(attribute);
}

const CHOICES: Readonly<Record<string, readonly Choice[]>> = {
  category: [
    { code: 'A', label: 'Category A' },
    { code: 'B', label: 'Category B' },
    { code: 'C', label: 'Category C' },
  ],
  tagging_owner: [
    { code: 'MARKETING', label: 'Marketing' },
    { code: 'OPERATIONS', label: 'Operations' },
  ],
  ops_action: [
    { code: 'NONE', label: 'None' },
    { code: 'CWT2307_REVERSAL', label: 'BIR 2307 reversal' },
    { code: 'DP_REVERSAL', label: 'DP reversal' },
    { code: 'CHECK_PICKUP', label: 'Check pick-up' },
    { code: 'CANCEL_REQUEST', label: 'Cancellation request' },
  ],
  allowed_roles: ROLE_OPTIONS,
  requires_invoice: [
    { code: 'true', label: 'Yes' },
    { code: 'false', label: 'No' },
  ],
  cashiering_action: [
    { code: 'APPLY_TO_INVOICE', label: 'Apply to invoice' },
    { code: 'REFUND', label: 'Refund' },
    { code: 'RECLASS', label: 'Reclass' },
    { code: 'TRANSFER', label: 'Transfer' },
    { code: 'NONE', label: 'None' },
  ],
};

/** The choices of an attribute (the reserved roles may take several). */
export function attributeChoices(attribute: string): readonly Choice[] {
  return CHOICES[attribute] ?? [];
}

/** The value of an attribute as the screen shows it: labels, never codes; blank reads "Not set". */
export function attributeText(attribute: string, value: string): string {
  if (value.trim() === '') {
    return attribute === 'allowed_roles' ? 'Every collector' : 'Not set';
  }
  if (attribute === 'allowed_roles') {
    return value
      .split(',')
      .map((r) => r.trim())
      .filter((r) => r !== '')
      .map(roleLabel)
      .join(', ');
  }
  if (attribute === 'tagging_owner') {
    return taggingOwnerLabel(value) ?? value;
  }
  return CHOICES[attribute]?.find((c) => c.code === value)?.label ?? humanize(value);
}
