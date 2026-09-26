import type { Journal, JournalInput, JournalLineInput, ManualJournalType } from '@/api/gl';
import type { FrbsJournal } from './glPlatformApi';
import { today } from '@/utils/format';
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
