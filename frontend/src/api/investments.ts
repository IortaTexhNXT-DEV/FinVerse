import { api, toQuery } from './client';
import type { RecordStatus } from './types';

export type Classification = 'AMORTIZED_COST' | 'FVOCI' | 'FVPL';
export type InstrumentType =
  'TIME_DEPOSIT' | 'TREASURY_BILL' | 'GOVERNMENT_BOND' | 'CORPORATE_BOND' | 'EQUITY';
export type CouponFrequency =
  'NONE' | 'MONTHLY' | 'QUARTERLY' | 'SEMI_ANNUAL' | 'ANNUAL' | 'AT_MATURITY';
export type DayCount = 'ACT_365' | 'THIRTY_360';
export type AmortizationMethod = 'EFFECTIVE_INTEREST' | 'STRAIGHT_LINE' | 'NONE';
export type HoldingStatus = 'PENDING_APPROVAL' | 'ACTIVE' | 'MATURED' | 'SOLD';
export type RunType = 'ACCRUAL' | 'AMORTIZATION';
export type TransactionType =
  | 'PURCHASE'
  | 'TAKE_ON'
  | 'ACCRUAL'
  | 'AMORTIZATION'
  | 'COUPON'
  | 'FAIR_VALUE'
  | 'MATURITY'
  | 'SALE';

export const INSTRUMENT_TYPES: readonly InstrumentType[] = [
  'TIME_DEPOSIT',
  'TREASURY_BILL',
  'GOVERNMENT_BOND',
  'CORPORATE_BOND',
  'EQUITY',
];
export const COUPON_FREQUENCIES: readonly CouponFrequency[] = [
  'NONE',
  'MONTHLY',
  'QUARTERLY',
  'SEMI_ANNUAL',
  'ANNUAL',
  'AT_MATURITY',
];

export interface Portfolio {
  id: number;
  companyId: number;
  code: string;
  name: string;
  classification: Classification;
  investmentAccount: string;
  accruedInterestAccount: string;
  interestIncomeAccount: string;
  realizedGainAccount: string;
  fairValueAccount?: string;
  recordStatus: RecordStatus;
  createdBy: string;
  authorizedBy?: string;
}

export type PortfolioInput = Omit<Portfolio, 'id' | 'recordStatus' | 'createdBy' | 'authorizedBy'>;

export interface Holding {
  id: number;
  companyId: number;
  branchId: number;
  portfolioId: number;
  portfolioCode: string;
  classification: Classification;
  holdingNo: string;
  instrumentType: InstrumentType;
  securityCode?: string;
  description: string;
  issuerCode: string;
  custodian?: string;
  currency: string;
  faceValue: number;
  purchasePrice: number;
  purchasedInterest: number;
  tradeDate: string;
  settlementDate: string;
  maturityDate?: string;
  couponRate: number;
  couponFrequency: CouponFrequency;
  dayCount: DayCount;
  amortizationMethod: AmortizationMethod;
  effectiveRate?: number;
  securityDeposit: boolean;
  bankAccount: string;
  takeOn: boolean;
  amortizedCost: number;
  accruedInterest: number;
  fairValueAdjustment: number;
  carryingAmount: number;
  fairValue?: number;
  fairValueDate?: string;
  lastAccrualDate?: string;
  lastAmortizationDate?: string;
  status: HoldingStatus;
  closedDate?: string;
  purchaseBatchNo?: string;
  recordStatus: RecordStatus;
  createdBy: string;
  authorizedBy?: string;
}

export interface HoldingInput {
  companyId: number;
  branchId: number;
  portfolioId: number;
  instrumentType: InstrumentType;
  securityCode?: string;
  description: string;
  issuerCode: string;
  custodian?: string;
  currency: string;
  faceValue: number;
  purchasePrice: number;
  purchasedInterest?: number;
  tradeDate: string;
  settlementDate: string;
  maturityDate?: string;
  couponRate: number;
  couponFrequency: CouponFrequency;
  dayCount: DayCount;
  amortizationMethod?: AmortizationMethod;
  securityDeposit: boolean;
  bankAccount: string;
  takeOn: boolean;
  takeOnDate?: string;
}

