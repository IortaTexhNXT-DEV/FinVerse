import type { RoleInfo } from '@/api/nbadmin';

/** Module heading of the profiles that belong to no product module. */
const PLATFORM_MODULE = 'Platform and Administration';

/**
 * The active group profiles matching a search (name, code or module), grouped by product module in
 * module order, the profiles by name; the selected ones always stay listed.
 */
export function profileGroups(
  roles: readonly RoleInfo[],
  search: string,
  selected: readonly string[],
): { module: string; roles: RoleInfo[] }[] {
  const term = search.trim().toLowerCase();
  const groups = new Map<string, RoleInfo[]>();
  roles
    .filter((r) => r.active)
    .filter(
      (r) =>
        term === '' ||
        selected.includes(r.code) ||
        `${r.name} ${r.code} ${r.module ?? ''}`.toLowerCase().includes(term),
    )
    .sort((a, b) => a.name.localeCompare(b.name))
    .forEach((r) => {
      const module = r.module ?? PLATFORM_MODULE;
      groups.set(module, [...(groups.get(module) ?? []), r]);
    });
  return [...groups.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([module, list]) => ({ module, roles: list }));
}
