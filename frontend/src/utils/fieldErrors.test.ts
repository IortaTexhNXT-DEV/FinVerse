import { fieldErrorLines, humanizeField, humanizeMessage, visibleErrors } from './fieldErrors';

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

describe('field error lines that already name their field', () => {
  it('does not repeat the field in front of a message that names it', () => {
    expect(
      fieldErrorLines({
        'items[0].sumInsured': 'Item 1: Sum insured is required',
        marketSegment: 'Market segment is required',
        productCode: 'Select the product',
      }),
    ).toEqual([
      'Item 1: Sum insured is required',
      'Market segment is required',
      'Product code: Select the product',
    ]);
  });
});

describe('errors shown on touch or submit', () => {
  const errors = { reasonCode: 'Choose the reason', description: 'Describe the cancellation' };

  it('shows nothing before the user acts, then the touched fields, then all on submit', () => {
    expect(visibleErrors(errors, [], false)).toEqual({});
    expect(visibleErrors(errors, ['description'], false)).toEqual({
      description: 'Describe the cancellation',
    });
    expect(visibleErrors(errors, [], true)).toEqual(errors);
  });
});