export interface HoldingTransaction {
  id: number;
  holdingId: number;
  holdingNo: string;
  runId?: number;
  txnType: TransactionType;
  txnDate: string;
  fromDate?: string;
  days: number;
  amount: number;
  cashAmount: number;
  finalTax: number;
  gainLoss: number;
  carryingAfter: number;
  accruedAfter: number;
  batchNo?: string;
  remarks?: string;
}

export interface InvestmentRun {
  id: number;
  runType: RunType;
  period: string;
  periodEnd: string;
  holdingCount: number;
  totalAmount: number;
  createdBy: string;
  createdAt: string;
}

export interface RunLine {
  holdingId: number;
  holdingNo: string;
  description: string;
  fromDate: string;
  toDate: string;
  days: number;
  amount: number;
  batchNo?: string;
}

export interface RunPreview {
  runType: RunType;
  period: string;
  posted: boolean;
  run?: InvestmentRun;
  total: number;
  lines: RunLine[];
}

export interface CouponInput {
  receiptDate: string;
  cashAmount: number;
  finalTax: number;
  remarks?: string;
}

export interface RedemptionInput {
  valueDate: string;
  proceeds: number;
  finalTax: number;
  remarks?: string;
}

export interface FairValueInput {
  valuationDate: string;
  fairValue: number;
  remarks?: string;
}

const HOLDINGS = '/investments/holdings';

export const investmentsApi = {
  portfolios: (companyId: number) =>
    api.get<Portfolio[]>(`/investments/portfolios${toQuery({ companyId })}`),
  createPortfolio: (body: PortfolioInput) => api.post<Portfolio>('/investments/portfolios', body),
  updatePortfolio: (id: number, body: PortfolioInput) =>
    api.put<Portfolio>(`/investments/portfolios/${id}`, body),
  authorizePortfolio: (id: number) =>
    api.post<Portfolio>(`/investments/portfolios/${id}/authorize`),
  holdings: (companyId: number, status?: HoldingStatus, portfolioId?: number, q?: string) =>
    api.get<Holding[]>(`${HOLDINGS}${toQuery({ companyId, status, portfolioId, q })}`),
  create: (body: HoldingInput) => api.post<Holding>(HOLDINGS, body),
  update: (id: number, body: HoldingInput) => api.put<Holding>(`${HOLDINGS}/${id}`, body),
  approve: (id: number) => api.post<Holding>(`${HOLDINGS}/${id}/approve`),
  transactions: (id: number) => api.get<HoldingTransaction[]>(`${HOLDINGS}/${id}/transactions`),
  coupon: (id: number, body: CouponInput) =>
    api.post<HoldingTransaction>(`${HOLDINGS}/${id}/coupons`, body),
  mature: (id: number, body: RedemptionInput) =>
    api.post<HoldingTransaction>(`${HOLDINGS}/${id}/maturity`, body),
  sell: (id: number, body: RedemptionInput) =>
    api.post<HoldingTransaction>(`${HOLDINGS}/${id}/sale`, body),
  fairValue: (id: number, body: FairValueInput) =>
    api.post<HoldingTransaction>(`${HOLDINGS}/${id}/fair-value`, body),
  preview: (companyId: number, type: RunType, period: string) =>
    api.get<RunPreview>(`/investments/runs/preview${toQuery({ companyId, type, period })}`),
  postRun: (companyId: number, type: RunType, period: string) =>
    api.post<InvestmentRun>(`/investments/runs${toQuery({ companyId, type, period })}`),
  runs: (companyId: number) =>
    api.get<InvestmentRun[]>(`/investments/runs${toQuery({ companyId })}`),
};
