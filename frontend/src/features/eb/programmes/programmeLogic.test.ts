import { checklistErrors } from './borChecklist';
import { PROGRAMME } from './fixtures';
import { sendRaSummary, tabOf } from './programmeList';
import {
  contactForm,
  emptyContact,
  emptyLine,
  emptyProgramme,
  errorsOf,
  lineForm,
  programmeInput,
  validateContact,
  validateLine,
  validateProgramme,
} from './programmeForm';
import { nextExpiry, stepLabel } from './programmeView';

describe('Employee Benefits programme logic', () => {
  it('reads the tab from the URL', () => {
    expect(tabOf('LOST')).toBe('LOST');
    expect(tabOf('SIDEWAYS')).toBe('RENEWAL_DUE');
    expect(tabOf(null)).toBe('RENEWAL_DUE');
  });

  it('summarises Send RA', () => {
    expect(sendRaSummary([{ sent: true }, { sent: true }])).toBe(
      'Renewal advice sent for 2 programme(s)',
    );
    expect(sendRaSummary([{ sent: true }, { sent: false }])).toBe(
      'Renewal advice sent for 1 programme(s); 1 not sent',
    );
  });

  it('validates a new programme with its lines and contacts', () => {
    const empty = validateProgramme(emptyProgramme());
    expect(empty).toMatchObject({
      clientId: 'Select the client',
      name: 'Enter the programme name',
      teamCode: 'Select the team',
      'lines.0.benefitLine': 'Select the benefit line',
      'contacts.0.email': 'Enter a valid e-mail address',
    });
    expect(validateProgramme({ ...emptyProgramme(), lines: [], contacts: [] })).toMatchObject({
      lines: 'Add at least one benefit line',
      contacts: 'Add at least one HR contact',
    });
    expect(errorsOf(empty, 'contacts.0')).toHaveProperty('role');
    expect(
      validateLine({
        ...emptyLine(),
        benefitLine: 'HMO',
        periodFrom: '2027-01-01',
        periodTo: '2026-01-01',
        headcount: 'x',
      }),
    ).toEqual({
      periodTo: 'The period must end after it starts',
      headcount: 'Enter a whole number',
    });
    expect(
      validateContact({
        ...emptyContact(),
        name: 'Hr',
        email: 'hr@client.example',
        role: 'HR_HEAD',
      }),
    ).toEqual({});
    const input = programmeInput({
      ...emptyProgramme(),
      clientId: 3,
      name: ' Plan ',
      teamCode: 'BDO',
      lines: [{ ...emptyLine(), benefitLine: 'HMO', headcount: '12' }],
      contacts: [{ ...emptyContact(), name: 'Hr', email: 'hr@client.example', role: 'HR_HEAD' }],
    });
    expect(input.profile.name).toBe('Plan');
    expect(input.lines[0]).toMatchObject({
      benefitLine: 'HMO',
      headcount: 12,
      periodTo: undefined,
    });
    expect(input.profile.accountOfficer).toBeUndefined();
  });

  it('turns the programme lines and contacts into forms', () => {
    const [line] = PROGRAMME.lines;
    const [contact] = PROGRAMME.contacts;
    if (line === undefined || contact === undefined) {
      throw new Error('fixture');
    }
    expect(lineForm(line).headcount).toBe('120');
    expect(contactForm(contact).role).toBe('HR_HEAD');
    expect(nextExpiry(PROGRAMME, '2026-09-26')).toBe('2099-01-10');
    expect(nextExpiry(PROGRAMME, '2100-01-01')).toBeUndefined();
    expect(stepLabel('remarket')).toBe('Go to Market');
  });

  it('checks the BOR checklist and validity', () => {
    const full = {
      signedBySignatory: true,
      notBlank: true,
      clientNameMatches: true,
      validFrom: '2026-01-01',
      validTo: '2027-01-01',
    };
    expect(checklistErrors(full)).toEqual({});
    expect(checklistErrors({ ...full, notBlank: false })).toHaveProperty('checklist');
    expect(checklistErrors({ ...full, validTo: '2025-01-01' })).toHaveProperty('validity');
    expect(checklistErrors({ ...full, validFrom: '' })).toHaveProperty('validity');
  });
});
