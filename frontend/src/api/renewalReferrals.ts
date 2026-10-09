import { api, toQuery } from './client';

/**
 * Transfer requests for a New Business opportunity, Transfer Request Monitoring and the approver
 * of a submission for posting (BDOI Renewal FRS FRRN.011, FRRN.016.01).
 */
export interface ReferralView {
  id: number;
  referralNo: string;
  renewalRef: string;
  assuredName: string | null;
  fromUnit: string | null;
  toUnit: string;
  justification: string;
  status: string;
  statusLabel: string;
  requestedBy: string;
  requestedAt: string;
  decidedBy: string | null;
  decidedAt: string | null;
  decisionRemarks: string | null;
  nbArn: string | null;
  nbCreatedAt: string | null;
}

export type ReferralOutcome = 'ACCEPTED' | 'REJECTED' | 'RETURNED';

export interface NewBusinessInput {
  productCode: string | null;
  marketSegment: string | null;
  periodFrom: string | null;
  periodTo: string | null;
  accountOfficer: string | null;
  copyRiskItems: boolean;
}

export interface PostingApprovers {
  mode: 'REQUIRED' | 'OPTIONAL' | 'NONE';
  approvers: { username: string; fullName: string }[];
}

const R = '/api/v1/renewal/referrals';
const co = (companyId: number) => toQuery({ companyId });

export const renewalReferralsApi = {
  modes: (companyId: number) =>
    api.get<{ referral: boolean; transfer: boolean }>(`${R}/modes${co(companyId)}`),
  list: (companyId: number) => api.get<ReferralView[]>(`${R}${co(companyId)}`),
  ofRenewal: (companyId: number, renewalRef: string) =>
    api.get<ReferralView[]>(`${R}/of/${encodeURIComponent(renewalRef)}${co(companyId)}`),
  request: (
    companyId: number,
    body: { renewalRef: string; toUnit: string; justification: string; submit: boolean },
  ) => api.post<ReferralView>(`${R}${co(companyId)}`, body),
  submit: (companyId: number, id: number, text: string) =>
    api.post<ReferralView>(`${R}/${String(id)}/submit${co(companyId)}`, { text }),
  decide: (companyId: number, id: number, outcome: ReferralOutcome, remarks: string) =>
    api.post<ReferralView>(`${R}/${String(id)}/decision${co(companyId)}`, { outcome, remarks }),
  cancel: (companyId: number, id: number) =>
    api.post<ReferralView>(`${R}/${String(id)}/cancel${co(companyId)}`, {}),
  proposal: (companyId: number, id: number) =>
    api.get<NewBusinessInput>(`${R}/${String(id)}/new-business${co(companyId)}`),
  createNewBusiness: (companyId: number, id: number, input: NewBusinessInput) =>
    api.post<ReferralView>(`${R}/${String(id)}/new-business${co(companyId)}`, input),
  postingApprovers: (companyId: number, renewalRef?: string) =>
    api.get<PostingApprovers>(
      `/api/v1/renewal/posting-approvers${toQuery({ companyId, renewalRef })}`,
    ),
};
