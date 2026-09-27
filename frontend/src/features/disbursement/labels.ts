import type {
  FundingStage,
  InstrumentStatus,
  Line,
  Mode,
  PayeeStage,
  RequestStatus,
  VoucherStage,
} from './api';

/** Labels, tab definitions and pure helpers of the Disbursement screens (DIS 2.2-3.28). */

export const MODE_LABELS: Record<Mode, string> = {
  CTA: 'Credit to Account',
  ATD: 'Authority to Debit',
  MC_DD: "Manager's Check / Demand Draft",
  CREDIT_TICKET: 'Credit Ticket',
  TT: 'Telegraphic Transfer',
  ONLINE_BANKING: 'Online Banking',
  CHECK: 'Check',
};

export const MODES = Object.keys(MODE_LABELS) as Mode[];

/** Modes paid into a payee bank account, which the voucher must name. */
export const CREDIT_MODES: readonly Mode[] = ['CTA', 'TT', 'ONLINE_BANKING'];

export type WorkbenchTab =
  'REQUESTS' | 'NO_PAYEE' | 'IN_PROCESS' | 'FOR_REVIEW' | 'FOR_APPROVAL' | 'APPROVED' | 'CLOSED';

interface WorkbenchTabDef {
  id: WorkbenchTab;
  label: string;
  requestStatuses?: RequestStatus[];
  stages?: VoucherStage[];
}

/** The first workbench tab: the requests received from the other units. */
const REQUESTS_TAB: WorkbenchTabDef = {
  id: 'REQUESTS',
  label: 'System Requests',
  requestStatuses: ['RECEIVED'],
};

/** Workbench tabs (DIS 2.4.0, 2.6.2, 2.13.0; design 11): requests, then vouchers by stage. */
export const WORKBENCH_TABS: readonly WorkbenchTabDef[] = [
  REQUESTS_TAB,
  { id: 'NO_PAYEE', label: 'No Payee', requestStatuses: ['NO_PAYEE'] },
  { id: 'IN_PROCESS', label: 'In Process', stages: ['IN_PROCESS'] },
  { id: 'FOR_REVIEW', label: 'For Review', stages: ['FOR_REVIEW'] },
  { id: 'FOR_APPROVAL', label: 'For Approval', stages: ['FOR_APPROVAL'] },
  { id: 'APPROVED', label: 'Approved', stages: ['APPROVED'] },
  { id: 'CLOSED', label: 'Cancelled / Rejected', stages: ['CANCELLED', 'REJECTED'] },
];

/** The definition of a workbench tab. */
export function workbenchTab(id: WorkbenchTab): WorkbenchTabDef {
  return WORKBENCH_TABS.find((t) => t.id === id) ?? REQUESTS_TAB;
}

/** The workbench tab named in the URL, else the system requests. */
export function workbenchTabOf(value: string | null): WorkbenchTab {
  return WORKBENCH_TABS.find((t) => t.id === value)?.id ?? 'REQUESTS';
}

export type PayeeTab = 'ACTIVE' | 'PENDING' | 'DRAFT' | 'INACTIVE' | 'REQUESTS';

/** Payee list tabs (DIS 2.2.1, 2.2.8). */
export const PAYEE_TABS: readonly { id: PayeeTab; label: string; stages: PayeeStage[] }[] = [
  { id: 'ACTIVE', label: 'Active', stages: ['ACTIVE', 'FOR_DEACTIVATION'] },
  {
    id: 'PENDING',
    label: 'For Authorisation',
    stages: ['FOR_AUTHORIZATION', 'FOR_DEACTIVATION', 'FOR_REACTIVATION'],
  },
  { id: 'DRAFT', label: 'Drafts', stages: ['DRAFT'] },
  { id: 'INACTIVE', label: 'Inactive', stages: ['INACTIVE', 'FOR_REACTIVATION'] },
  { id: 'REQUESTS', label: 'Payee Requests', stages: [] },
];

