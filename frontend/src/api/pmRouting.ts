import { api, toQuery } from './client';
import type { RequestDetails } from './productmaintTypes';
import type { PageResponse } from './types';

/** One Marketing approval of a package request. */
export interface MarketingStep {
  level: number;
  approver: string;
  decision: string;
  remarks: string | null;
  decidedAt: string;
}

/** One ManCom approval task. */
export interface ManComStep {
  round: number;
  approver: string;
  president: boolean;
  status: 'WAITING' | 'PENDING' | 'APPROVED' | 'RETURNED' | 'REJECTED' | 'CLOSED';
  remarks: string | null;
  decidedAt: string | null;
}

/** The routing of a package request (BDOI FRS FRPM.011.02, FRPM.014.01, FRPM.015.01). */
export interface RoutingView {
  source: string;
  details: RequestDetails | null;
  marketing: { chained: boolean; levelsGiven: number; history: MarketingStep[] };
  mancom: { selectedRouting: boolean; president: string; members: string[]; tasks: ManComStep[] };
  deployment: {
    /** AUTO or MANUAL. */
    mode: string;
    number: string | null;
    requestedAt: string | null;
    requestedBy: string | null;
  };
}

export type ManComDecision = 'APPROVE' | 'RETURN' | 'REJECT';

/** A product master change sent to the other BDOI systems (BDOI FRS FRPM.029.01). */
export interface MasterChange {
  id: number;
  productCode: string;
  versionNo: number | null;
  changeKind: string;
  effectiveDate: string | null;
  sourceRequestNo: string | null;
  status: 'PENDING' | 'SENT' | 'FAILED';
  attempts: number;
  fileName: string | null;
  sentAt: string | null;
  error: string | null;
  recordedAt: string;
}

export interface MasterMonitor {
  target: string;
  connected: boolean;
  pending: number;
  failed: number;
  changes: PageResponse<MasterChange>;
}

export interface TransferRun {
  fileName: string | null;
  sent: number;
  waiting: number;
  target: string;
}

export interface InboxFile {
  id: number;
  fileName: string;
  recordCount: number;
  receivedAt: string;
  content: string;
}

const req = (id: number) => `/product-maintenance/requests/${id}`;
const master = '/product-maintenance/master-changes';

/** Routing of package requests and the product master changes. */
export const pmRoutingApi = {
  routing: (id: number) => api.get<RoutingView>(`${req(id)}/routing`),
  selectManCom: (id: number, approvers: string[]) =>
    api.post<RoutingView>(`${req(id)}/mancom-approvers`, { approvers }),
  decideManCom: (id: number, decision: ManComDecision, remarks?: string) =>
    api.post<RoutingView>(`${req(id)}/mancom-decision`, { decision, remarks }),
  requestDeployment: (id: number, text?: string) =>
    api.post<RoutingView>(`${req(id)}/request-deployment`, { text }),
  productApprovers: () => api.get<string[]>('/product-maintenance/product-approvers'),
  masterChanges: (status?: string, page = 0) =>
    api.get<MasterMonitor>(`${master}${toQuery({ status, page, size: 20 })}`),
  transferNow: () => api.post<TransferRun>(`${master}/transfer`),
  reprocess: (id: number) => api.post<TransferRun>(`${master}/${id}/reprocess`),
  receivedFiles: () => api.get<InboxFile[]>(`${master}/simulator/files`),
};
