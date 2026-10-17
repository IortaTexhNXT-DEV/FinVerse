import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/** The list of the unapplied payments (FRS.CSH.06.01.03 / 06.01.04). */

export type UnappliedType =
  | 'EXCESS'
  | 'UNAPPLIED'
  | 'AR_INSURER_REFUND'
  | 'UNBOOKED'
  | 'PREBOOKED'
  | 'AP_UNAPPLIED_COMMISSION';

export const UNAPPLIED_TYPE_LABELS: Record<UnappliedType, string> = {
  EXCESS: 'Excess Payment',
  UNAPPLIED: 'Unapplied Payment',
  AR_INSURER_REFUND: 'AR Insurer Refund',
  UNBOOKED: 'Unbooked/Unmatched Payment',
  PREBOOKED: 'Pre-booked Payment',
  AP_UNAPPLIED_COMMISSION: 'AP Unapplied Commission',
};

export interface UnappliedFilters {
  type: UnappliedType | '';
  from: string;
  to: string;
  insurer: string;
  client: string;
  assured: string;
  account: string;
  q: string;
}

export const NO_FILTERS: UnappliedFilters = {
  type: '',
  from: '',
  to: '',
  insurer: '',
  client: '',
  assured: '',
  account: '',
  q: '',
};

export interface UnappliedRow {
  id: number;
  reference: string;
  source: string;
  type: UnappliedType;
  paidOn?: string;
  account: { accountNo?: string; invoiceNo?: string; pnNo?: string; loanApplicationNo?: string };
  parties: { clientName?: string; assured?: string; insurerName?: string; salesUnit?: string };
  currency: string;
  amount: number;
  outstanding: number;
  status: string;
}

/** The filters of the address of the page (the dashboard opens the list of one type). */
export function filtersFromSearch(search: string): UnappliedFilters {
  const type = new URLSearchParams(search).get('type') ?? '';
  return {
    ...NO_FILTERS,
    type: type in UNAPPLIED_TYPE_LABELS ? (type as UnappliedType) : '',
  };
}

/** PN and loan application numbers of an account, as one cell. */
export function pnAndLoan(row: Pick<UnappliedRow, 'account'>): string {
  return [row.account.pnNo, row.account.loanApplicationNo].filter(Boolean).join(' / ');
}

export const unappliedInquiryApi = {
  search: (companyId: number, f: UnappliedFilters, page: number) =>
    api.get<PageResponse<UnappliedRow>>(
      `/cashiering/unapplied-inquiry${toQuery({ companyId, ...f, page })}`,
    ),
};
