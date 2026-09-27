import { api, toQuery } from './client';
import type { RecordStatus } from './types';

/** Tax & statutory reporting API (backend module `tax`). */

export type TaxType =
  | 'VAT_OUTPUT'
  | 'VAT_INPUT'
  | 'VAT_ZERO_RATED'
  | 'VAT_EXEMPT'
  | 'PREMIUM_TAX'
  | 'DST'
  | 'LGT'
  | 'FST'
  | 'EWT';
export type PayeeClass = 'INDIVIDUAL' | 'CORPORATE';
export type VatTreatment = 'REGULAR' | 'ZERO_RATED' | 'EXEMPT';
export type WorksheetKind = 'VAT' | 'EWT' | 'DST' | 'PREMIUM_TAX' | 'LGT' | 'FST' | 'NONE';
export type FilingFrequency = 'MONTHLY' | 'QUARTERLY' | 'MONTHLY_EXCEPT_QUARTER_END' | 'ANNUAL';
export type TaxAuthority = 'BIR' | 'LGU' | 'BFP';
export type ReturnStatus = 'DRAFT' | 'FILED' | 'PAID' | 'CANCELLED';
export type DueState = 'PAID' | 'OVERDUE' | 'DUE_SOON' | 'UPCOMING' | 'REMINDER';
export type IcSchedule =
  'PREMIUMS' | 'LOSSES' | 'COMMISSIONS' | 'NET_WORTH' | 'RBC' | 'RESERVES' | 'INVESTMENTS';
export type NormalBalance = 'DEBIT' | 'CREDIT';
export type IcMeasure = 'BALANCE' | 'MOVEMENT';
export type BirList = 'SLS' | 'SLP' | 'QAP';

interface Maintained {
  id: number;
  companyId: number;
  recordStatus: RecordStatus;
  maker: string;
}

export interface TaxCode extends Maintained {
  code: string;
  name: string;
  taxType: TaxType;
  atc?: string;
  payeeClass?: PayeeClass;
  rate: number;
  glAccountCode: string;
  incomeNature?: string;
  effectiveFrom: string;
  effectiveTo?: string;
  authorizedBy?: string;
}

export type TaxCodeRequest = Omit<TaxCode, 'id' | 'recordStatus' | 'maker' | 'authorizedBy'>;

export interface TaxForm extends Maintained {
  code: string;
  name: string;
  authority: TaxAuthority;
  frequency: FilingFrequency;
  worksheet: WorksheetKind;
  dueMonthsAfter: number;
  dueDay: number;
  payableAccountCode?: string;
  creditAccountCode?: string;
  trackFiling: boolean;
  effectiveFrom: string;
}

export type TaxFormRequest = Omit<TaxForm, 'id' | 'recordStatus' | 'maker'>;

export interface PartyTaxProfile extends Maintained {
  partyCode: string;
  tin: string;
  branchCode: string;
  payeeClass: PayeeClass;
  registeredName: string;
  lastName?: string;
  firstName?: string;
  middleName?: string;
  registeredAddress?: string;
  zipCode?: string;
  vatTreatment: VatTreatment;
  defaultAtcCode?: string;
}

export type PartyTaxProfileRequest = Omit<PartyTaxProfile, 'id' | 'recordStatus' | 'maker'>;

export interface IcLine extends Maintained {
  schedule: IcSchedule;
  lineCode: string;
  description: string;
  lineOrder: number;
  accountFrom?: string;
  accountTo?: string;
  reportGroup?: string;
  normalBalance: NormalBalance;
  signFactor: number;
  measure: IcMeasure;
  rbcFactor?: number;
}

export type IcLineRequest = Omit<IcLine, 'id' | 'recordStatus' | 'maker' | 'measure'> & {
  measure?: IcMeasure;
};

export interface WorksheetLine {
  code: string;
  description: string;
  base?: number;
  amount: number;
}

export interface ReturnFigures {
  taxBase: number;
  taxDue: number;
  taxCredits: number;
  amountPayable: number;
  excessCredit: number;
}

export interface TaxDocument {
  section: 'SALES' | 'PURCHASES' | 'WITHHOLDING' | 'PREMIUMS';
  sourceType: 'POLICY' | 'ENDORSEMENT' | 'COMMISSION' | 'SUPPLIER_INVOICE';
  sourceId: number;
  documentNo: string;
  documentDate: string;
  partyCode: string;
  partyName: string;
  tin: string;
  taxCode: string;
  incomeNature?: string;
  businessLine?: string;
  taxableAmount: number;
  exemptAmount: number;
  zeroRatedAmount: number;
  taxAmount: number;
  rate: number;
}

export interface LedgerControl {
  accountCode: string;
  description: string;
  perDocuments: number;
  perLedger: number;
  difference: number;
}

export interface Worksheet {
  kind: WorksheetKind;
  from: string;
  to: string;
  periodLabel: string;
  lines: WorksheetLine[];
  figures: ReturnFigures;
  documents: TaxDocument[];
  controls: LedgerControl[];
  notes: string[];
}

export interface CalendarEntry {
  formCode: string;
  formName: string;
  authority: TaxAuthority;
  worksheet: WorksheetKind;
  periodStart: string;
  periodEnd: string;
  periodLabel: string;
  dueDate: string;
  tracked: boolean;
  returnId?: number;
  returnNo?: string;
  returnStatus: ReturnStatus | 'NOT_PREPARED';
  dueState: DueState;
  daysToDue: number;
}

export interface Remittance {
  paidOn: string;
  amount: number;
  payableCleared: number;
  creditApplied: number;
  bankAccountCode?: string;
  paymentReference: string;
  journalBatchNo?: string;
  late: boolean;
}

