import type { ApproverOption } from '@/api/nbadmin';

/**
 * How each approver is shown: the full name, as on every screen. Only two approvers of the same
 * name are told apart by their user ID.
 */
export function approverLabels(options: readonly ApproverOption[]): Map<string, string> {
  const count = new Map<string, number>();
  for (const o of options) {
    count.set(o.fullName, (count.get(o.fullName) ?? 0) + 1);
  }
  return new Map(
    options.map((o) => [
      o.username,
      (count.get(o.fullName) ?? 0) > 1 ? `${o.fullName} (${o.username})` : o.fullName,
    ]),
  );
}
