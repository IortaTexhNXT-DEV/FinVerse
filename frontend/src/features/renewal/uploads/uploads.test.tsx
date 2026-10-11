import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { bulkApi } from '@/api/bulk';
import type { BulkJob } from '@/api/bulk';
import { ApiError } from '@/api/client';
import { renewalUploadsApi } from '@/api/renewalUploads';
import { renewalWrapper } from '../testWrapper';
import { LandingUploads } from './LandingUploads';
import { RenewalUploadDialog } from './RenewalUploadDialog';
import { UPLOAD_MESSAGES, UPLOADS, uploadErrorMessage } from './uploadMessages';

const JOB = { id: 5, jobNo: 'BLK-2026-000005', invalidRows: 0, failedRows: 0 } as BulkJob;
const LAMD = UPLOADS.find((u) => u.kind === 'LAMD');

function pickFile() {
  const input = document.querySelector('input[type="file"]');
  if (input === null) throw new Error('no file input');
  fireEvent.change(input, {
    target: { files: [new File(['PN Number\n1'], 'paid-off.csv', { type: 'text/csv' })] },
  });
}

describe('Renewal landing page uploads', () => {
  beforeEach(() => {
    vi.spyOn(renewalUploadsApi, 'uploads').mockResolvedValue([
      {
        uploadId: 'BLK-2026-000001',
        handler: 'RNW_LAMD_PAID_OFF',
        fileName: 'paid-off.csv',
        uploadedAt: '2026-10-01T02:00:00Z',
        uploadedBy: 'lamd',
        status: 'Partially Successful',
        remarks: '1 of 2 records not processed: see the record-level results',
        total: 2,
        processed: 1,
        failed: 1,
      },
    ]);
  });

  it('shows the uploads the user may run and their outcome summary', async () => {
    const wrap = renewalWrapper(new Set(['RNW_LAMD_UPLOAD']));
    render(wrap(<LandingUploads />));
    expect(screen.getByRole('button', { name: 'LAMD Report Upload' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'BDOFC/SOLD Report Upload' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Insurer Disposition Upload' })).toBeNull();
    expect(await screen.findByText('paid-off.csv')).toBeInTheDocument();
    expect(screen.getByText(/1 of 2 records not processed/)).toBeInTheDocument();
  });

  it('processes a valid LAMD file at once with BDOI message', async () => {
    vi.spyOn(bulkApi, 'upload').mockResolvedValue(JOB);
    const commit = vi.spyOn(bulkApi, 'commit').mockResolvedValue(JOB);
    const wrap = renewalWrapper(new Set(['RNW_LAMD_UPLOAD']));
    if (LAMD === undefined) throw new Error('no LAMD upload');
    render(wrap(<RenewalUploadDialog upload={LAMD} onClose={vi.fn()} />));
    fireEvent.change(screen.getByRole('combobox'), { target: { value: '1' } });
    pickFile();
    fireEvent.click(screen.getByRole('button', { name: 'Upload' }));
    expect(await screen.findByText(LAMD.done)).toBeInTheDocument();
    await waitFor(() => expect(commit).toHaveBeenCalledWith(5));
  });

  it('names the BDOI message of a refused file', () => {
    expect(uploadErrorMessage(new ApiError(400, { code: 'BULK_FILE_TYPE', detail: 'type' }))).toBe(
      UPLOAD_MESSAGES.invalidFormat,
    );
    expect(
      uploadErrorMessage(new ApiError(400, { code: 'BULK_FILE_UNREADABLE', detail: 'x' })),
    ).toBe(UPLOAD_MESSAGES.unreadable);
    expect(uploadErrorMessage(new Error('x'))).toBe(UPLOAD_MESSAGES.processingError);
  });
});
