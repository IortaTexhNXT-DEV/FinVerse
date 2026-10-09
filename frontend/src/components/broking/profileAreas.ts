/**
 * Business areas of the group profiles (profile picker): each profile sits in the area where it
 * does its work, read from the permissions that act (create, maintain, process, approve), not from
 * the view and common permissions every profile holds. A profile that only views, or that acts in
 * many areas without a main one (auditor, system administrator, management committee), sits under
 * Administration & Control.
 */

/** The profile fields the picker reads (both the access-request and the user-maintenance APIs). */
export interface ProfileInfo {
  code: string;
  name: string;
  permissions: readonly string[];
  active?: boolean;
  description?: string;
  privilegeLevel?: string;
  /** Product module the server names for the profile, used when no area is found. */
  module?: string;
}

export const ADMINISTRATION = 'Administration & Control';

/** Business areas in menu order, each with the permission prefixes of its work. */
export const PROFILE_AREAS: readonly { area: string; prefixes: readonly string[] }[] = [
  {
    area: 'Client & Sales',
    prefixes: ['CLIENT_', 'QUOTE', 'QUOTATION', 'PROPOSAL', 'PRF_', 'NB_', 'ACCOUNT_MAINTAIN'],
  },
  { area: 'Product & Technical Services', prefixes: ['PKG_', 'PRODUCT_', 'INCENTIVE_'] },
  { area: 'Employee Benefits', prefixes: ['EB_'] },
  {
    area: 'Placement, Issuance & Booking',
    prefixes: ['ACCOUNT_PROCESS', 'PLACEMENT', 'ISSUANCE', 'EPOLICY', 'BOOKING', 'BILLING_'],
  },
  { area: 'Renewal', prefixes: ['RNW_'] },
  { area: 'Submitted Policies', prefixes: ['SBM_', 'IAAF_'] },
  { area: 'Adjustment & Reconciliation', prefixes: ['ADJ_', 'RECON_', 'PRODRECON'] },
  { area: 'Contact Centre', prefixes: ['CSF_'] },
  { area: 'Claims Handling', prefixes: ['BCL_'] },
  { area: 'Collections', prefixes: ['CLX_'] },
  {
    area: 'Cashiering, Remittance & Commission',
    prefixes: ['CASH_', 'REMIT', 'COMMREC', 'COMMISSION', 'DP_', 'BIR_', 'OPS_'],
  },
  { area: 'Disbursement & Payment Requests', prefixes: ['DISB_', 'PRQ_'] },
  {
    area: 'Accounting & Finance',
    prefixes: [
      'ACSL_',
      'JOURNAL_',
      'GL_',
      'FRBS_',
      'COA_',
      'ASSET_',
      'BUDGET_',
      'INVESTMENT_',
      'ACCOUNTING_',
      'RECEIPT_PAYMENT',
      'TAX_',
      'PAYABLE',
      'RECEIVABLE',
      'BANK_',
      'PERIOD_',
      'FX_',
      'EMPLOYEE_',
      'SERVICE_FEE',
      'CWT_',
      'REVALUATION',
    ],
  },
  { area: 'Compliance & Screening', prefixes: ['SCR_', 'AML_'] },
  { area: 'Data Migration & Legacy', prefixes: ['MIG_', 'LEGACY_'] },
];

/** Permissions every profile holds or that only open a list, not counted for the area. */
const COMMON = new Set([
  'ATTACHMENT_MANAGE',
  'ATTACHMENT_VIEW',
  'DASHBOARD_VIEW',
  'REPORT_VIEW',
  'WORK_VIEW',
  'MASTER_VIEW',
  'BULK_PROCESS',
  'ALERT_VIEW',
]);

/** Permissions that approve, authorise or sign off (a checker or an approver). */
const APPROVING = /_(APPROVE|AUTHORIZE|AUTHORISE|SIGNOFF|DECIDE|COMMITTEE)$|^ACCESS_APPROVE$/;

/** Permissions that only read (views, reports, exports, monitoring). */
const READING = /_(VIEW|EXPORT|MONITOR)$|^REPORT_/;

