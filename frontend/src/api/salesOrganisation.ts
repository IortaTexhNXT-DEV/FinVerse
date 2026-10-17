import type { Authorizable } from './catalog';

/** Sales organisation of the catalog module: regions, departments, teams and account officers. */
export type SalesLevel = 'REGION' | 'DEPARTMENT' | 'TEAM';

export interface SalesUnitInput {
  companyId: number;
  level: SalesLevel;
  code: string;
  name: string;
  parentCode?: string;
  costCenter?: string;
}

export interface SalesUnit extends Authorizable {
  level: SalesLevel;
  code: string;
  name: string;
  parentCode?: string;
  costCenter?: string;
  headUsername?: string;
  /** Reason of the last deactivation or reactivation. */
  statusReason?: string;
  createdBy?: string;
  createdAt?: string;
  lastChangedAt?: string;
  authorizedAt?: string;
}

export interface SalesOfficer extends Authorizable {
  teamCode: string;
  username: string;
  /** Date of the current team assignment. */
  assignedSince?: string;
  /** Reason of the removal from the team. */
  statusReason?: string;
  lastChangedAt?: string;
  authorizedAt?: string;
}

export interface SalesOrganisation {
  units: SalesUnit[];
  officers: SalesOfficer[];
}

export interface SalesAssignment {
  region?: string;
  department?: string;
  team?: string;
  costCenter?: string;
}
