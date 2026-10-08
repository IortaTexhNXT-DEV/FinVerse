import { api } from './client';
import type { EffectiveDated } from './catalog';

/** How an other charge is computed. */
export type ChargeBasis = 'AMOUNT' | 'RATE';
/** VAT treatment of an other charge. */
export type ChargeVatTreatment = 'VATABLE' | 'EXEMPT' | 'ZERO_RATED';

/** A charge billed with the premium besides the taxes (documentation, notarial fees...). */
export interface OtherChargeInput {
  companyId: number;
  chargeCode: string;
  name: string;
  lineCode?: string;
  productCode?: string;
  basis: ChargeBasis;
  value: number;
  vatTreatment: ChargeVatTreatment;
  glAccountCode: string;
  effectiveFrom: string;
  effectiveTo?: string;
}

export interface OtherCharge extends EffectiveDated, Omit<OtherChargeInput, 'companyId'> {}

/** The other charges and whether they are billed with the premium. */
export interface OtherChargesView {
  enabled: boolean;
  charges: OtherCharge[];
}

/** Other charges billed with the premium (Product Maintenance › Rates & Taxes). */
export const otherChargesApi = {
  list: () => api.get<OtherChargesView>('/catalog/rates/other-charges'),
  create: (input: OtherChargeInput) => api.post<OtherCharge>('/catalog/rates/other-charges', input),
};