/**
 * Permissions of administration and control: user access, security, set-up of the modules, lists
 * of values, system parameters, interfaces, audit and the management committee sign-off.
 */
const CONTROL =
  /^(UAM_|ACCESS_|USER_|ROLE_|LOV_|SYSTEM_|MFA_|MODULE_SWITCH|FLOWIN_|SECURITY_|FILE_|AUDIT_|ALERT_MANAGE)|_SETUP$|_MANCOM_/;

/**
 * Permissions other areas' profiles hold for a hand-off (an officer raising a request, accepting a
 * renewal, working a collection): they never make a profile belong to their area.
 */
const SATELLITE = new Set([
  'ACCOUNT_MAINTAIN',
  'ADJ_REQUEST',
  'BIR_CERT_ACK',
  'BOOKING_ADJUST',
  'CASH_UPP_INCOME_REQUEST',
  'CLX_ESCALATE',
  'CLX_UNAPPLIED_WORK',
  'CLX_WORK',
  'LEGACY_REVERSAL_REQUEST',
  'MIG_TRUEUP_PREPARE',
  'PKG_REQUEST',
  'PRQ_CREATE',
  'RNW_ACCEPT',
  'RNW_ASSIGN',
  'RNW_DISPOSE',
  'RNW_RA_GENERATE',
  'RNW_RA_SEND',
  'SBM_MAINTAIN',
  'TOR_PREPARE',
  'WORK_ASSIGN',
]);

/** Whether a permission acts (anything but reading and the common permissions). */
export function isActing(permission: string): boolean {
  return !COMMON.has(permission) && !READING.test(permission);
}

/** The business area of one permission, or null when it belongs to no business area. */
export function areaOfPermission(permission: string): string | null {
  if (CONTROL.test(permission)) {
    return ADMINISTRATION;
  }
  const found = PROFILE_AREAS.find((a) => a.prefixes.some((p) => permission.startsWith(p)));
  return found?.area ?? null;
}

/** The area holding most of the permissions (the earlier area on a near tie), with its count. */
function mostHeld(permissions: readonly string[]): [string, number] | undefined {
  const counts = new Map<string, number>();
  permissions.forEach((p) => {
    const area = areaOfPermission(p);
    if (area !== null) {
      counts.set(area, (counts.get(area) ?? 0) + 1);
    }
  });
  const top = Math.max(0, ...counts.values());
  // Areas within one permission of the most held are a near tie: the earlier area of the business
  // flow wins (a processing team that also processes renewals sits in Placement, Issuance & Booking).
  return [...counts.entries()]
    .filter(([, n]) => n >= top - 1)
    .sort((a, b) => areaRank(a[0]) - areaRank(b[0]))[0];
}

/**
 * The business area of a profile, from what it acts on: Employee Benefits for a profile working
 * the employee benefits cycle; else the area of most of its own acting permissions (hand-off
 * permissions of other areas not counted); a view-only profile sits in the area of its views when
 * one area holds most of them, else under Administration & Control (auditors, read-only users).
 */
export function areaOfProfile(profile: Pick<ProfileInfo, 'permissions'>): string {
  const acting = profile.permissions.filter(isActing);
  if (acting.some((p) => p.startsWith('EB_') && areaOfPermission(p) !== ADMINISTRATION)) {
    return 'Employee Benefits';
  }
  const own = mostHeld(acting.filter((p) => !SATELLITE.has(p))) ?? mostHeld(acting);
  if (own !== undefined) {
    return own[0];
  }
  const views = profile.permissions.filter((p) => !COMMON.has(p));
  const viewed = mostHeld(views);
  const classified = views.filter((p) => areaOfPermission(p) !== null).length;
  if (viewed !== undefined && viewed[1] >= 2 && viewed[1] * 5 >= classified * 3) {
    return viewed[0];
  }
  return ADMINISTRATION;
}

/** Order of an area in the picker (menu order, Administration & Control last). */
export function areaRank(area: string): number {
  const at = PROFILE_AREAS.findIndex((a) => a.area === area);
  return at < 0 ? PROFILE_AREAS.length : at;
}

