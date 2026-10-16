import { humanize } from '@/utils/format';
import { permissionDescription, permissionLabel } from '@/utils/permissionLabel';

/** One permission of a list: its code, functional area and where it comes from. */
export interface PermissionRow {
  permission: string;
  area?: string | null;
  /** Names of the group profiles that grant it (the user's effective permissions). */
  groupProfiles?: readonly string[];
  /** Action classes (VIEW, CREATE, AMEND, APPROVE) of the permission. */
  actions?: readonly string[];
}

/** Area of a permission in words; an unclassified permission is listed under General. */
export function areaOf(row: PermissionRow): string {
  return row.area ? humanize(row.area) : 'General';
}

/** The rows by area, then by permission name. */
export function sortPermissions<T extends PermissionRow>(rows: readonly T[]): T[] {
  return [...rows].sort(
    (a, b) =>
      areaOf(a).localeCompare(areaOf(b)) ||
      permissionLabel(a.permission).localeCompare(permissionLabel(b.permission)),
  );
}

/** Whether a permission matches the search (name, description, area or granting profile). */
export function matchesPermission(row: PermissionRow, text: string): boolean {
  const needle = text.trim().toLowerCase();
  if (needle === '') {
    return true;
  }
  return [
    row.permission,
    permissionLabel(row.permission),
    permissionDescription(row.permission),
    areaOf(row),
    ...(row.groupProfiles ?? []),
  ].some((v) => v.toLowerCase().includes(needle));
}