export type FundingTab = 'OPEN' | 'APPROVAL' | 'DONE';

/** Funding list tabs (DIS 2.17). */
export const FUNDING_TABS: readonly { id: FundingTab; label: string; stages: FundingStage[] }[] = [
  { id: 'OPEN', label: 'With the Maker', stages: ['CREATED'] },
  {
    id: 'APPROVAL',
    label: 'For Verification and Approval',
    stages: ['FOR_VERIFICATION', 'FOR_APPROVAL_1', 'FOR_APPROVAL_2'],
  },
  { id: 'DONE', label: 'Approved and Closed', stages: ['APPROVED', 'DECLINED', 'CANCELLED'] },
];

/** A user action on an instrument in its current status (DIS 2.8.x). */
export type InstrumentAction = 'print' | 'release' | 'email' | 'debited' | 'received' | 'reissue';

/** The actions an instrument allows now: print, release, e-mail, confirm, receive, re-issue. */
export function instrumentActions(mode: Mode, status: InstrumentStatus): InstrumentAction[] {
  if (status === 'STALE') {
    return ['reissue'];
  }
  if (status === 'PENDING') {
    return mode === 'CTA' ? [] : ['print'];
  }
  const next: Partial<Record<Mode, Partial<Record<InstrumentStatus, InstrumentAction[]>>>> = {
    CHECK: { PRINTED: ['release'] },
    ATD: { PRINTED: ['email'], EMAILED: ['debited'] },
    CREDIT_TICKET: { PRINTED: ['debited'] },
    TT: { PRINTED: ['debited'] },
    MC_DD: { PRINTED: ['received'], RECEIVED: ['release'] },
    ONLINE_BANKING: { APPROVED: ['debited'] },
  };
  return next[mode]?.[status] ?? [];
}

export const INSTRUMENT_ACTION_LABELS: Record<InstrumentAction, string> = {
  print: 'Print',
  release: 'Release',
  email: 'Email to Branch',
  debited: 'Confirm Debit',
  received: 'Receive from Branch',
  reissue: 'Re-issue Check',
};

/** Totals of a proforma entry and whether it balances. */
export function entryTotals(lines: readonly Line[]): {
  debit: number;
  credit: number;
  balanced: boolean;
} {
  const cents = (side: Line['side']) =>
    lines.filter((l) => l.side === side).reduce((sum, l) => sum + Math.round(l.amount * 100), 0);
  const debit = cents('DEBIT');
  const credit = cents('CREDIT');
  return { debit: debit / 100, credit: credit / 100, balanced: debit === credit && debit > 0 };
}

/** Field errors of the encode form (DIS 2.6.1, 2.7.4). */
export function encodeErrors(form: {
  disbursementType: string;
  payeeCode: string;
  currency: string;
  amount: string;
  purpose: string;
}): Record<string, string> {
  const errors: Record<string, string> = {};
  if (form.disbursementType === '') {
    errors.disbursementType = 'Select the disbursement type';
  }
  if (form.payeeCode.trim() === '') {
    errors.payeeCode = 'Enter the payee code';
  }
  if (!/^[A-Z]{3}$/.test(form.currency)) {
    errors.currency = 'Enter a 3-letter currency';
  }
  const amount = Number(form.amount);
  if (!Number.isFinite(amount) || amount <= 0) {
    errors.amount = 'Enter an amount above zero';
  }
  if (form.purpose.trim() === '') {
    errors.purpose = 'Enter the purpose';
  }
  return errors;
}

/** "No payee" requests need the payee first; received ones get their voucher from the processor. */
export function requestActions(status: RequestStatus, type: string): string[] {
  if (status === 'RECEIVED') {
    return type === 'CWT2307' ? ['release', 'return'] : ['voucher', 'return'];
  }
  return status === 'NO_PAYEE' ? ['voucher', 'return'] : [];
}
