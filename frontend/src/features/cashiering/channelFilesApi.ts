import { api, toQuery } from '@/api/client';
import type { PageResponse } from '@/api/types';

/** BDOI's payment files (FRS.CSH.05.01, 02.02.03 to 02.02.09). */

export type ChannelFileType =
  'BILLS_PAYMENT' | 'CLPC' | 'TRADE' | 'DIRECT_CREDIT' | 'PDC' | 'COMMISSION_SCHEDULE';

export interface ChannelFile {
  id: number;
  uploadRef: string;
  fileType: ChannelFileType;
  fileName: string;
  fileSize: number;
  source: 'UPLOAD' | 'MFT';
  status: 'RECEIVED' | 'REFUSED' | 'PROCESSED' | 'PARTIAL';
  message?: string;
  rowsRead: number;
  rowsFailed: number;
  headerCount?: number;
  controlTotal?: number;
  detailTotal?: number;
  runNo?: string;
  uploadedBy: string;
  receivedAt: string;
  processedAt?: string;
  hasRaw: boolean;
}

export interface RunSummaryRow {
  category: string;
  count: number;
  amount: number;
}

export interface RunLine {
  rowNo: number;
  transactionDate?: string;
  fileName?: string;
  transactionNo?: string;
  amount: number;
  reference?: string;
  accountNo?: string;
  status: string;
  messages?: string;
}

export interface RunReport {
  uploadRef: string;
  fileName: string;
  fileStatus: string;
  message?: string;
  summary: RunSummaryRow[];
  lines: RunLine[];
}

export interface ChannelProfile {
  fileType: ChannelFileType;
  name: string;
  namePattern: string;
  nameExample: string;
  fieldMap: string;
  maxMb: number;
  mftFolder?: string;
  mftEnabled: boolean;
  sourceSystem: string;
}

const BASE = '/cashiering/payment-files';

export const channelFilesApi = {
  upload: (companyId: number, picked: { type: ChannelFileType; file: File }[]) => {
    const form = new FormData();
    form.append('companyId', String(companyId));
    picked.forEach((p) => {
      form.append('types', p.type);
      form.append('files', p.file);
    });
    return api.upload<ChannelFile[]>(BASE, form);
  },
  list: (companyId: number, page = 0) =>
    api.get<PageResponse<ChannelFile>>(`${BASE}${toQuery({ companyId, page })}`),
  report: (id: number) => api.get<RunReport>(`${BASE}/${id}/report`),
  reportFile: (id: number) => api.getFile(`${BASE}/${id}/report.xlsx`),
  validationFile: (id: number) => api.getFile(`${BASE}/${id}/validation.xlsx`),
  failedFile: (id: number) => api.getFile(`${BASE}/${id}/failed.xlsx`),
  rawFile: (id: number) => api.getFile(`${BASE}/${id}/raw`),
  reprocess: (id: number) => api.post<ChannelFile>(`${BASE}/${id}/reprocess`),
  mftIntake: () => api.post<ChannelFile[]>(`${BASE}/mft-intake`),
  profiles: () => api.get<ChannelProfile[]>(`${BASE}/profiles`),
  simulate: (companyId: number, fileType: ChannelFileType, rows: number) =>
    api.download(`${BASE}/simulator${toQuery({ companyId, fileType, rows })}`, {}),
};

/** The file types in the order of BDOI's FRS. */
export const FILE_TYPE_LABELS: Record<ChannelFileType, string> = {
  BILLS_PAYMENT: 'Bills Payment',
  CLPC: 'CLPC',
  TRADE: 'Trade',
  DIRECT_CREDIT: 'Direct Credit',
  PDC: 'Post-dated checks (PMS)',
  COMMISSION_SCHEDULE: 'Commission Schedule',
};

/** Labels of the statuses of a file. */
export const FILE_STATUS_LABELS: Record<ChannelFile['status'], string> = {
  RECEIVED: 'Received',
  REFUSED: 'Refused',
  PROCESSED: 'Processed',
  PARTIAL: 'Processed with failed rows',
};

/** The rows of the upload form that hold a file, in order. */
export function pickedFiles(
  rows: readonly { type: ChannelFileType; file?: File }[],
): { type: ChannelFileType; file: File }[] {
  return rows.flatMap((r) => (r.file === undefined ? [] : [{ type: r.type, file: r.file }]));
}

/** The files of the upload grouped per type, for the list of selected files. */
export function filesPerType(
  rows: readonly { type: ChannelFileType; file?: File }[],
): Record<string, string[]> {
  const grouped: Record<string, string[]> = {};
  pickedFiles(rows).forEach((p) => {
    grouped[FILE_TYPE_LABELS[p.type]] = [...(grouped[FILE_TYPE_LABELS[p.type]] ?? []), p.file.name];
  });
  return grouped;
}
