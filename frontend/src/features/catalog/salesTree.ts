import type { SalesLevel, SalesOfficer, SalesOrganisation, SalesUnit } from '@/api/catalog';
import type { TreeNode } from '@/components/ui/treeRows';

/** A sales unit with its sub-units and, for teams, its account officers. */
export interface SalesNode {
  unit: SalesUnit;
  children: SalesNode[];
  officers: SalesOfficer[];
  /** Cost center of the unit, else of the nearest parent that has one. */
  costCenter?: string;
  /** Code of the unit whose cost center applies (the unit itself when it has its own). */
  costCenterFrom?: string;
}

/**
 * Builds the region → department → team tree. Units whose parent is missing are shown at the
 * top so nothing is hidden; cost centers are inherited from the nearest parent.
 */
export function salesTree(org: SalesOrganisation): SalesNode[] {
  const codes = new Set(org.units.map((u) => u.code));
  const build = (unit: SalesUnit, inherited?: string, inheritedFrom?: string): SalesNode => {
    const own = Boolean(unit.costCenter);
    const costCenter = own ? unit.costCenter : inherited;
    const costCenterFrom = own ? unit.code : inheritedFrom;
    return {
      unit,
      costCenter,
      costCenterFrom,
      officers: org.officers.filter((o) => o.teamCode === unit.code),
      children: org.units
        .filter((u) => u.parentCode === unit.code)
        .map((u) => build(u, costCenter, costCenterFrom)),
    };
  };
  return org.units.filter((u) => !u.parentCode || !codes.has(u.parentCode)).map((u) => build(u));
}

/** Units of a level (parents offered when adding a unit). */
export function unitsOf(
  org: SalesOrganisation | undefined,
  level: SalesUnit['level'],
): SalesUnit[] {
  return (org?.units ?? []).filter((u) => u.level === level);
}

/** Level of the sub-units of a unit (a team has none). */
export const CHILD_LEVEL: Record<SalesLevel, SalesLevel | undefined> = {
  REGION: 'DEPARTMENT',
  DEPARTMENT: 'TEAM',
  TEAM: undefined,
};

export const LEVEL_LABEL: Record<SalesLevel, string> = {
  REGION: 'Region',
  DEPARTMENT: 'Department',
  TEAM: 'Team',
};

/** One row of the organisation tree table: a unit or an account officer of a team. */
export type OrgRow =
  | {
      kind: 'unit';
      node: SalesNode;
      /** Officers of the team that are not removed. */
      activeOfficers: number;
      /** Sub-units that are not inactive. */
      activeSubUnits: number;
    }
  | { kind: 'officer'; officer: SalesOfficer; team: SalesUnit };

export const unitKey = (code: string) => `unit:${code}`;
export const officerKey = (id: number) => `officer:${String(id)}`;

const isActive = (r: { recordStatus: string }) => r.recordStatus !== 'INACTIVE';

interface OrgFilter {
  /** Show inactive units and removed officers. */
  showInactive: boolean;
  /** Search text on unit code and name and officer name. */
  search: string;
  /** Display name of a login id. */
  nameOf: (login: string) => string;
}

/** The rows of the tree table, and the rows to expand so every search match is visible. */
export interface OrgView {
  nodes: TreeNode<OrgRow>[];
  /** Keys of the rows with a match below them. */
  matchPath: string[];
  counts: { regions: number; departments: number; teams: number; officers: number };
}

interface Built {
  node: TreeNode<OrgRow>;
  /** A search match is at or below this row. */
  hit: boolean;
}

/**
 * The organisation as tree-table rows: inactive records only when asked; with a search, the
 * matching units and officers with their parents (a matching unit keeps its whole branch), the
 * branches leading to a match listed in `matchPath` so they can be expanded.
 */
