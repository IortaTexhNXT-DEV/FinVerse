import { api, toQuery } from './client';
import type { RecordStatus } from './types';

/** Product catalog, insurer panel, rate tables, sales organisation and rating (catalog module). */

export type RiskItemKind = 'VEHICLE' | 'PROPERTY_LOCATION' | 'PERSON' | 'GENERIC';
export type RatingMethod = 'PROPERTY' | 'MOTOR' | 'GENERIC';
export type PaymentGate = 'PAID' | 'CLIENT_CONFIRMATION';
export type TsuInvolvement = 'ALWAYS' | 'NEVER' | 'BY_RULES';
export type RuleScope = 'ALL' | 'LINE' | 'PRODUCT';
export type FieldTarget = 'ACCOUNT' | 'ITEM';
export type PlacementChannel = 'EMAIL' | 'SFTP' | 'API';
export type RateCode =
  | 'DST'
  | 'PREMIUM_TAX'
  | 'VAT_PREMIUM'
  | 'FIRE_SERVICE_TAX'
  | 'VAT_COMMISSION'
  | 'MOTOR_OD_ANNUAL'
  | 'MOTOR_OD_MULTI_YEAR';
export type MotorCoverage = 'BI' | 'PD';
export type ProductClass = 'PACKAGE' | 'NON_PACKAGE' | 'ANY';
export type SalesLevel = 'REGION' | 'DEPARTMENT' | 'TEAM';
export type PeriodBasis = 'ANNUAL' | 'PRO_RATA' | 'SHORT_PERIOD';
export type CatalogKind =
  | 'PRODUCT_LINE'
  | 'COVER_TYPE'
  | 'PRODUCT'
  | 'FIELD_RULE'
  | 'DOCUMENT_RULE'
  | 'TSU_RULE'
  | 'INSURER'
  | 'INSURER_BRANCH'
  | 'COMMISSION_RATE'
  | 'RATE'
  | 'SHORT_PERIOD_RATE'
  | 'MOTOR_LIMIT'
  | 'SALES_UNIT'
  | 'SALES_OFFICER';

export const RATE_CODES: RateCode[] = [
  'DST',
  'PREMIUM_TAX',
  'VAT_PREMIUM',
  'FIRE_SERVICE_TAX',
  'VAT_COMMISSION',
  'MOTOR_OD_ANNUAL',
  'MOTOR_OD_MULTI_YEAR',
];

/** Maker-checker state shared by every catalog record. */
export interface Authorizable {
  id: number;
  recordStatus: RecordStatus;
  maker?: string;
  authorizedBy?: string;
}

