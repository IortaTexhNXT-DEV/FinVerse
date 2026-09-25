import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/**
 * Received BIR 2307 certificates (DIS 2.11, V702): the register of the creditable taxes that the
 * withholding agents (insurers) withheld from BDOI's commission and incentives.
 */

export type CertificateKind = 'COMMISSION' | 'INCENTIVE';
export type CertificateStatus = 'RECORDED' | 'CANCELLED';

export interface CertificateLine {
  lineNo?: number;
  kind: CertificateKind;
  atc: string;
  incomeNature?: string;
  income: number;
  tax: number;
}

export interface ReceivedCertificate {
  id: number;
  certificateNo: string;
  agentCode: string;
  agentName: string;
  agentTin?: string;
  periodFrom: string;
  periodTo: string;
  receivedOn: string;
  sourceModule?: string;
  sourceRef?: string;
  status: CertificateStatus;
  incomeTotal: number;
  taxTotal: number;
  journalBatchNo?: string;
  cancelJournalNo?: string;
  cancelReason?: string;
  remarks?: string;
  lines: CertificateLine[];
  recordedBy: string;
  recordedAt: string;
}

export interface CertificateDraft {
  certificateNo: string;
  agentCode: string;
  agentName: string;
  agentTin: string;
  periodFrom: string;
  periodTo: string;
  receivedOn: string;
  sourceRef: string;
  remarks: string;
  lines: {
    kind: CertificateKind;
    atc: string;
    incomeNature: string;
    income: string;
    tax: string;
  }[];
}

export const receivedApi = {
  search: (companyId: number, filter: { status?: CertificateStatus; q?: string; page?: number }) =>
    api.get<PageResponse<ReceivedCertificate>>(
      `/tax/received-certificates${toQuery({ companyId, status: filter.status, q: filter.q, page: filter.page })}`,
    ),
  record: (companyId: number, draft: CertificateDraft) =>
    api.post<ReceivedCertificate>(
      `/tax/received-certificates${toQuery({ companyId })}`,
      toBody(draft),
    ),
  cancel: (id: number, reason: string) =>
    api.post<ReceivedCertificate>(`/tax/received-certificates/${String(id)}/cancel`, { reason }),
};

/** An empty certificate received today with one commission line. */
export function emptyDraft(today: string): CertificateDraft {
  return {
    certificateNo: '',
    agentCode: '',
    agentName: '',
    agentTin: '',
    periodFrom: `${today.slice(0, 8)}01`,
    periodTo: today,
    receivedOn: today,
    sourceRef: '',
    remarks: '',
    lines: [{ kind: 'COMMISSION', atc: 'WC158', incomeNature: '', income: '', tax: '' }],
  };
}

export type DraftErrors = Partial<
  Record<'certificateNo' | 'agentCode' | 'agentName' | 'periodTo' | 'receivedOn' | 'lines', string>
>;

/** Field errors of a certificate (the server applies the same rules). */
export function draftErrors(d: CertificateDraft, today: string): DraftErrors {
  const errors: DraftErrors = {};
  if (d.certificateNo.trim() === '') {
    errors.certificateNo = 'Enter the certificate number';
  }
  if (d.agentCode.trim() === '') {
    errors.agentCode = 'Enter the withholding agent';
  }
  if (d.agentName.trim() === '') {
    errors.agentName = 'Enter the agent name';
  }
  if (d.periodFrom === '' || d.periodTo === '' || d.periodTo < d.periodFrom) {
    errors.periodTo = 'The period covered ends after it starts';
  }
  if (d.receivedOn === '' || d.receivedOn > today) {
    errors.receivedOn = 'Enter the date received, not in the future';
  }
  const bad = d.lines.some((l) => !lineComplete(l));
  if (d.lines.length === 0 || bad) {
    errors.lines = 'Each income payment needs its ATC, income and a tax withheld above zero';
  }
  return errors;
}

function lineComplete(l: CertificateDraft['lines'][number]): boolean {
  const income = Number(l.income);
  const tax = Number(l.tax);
  const amounts = l.income !== '' && Number.isFinite(income) && income >= 0;
  return l.atc.trim() !== '' && amounts && Number.isFinite(tax) && tax > 0;
}

function blank(value: string): string | undefined {
  return value.trim() === '' ? undefined : value.trim();
}

/** The request body of a draft. */
export function toBody(d: CertificateDraft) {
  return {
    certificateNo: d.certificateNo.trim(),
    agentCode: d.agentCode.trim(),
    agentName: d.agentName.trim(),
    agentTin: blank(d.agentTin),
    periodFrom: d.periodFrom,
    periodTo: d.periodTo,
    receivedOn: d.receivedOn,
    sourceModule: 'TAX',
    sourceRef: blank(d.sourceRef),
    remarks: blank(d.remarks),
    lines: d.lines.map((l) => ({
      kind: l.kind,
      atc: l.atc.trim(),
      incomeNature: blank(l.incomeNature),
      income: Number(l.income),
      tax: Number(l.tax),
    })),
  };
}

/** Total tax withheld of a draft. */
export function draftTax(d: CertificateDraft): number {
  return d.lines.reduce((sum, l) => sum + (Number(l.tax) || 0), 0);
}
