import type { SalesOfficer, SalesOrganisation, SalesUnit } from '@/api/catalog';

/** A sales unit with its sub-units and, for teams, its account officers. */
export interface SalesNode {
  unit: SalesUnit;
  children: SalesNode[];
  officers: SalesOfficer[];
  /** Cost center of the unit, else of the nearest parent that has one. */
  costCenter?: string;
}

/**
 * Builds the region → department → team tree. Units whose parent is missing are shown at the
 * top so nothing is hidden; cost centers are inherited from the nearest parent.
 */
export function salesTree(org: SalesOrganisation): SalesNode[] {
  const codes = new Set(org.units.map((u) => u.code));
  const build = (unit: SalesUnit, inherited?: string): SalesNode => {
    const costCenter = unit.costCenter ?? inherited;
    return {
      unit,
      costCenter,
      officers: org.officers.filter((o) => o.teamCode === unit.code),
      children: org.units
        .filter((u) => u.parentCode === unit.code)
        .map((u) => build(u, costCenter)),
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
