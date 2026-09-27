import type { MemberAction, MemberChangeInput, MemberChangeLineInput } from '@/api/ebMarket';

/** A member line as typed. */
export interface ChangeRow {
  action: MemberAction | '';
  employeeNo: string;
  lastName: string;
  firstName: string;
  birthDate: string;
  gender: string;
  civilStatus: string;
  planCode: string;
  dependants: string;
  effectiveDate: string;
}

export interface ChangeForm {
  lineNo: string;
  policyYear: string;
  source: 'AO' | 'CLIENT' | '';
  financial: boolean;
  description: string;
  rows: ChangeRow[];
}

export const BLANK_ROW: ChangeRow = {
  action: '',
  employeeNo: '',
  lastName: '',
  firstName: '',
  birthDate: '',
  gender: '',
  civilStatus: '',
  planCode: '',
  dependants: '',
  effectiveDate: '',
};

export const ACTIONS: readonly { code: MemberAction; label: string }[] = [
  { code: 'ADD', label: 'Add member' },
  { code: 'DELETE', label: 'Delete member' },
  { code: 'CHANGE_PLAN', label: 'Change plan' },
  { code: 'CHANGE_DATA', label: 'Change data' },
];

/** Which member fields an action asks for. */
export function fieldsOf(action: ChangeRow['action']): {
  personal: boolean;
  plan: boolean;
} {
  return {
    personal: action === 'ADD' || action === 'CHANGE_DATA',
    plan: action === 'ADD' || action === 'CHANGE_PLAN',
  };
}

function rowError(r: ChangeRow, n: string): string | undefined {
  if (r.action === '' || r.employeeNo.trim() === '') {
    return `Line ${n}: select the change and enter the employee no.`;
  }
  if (r.effectiveDate === '') {
    return 'Enter the effective date of each line';
  }
  if (
    r.action === 'ADD' &&
    (r.lastName.trim() === '' ||
      r.firstName.trim() === '' ||
      r.birthDate === '' ||
      r.planCode.trim() === '')
  ) {
    return `Enter the name, birth date and plan of employee ${r.employeeNo}`;
  }
  if (r.action === 'CHANGE_PLAN' && r.planCode.trim() === '') {
    return `Enter the new plan of employee ${r.employeeNo}`;
  }
  return undefined;
}

/** The errors of the member change form, by field; empty when it may be captured. */
export function changeErrors(form: ChangeForm): Record<string, string> {
  const errors: Record<string, string> = {};
  if (form.lineNo === '') {
    errors.lineNo = 'Select the benefit line';
  }
  if (form.source === '') {
    errors.source = 'Select who asked for the change';
  }
  if (form.rows.length === 0) {
    errors.rows = 'Add at least one member line';
  }
  const seen = new Set<string>();
  form.rows.forEach((r, i) => {
    const error = rowError(r, String(i + 1));
    const no = r.employeeNo.trim();
    if (error) {
      errors.rows = error;
    } else if (seen.has(no)) {
      errors.rows = `Employee ${no} appears twice in the member change`;
    }
    seen.add(no);
  });
  return errors;
}

const text = (v: string) => (v.trim() === '' ? undefined : v.trim());

function toLine(r: ChangeRow): MemberChangeLineInput {
  const { personal, plan } = fieldsOf(r.action);
  return {
    action: r.action as MemberAction,
    employeeNo: r.employeeNo.trim(),
    effectiveDate: r.effectiveDate,
    member:
      personal || plan
        ? {
            lastName: personal ? text(r.lastName) : undefined,
            firstName: personal ? text(r.firstName) : undefined,
            birthDate: personal ? text(r.birthDate) : undefined,
            gender: personal ? text(r.gender) : undefined,
            civilStatus: personal ? text(r.civilStatus) : undefined,
            planCode: plan ? text(r.planCode) : undefined,
            dependants: r.dependants.trim() === '' ? undefined : Number(r.dependants),
          }
        : undefined,
  };
}

/** The API input of a valid form. */
export function toChangeInput(form: ChangeForm): MemberChangeInput {
  return {
    lineNo: Number(form.lineNo),
    policyYear: form.policyYear === '' ? undefined : Number(form.policyYear),
    source: form.source as 'AO' | 'CLIENT',
    financial: form.financial,
    description: text(form.description),
    lines: form.rows.map(toLine),
  };
}
