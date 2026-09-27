import { api, toQuery } from './client';
import type { JournalLineInput, ManualJournalType } from './gl';

export type Frequency = 'MONTHLY' | 'QUARTERLY' | 'ANNUALLY';

export interface RecurringTemplateInput {
  companyId: number;
  branchId: number;
  name: string;
  journalType: ManualJournalType;
  currency: string;
  narration: string;
  reference?: string;
  frequency: Frequency;
  dayOfMonth: number;
  startDate: string;
  endDate?: string;
  autoReverse: boolean;
  autoSubmit: boolean;
  lines: JournalLineInput[];
}

export interface RecurringTemplate extends RecurringTemplateInput {
  id: number;
  active: boolean;
  lastOccurrenceDate?: string;
  nextOccurrence?: string;
  createdBy: string;
}

export interface GeneratedJournal {
  templateId: number;
  templateName: string;
  occurrenceDate: string;
  batchId: number;
  batchNo: string;
  reversalBatchNo?: string;
  submitted: boolean;
  note?: string;
}

export interface GenerationResult {
  journals: GeneratedJournal[];
  errors: string[];
}

export interface Occurrence {
  occurrenceDate: string;
  batchId: number;
  batchNo?: string;
  batchStatus?: string;
  reversalBatchId?: number;
  reversalBatchNo?: string;
  generatedAt: string;
  generatedBy: string;
}

export type VoucherStatus = 'VALID' | 'CREATED' | 'ERROR';

export interface VoucherResult {
  voucherKey: string;
  firstRow: number;
  lineCount: number;
  totalDebit: number;
  status: VoucherStatus;
  batchId?: number;
  batchNo?: string;
  messages: string[];
}

export interface RowResult {
  rowNumber: number;
  voucherKey: string;
  valid: boolean;
  messages: string[];
}

export interface UploadResult {
  fileName: string;
  /** Upload number recorded as the source of the journals created (import only). */
  uploadReference?: string;
  committed: boolean;
  totalRows: number;
  vouchers: VoucherResult[];
  rows: RowResult[];
}

export const recurringApi = {
  list: (companyId: number) =>
    api.get<RecurringTemplate[]>(`/journals/recurring${toQuery({ companyId })}`),
  create: (body: RecurringTemplateInput) =>
    api.post<RecurringTemplate>('/journals/recurring', body),
  update: (id: number, body: RecurringTemplateInput) =>
    api.put<RecurringTemplate>(`/journals/recurring/${id}`, body),
  setActive: (id: number, active: boolean) =>
    api.post<RecurringTemplate>(`/journals/recurring/${id}/${active ? 'activate' : 'deactivate'}`),
  run: (id: number, date: string) =>
    api.post<GenerationResult>(`/journals/recurring/${id}/run${toQuery({ date })}`),
  occurrences: (id: number) => api.get<Occurrence[]>(`/journals/recurring/${id}/occurrences`),
};

export const journalUploadApi = {
  upload: (companyId: number, file: File, mode: 'VALIDATE' | 'IMPORT') => {
    const form = new FormData();
    form.append('file', file);
    return api.upload<UploadResult>(`/journals/upload${toQuery({ companyId, mode })}`, form);
  },
  template: (format: 'csv' | 'xlsx') =>
    api.getFile(`/journals/upload/template${toQuery({ format })}`),
};
