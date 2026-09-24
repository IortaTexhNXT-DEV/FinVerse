import type { ClientDetail, ClientRequest, DuplicateQuery } from '@/api/clients';
import type { ClientType } from '@/api/crm';

/** Editable client form (all text, blank = not entered). */
export interface ClientForm {
  clientType: ClientType;
  lastName: string;
  firstName: string;
  middleName: string;
  suffix: string;
  corporateName: string;
  birthDate: string;
  tin: string;
  idType: string;
  idNumber: string;
  email: string;
  mobile: string;
  phone: string;
  addressLine: string;
  city: string;
  province: string;
  postalCode: string;
  marketSegment: string;
  bankClient: boolean;
  bankCif: string;
  nationality: string;
  civilStatus: string;
  occupation: string;
  sourceOfFunds: string;
  riskRating: string;
}

export type ClientFormErrors = Partial<Record<keyof ClientForm, string>>;

export const EMPTY_CLIENT_FORM: ClientForm = {
  clientType: 'INDIVIDUAL',
  lastName: '',
  firstName: '',
  middleName: '',
  suffix: '',
  corporateName: '',
  birthDate: '',
  tin: '',
  idType: '',
  idNumber: '',
  email: '',
  mobile: '',
  phone: '',
  addressLine: '',
  city: '',
  province: '',
  postalCode: '',
  marketSegment: '',
  bankClient: false,
  bankCif: '',
  nationality: '',
  civilStatus: '',
  occupation: '',
  sourceOfFunds: '',
  riskRating: '',
};

const TIN = /^\d{3}-\d{3}-\d{3}-\d{3}$/;
const MOBILE = /^(09\d{9}|\+639\d{9})$/;

/** Plausible e-mail: one @, a local part and a dotted domain (same rule as the server). */
export function isEmail(text: string): boolean {
  const at = text.indexOf('@');
  if (at <= 0 || at !== text.lastIndexOf('@') || /\s/.test(text)) {
    return false;
  }
  const domain = text.slice(at + 1);
  const dot = domain.lastIndexOf('.');
  return dot > 0 && dot < domain.length - 2;
}

/** Whole years between a birth date and today. */
export function ageOn(birthDate: string, today: string): number {
  const [by, bm, bd] = birthDate.split('-').map(Number);
  const [ty, tm, td] = today.split('-').map(Number);
  const years = (ty ?? 0) - (by ?? 0);
  const beforeBirthday =
    (tm ?? 0) < (bm ?? 0) || ((tm ?? 0) === (bm ?? 0) && (td ?? 0) < (bd ?? 0));
  return beforeBirthday ? years - 1 : years;
}

function nameErrors(f: ClientForm): ClientFormErrors {
  if (f.clientType === 'CORPORATE') {
    return f.corporateName.trim() === '' ? { corporateName: 'Enter the corporate name' } : {};
  }
  const errors: ClientFormErrors = {};
  if (f.lastName.trim() === '') {
    errors.lastName = 'Enter the last name';
  }
  if (f.firstName.trim() === '') {
    errors.firstName = 'Enter the first name';
  }
  return errors;
}

function birthDateError(f: ClientForm, today: string, minimumAge: number): string | undefined {
  if (f.birthDate === '') {
    return undefined;
  }
  if (f.birthDate >= today) {
    return 'The birth date must be in the past';
  }
  if (f.clientType === 'INDIVIDUAL' && ageOn(f.birthDate, today) < minimumAge) {
    return `An individual policyholder must be at least ${String(minimumAge)} years old`;
  }
  return undefined;
}

/**
 * Field errors of the client form (BRNB.030): names per client type, TIN 000-000-000-000,
 * e-mail, mobile 09xxxxxxxxx or +639xxxxxxxxx, birth date in the past and minimum age. Only the
 * type and the name are needed to save a prospect (BRNB.029).
 */
export function validateClient(f: ClientForm, today: string, minimumAge = 18): ClientFormErrors {
  const errors: ClientFormErrors = { ...nameErrors(f) };
  if (f.tin !== '' && !TIN.test(f.tin.trim())) {
    errors.tin = 'Use the format 000-000-000-000';
  }
  if (f.email !== '' && !isEmail(f.email.trim())) {
    errors.email = 'Enter a valid e-mail address';
  }
  if (f.mobile !== '' && !MOBILE.test(f.mobile.trim())) {
    errors.mobile = 'Use 09xxxxxxxxx or +639xxxxxxxxx';
  }
  if (f.idNumber !== '' && f.idType === '') {
    errors.idType = 'Select the ID type';
  }
  const birth = birthDateError(f, today, minimumAge);
  if (birth !== undefined) {
    errors.birthDate = birth;
  }
  return errors;
}

