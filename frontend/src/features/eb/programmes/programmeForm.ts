import { isEmail } from '@/features/crm/clientForm';
import type {
  ContactInput,
  ContactRole,
  ContactView,
  Funding,
  LineInput,
  LineView,
  NewProgrammeInput,
} from '@/api/eb';

/** A benefit line as edited (text fields; numbers as text). */
export interface LineForm {
  benefitLine: string;
  productCode: string;
  incumbentInsurer: string;
  currentPolicyNo: string;
  currentArn: string;
  periodFrom: string;
  periodTo: string;
  headcount: string;
}

/** An HR contact as edited. */
export interface ContactForm {
  name: string;
  email: string;
  mobile: string;
  role: ContactRole | '';
  receivesRa: boolean;
  receivesSoa: boolean;
}

/** The New Programme screen (FR-EB-021 fields). */
export interface ProgrammeForm {
  clientId?: number;
  name: string;
  teamCode: string;
  funding: Funding | '';
  accountOfficer: string;
  renewalEligible: boolean;
  lines: LineForm[];
  contacts: ContactForm[];
}

export type Errors = Record<string, string>;

const MAX_NAME = 200;

export function emptyLine(): LineForm {
  return {
    benefitLine: '',
    productCode: '',
    incumbentInsurer: '',
    currentPolicyNo: '',
    currentArn: '',
    periodFrom: '',
    periodTo: '',
    headcount: '',
  };
}

export function emptyContact(): ContactForm {
  return { name: '', email: '', mobile: '', role: '', receivesRa: true, receivesSoa: false };
}

export function emptyProgramme(): ProgrammeForm {
  return {
    name: '',
    teamCode: '',
    funding: 'EMPLOYER',
    accountOfficer: '',
    renewalEligible: true,
    lines: [emptyLine()],
    contacts: [emptyContact()],
  };
}

/** Field errors of one benefit line (keys without the line prefix). */
export function validateLine(line: LineForm): Errors {
  const errors: Errors = {};
  if (line.benefitLine === '') {
    errors.benefitLine = 'Select the benefit line';
  }
  if (line.periodFrom !== '' && line.periodTo !== '' && line.periodTo <= line.periodFrom) {
    errors.periodTo = 'The period must end after it starts';
  }
  if (line.headcount !== '' && !/^\d+$/.test(line.headcount)) {
    errors.headcount = 'Enter a whole number';
  }
  return errors;
}

/** Field errors of one contact (keys without the contact prefix). */
export function validateContact(contact: ContactForm): Errors {
  const errors: Errors = {};
  if (contact.name.trim() === '') {
    errors.name = 'Enter the contact name';
  }
  if (!isEmail(contact.email.trim())) {
    errors.email = 'Enter a valid e-mail address';
  }
  if (contact.role === '') {
    errors.role = 'Select the contact role';
  }
  return errors;
}

function prefixed(prefix: string, errors: Errors): Errors {
  return Object.fromEntries(Object.entries(errors).map(([k, v]) => [`${prefix}.${k}`, v]));
}

/** Field errors of the whole form, keyed like `name`, `lines.0.benefitLine`, `contacts`. */
export function validateProgramme(form: ProgrammeForm): Errors {
  const errors: Errors = {};
  if (form.clientId === undefined) {
    errors.clientId = 'Select the client';
  }
  if (form.name.trim() === '') {
    errors.name = 'Enter the programme name';
  } else if (form.name.trim().length > MAX_NAME) {
    errors.name = `The programme name can have at most ${String(MAX_NAME)} characters`;
  }
  if (form.teamCode === '') {
    errors.teamCode = 'Select the team';
  }
  if (form.funding === '') {
    errors.funding = 'Select the funding';
  }
  if (form.lines.length === 0) {
    errors.lines = 'Add at least one benefit line';
  }
  if (form.contacts.length === 0) {
    errors.contacts = 'Add at least one HR contact';
  }
  form.lines.forEach((l, i) =>
    Object.assign(errors, prefixed(`lines.${String(i)}`, validateLine(l))),
  );
  form.contacts.forEach((c, i) =>
    Object.assign(errors, prefixed(`contacts.${String(i)}`, validateContact(c))),
  );
  return errors;
}

const blank = (v: string) => (v.trim() === '' ? undefined : v.trim());

export function lineInput(line: LineForm): LineInput {
  return {
    benefitLine: line.benefitLine,
    productCode: blank(line.productCode),
    incumbentInsurer: blank(line.incumbentInsurer),
    currentPolicyNo: blank(line.currentPolicyNo),
    currentArn: blank(line.currentArn),
    periodFrom: blank(line.periodFrom),
    periodTo: blank(line.periodTo),
    headcount: line.headcount === '' ? undefined : Number(line.headcount),
  };
}

export function contactInput(contact: ContactForm): ContactInput {
  return {
    name: contact.name.trim(),
    email: contact.email.trim(),
    mobile: blank(contact.mobile),
    role: contact.role,
    receivesRa: contact.receivesRa,
    receivesSoa: contact.receivesSoa,
  };
}

/** The request of the form. */
export function programmeInput(form: ProgrammeForm): NewProgrammeInput {
  return {
    clientId: form.clientId,
    profile: {
      name: form.name.trim(),
      teamCode: form.teamCode,
      funding: form.funding,
      accountOfficer: blank(form.accountOfficer),
      renewalEligible: form.renewalEligible,
    },
    lines: form.lines.map(lineInput),
    contacts: form.contacts.map(contactInput),
  };
}

/** A line of a programme as edited. */
export function lineForm(line: LineView): LineForm {
  return {
    benefitLine: line.benefitLine,
    productCode: line.productCode ?? '',
    incumbentInsurer: line.incumbentInsurer ?? '',
    currentPolicyNo: line.currentPolicyNo ?? '',
    currentArn: line.currentArn ?? '',
    periodFrom: line.periodFrom ?? '',
    periodTo: line.periodTo ?? '',
    headcount:
      line.headcount === null || line.headcount === undefined ? '' : String(line.headcount),
  };
}

/** A contact of a programme as edited. */
export function contactForm(contact: ContactView): ContactForm {
  return {
    name: contact.name,
    email: contact.email,
    mobile: contact.mobile ?? '',
    role: contact.role,
    receivesRa: contact.receivesRa,
    receivesSoa: contact.receivesSoa,
  };
}

/** The errors of one line or contact of the form, without their prefix. */
export function errorsOf(errors: Errors, prefix: string): Errors {
  const start = `${prefix}.`;
  return Object.fromEntries(
    Object.entries(errors)
      .filter(([k]) => k.startsWith(start))
      .map(([k, v]) => [k.slice(start.length), v]),
  );
}
