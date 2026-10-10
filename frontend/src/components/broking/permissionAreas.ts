/**
 * Grouping, search and checks of the permission picker of the group-profile requests: the
 * permissions by the business areas of the profile picker, separation-of-duties conflicts read
 * from the profile rules, and the permissions that call for a higher privilege level.
 */
import { permissionDescription, permissionLabel } from '@/utils/permissionLabel';
import { ADMINISTRATION, PROFILE_AREAS, areaOfPermission } from './profileAreas';
import type { ProfileConflictRule, ProfileInfo } from './profileAreas';

/** Area of the permissions every profile may hold (views, attachments, dashboards). */
export const GENERAL = 'General';

/** Areas in picker order: the business areas, then Administration & Control, then General. */
const ORDER = [...PROFILE_AREAS.map((a) => a.area), ADMINISTRATION, GENERAL];

/** The picker area of one permission. */
export function pickerArea(permission: string): string {
  return areaOfPermission(permission) ?? GENERAL;
}

/** A group of the permission picker: an area, all its permissions and those matching the search. */
export interface PermissionGroup {
  area: string;
  /** Every permission of the area, by name. */
  all: string[];
  /** The permissions of the area matching the search, by name. */
  shown: string[];
}

/** Whether a permission matches a search term (name or description). */
export function matchesPermission(permission: string, term: string): boolean {
  const t = term.trim().toLowerCase();
  if (t === '') {
    return true;
  }
  return `${permissionLabel(permission)} ${permissionDescription(permission)}`
    .toLowerCase()
    .includes(t);
}

/** The permissions grouped by area in picker order; areas with no match to the search are left out. */
export function groupPermissions(
  permissions: readonly string[],
  search: string,
): PermissionGroup[] {
  const groups = new Map<string, string[]>();
  [...new Set(permissions)]
    .sort((a, b) => permissionLabel(a).localeCompare(permissionLabel(b)))
    .forEach((p) => {
      const area = pickerArea(p);
      groups.set(area, [...(groups.get(area) ?? []), p]);
    });
  return [...groups.entries()]
    .sort(([a], [b]) => ORDER.indexOf(a) - ORDER.indexOf(b))
    .map(([area, all]) => ({ area, all, shown: all.filter((p) => matchesPermission(p, search)) }))
    .filter((g) => g.shown.length > 0);
}

/** A separation-of-duties conflict inside one profile: a permission of each side of a rule. */
export interface PermissionConflict {
  key: string;
  first: string;
  second: string;
  profileA: string;
  profileB: string;
  reason: string;
}

const ACTING_SKIP = /_(VIEW|EXPORT|MONITOR)$|^REPORT_|^ATTACHMENT_|^DASHBOARD_|^WORK_VIEW$/;

/**
 * The active separation-of-duties rules the selected permissions break: a rule pairs two
 * profiles; the selection breaks it when it holds a working permission only the first profile
 * holds together with one only the second holds (the maker's and the approver's work).
 */
export function permissionConflicts(
  selected: readonly string[],
  rules: readonly ProfileConflictRule[],
  profiles: readonly ProfileInfo[],
): PermissionConflict[] {
  const chosen = new Set(selected);
  const byCode = new Map(profiles.map((p) => [p.code, p]));
  const conflicts: PermissionConflict[] = [];
  rules
    .filter((r) => (r.status ?? 'ACTIVE') === 'ACTIVE')
    .forEach((r) => {
      const a = byCode.get(r.profileA);
      const b = byCode.get(r.profileB);
      if (a === undefined || b === undefined) {
        return;
      }
      const only = (x: ProfileInfo, y: ProfileInfo) =>
        x.permissions.filter((p) => !ACTING_SKIP.test(p) && !y.permissions.includes(p));
      const first = only(a, b).find((p) => chosen.has(p));
      const second = only(b, a).find((p) => chosen.has(p));
      if (first !== undefined && second !== undefined) {
        conflicts.push({
          key: `${r.profileA}-${r.profileB}`,
          first,
          second,
          profileA: a.name,
          profileB: b.name,
          reason: r.description,
        });
      }
    });
  return conflicts;
}

const APPROVING = /_(APPROVE|AUTHORIZE|AUTHORISE|SIGNOFF|DECIDE)$/;
const ADMINISTERING =
  /^(USER_|ROLE_|ACCESS_|UAM_|SECURITY_|SYSTEM_|MFA_|MODULE_SWITCH|AUDIT_MANAGE)/;

/** Why a permission raises the privilege of a profile, or null for a standard permission. */
export function privilegeOf(
  permission: string,
  approving = APPROVING.test(permission),
): 'Administration' | 'Approval' | null {
  if (ADMINISTERING.test(permission) && !/_VIEW$/.test(permission)) {
    return 'Administration';
  }
  return approving ? 'Approval' : null;
}

/** Added and removed permissions against the current ones, in name order. */
export function permissionDiff(current: readonly string[], selected: readonly string[]) {
  const byName = (a: string, b: string) => permissionLabel(a).localeCompare(permissionLabel(b));
  return {
    added: selected.filter((p) => !current.includes(p)).sort(byName),
    removed: current.filter((p) => !selected.includes(p)).sort(byName),
  };
}

/** Permissions grouped by area in picker order (the summary chips). */
export function byArea(permissions: readonly string[]): [string, string[]][] {
  const map = new Map<string, string[]>();
  permissions.forEach((p) => map.set(pickerArea(p), [...(map.get(pickerArea(p)) ?? []), p]));
  return [...map.entries()].sort(([a], [b]) => ORDER.indexOf(a) - ORDER.indexOf(b));
}
