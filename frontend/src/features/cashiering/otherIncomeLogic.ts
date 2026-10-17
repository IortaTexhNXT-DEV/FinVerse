import type { RecordForm } from './recordLogic';

/** An SOA or fee invoice an OR of other income settles (FRS.CSH.02.02.08). */
export interface OtherIncomeItem {
  reference: string;
  description: string;
  date?: string;
  amount: number;
  outstanding: number;
}

/** The party whose open items an OR type offers: the insurer of an incentive, the client of a fee. */
export function otherIncomeParty(
  form: Pick<RecordForm, 'receiptType' | 'insurerCode' | 'clientCode'>,
): string {
  if (form.receiptType === 'INCENTIVE') {
    return form.insurerCode;
  }
  return form.receiptType === 'SERVICE_FEE' ? form.clientCode : '';
}
