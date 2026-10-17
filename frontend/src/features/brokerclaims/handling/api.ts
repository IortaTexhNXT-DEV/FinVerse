import { api, toQuery } from '@/api/client';
import type { Claim } from '../record/api';

/**
 * Claim handling of the FRS BRD-07 Claims v1.0 in the BRD scope: follow-ups, document checklist,
 * motor details, return to the Account Officer, loss advice preview, repair shops and the
 * checklist setup.
 */

export interface FollowUpInput {
  type: string;
  actionTaken: string;
  nextPlanSummary?: string;
  nextPlanDetail?: string;
  statusCode?: string;
  settlementAmount?: number;
  adjusterEvaluation?: number;
  insurerDeductible?: number;
}

export interface FollowUp extends FollowUpInput {
  id: number;
  statusCode: string;
  followedUpOn: string;
  createdBy: string;
  createdAt: string;
}

export interface ChecklistItem {
  documentType: string;
  label: string;
  required: boolean;
  uploaded: boolean;
}

export interface DocumentRequirement {
  id: number;
  claimType: string;
  documentType: string;
  required: boolean;
  active: boolean;
}

export interface RepairShop {
  code: string;
  name: string;
}

const CLAIMS = '/broker-claims';
const one = (companyId: number, id: number, action: string) =>
  `${CLAIMS}/${String(id)}${action}${toQuery({ companyId })}`;

export const handlingApi = {
  followUps: (companyId: number, id: number) =>
    api.get<FollowUp[]>(one(companyId, id, '/follow-ups')),
  recordFollowUp: (companyId: number, id: number, input: FollowUpInput) =>
    api.post<FollowUp>(one(companyId, id, '/follow-ups'), input),
  checklist: (companyId: number, id: number) =>
    api.get<ChecklistItem[]>(one(companyId, id, '/document-checklist')),
  motor: (companyId: number, id: number, plateNo: string, preferredShopCode: string) =>
    api.put<Claim>(one(companyId, id, '/motor'), { plateNo, preferredShopCode }),
  returnToAo: (companyId: number, id: number, note: string) =>
    api.post<Claim>(one(companyId, id, '/return-to-ao'), { note }),
  advicePreview: (companyId: number, id: number, insurerCode: string) =>
    api.getFile(
      `${CLAIMS}/${String(id)}/loss-advice/preview${toQuery({ companyId, insurerCode })}`,
    ),
  repairShops: (companyId: number) =>
    api.get<RepairShop[]>(`${CLAIMS}/repair-shops${toQuery({ companyId })}`),
  requirements: () => api.get<DocumentRequirement[]>(`${CLAIMS}/setup/document-requirements`),
  saveRequirement: (input: Omit<DocumentRequirement, 'id'>) =>
    api.post<DocumentRequirement>(`${CLAIMS}/setup/document-requirements`, input),
};

/** Whether a follow-up can be saved: a type and the action taken today. */
export function followUpReady(input: Partial<FollowUpInput>): boolean {
  const amounts = [input.settlementAmount, input.adjusterEvaluation, input.insurerDeductible];
  return (
    Boolean(input.type) &&
    (input.actionTaken ?? '').trim() !== '' &&
    amounts.every((a) => a === undefined || a >= 0)
  );
}

/** The documents still to upload before the claim is filed with complete documents. */
export function missingDocuments(items: readonly ChecklistItem[]): string[] {
  return items.filter((i) => i.required && !i.uploaded).map((i) => i.label);
}

/** The follow-up of the dialog fields (blank texts and amounts left out). */
export function toFollowUpInput(form: Readonly<Record<string, string | undefined>>): FollowUpInput {
  const text = (key: string) => {
    const v = form[key]?.trim() ?? '';
    return v === '' ? undefined : v;
  };
  const amount = (key: string) => {
    const v = text(key);
    return v === undefined ? undefined : Number(v);
  };
  return {
    type: form.type ?? '',
    actionTaken: form.actionTaken ?? '',
    nextPlanSummary: text('nextPlanSummary'),
    nextPlanDetail: text('nextPlanDetail'),
    statusCode: text('statusCode'),
    settlementAmount: amount('settlementAmount'),
    adjusterEvaluation: amount('adjusterEvaluation'),
    insurerDeductible: amount('insurerDeductible'),
  };
}
