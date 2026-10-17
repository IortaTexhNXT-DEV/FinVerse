import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/** The claims diary (BRCLM.022/034): /api/v1/broker-claims/{id}/diary and /diary/mine. */

export interface DiaryEntry {
  id: number;
  claimId: number;
  entryType: string;
  typeLabel: string;
  entryDate: string;
  dueDate?: string;
  assignee?: string;
  text: string;
  doneAt?: string;
  doneBy?: string;
  doneRemark?: string;
  createdBy: string;
  createdAt: string;
}

export interface DiaryItem {
  id: number;
  claimId: number;
  claimNo: string;
  assuredName?: string;
  entryType: string;
  typeLabel: string;
  entryDate: string;
  dueDate?: string;
  assignee?: string;
  text: string;
  doneAt?: string;
  doneBy?: string;
  createdBy: string;
  overdue: boolean;
}

export interface DiaryInput {
  entryType: string;
  entryDate?: string;
  dueDate?: string;
  assignee?: string;
  text: string;
}

export const diaryApi = {
  entries: (claimId: number, companyId: number) =>
    api.get<DiaryEntry[]>(`/broker-claims/${claimId}/diary${toQuery({ companyId })}`),
  add: (claimId: number, companyId: number, input: DiaryInput) =>
    api.post<DiaryEntry>(`/broker-claims/${claimId}/diary${toQuery({ companyId })}`, input),
  done: (entryId: number, companyId: number, remark?: string) =>
    api.post<DiaryEntry>(`/broker-claims/diary/${entryId}/done${toQuery({ companyId })}`, {
      remark,
    }),
  mine: (companyId: number, includeDone: boolean, page: number) =>
    api.get<PageResponse<DiaryItem>>(
      `/broker-claims/diary/mine${toQuery({ companyId, includeDone, page, size: 20 })}`,
    ),
};

export interface DiaryForm {
  entryType: string;
  entryDate: string;
  dueDate: string;
  assignee: string;
  text: string;
}

/** Field errors of a diary entry (FR-CM-052). */
export function validateDiary(form: DiaryForm): Partial<Record<keyof DiaryForm, string>> {
  const errors: Partial<Record<keyof DiaryForm, string>> = {};
  if (form.entryType === '') {
    errors.entryType = 'Select the type of entry';
  }
  if (form.text.trim() === '') {
    errors.text = 'Enter the diary text';
  } else if (form.text.length > 2000) {
    errors.text = 'The diary text can have up to 2000 characters';
  }
  if (form.dueDate !== '' && form.entryDate !== '' && form.dueDate < form.entryDate) {
    errors.dueDate = 'The due date must be on or after the entry date';
  }
  return errors;
}

/** Whether the signed-in user may complete an entry (its assignee or author). */
export function mayComplete(entry: DiaryEntry, username: string | undefined): boolean {
  if (entry.doneAt !== undefined || username === undefined) {
    return false;
  }
  const me = username.toLowerCase();
  return entry.assignee?.toLowerCase() === me || entry.createdBy.toLowerCase() === me;
}