export interface EffectiveDated extends Authorizable {
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface ProductLine extends Authorizable {
  code: string;
  name: string;
  riskItemKind: RiskItemKind;
  ratingMethod: RatingMethod;
  sortOrder: number;
}

export interface CoverType extends Authorizable {
  lineCode: string;
  code: string;
  name: string;
  sortOrder: number;
}

export interface ProductInput {
  code: string;
  name: string;
  lineCode: string;
  coverTypeCode?: string;
  packaged: boolean;
  fleetCapable: boolean;
  marketSegments: string[];
  mortgageApplicable: boolean;
  directPaymentEligible: boolean;
  multiYearAllowed: boolean;
  maxTermYears: number;
  ffyEligible: boolean;
  paymentGate: PaymentGate;
  defaultRate?: number;
  defaultCommissionRate: number;
  minimumPremium: number;
  maxSumInsured?: number;
  tsuInvolvement?: TsuInvolvement;
}

export type Product = ProductInput & Authorizable;

export interface FieldRuleInput {
  scope: RuleScope;
  scopeCode: string;
  target: FieldTarget;
  fieldKey: string;
  label: string;
  required: boolean;
  sortOrder: number;
}

export type FieldRule = FieldRuleInput & Authorizable;

export interface DocumentRuleInput {
  scope: RuleScope;
  scopeCode: string;
  documentType: string;
  required: boolean;
}

export type DocumentRule = DocumentRuleInput & Authorizable;

export interface TsuRuleInput {
  code: string;
  description: string;
  productClass?: ProductClass;
  lineCode?: string;
  minFleetUnits?: number;
  minLocations?: number;
  tsiAbove?: number;
  endorsementType?: string;
  priority: number;
}

export type TsuRule = TsuRuleInput & Authorizable;

export interface ProductDetail {
  product: Product;
  lineName: string;
  riskItemKind: RiskItemKind;
  ratingMethod: RatingMethod;
  fieldRules: FieldRule[];
  requiredDocuments: string[];
}

export interface ProductFilter {
  line?: string;
  packaged?: boolean;
  segment?: string;
  q?: string;
  activeOnly?: boolean;
}

export interface InsurerInput {
  companyId: number;
  partyCode: string;
  name: string;
  shortName?: string;
  accreditationNo?: string;
  accreditedUntil?: string;
  placementChannel: PlacementChannel;
  placementEmails: string[];
  defaultCreditDays: number;
  taxId?: string;
  address?: string;
  email?: string;
  phone?: string;
}

export interface Insurer extends Authorizable {
  companyId: number;
  partyCode: string;
  name: string;
  shortName?: string;
  accreditationNo?: string;
  accreditedUntil?: string;
  placementChannel: PlacementChannel;
  placementEmails: string[];
  defaultCreditDays: number;
}

export interface BranchInput {
  code: string;
  name: string;
  city?: string;
  lgtRate: number;
  placementEmail?: string;
}

export interface InsurerBranch extends BranchInput, Authorizable {
  insurerId: number;
}

export interface CommissionInput {
  productCode?: string;
  rate: number;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface Commission extends EffectiveDated {
  insurerCode: string;
  productCode?: string;
  rate: number;
}

export interface InsurerDetail {
  insurer: Insurer;
  branches: InsurerBranch[];
  commissions: Commission[];
}

export interface RateInput {
  rateCode: RateCode;
  lineCode?: string;
  rate: number;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface TaxRate extends EffectiveDated {
  rateCode: RateCode;
  lineCode?: string;
  rate: number;
}

export interface ShortPeriodInput {
  monthsCovered: number;
  percentOfAnnual: number;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface ShortPeriodRate extends EffectiveDated {
  monthsCovered: number;
  percentOfAnnual: number;
}

export interface MotorLimitInput {
  coverage: MotorCoverage;
  limitAmount: number;
  premium: number;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface MotorLimit extends EffectiveDated {
  coverage: MotorCoverage;
  limitAmount: number;
  premium: number;
}

export interface SalesUnitInput {
  companyId: number;
  level: SalesLevel;
  code: string;
  name: string;
  parentCode?: string;
  costCenter?: string;
}

export interface SalesUnit extends Authorizable {
  level: SalesLevel;
  code: string;
  name: string;
  parentCode?: string;
  costCenter?: string;
}

export interface SalesOfficer extends Authorizable {
  teamCode: string;
  username: string;
}

export interface SalesOrganisation {
  units: SalesUnit[];
  officers: SalesOfficer[];
}

export interface SalesAssignment {
  region?: string;
  department?: string;
  team?: string;
  costCenter?: string;
}

export interface RatingItem {
  label?: string;
  sumInsured: number;
  ratePercent?: number;
  biLimit?: number;
  pdLimit?: number;
}

export interface RatingInput {
  companyId: number;
  productCode: string;
  insurerCode?: string;
  branchCode?: string;
  items: RatingItem[];
  multiYear: boolean;
  basis?: PeriodBasis;
  periodFrom?: string;
  periodTo?: string;
  commissionRate?: number;
  endorsement: boolean;
}

export interface ItemPremium {
  label: string;
  sumInsured: number;
  ratePercent?: number;
  premium: number;
}

export interface PremiumBreakdown {
  method: RatingMethod;
  sumInsured: number;
  odTheftCoverage?: number;
  odTheftPremium?: number;
  biPremium?: number;
  pdPremium?: number;
  annualPremium: number;
  periodFactor: number;
  minimumApplied: boolean;
  netPremium: number;
  dst: number;
  premiumTax: number;
  vat: number;
  fst: number;
  lgt: number;
  totalCharges: number;
  grossPremium: number;
  commission: number;
  vatOnCommission: number;
  items: ItemPremium[];
}

export interface RatingRates {
  dst: number;
  premiumTax: number;
  vatPremium: number;
  fireServiceTax: number;
  lgt: number;
  commission: number;
  vatCommission: number;
  odAnnual?: number;
  odMultiYear?: number;
}

export interface RatingResult {
  breakdown: PremiumBreakdown;
  rates: RatingRates;
  basis: PeriodBasis;
  shortPeriodPercent?: number;
}

export interface CatalogRecordResult extends Authorizable {
  kind: CatalogKind;
  reference: string;
  description: string;
}

const base = '/catalog';

export const catalogApi = {
  lines: () => api.get<ProductLine[]>(`${base}/lines`),
  coverTypes: () => api.get<CoverType[]>(`${base}/cover-types`),
  products: (filter: ProductFilter = {}) =>
    api.get<Product[]>(`${base}/products${toQuery({ ...filter })}`),
  product: (code: string) => api.get<ProductDetail>(`${base}/products/${code}`),
  createProduct: (input: ProductInput) => api.post<Product>(`${base}/products`, input),
  updateProduct: (code: string, input: ProductInput) =>
    api.put<Product>(`${base}/products/${code}`, input),

  fieldRules: () => api.get<FieldRule[]>(`${base}/field-rules`),
  createFieldRule: (input: FieldRuleInput) => api.post<FieldRule>(`${base}/field-rules`, input),
  updateFieldRule: (id: number, input: FieldRuleInput) =>
    api.put<FieldRule>(`${base}/field-rules/${id}`, input),
  documentRules: () => api.get<DocumentRule[]>(`${base}/document-rules`),
  createDocumentRule: (input: DocumentRuleInput) =>
    api.post<DocumentRule>(`${base}/document-rules`, input),
  tsuRules: () => api.get<TsuRule[]>(`${base}/tsu-rules`),
  createTsuRule: (input: TsuRuleInput) => api.post<TsuRule>(`${base}/tsu-rules`, input),

  insurers: (companyId: number) => api.get<Insurer[]>(`${base}/insurers${toQuery({ companyId })}`),
  insurer: (id: number) => api.get<InsurerDetail>(`${base}/insurers/${id}`),
  createInsurer: (input: InsurerInput) => api.post<Insurer>(`${base}/insurers`, input),
  updateInsurer: (id: number, input: InsurerInput) =>
    api.put<Insurer>(`${base}/insurers/${id}`, input),
  createBranch: (insurerId: number, input: BranchInput) =>
    api.post<InsurerBranch>(`${base}/insurers/${insurerId}/branches`, input),
  updateBranch: (branchId: number, input: BranchInput) =>
    api.put<InsurerBranch>(`${base}/insurers/branches/${branchId}`, input),
  createCommission: (insurerId: number, input: CommissionInput) =>
    api.post<Commission>(`${base}/insurers/${insurerId}/commissions`, input),
  updateCommission: (rateId: number, input: CommissionInput) =>
    api.put<Commission>(`${base}/insurers/commissions/${rateId}`, input),

  taxes: () => api.get<TaxRate[]>(`${base}/rates/taxes`),
  createTax: (input: RateInput) => api.post<TaxRate>(`${base}/rates/taxes`, input),
  shortPeriod: () => api.get<ShortPeriodRate[]>(`${base}/rates/short-period`),
  createShortPeriod: (input: ShortPeriodInput) =>
    api.post<ShortPeriodRate>(`${base}/rates/short-period`, input),
  motorLimits: () => api.get<MotorLimit[]>(`${base}/rates/motor-limits`),
  createMotorLimit: (input: MotorLimitInput) =>
    api.post<MotorLimit>(`${base}/rates/motor-limits`, input),

  salesOrganisation: (companyId: number) =>
    api.get<SalesOrganisation>(`${base}/sales-organisation${toQuery({ companyId })}`),
  salesAssignment: (companyId: number, username: string) =>
    api
      .get<SalesAssignment | undefined>(
        `${base}/sales-organisation/assignment${toQuery({ companyId, username })}`,
      )
      .then((a) => a ?? null),
  createSalesUnit: (input: SalesUnitInput) =>
    api.post<SalesUnit>(`${base}/sales-organisation/units`, input),
  assignOfficer: (input: { companyId: number; teamCode: string; username: string }) =>
    api.post<SalesOfficer>(`${base}/sales-organisation/officers`, input),

  quote: (input: RatingInput) => api.post<RatingResult>(`${base}/rating/quote`, input),

  authorize: (kind: CatalogKind, id: number) =>
    api.post<CatalogRecordResult>(`${base}/records/${kind}/${id}/authorize`),
  deactivate: (kind: CatalogKind, id: number) =>
    api.post<CatalogRecordResult>(`${base}/records/${kind}/${id}/deactivate`),
};
