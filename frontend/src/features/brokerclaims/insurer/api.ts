import { api, toQuery } from '@/api/client';

/**
 * Insurer side of a claim API (BRCLM.018/023/024/041/043): insurer lines, reserve history,
 * insurer updates and the loss advice, under /api/v1/broker-claims/{id}.
 */

export interface InsurerLine {
  id: number;
  insurerCode: string;
  insurerName?: string;
  sharePct?: number;
  insurerClaimNo?: string;
  reportedToInsurerOn?: string;
  reserveAmount?: number;
  settledAmount?: number;
  adjusterCode?: string;
  adjusterLabel?: string;
}

export interface ReserveChange {
  id: number;
  insurerClaimId: number;
  previousAmount?: number;
  newAmount: number;
  reason: string;
  changedBy: string;
  changedAt: string;
}

export interface InsurerUpdate {
  id: number;
  insurerClaimId?: number;
  updateDate: string;
  source: string;
  reference?: string;
  remarks: string;
  attachmentIds: number[];
  correctsUpdateId?: number;
  uploadRef?: string;
  recordedBy: string;
  recordedAt: string;
}

export interface NewInsurerLine {
  insurerCode: string;
  sharePct?: number;
  insurerClaimNo?: string;
  reportedToInsurerOn?: string;
  reserveAmount?: number;
}

export interface NewUpdate {
  insurerClaimId?: number;
  updateDate: string;
  source: string;
  reference?: string;
  remarks: string;
  attachmentIds: number[];
  correctsUpdateId?: number;
}

export interface AdviceDraft {
  insurerCode: string;
  insurerName: string;
  suggestedTo: string[];
  subject: string;
  body: string;
}

export interface AdviceRecipient {
  insurerCode: string;
  to: string[];
  cc: string[];
  subject?: string;
  body?: string;
}

export interface AdviceSent {
  insurerCode: string;
  messageId: number;
  attachmentId: number;
  fileName: string;
}

const base = (claimId: number) => `/broker-claims/${claimId}`;

export const insurerApi = {
  lines: (companyId: number, claimId: number) =>
    api.get<InsurerLine[]>(`${base(claimId)}/insurers${toQuery({ companyId })}`),
  add: (companyId: number, claimId: number, line: NewInsurerLine, confirmReuse = false) =>
    api.post<InsurerLine[]>(
      `${base(claimId)}/insurers${toQuery({ companyId, confirmReuse })}`,
      line,
    ),
  number: (
    companyId: number,
    claimId: number,
    lineId: number,
    body: { insurerClaimNo: string; reportedToInsurerOn?: string; confirmReuse: boolean },
  ) =>
    api.post<InsurerLine[]>(
      `${base(claimId)}/insurers/${lineId}/number${toQuery({ companyId })}`,
      body,
    ),
  share: (companyId: number, claimId: number, lineId: number, sharePct: number) =>
    api.put<InsurerLine[]>(`${base(claimId)}/insurers/${lineId}/share${toQuery({ companyId })}`, {
      sharePct,
    }),
  reserve: (companyId: number, claimId: number, lineId: number, amount: number, reason: string) =>
    api.post<InsurerLine[]>(
      `${base(claimId)}/insurers/${lineId}/reserve${toQuery({ companyId })}`,
      { amount, reason },
    ),
  adjuster: (companyId: number, claimId: number, lineId: number, adjusterCode: string) =>
    api.post<InsurerLine[]>(
      `${base(claimId)}/insurers/${lineId}/adjuster${toQuery({ companyId })}`,
      { adjusterCode },
    ),
  reserveHistory: (companyId: number, claimId: number) =>
    api.get<ReserveChange[]>(`${base(claimId)}/insurers/reserve-history${toQuery({ companyId })}`),
  updates: (companyId: number, claimId: number) =>
    api.get<InsurerUpdate[]>(`${base(claimId)}/updates${toQuery({ companyId })}`),
  recordUpdate: (companyId: number, claimId: number, update: NewUpdate) =>
    api.post<InsurerUpdate>(`${base(claimId)}/updates${toQuery({ companyId })}`, update),
  adviceDrafts: (companyId: number, claimId: number) =>
    api.get<AdviceDraft[]>(`${base(claimId)}/loss-advice${toQuery({ companyId })}`),
  sendAdvice: (companyId: number, claimId: number, recipients: AdviceRecipient[]) =>
    api.post<AdviceSent[]>(`${base(claimId)}/loss-advice${toQuery({ companyId })}`, {
      recipients,
    }),
};
