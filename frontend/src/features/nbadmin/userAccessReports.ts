/** A user access report of the report catalogue (category Control & Audit, UAM_REPORT_VIEW). */
export interface UserAccessReportLink {
  code: string;
  name: string;
  text: string;
  brd: string;
  parameters: string;
}

/** The five user access reports (USER_ACCESS_DESIGN section 11.1; FRS BRD-11 section 6). */
export const USER_ACCESS_REPORTS: readonly UserAccessReportLink[] = [
  {
    code: 'UAM-USER-ACCESS',
    name: 'User Access Report',
    text: 'Users with their group profiles, business unit, level and status, who created and last modified them (approver and request number) and the last action.',
    brd: 'BRD 3.003.1',
    parameters: 'as of date, business unit, status, group profile',
  },
  {
    code: 'UAM-GROUP-PROFILE',
    name: 'User Group Profile Report',
    text: 'For each group profile, the modules and tasks marked With Access or No Access, with its created and modified dates and actors.',
    brd: 'BRD 3.003.2',
    parameters: 'group profile, module, active',
  },
  {
    code: 'UAM-GROUP-MEMBERS',
    name: 'Group Profile Membership',
    text: 'The members of each group profile on a date, with who added each member and when.',
    brd: 'BRD 3.003.3',
    parameters: 'group profile, as of date',
  },
  {
    code: 'UAM-AUDIT-LOG',
    name: 'User Access Audit Log',
    text: 'Every access activity with its from and to values, done by, approved by and request number; log-ins, failed log-ins and log-outs on request.',
    brd: 'BRD 4.003.1',
    parameters: 'date from / to, user, activity, log-ins and log-outs',
  },
  {
    code: 'UAM-REQUESTS',
    name: 'Access Requests',
    text: 'Access requests by status, type, requester and approver, with their age in days.',
    brd: 'BRD 1.008',
    parameters: 'date from / to, status, request type',
  },
];
