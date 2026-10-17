import { describe, expect, it } from 'vitest';
import type { ConfigUploadJob, ConfigUploadRow } from '@/api/configUploads';
import {
  previewCounts,
  rowAction,
  rowSummary,
  rowTabs,
  uploadActions,
  uploadLink,
  uploadStatus,
} from './uploads';

const job = (over: Partial<ConfigUploadJob> = {}): ConfigUploadJob => ({
  id: 7,
  jobNo: 'BLK-2027-000007',
  handlerCode: 'CFG_BRANCH',
  fileName: 'branches.xlsx',
  status: 'VALIDATED',
  totalRows: 3,
  validRows: 2,
  invalidRows: 1,
  committedRows: 0,
  failedRows: 0,
  createdBy: 'maria',
  createdAt: '2027-01-04T02:00:00Z',
  ...over,
});

const row = (over: Partial<ConfigUploadRow>): ConfigUploadRow => ({
  rowNo: 18,
  status: 'VALID',
  values: {},
  ...over,
});

describe('uploads of the configuration screens', () => {
  it('lets the uploader submit or discard a checked upload', () => {
    const who = { username: 'Maria', mayUpload: true, mayApprove: true };
    expect(uploadActions(job(), who)).toEqual(['submit', 'discard']);
    expect(uploadActions(job({ validRows: 0 }), who)).toEqual(['discard']);
    expect(uploadActions(job({ status: 'SUBMITTED', submittedBy: 'maria' }), who)).toEqual([]);
  });

  it('lets another approver decide a submitted upload', () => {
    const who = { username: 'jose', mayUpload: false, mayApprove: true };
    expect(uploadActions(job({ status: 'SUBMITTED', submittedBy: 'maria' }), who)).toEqual([
      'approve',
      'reject',
    ]);
    expect(uploadActions(job({ status: 'COMPLETED' }), who)).toEqual([]);
    expect(uploadActions(job({ status: 'SUBMITTED' }), { ...who, mayApprove: false })).toEqual([]);
  });

  it('tells what the valid rows do', () => {
    const rows = [
      row({ action: 'ADD' }),
      row({ action: 'UPDATE' }),
      row({ action: 'UPDATE', status: 'COMMITTED' }),
      row({ action: 'ADD', status: 'INVALID' }),
    ];
    expect(previewCounts(rows)).toEqual({ add: 1, update: 2 });
    expect(rowAction('ADD')).toBe('Adds a record');
    expect(rowAction(null)).toBe('—');
  });

  it('shows labels instead of codes', () => {
    expect(uploadStatus('SUBMITTED').label).toBe('Waiting for Approval');
    expect(uploadStatus('COMPLETED').tone).toBe('success');
    expect(
      rowSummary(row({ values: { Code: 'HO', Name: 'Head Office', City: ' ', Region: 'NCR' } })),
    ).toBe('HO · Head Office · NCR');
    expect(uploadLink(7)).toBe('/admin/config-uploads?job=7');
  });

  it('offers the views of the rows of the upload', () => {
    expect(rowTabs(job()).map((t) => t.id)).toEqual(['ALL', 'INVALID', 'VALID']);
    expect(
      rowTabs(job({ status: 'COMPLETED', committedRows: 1, failedRows: 1 })).map((t) => t.label),
    ).toEqual(['All rows', 'Not valid', 'Applied', 'Not applied']);
  });
});
