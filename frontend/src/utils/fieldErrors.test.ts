import { fieldErrorLines, humanizeField, humanizeMessage } from './fieldErrors';

describe('field error labels', () => {
  it.each([
    ['lines[0].amount', 'Line 1 amount'],
    ['lines[12].accountCode', 'Line 13 account code'],
    ['risks[1].sumInsured', 'Risk 2 sum insured'],
    ['entries[0].value', 'Entry 1 value'],
    ['code', 'Code'],
    ['businessLine', 'Business line'],
    ['vatRate', 'VAT rate'],
    ['glAccountCode', 'GL account code'],
    ['accountNo', 'Account no.'],
    ['initialPassword.newPassword', 'New password'],
    ['roleCodes', 'Role codes'],
    ['mfad_pct', 'MfAD %'],
  ])('%s reads as "%s"', (path, label) => {
    expect(humanizeField(path)).toBe(label);
  });

  it('lists every field error with its message in server order', () => {
    expect(
      fieldErrorLines({
        'lines[0].amount': 'must be greater than 0',
        narration: 'must not be blank',
      }),
    ).toEqual(['Line 1 amount: must be greater than 0', 'Narration: must not be blank']);
    expect(fieldErrorLines({})).toEqual([]);
  });

  it('explains pattern violations instead of showing the regular expression', () => {
    expect(humanizeMessage('must match "[0-9A-Z.\\-]+"')).toBe(
      'has an invalid format (allowed: capital letters, digits, hyphens, dots)',
    );
    expect(humanizeMessage('must match "x+"')).toBe('has an invalid format');
    expect(humanizeMessage('must not be blank')).toBe('must not be blank');
    expect(fieldErrorLines({ code: 'must match "[A-Z0-9_]+"' })).toEqual([
      'Code: has an invalid format (allowed: capital letters, digits, underscores)',
    ]);
  });
});
