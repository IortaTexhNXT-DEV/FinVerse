import {
  formatAmount,
  formatCompact,
  formatDate,
  formatDateTime,
  statusPhrase,
  versionLabel,
  formatDuration,
  humanize,
  titleCase,
  today,
} from './format';

describe('format', () => {
  it('gives the business date in Philippine time, not the UTC date', () => {
    expect(today(new Date('2026-09-26T20:30:00Z'))).toBe('2026-09-27');
    expect(today(new Date('2026-09-27T15:59:00Z'))).toBe('2026-09-27');
    expect(today(new Date('2026-09-27T16:00:00Z'))).toBe('2026-09-28');
  });

  it('formats amounts in accounting style', () => {
    expect(formatAmount(1234.5)).toBe('1,234.50');
    expect(formatAmount(-99)).toBe('(99.00)');
    expect(formatAmount(null)).toBe('');
    expect(formatAmount('abc')).toBe('abc');
  });

  it('compacts large numbers', () => {
    expect(formatCompact(1_500_000)).toBe('1.5M');
    expect(formatCompact(2_300)).toBe('2.3K');
    expect(formatCompact(12)).toBe('12');
  });

  it('formats ISO dates as dd-MMM-yyyy', () => {
    expect(formatDate('2026-09-23')).toBe('23-Sep-2026');
    expect(formatDate('2026-01-05T10:00:00Z')).toBe('05-Jan-2026');
    expect(formatDate(undefined)).toBe('');
  });

  it('formats timestamps as dd-MMM-yyyy HH:mm in Philippine time', () => {
    expect(formatDateTime('2026-09-25T11:32:00Z')).toBe('25-Sep-2026 19:32');
    expect(formatDateTime('2026-12-31T16:05:00Z')).toBe('01-Jan-2027 00:05');
    expect(formatDateTime(null)).toBe('');
  });

  it('formats elapsed time', () => {
    expect(formatDuration('2026-09-25T10:00:00Z', '2026-09-25T10:45:00Z')).toBe('45m');
    expect(formatDuration('2026-09-25T10:00:00Z', '2026-09-25T13:30:00Z')).toBe('3h 30m');
    expect(formatDuration('2026-09-23T10:00:00Z', '2026-09-25T13:00:00Z')).toBe('2d 3h');
  });

  it('humanizes enum codes', () => {
    expect(humanize('PENDING_APPROVAL')).toBe('Pending Approval');
  });
});

describe('titleCase', () => {
  it('capitalises sentence labels and keeps minor words lowercase', () => {
    expect(titleCase('Revise quotation slip (new round)')).toBe(
      'Revise Quotation Slip (New Round)',
    );
    expect(titleCase('Holds for approval')).toBe('Holds for Approval');
    expect(titleCase('not proceeded')).toBe('Not Proceeded');
  });

  it('keeps acronyms and mixed-case words as written', () => {
    expect(titleCase('TSU recommendation')).toBe('TSU Recommendation');
    expect(titleCase('Sent to ManCom')).toBe('Sent to ManCom');
    expect(titleCase('Awaiting insurer OR')).toBe('Awaiting Insurer OR');
    expect(titleCase('')).toBe('');
  });
});

describe('status phrases and template versions in messages', () => {
  it('writes a status inside a sentence with its acronyms kept', () => {
    expect(statusPhrase('QS_SENT')).toBe('QS sent');
    expect(statusPhrase('PS_RELEASED')).toBe('PS released');
    expect(statusPhrase('APPROVED')).toBe('approved');
    expect(statusPhrase('READY_FOR_PLACEMENT')).toBe('ready for placement');
    expect(statusPhrase(undefined)).toBe('');
  });

  it('shows a template version as a version number', () => {
    expect(versionLabel('PLACEMENT_SLIP v1')).toBe('Version 1');
    expect(versionLabel('QUOTATION_INTAKE v12')).toBe('Version 12');
    expect(versionLabel('Manual')).toBe('Manual');
    expect(versionLabel(undefined)).toBe('');
  });
});
