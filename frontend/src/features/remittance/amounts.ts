/** Amounts and settlement of a remittance line or batch (RMTID.002, DIS 3.29.x, ACSL 2.9.2). */

export interface Amounts {
  paidAr: number;
  commission: number;
  commissionVat: number;
  wtax: number;
  dtip: number;
  incentive: number;
  incentiveVat: number;
  netDue: number;
  /** CPC2 incentive on packaged Fire and Motor products (DIS 3.29.2). */
  cpc2: number;
  cpc2Vat: number;
  payable: number;
}

/** Deductions, amount due, send cycle, early-incentive SI and last cancelled DV of a batch. */
export interface Settlement {
  deductionAmount: number;
  amountDue: number;
  sendCycle: number;
  reference: string;
  earlySiNo?: string;
  earlySiWtax?: number;
  cancelledDvNo?: string;
  cancelReason?: string;
  cancelledAt?: string;
}