export interface TaxReturn extends ReturnFigures {
  id: number;
  companyId: number;
  returnNo: string;
  formCode: string;
  worksheet: WorksheetKind;
  periodStart: string;
  periodEnd: string;
  periodLabel: string;
  dueDate: string;
  status: ReturnStatus;
  overdue: boolean;
  preparedBy: string;
  preparedAt: string;
  filedBy?: string;
  filedOn?: string;
  filingReference?: string;
  statusReason?: string;
  lines: {
    lineNo: number;
    lineCode: string;
    description: string;
    baseAmount?: number;
    amount: number;
  }[];
  remittance?: Remittance;
}

export interface Certificate {
  id: number;
  certificateNo: string;
  batchNo: string;
  partyCode: string;
  payeeTin: string;
  payeeName: string;
  periodStart: string;
  periodEnd: string;
  totalIncome: number;
  totalTax: number;
  status: 'ISSUED' | 'CANCELLED';
  statusReason?: string;
}

export interface CertificateBatch {
  id: number;
  batchNo: string;
  periodStart: string;
  periodEnd: string;
  certificateCount: number;
  totalIncome: number;
  totalTax: number;
  createdBy: string;
  createdAt: string;
  skippedPayees: string[];
}

export interface Period {
  from: string;
  to: string;
}

const base = '/tax';

export const taxApi = {
  codes: (companyId: number) => api.get<TaxCode[]>(`${base}/codes${toQuery({ companyId })}`),
  createCode: (body: TaxCodeRequest) => api.post<TaxCode>(`${base}/codes`, body),
  updateCode: (id: number, body: TaxCodeRequest) => api.put<TaxCode>(`${base}/codes/${id}`, body),
  authorizeCode: (id: number) => api.post<TaxCode>(`${base}/codes/${id}/authorize`),
  forms: (companyId: number) => api.get<TaxForm[]>(`${base}/forms${toQuery({ companyId })}`),
  createForm: (body: TaxFormRequest) => api.post<TaxForm>(`${base}/forms`, body),
  updateForm: (id: number, body: TaxFormRequest) => api.put<TaxForm>(`${base}/forms/${id}`, body),
  authorizeForm: (id: number) => api.post<TaxForm>(`${base}/forms/${id}/authorize`),
  profiles: (companyId: number) =>
    api.get<PartyTaxProfile[]>(`${base}/profiles${toQuery({ companyId })}`),
  createProfile: (body: PartyTaxProfileRequest) =>
    api.post<PartyTaxProfile>(`${base}/profiles`, body),
  updateProfile: (id: number, body: PartyTaxProfileRequest) =>
    api.put<PartyTaxProfile>(`${base}/profiles/${id}`, body),
  authorizeProfile: (id: number) => api.post<PartyTaxProfile>(`${base}/profiles/${id}/authorize`),
  worksheet: (companyId: number, kind: WorksheetKind, period: Period) =>
    api.get<Worksheet>(`${base}/worksheets/${kind}${toQuery({ companyId, ...period })}`),
  calendar: (companyId: number, year: number) =>
    api.get<CalendarEntry[]>(`${base}/calendar${toQuery({ companyId, year })}`),
  exportList: (list: BirList, companyId: number, year: number, quarter: number) =>
    api.getFile(`${base}/exports/${list}${toQuery({ companyId, year, quarter })}`),
  returns: (companyId: number, year: number, status?: ReturnStatus) =>
    api.get<TaxReturn[]>(`${base}/returns${toQuery({ companyId, year, status })}`),
  getReturn: (id: number) => api.get<TaxReturn>(`${base}/returns/${id}`),
  createReturn: (companyId: number, formCode: string, periodStart: string) =>
    api.post<TaxReturn>(`${base}/returns`, { companyId, formCode, periodStart }),
  refreshReturn: (id: number) => api.post<TaxReturn>(`${base}/returns/${id}/refresh`),
  fileReturn: (id: number, filedOn: string, reference: string) =>
    api.post<TaxReturn>(`${base}/returns/${id}/file`, { filedOn, reference }),
  payReturn: (id: number, paidOn: string, bankAccountCode: string, reference: string) =>
    api.post<TaxReturn>(`${base}/returns/${id}/pay`, { paidOn, bankAccountCode, reference }),
  cancelReturn: (id: number, reason: string) =>
    api.post<TaxReturn>(`${base}/returns/${id}/cancel`, { reason }),
  certificates: (companyId: number, year: number) =>
    api.get<Certificate[]>(`${base}/2307/certificates${toQuery({ companyId, year })}`),
  batches: (companyId: number) =>
    api.get<CertificateBatch[]>(`${base}/2307/batches${toQuery({ companyId })}`),
  generateBatch: (companyId: number, year: number, quarter: number) =>
    api.post<CertificateBatch>(`${base}/2307/batches`, { companyId, year, quarter }),
  cancelCertificate: (id: number, reason: string) =>
    api.post<Certificate>(`${base}/2307/certificates/${id}/cancel`, { reason }),
  certificatePdf: (id: number) => api.getFile(`${base}/2307/certificates/${id}/pdf`),
  batchPdf: (id: number) => api.getFile(`${base}/2307/batches/${id}/pdf`),
  icMappings: (companyId: number) =>
    api.get<IcLine[]>(`${base}/ic/mappings${toQuery({ companyId })}`),
  createIcLine: (body: IcLineRequest) => api.post<IcLine>(`${base}/ic/mappings`, body),
  updateIcLine: (id: number, body: IcLineRequest) =>
    api.put<IcLine>(`${base}/ic/mappings/${id}`, body),
  authorizeIcLine: (id: number) => api.post<IcLine>(`${base}/ic/mappings/${id}/authorize`),
};