/** Formats a TIN as the user types: digits grouped 000-000-000-000. */
export function formatTin(input: string): string {
  const digits = input.replace(/\D/g, '').slice(0, 12);
  return (digits.match(/.{1,3}/g) ?? []).join('-');
}

const opt = (value: string): string | undefined => (value.trim() === '' ? undefined : value.trim());

/** Request body of the form (blank fields omitted; person or corporate names only). */
export function toRequest(f: ClientForm, companyId?: number): ClientRequest {
  const person = f.clientType === 'INDIVIDUAL';
  return {
    companyId,
    clientType: f.clientType,
    lastName: person ? opt(f.lastName) : undefined,
    firstName: person ? opt(f.firstName) : undefined,
    middleName: person ? opt(f.middleName) : undefined,
    suffix: person ? opt(f.suffix) : undefined,
    corporateName: person ? undefined : opt(f.corporateName),
    birthDate: person ? opt(f.birthDate) : undefined,
    tin: opt(f.tin),
    idType: opt(f.idType),
    idNumber: opt(f.idNumber),
    email: opt(f.email),
    mobile: opt(f.mobile),
    phone: opt(f.phone),
    addressLine: opt(f.addressLine),
    city: opt(f.city),
    province: opt(f.province),
    postalCode: opt(f.postalCode),
    marketSegment: opt(f.marketSegment),
    bankClient: f.bankClient,
    bankCif: f.bankClient ? opt(f.bankCif) : undefined,
    nationality: person ? opt(f.nationality) : undefined,
    civilStatus: person ? opt(f.civilStatus) : undefined,
    occupation: opt(f.occupation),
    sourceOfFunds: opt(f.sourceOfFunds),
    riskRating: opt(f.riskRating),
  };
}

/** Form values of an existing client (edit). */
export function fromClient(c: ClientDetail): ClientForm {
  const v = (value: string | undefined) => value ?? '';
  return {
    clientType: c.clientType,
    lastName: v(c.lastName),
    firstName: v(c.firstName),
    middleName: v(c.middleName),
    suffix: v(c.suffix),
    corporateName: v(c.corporateName),
    birthDate: v(c.birthDate),
    tin: v(c.tin),
    idType: v(c.idType),
    idNumber: v(c.idNumber),
    email: v(c.email),
    mobile: v(c.mobile),
    phone: v(c.phone),
    addressLine: v(c.addressLine),
    city: v(c.city),
    province: v(c.province),
    postalCode: v(c.postalCode),
    marketSegment: v(c.marketSegment),
    bankClient: c.bankClient,
    bankCif: v(c.bankCif),
    nationality: v(c.profile.nationality),
    civilStatus: v(c.profile.civilStatus),
    occupation: v(c.profile.occupation),
    sourceOfFunds: v(c.profile.sourceOfFunds),
    riskRating: v(c.profile.riskRating),
  };
}

/**
 * Duplicate check of the entered identifiers (BRNB.032), or undefined while nothing identifying
 * has been entered yet (no request is sent).
 */
export function duplicateQuery(
  f: ClientForm,
  companyId: number,
  excludeId?: number,
): DuplicateQuery | undefined {
  const r = toRequest(f, companyId);
  const query: DuplicateQuery = {
    companyId,
    excludeId,
    clientType: f.clientType,
    tin: TIN.test(f.tin) ? f.tin : undefined,
    idType: r.idType,
    idNumber: r.idType === undefined ? undefined : r.idNumber,
    email: r.email !== undefined && isEmail(r.email) ? r.email : undefined,
    mobile: r.mobile !== undefined && MOBILE.test(r.mobile) ? r.mobile : undefined,
    lastName: r.lastName,
    firstName: r.firstName,
    birthDate: r.birthDate,
    corporateName: r.corporateName,
  };
  const identifying = [
    query.tin,
    query.idNumber,
    query.email,
    query.mobile,
    query.birthDate === undefined ? undefined : query.lastName,
    query.corporateName,
  ];
  return identifying.some((v) => v !== undefined) ? query : undefined;
}

/** Human names of the duplicate keys. */
export const DUPLICATE_KEY_LABELS: Record<string, string> = {
  TIN: 'TIN',
  ID: 'ID number',
  NAME_BIRTH_DATE: 'name and birth date',
  EMAIL: 'e-mail',
  MOBILE: 'mobile',
  CORPORATE_NAME: 'corporate name',
};
