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
