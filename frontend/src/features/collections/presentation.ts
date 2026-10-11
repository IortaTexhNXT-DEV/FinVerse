import { formatAmount, humanize } from '@/utils/format';
import type { RowAction } from '@/components/ui/RowActionMenu';
import type { DispositionRule } from './api';

/**
 * Presentation rules of the Collections screens (screen standards): labels instead of codes,
 * names instead of logins, formatted amounts. Pure functions, tested with Vitest.
 */

/** Names of the roles a disposition may be reserved to (the role codes are never shown). */
const ROLE_LABELS: Readonly<Record<string, string>> = {
  MKT_AO: 'Marketing Account Officer',
  MKT_TL: 'Marketing Team Lead',
  MKT_HANDLER: 'Marketing Handler',
  MKT_COLLECTION: 'Collection Handler',
  CLX_TL: 'Collection Team Lead',
  MKT_SECTION_HEAD: 'Section Head',
  UNAPPLIED_HANDLER: 'Unapplied Payment Handler',
  PROCESSOR: 'Processing',
};

/** The roles a disposition may be reserved to, as the choices of the set-up screen. */
export const ROLE_OPTIONS: readonly { code: string; label: string }[] = Object.entries(
  ROLE_LABELS,
).map(([code, label]) => ({ code, label }));

/** The name of a role, for the reserved roles of a disposition. */
export function roleLabel(code: string): string {
  return ROLE_LABELS[code] ?? humanize(code);
}

/** Who acts next on an account (p.41): Marketing or Operations, never the code. */
export function taggingOwnerLabel(owner: string | undefined): string | undefined {
  if (owner === undefined || owner === '') {
    return undefined;
  }
  return owner === 'OPERATIONS' ? 'Operations' : 'Marketing';
}

/** The team that holds the invoice lock, by name: "Locked by Adjustment". */
export function lockText(owner: string): string {
  return `Locked by ${humanize(owner)}`;
}

/** The information line of a chosen disposition: category, who acts next, reserved roles. */
export function ruleSummary(rule: DispositionRule): string {
  const owner = taggingOwnerLabel(rule.taggingOwner);
  return [
    rule.category && `Category ${rule.category}`,
    owner && `${owner} action`,
    rule.allowedRoles.length > 0 && `Reserved to ${rule.allowedRoles.map(roleLabel).join(', ')}`,
  ]
    .filter(Boolean)
    .join(' · ');
}

/** The label of a disposition code from the active rules, the code in words while they load. */
export function dispositionLabel(
  rules: readonly Pick<DispositionRule, 'code' | 'label'>[] | undefined,
  code: string | undefined,
): string {
  if (code === undefined || code === '') {
    return '';
  }
  return rules?.find((r) => r.code === code)?.label ?? humanize(code);
}

/** The 2307 paths of the BIR 2307 hand-off, as the drop-down shows them. */
export const CWT_PATHS: readonly { code: string; label: string }[] = [
  { code: 'CERTIFICATE', label: 'Certificate received (BIR 2307)' },
  { code: 'CASH', label: 'Paid in cash' },
];

/** A threshold of an escalation rule as text: an amount with separators, else the number. */
export function thresholdText(basis: string, threshold: number): string {
  return basis === 'AMOUNT_OVER' ? formatAmount(threshold) : String(threshold);
}

/** The row actions of an assignment rule: change it, then activate or deactivate it. */
export function assignmentRuleActions(
  rule: { active: boolean },
  on: { change: () => void; activate: () => void },
): RowAction[] {
  return [
    { label: 'Change Rule', onSelect: on.change },
    rule.active
      ? { label: 'Deactivate', onSelect: on.activate, danger: true }
      : { label: 'Activate', onSelect: on.activate },
  ];
}

/** The premium components of the net PR breakdown, by name (the component codes are never shown). */
const COMPONENT_LABELS: Readonly<Record<string, string>> = {
  BASIC: 'Basic Premium',
  DST: 'DST',
  PREMIUM_TAX_VAT: 'Premium Tax / VAT',
  LGT: 'LGT',
  FST: 'FST',
  PR2307: 'PR 2307',
  OTHER: 'Other Charges',
};

/** The name of a premium component of an account. */
export function componentText(code: string): string {
  return COMPONENT_LABELS[code] ?? humanize(code);
}

/** The fields of the change log of an account, by name. */
const CHANGED_FIELDS: Readonly<Record<string, string>> = {
  status: 'Status',
  currentHandler: 'Handler',
  category: 'Category',
  remarks: 'Remarks',
  taggingOwner: 'Tagging Owner',
  dispositionCode: 'Disposition',
  lastEffortCode: 'Last Effort',
};

/** The name of a changed field of an account ("dispositionCode" reads "Disposition"). */
export function changedFieldText(field: string): string {
  return CHANGED_FIELDS[field] ?? humanize(field.replace(/([a-z])([A-Z])/g, '$1_$2'));
}

/** How the values of the change log read: the handler by name, a disposition or effort by its label. */
export interface ValueLookups {
  name: (login: string) => string;
  disposition: (code: string) => string;
  effort: (code: string) => string;
}

/** A value of the change log as the screens show it, never a code or a login. */
export function changedValueText(
  field: string,
  value: string | null | undefined,
  look: ValueLookups,
): string {
  if (value === null || value === undefined || value === '') {
    return '—';
  }
  switch (field) {
    case 'currentHandler':
      return look.name(value);
    case 'dispositionCode':
      return look.disposition(value);
    case 'lastEffortCode':
      return look.effort(value);
    case 'taggingOwner':
      return taggingOwnerLabel(value) ?? value;
    case 'status':
      return humanize(value);
    case 'category':
      return `Category ${value}`;
    default:
      return value;
  }
}

/** The template line of a statement of account: "CLX_SOA v1" reads "Statement template version 1". */
export function templateText(version: string | undefined): string {
  const n = /v(\d+)$/i.exec(version ?? '')?.[1];
  return n === undefined ? '' : `Statement template version ${n}.`;
}

/** What a disposition hands to Operations, and the feed Operations takes it from, by name. */
const HAND_OFF_LABELS: Readonly<Record<string, string>> = {
  NONE: 'None',
  CHECK_PICKUP: 'Check pick-up',
  CWT2307_REVERSAL: 'BIR 2307 reversal',
  CWT: 'BIR 2307 reversal',
  DP_REVERSAL: 'DP reversal',
  DP_LIST: 'DP reversal',
  DP_RETURNED: 'DP returned by insurer',
  CANCEL_REQUEST: 'Cancellation request',
  REFUND: 'Refund',
};

/** The name of a hand-off to Operations or of its feed ("Check pick-up", never CHECK_PICKUP). */
export function handOffLabel(code: string): string {
  const key = code.replace(/^COLLECTION_/, '');
  return HAND_OFF_LABELS[key] ?? humanize(key);
}
