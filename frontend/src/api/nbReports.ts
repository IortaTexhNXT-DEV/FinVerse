import { api, toQuery } from './client';

/** A count by status, stage or funnel step of the NB dashboard. */
export interface StatusCount {
  group: string;
  code: string;
  label: string;
  count: number;
}

/** Open accounts of one stage by time spent in it. */
export interface StageAgeing {
  stage: string;
  label: string;
  upToOneDay: number;
  upToThreeDays: number;
  upToSevenDays: number;
  overSevenDays: number;
  overdue: number;
}

/** Production of a sales unit against its target (BRNB.075). */
export interface UnitProduction {
  level: string;
  code: string;
  name: string;
  bookings: number;
  premium: number;
  commission: number;
  targetCount: number;
  targetPremium: number;
  targetCommission: number;
  achievement: number | null;
}

/** The New Business dashboard (BRNB.012). */
export interface NbDashboard {
  asOf: string;
  requests: StatusCount[];
  quotationsSent: number;
  awaitingClient: number;
  accounts: StatusCount[];
  overdue: StatusCount[];
  booked: { count: number; premium: number; commission: number };
  funnel: StatusCount[];
  ageing: StageAgeing[];
  production: UnitProduction[];
}

/** A saved report variant (BRNB.057). */
export interface ReportVariant {
  id: number;
  reportCode: string;
  name: string;
  owner: string;
  shared: boolean;
  mine: boolean;
  parameters: Record<string, string>;
}

export type UnitLevel = 'REGION' | 'DEPARTMENT' | 'TEAM' | 'OFFICER';

export const UNIT_LEVELS: UnitLevel[] = ['REGION', 'DEPARTMENT', 'TEAM', 'OFFICER'];

/** A production target of a sales unit for a period (PHP). */
export interface SalesTarget {
  id?: number;
  unitLevel: UnitLevel;
  unitCode: string;
  periodFrom: string;
  periodTo: string;
  targetCount: number;
  targetPremium: number;
  targetCommission: number;
  currency?: string;
}

export const nbReportsApi = {
  dashboard: (companyId: number, asOf?: string) =>
    api.get<NbDashboard>(`/nb/dashboard${toQuery({ companyId, asOf })}`),
  variants: (reportCode: string) =>
    api.get<ReportVariant[]>(`/nb/report-variants${toQuery({ reportCode })}`),
  saveVariant: (body: {
    reportCode: string;
    name: string;
    parameters: Record<string, string>;
    shared: boolean;
  }) => api.post<ReportVariant>('/nb/report-variants', body),
  deleteVariant: (id: number) => api.delete(`/nb/report-variants/${id}`),
  targets: (companyId: number, from: string, to: string) =>
    api.get<SalesTarget[]>(`/nb/targets${toQuery({ companyId, from, to })}`),
  saveTarget: (companyId: number, target: SalesTarget) =>
    api.put<SalesTarget>(`/nb/targets${toQuery({ companyId })}`, target),
};
