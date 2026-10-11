import type { Journal, JournalInput, JournalLineInput, ManualJournalType } from '@/api/gl';
import type { BulkPostOutcome, FrbsJournal } from './glPlatformApi';
import { displayNameOf } from '@/api/users';
import { statusLabel } from '@/components/ui/statusTones';
import { humanize, today } from '@/utils/format';
import { emptyLine, isBlankLine } from './journalMath';

export interface JournalHeaderValues {
  branchId: number;
  journalType: ManualJournalType;
  valueDate: string;
  currency: string;
  narration: string;
  reference: string;
  /** Date on which the posted voucher is reversed automatically (FRBS 2.8.1), '' for none. */
  reverseOn: string;
}

export interface JournalFormValues {
  header: JournalHeaderValues;
  lines: JournalLineInput[];
}

/** Initial values for a new voucher. */
export function newJournalValues(branchId: number, currency: string): JournalFormValues {
  return {
    header: {
      branchId,
      journalType: 'MANUAL',
      valueDate: today(),
      currency,
      narration: '',
      reference: '',
      reverseOn: '',
    },
    lines: [emptyLine('DEBIT'), emptyLine('CREDIT')],
  };
}

/** Form values of an existing draft or rejected voucher. */
export function journalValues(j: Journal | FrbsJournal): JournalFormValues {
  return {
    header: {
      branchId: j.branchId,
      journalType: j.journalType as ManualJournalType,
      valueDate: j.valueDate,
      currency: j.currency,
      narration: j.narration,
      reference: j.reference ?? '',
      reverseOn: ('reverseOn' in j ? j.reverseOn : undefined) ?? '',
    },
    lines: j.lines.map((l) => ({
      accountCode: l.accountCode,
      side: l.side,
      amount: l.amount,
      costCenter: l.costCenter,
      businessLine: l.businessLine,
      partyCode: l.partyCode,
      reference: l.reference,
      narration: l.narration,
    })),
  };
}

/**
 * Request body. Only blank placeholder rows are left out; incomplete or non-positive lines are
 * sent as entered (the form blocks them first, see lineProblems).
 */
export function toJournalInput(
  companyId: number,
  v: JournalFormValues,
): JournalInput & { reverseOn?: string } {
  return {
    ...v.header,
    companyId,
    reference: v.header.reference || undefined,
    reverseOn: v.header.reverseOn || undefined,
    lines: v.lines.filter((l) => !isBlankLine(l)),
  };
}

/** A journal status as the filter offers it: the label of its pill, or All. */
export function journalStatusOption(status: string): string {
  return status === '' ? 'All' : statusLabel(status);
}

/** Why a selected journal was not posted, naming it by its batch number. */
export function notPostedText(o: BulkPostOutcome): string {
  const name = o.batchNo ?? 'Journal ' + String(o.id);
  return `${name}: ${o.message}`;
}

/** The users a journal may be assigned to, by name and sorted. */
export function assigneeOptions(users: readonly string[]): { value: string; label: string }[] {
  return users
    .map((u) => ({ value: u, label: displayNameOf(u) || u }))
    .sort((a, b) => a.label.localeCompare(b.label));
}

const JOURNAL_TYPE_LABELS: Record<string, string> = {
  MANUAL: 'Manual journal',
  ADJUSTMENT: 'Adjustment',
  ACCRUAL: 'Accrual',
};

/** The type of a journal in words (Accrual, Premium...), never its code. */
export function journalTypeLabel(type: string): string {
  return JOURNAL_TYPE_LABELS[type] ?? humanize(type);
}

/** The message after a journal is saved, with its status as its pill reads it. */
export function journalSavedText(batchNo: string, status: string): string {
  return `Journal ${batchNo} saved (${statusLabel(status)})`;
}

/**
 * Smallest width (pixels) of each input column of the journal lines, so a full amount, the side
 * and the cost centre stay readable; the table scrolls inside its card when the page is narrower.
 */
export const LINE_MIN_WIDTHS = {
  side: 110,
  amount: 150,
  costCentre: 200,
  lineOfBusiness: 140,
  reference: 150,
  narration: 220,
} as const;