export function orgView(tree: readonly SalesNode[], filter: OrgFilter): OrgView {
  const needle = filter.search.trim().toLowerCase();
  const matchPath: string[] = [];
  const counts = { regions: 0, departments: 0, teams: 0, officers: 0 };
  const unitMatches = (u: SalesUnit) =>
    needle !== '' && `${u.code} ${u.name}`.toLowerCase().includes(needle);
  const officerMatches = (o: SalesOfficer) =>
    needle !== '' && filter.nameOf(o.username).toLowerCase().includes(needle);

  const officerRow = (o: SalesOfficer, team: SalesUnit, keepAll: boolean): Built | null => {
    const hit = officerMatches(o);
    if (needle !== '' && !keepAll && !hit) {
      return null;
    }
    return { node: { key: officerKey(o.id), row: { kind: 'officer', officer: o, team } }, hit };
  };

  const build = (n: SalesNode, ancestorHit: boolean): Built | null => {
    const selfHit = unitMatches(n.unit);
    const keepAll = needle === '' || ancestorHit || selfHit;
    const officers = n.officers
      .filter((o) => filter.showInactive || isActive(o))
      .map((o) => officerRow(o, n.unit, keepAll))
      .filter((b): b is Built => b !== null);
    const units = n.children
      .filter((c) => filter.showInactive || isActive(c.unit))
      .map((c) => build(c, keepAll && needle !== ''))
      .filter((b): b is Built => b !== null);
    const below = [...officers, ...units];
    const hitBelow = below.some((b) => b.hit);
    if (!keepAll && !hitBelow) {
      return null;
    }
    const key = unitKey(n.unit.code);
    if (hitBelow) {
      matchPath.push(key);
    }
    return {
      hit: selfHit || hitBelow,
      node: {
        key,
        row: {
          kind: 'unit',
          node: n,
          activeOfficers: n.officers.filter(isActive).length,
          activeSubUnits: n.children.filter((c) => isActive(c.unit)).length,
        },
        children: below.map((b) => b.node),
      },
    };
  };

  const nodes = tree
    .filter((n) => filter.showInactive || isActive(n.unit))
    .map((n) => build(n, false))
    .filter((b): b is Built => b !== null)
    .map((b) => b.node);
  const count = (list: readonly TreeNode<OrgRow>[]) => {
    list.forEach((n) => {
      const row = n.row;
      if (row.kind === 'officer') {
        counts.officers += 1;
      } else if (row.node.unit.level === 'REGION') {
        counts.regions += 1;
      } else if (row.node.unit.level === 'DEPARTMENT') {
        counts.departments += 1;
      } else {
        counts.teams += 1;
      }
      count(n.children ?? []);
    });
  };
  count(nodes);
  return { nodes, matchPath, counts };
}

/** What the Add dialog creates: a unit, or an officer placed in (or moved to) a team. */
export interface AddForm {
  mode: 'unit' | 'officer';
  level: SalesLevel;
  code: string;
  name: string;
  parentCode: string;
  costCenter: string;
  username: string;
  /** Moving an officer already in a team (Reassign): the user is fixed. */
  reassign?: boolean;
}

export const BLANK_FORM: AddForm = {
  mode: 'unit',
  level: 'REGION',
  code: '',
  name: '',
  parentCode: '',
  costCenter: '',
  username: '',
};

/** A unit or officer action that needs a reason: deactivate, reactivate, remove from team. */
export type ReasonAction =
  | { kind: 'deactivate'; node: SalesNode }
  | { kind: 'reactivate'; node: SalesNode }
  | { kind: 'remove'; officer: SalesOfficer };

const plural = (count: number, singular: string) => {
  const noun = count === 1 ? singular : singular + 's';
  return `${String(count)} ${noun}`;
};

/** What still hangs below a unit and blocks its deactivation, e.g. "2 active account officers". */
export function blockers(node: SalesNode): string[] {
  const subUnits = node.children.filter((c) => isActive(c.unit)).length;
  const officers = node.officers.filter(isActive).length;
  return [
    ...(subUnits > 0 ? [plural(subUnits, 'active sub-unit')] : []),
    ...(officers > 0 ? [plural(officers, 'active account officer')] : []),
  ];
}
