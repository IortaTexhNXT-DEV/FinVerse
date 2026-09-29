import { isEmail } from '@/features/crm/clientForm';
import type { ItemInput, ItemRow, Responsible } from '@/api/eb';
import { clientShortName } from '@/context/clientNames';

/** A tracked item as edited on the Add / Edit dialog (FR-EB-057). */
export interface ItemForm {
  programmeId: string;
  itemType: string;
  subject: string;
  memberRef: string;
  memberChangeRef: string;
  accountArn: string;
  responsible: Responsible | '';
  partyCode: string;
  recipientEmail: string;
  dueDate: string;
  remarks: string;
}

export function emptyItem(programmeId?: number): ItemForm {
  return {
    programmeId: programmeId === undefined ? '' : String(programmeId),
    itemType: '',
    subject: '',
    memberRef: '',
    memberChangeRef: '',
    accountArn: '',
    responsible: 'INSURER',
    partyCode: '',
    recipientEmail: '',
    dueDate: '',
    remarks: '',
  };
}

/** The form of an existing item. */
export function itemForm(row: ItemRow): ItemForm {
  return {
    programmeId: String(row.programmeId),
    itemType: row.itemType,
    subject: row.subject,
    memberRef: row.memberRef ?? '',
    memberChangeRef: row.memberChangeRef ?? '',
    accountArn: row.accountArn ?? '',
    responsible: row.responsible,
    partyCode: row.partyCode ?? '',
    recipientEmail: row.recipientEmail ?? '',
    dueDate: row.dueDate,
    remarks: row.remarks ?? '',
  };
}

/** Field errors of the item form. */
export function validateItem(form: ItemForm, isNew: boolean): Record<string, string> {
  const errors: Record<string, string> = {};
  if (isNew && form.programmeId === '') {
    errors.programmeId = 'Select the programme';
  }
  if (isNew && form.itemType === '') {
    errors.itemType = 'Select the item type';
  }
  if (form.subject.trim() === '') {
    errors.subject = 'Enter what is expected';
  }
  if (form.responsible === '') {
    errors.responsible = 'Select who is responsible for the item';
  }
  if (form.dueDate === '') {
    errors.dueDate = 'Enter the due date';
  }
  const invalid = form.recipientEmail
    .split(',')
    .map((e) => e.trim())
    .filter((e) => e !== '')
    .some((e) => !isEmail(e));
  if (invalid) {
    errors.recipientEmail = 'Enter valid e-mail addresses separated by commas';
  }
  return errors;
}

const blank = (v: string) => (v.trim() === '' ? undefined : v.trim());

/** The request of the item form. */
export function itemInput(form: ItemForm): ItemInput {
  return {
    programmeId: form.programmeId === '' ? undefined : Number(form.programmeId),
    itemType: blank(form.itemType),
    subject: form.subject.trim(),
    memberRef: blank(form.memberRef),
    memberChangeRef: blank(form.memberChangeRef),
    accountArn: blank(form.accountArn),
    responsible: form.responsible,
    partyCode: blank(form.partyCode),
    recipientEmail: blank(form.recipientEmail),
    dueDate: form.dueDate,
    remarks: blank(form.remarks),
  };
}

/** The status changes offered for an item in a status. */
export function actionsFor(status: string): ('RECEIVE' | 'RELEASE' | 'CLOSE')[] {
  switch (status) {
    case 'PENDING':
      return ['RECEIVE', 'CLOSE'];
    case 'RECEIVED':
      return ['RELEASE', 'CLOSE'];
    case 'RELEASED':
      return ['CLOSE'];
    default:
      return [];
  }
}

export const ACTION_LABELS: Record<'RECEIVE' | 'RELEASE' | 'CLOSE', string> = {
  RECEIVE: 'Mark Received',
  RELEASE: 'Mark Released',
  CLOSE: 'Close Item',
};

export const RESPONSIBLE: { code: Responsible; label: string }[] = [
  { code: 'INSURER', label: 'Insurer' },
  { code: 'CLIENT', label: 'Client' },
  {
    code: 'BROKER',
    get label() {
      return clientShortName();
    },
  },
];
