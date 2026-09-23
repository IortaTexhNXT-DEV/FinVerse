import type { Journal, JournalInput, JournalLineInput, ManualJournalType } from '@/api/gl';
import { today } from '@/utils/format';
import { emptyLine, isBlankLine } from './journalMath';

export interface JournalHeaderValues {
  branchId: number;
  journalType: ManualJournalType;
  valueDate: string;
  currency: string;
  narration: string;
  reference: string;
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
    },
    lines: [emptyLine('DEBIT'), emptyLine('CREDIT')],
  };
}

/** Form values of an existing draft or rejected voucher. */
export function journalValues(j: Journal): JournalFormValues {
  return {
    header: {
      branchId: j.branchId,
      journalType: j.journalType as ManualJournalType,
      valueDate: j.valueDate,
      currency: j.currency,
      narration: j.narration,
      reference: j.reference ?? '',
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
export function toJournalInput(companyId: number, v: JournalFormValues): JournalInput {
  return {
    ...v.header,
    companyId,
    reference: v.header.reference || undefined,
    lines: v.lines.filter((l) => !isBlankLine(l)),
  };
}
