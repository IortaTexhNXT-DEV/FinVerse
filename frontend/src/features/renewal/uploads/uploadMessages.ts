import { ApiError } from '@/api/client';

/** BDOI's messages of an upload (FRRN.012.01, 012.06, 013.01, 015.03). */
export const UPLOAD_MESSAGES = {
  invalidFormat:
    'Upload failed. Invalid file format. Please upload a supported file type (.xlsx or .csv).',
  missingFields: 'Upload failed. The file is missing required fields. Please verify and try again.',
  unreadable: 'Upload failed. The file is corrupted or cannot be read.',
  processingError:
    'Upload successful, but an error occurred during processing. Please review logs or contact support.',
};

/** The message of a refused upload by the code of the error. */
export function uploadErrorMessage(error: unknown): string {
  const code = error instanceof ApiError ? error.code : '';
  if (code === 'BULK_FILE_TYPE') return UPLOAD_MESSAGES.invalidFormat;
  if (code === 'BULK_FILE_UNREADABLE' || code === 'BULK_FILE_EMPTY')
    return UPLOAD_MESSAGES.unreadable;
  if (code === 'BULK_TEMPLATE_MISMATCH') return UPLOAD_MESSAGES.missingFields;
  return UPLOAD_MESSAGES.processingError;
}

export interface UploadType {
  /** Option shown in the type pop-up. */
  label: string;
  handler: string;
  parameters?: Record<string, string>;
}

export interface UploadDefinition {
  kind: 'LAMD' | 'BDOFC' | 'INSURER' | 'UPDATE' | 'HOLD_COVER' | 'PLACEMENT';
  button: string;
  permission: string;
  /** Completion message of BDOI. */
  done: string;
  types: UploadType[];
}

/** The uploads of the Renewal landing page. */
export const UPLOADS: UploadDefinition[] = [
  {
    kind: 'LAMD',
    button: 'LAMD Report Upload',
    permission: 'RNW_LAMD_UPLOAD',
    done: 'LAMD report is successfully processed. Records have been matched and updated accordingly.',
    types: [
      { label: 'CBG Loans excluding Personal', handler: 'RNW_LAMD_CBG_LOANS' },
      { label: 'List of Paid Off Accounts', handler: 'RNW_LAMD_PAID_OFF' },
    ],
  },
  {
    kind: 'BDOFC',
    button: 'BDOFC/SOLD Report Upload',
    permission: 'RNW_LAMD_UPLOAD',
    done: 'BDOFC/SOLD report is successfully processed. Records have been matched and updated accordingly.',
    types: [
      { label: 'BDOFC', handler: 'RNW_BDOFC_SOLD', parameters: { reportType: 'BDOFC' } },
      { label: 'BDOSOLD', handler: 'RNW_BDOFC_SOLD', parameters: { reportType: 'BDOSOLD' } },
    ],
  },
  {
    kind: 'INSURER',
    button: 'Insurer Disposition Upload',
    permission: 'RNW_INSURER',
    done: 'Insurer Disposition file is successfully processed. Records have been matched and updated accordingly.',
    types: [{ label: 'Insurer disposition file', handler: 'RNW_INSURER_DISPOSITION' }],
  },
  {
    kind: 'UPDATE',
    button: 'Renewal Update Upload',
    permission: 'RNW_UPLOAD',
    done: 'Renewal Update file is successfully processed. Records have been matched and updated accordingly.',
    types: [{ label: 'Renewal Update file', handler: 'RNW_RENEWAL_UPDATE' }],
  },
  {
    kind: 'HOLD_COVER',
    button: 'Hold Cover Response Upload',
    permission: 'RNW_PROCESS',
    done: 'Hold Cover Response file is successfully processed. Records have been matched and updated accordingly.',
    types: [{ label: 'Hold cover response file', handler: 'RNW_HOLD_COVER_RESPONSE' }],
  },
  {
    kind: 'PLACEMENT',
    button: 'Placement Response Upload',
    permission: 'RNW_PROCESS',
    done: 'Placement Response file is successfully processed. Records have been matched and updated accordingly.',
    types: [{ label: 'Placement response file', handler: 'RNW_PLACEMENT_RESPONSE' }],
  },
];