/** The kind of work of a profile: approver (checker), maker, or view only. */
export function profileKind(profile: Pick<ProfileInfo, 'permissions'>): string {
  if (profile.permissions.some((p) => APPROVING.test(p))) {
    return 'Approver';
  }
  return profile.permissions.some(isActing) ? 'Maker' : 'View only';
}

/**
 * One line on what a profile does: its own description when it has one, else its kind, the
 * number of permissions and the areas it acts in ("Approver · 14 permissions · Disbursement &
 * Payment Requests").
 */
export function profileSummary(profile: ProfileInfo): string {
  if (profile.description && profile.description.trim() !== '') {
    return profile.description.trim();
  }
  const areas = [
    ...new Set(
      profile.permissions
        .filter(isActing)
        .map(areaOfPermission)
        .filter((a): a is string => a !== null),
    ),
  ].sort((a, b) => areaRank(a) - areaRank(b));
  const count = profile.permissions.length;
  const parts = [
    profileKind(profile),
    `${String(count)} ${count === 1 ? 'permission' : 'permissions'}`,
  ];
  if (areas.length > 0) {
    parts.push(areas.length > 2 ? `${areas.slice(0, 2).join(', ')} and more` : areas.join(', '));
  }
  return parts.join(' · ');
}

/** A group of the picker: an area and its profiles by name. */
export interface ProfileGroup<P extends ProfileInfo> {
  area: string;
  profiles: P[];
}

/**
 * The active profiles matching a search (name, code, area or description), grouped by business
 * area in menu order, the profiles by name; selected profiles always stay listed.
 */
export function groupProfiles<P extends ProfileInfo>(
  profiles: readonly P[],
  search: string,
  selected: readonly string[],
): ProfileGroup<P>[] {
  const term = search.trim().toLowerCase();
  const groups = new Map<string, P[]>();
  profiles
    .filter((p) => p.active !== false || selected.includes(p.code))
    .filter((p) => {
      if (term === '' || selected.includes(p.code)) {
        return true;
      }
      const text = `${p.name} ${p.code} ${areaOfProfile(p)} ${profileSummary(p)}`;
      return text.toLowerCase().includes(term);
    })
    .sort((a, b) => a.name.localeCompare(b.name))
    .forEach((p) => {
      const area = areaOfProfile(p);
      groups.set(area, [...(groups.get(area) ?? []), p]);
    });
  return [...groups.entries()]
    .sort(([a], [b]) => areaRank(a) - areaRank(b))
    .map(([area, list]) => ({ area, profiles: list }));
}

/** A separation-of-duties rule: two profiles one user may not hold together. */
export interface ProfileConflictRule {
  profileA: string;
  profileB: string;
  description: string;
  status?: string;
}

/** A conflict between two selected profiles. */
export interface ProfileConflict {
  a: string;
  b: string;
  reason: string;
}

/** The active rules broken by the selected profiles. */
export function profileConflicts(
  selected: readonly string[],
  rules: readonly ProfileConflictRule[],
): ProfileConflict[] {
  const chosen = new Set(selected);
  return rules
    .filter((r) => (r.status ?? 'ACTIVE') === 'ACTIVE')
    .filter((r) => chosen.has(r.profileA) && chosen.has(r.profileB))
    .map((r) => ({ a: r.profileA, b: r.profileB, reason: r.description }));
}

/** How a profile of a change request moves: kept, added or removed against the user's current ones. */
export function profileChange(
  code: string,
  selected: readonly string[],
  current: readonly string[],
): 'current' | 'added' | 'removed' | 'none' {
  const isSelected = selected.includes(code);
  const isCurrent = current.includes(code);
  if (isSelected && isCurrent) {
    return 'current';
  }
  if (isSelected) {
    return 'added';
  }
  return isCurrent ? 'removed' : 'none';
}

/** The key permissions of a profile: what it approves, what else it does, what it only sees. */
export function keyPermissions(permissions: readonly string[]) {
  const approving = (p: string) => APPROVING.test(p);
  return {
    approves: permissions.filter(approving),
    does: permissions.filter((p) => isActing(p) && !approving(p)),
    sees: permissions.filter((p) => !isActing(p)),
  };
}
