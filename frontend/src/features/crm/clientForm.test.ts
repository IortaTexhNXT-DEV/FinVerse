import type { ClientDetail } from '@/api/clients';
import {
  EMPTY_CLIENT_FORM,
  ageOn,
  duplicateQuery,
  formatTin,
  fromClient,
  isEmail,
  toRequest,
  validateClient,
} from './clientForm';

const TODAY = '2026-09-24';

describe('client form validation', () => {
  it('needs only the type and the name of a prospect', () => {
    expect(validateClient(EMPTY_CLIENT_FORM, TODAY)).toEqual({
      lastName: 'Enter the last name',
      firstName: 'Enter the first name',
    });
    expect(
      validateClient({ ...EMPTY_CLIENT_FORM, lastName: 'Reyes', firstName: 'Ana' }, TODAY),
    ).toEqual({});
    expect(validateClient({ ...EMPTY_CLIENT_FORM, clientType: 'CORPORATE' }, TODAY)).toEqual({
      corporateName: 'Enter the corporate name',
    });
  });

  it('checks formats, ID type and age', () => {
    const errors = validateClient(
      {
        ...EMPTY_CLIENT_FORM,
        lastName: 'Reyes',
        firstName: 'Ana',
        tin: '123456789',
        email: 'ana@bdo',
        mobile: '0917',
        idNumber: 'P1',
        birthDate: '2015-01-01',
      },
      TODAY,
    );
    expect(Object.keys(errors).sort((a, b) => a.localeCompare(b))).toEqual([
      'birthDate',
      'email',
      'idType',
      'mobile',
      'tin',
    ]);
    expect(errors.birthDate).toContain('at least 18');
    expect(
      validateClient(
        { ...EMPTY_CLIENT_FORM, lastName: 'R', firstName: 'A', birthDate: TODAY },
        TODAY,
      ).birthDate,
    ).toBe('The birth date must be in the past');
    expect(
      validateClient(
        {
          ...EMPTY_CLIENT_FORM,
          clientType: 'CORPORATE',
          corporateName: 'X Inc.',
          birthDate: '2020-01-01',
          mobile: '+639171234567',
          tin: '123-456-789-000',
        },
        TODAY,
      ),
    ).toEqual({});
  });

  it('computes ages and formats TINs and e-mails', () => {
    expect(ageOn('2008-09-24', TODAY)).toBe(18);
    expect(ageOn('2008-09-25', TODAY)).toBe(17);
    expect(ageOn('2008-10-01', TODAY)).toBe(17);
    expect(formatTin('123456789000')).toBe('123-456-789-000');
    expect(formatTin('12-34a5')).toBe('123-45');
    expect(isEmail('ana.reyes@bdo.com.ph')).toBe(true);
    expect(isEmail('a b@bdo.ph')).toBe(false);
    expect(isEmail('a@b@bdo.ph')).toBe(false);
  });
});

describe('client form mapping', () => {
  it('sends only the fields of the client type', () => {
    const body = toRequest(
      {
        ...EMPTY_CLIENT_FORM,
        clientType: 'CORPORATE',
        corporateName: ' Acme Corp. ',
        lastName: 'ignored',
        nationality: 'FILIPINO',
        bankCif: 'CIF-1',
      },
      7,
    );
    expect(body.companyId).toBe(7);
    expect(body.corporateName).toBe('Acme Corp.');
    expect(body.lastName).toBeUndefined();
    expect(body.nationality).toBeUndefined();
    expect(body.bankCif).toBeUndefined();
    expect(toRequest({ ...EMPTY_CLIENT_FORM, bankClient: true, bankCif: 'CIF-1' }).bankCif).toBe(
      'CIF-1',
    );
  });

  it('round-trips an existing client', () => {
    const client = {
      clientType: 'INDIVIDUAL',
      lastName: 'Reyes',
      firstName: 'Ana',
      bankClient: true,
      bankCif: 'CIF-9',
      profile: { riskRating: 'HIGH' },
    } as unknown as ClientDetail;
    const form = fromClient(client);
    expect(form.lastName).toBe('Reyes');
    expect(form.riskRating).toBe('HIGH');
    expect(form.tin).toBe('');
  });

  it('asks for duplicates only once something identifying is entered', () => {
    const named = { ...EMPTY_CLIENT_FORM, lastName: 'Reyes', firstName: 'Ana' };
    expect(duplicateQuery(named, 1)).toBeUndefined();
    expect(duplicateQuery({ ...named, tin: '123-45' }, 1)).toBeUndefined();
    expect(duplicateQuery({ ...named, birthDate: '1990-01-01' }, 1)?.lastName).toBe('Reyes');
    const query = duplicateQuery({ ...named, tin: '123-456-789-000', idNumber: 'P1' }, 1, 5);
    expect(query?.tin).toBe('123-456-789-000');
    expect(query?.idNumber).toBeUndefined();
    expect(query?.excludeId).toBe(5);
    expect(duplicateQuery({ ...named, mobile: '09171234567', email: 'x' }, 1)?.email).toBe(
      undefined,
    );
  });
});
