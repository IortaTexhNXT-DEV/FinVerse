import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { migrationApi } from '@/api/migration';
import type { Extract } from '@/api/migration';
import { MissingInputs } from '@/components/ui/MissingInputs';
import { ebWrapper } from '@/features/eb/testWrapper';
import ExtractsPage from './ExtractsPage';

const REJECTED: Extract = {
  extractNo: 'EXT-2026-000002',
  objectCode: 'R01',
  layoutCode: 'R01',
  layoutVersion: 1,
  sourceSystem: 'QPS',
  asOf: '2026-11-20T18:00:00',
  sequenceNo: 1,
  mode: 'FULL',
  fileName: 'R01_QPS_20261120_01.csv',
  sha256: 'abc',
  declaredRows: 4,
  parsedRows: 3,
  stagedRows: 0,
  masked: false,
  status: 'REJECTED',
  rejectCode: 'MIG_ROW_COUNT',
  rejectMessage: 'Row count: The file has 3 rows; the control file says 4',
  rejectChecks: [
    { check: 'Row count', reason: 'The file has 3 rows; the control file says 4' },
    {
      check: 'Hash total',
      reason: 'The hash total of the key column is 3; the control file says 4',
    },
  ],
  receivedBy: 'migops',
  receivedAt: '2026-11-21T01:00:00Z',
};

describe('extracts', () => {
  afterEach(() => vi.restoreAllMocks());

  it('says what is still needed next to a disabled button', () => {
    const { rerender } = render(<MissingInputs missing={['the data file', 'the control file']} />);
    expect(
      screen.getByText('Still needed: the data file and the control file.'),
    ).toBeInTheDocument();
    rerender(<MissingInputs missing={[false, undefined]} />);
    expect(screen.queryByText(/Still needed/)).not.toBeInTheDocument();
  });

  it('shows rows received and staged apart and lists every failed check of a rejection', async () => {
    vi.spyOn(migrationApi, 'extracts').mockResolvedValue({
      content: [REJECTED],
      page: 0,
      size: 25,
      totalElements: 1,
      totalPages: 1,
    });
    const upload = vi.spyOn(migrationApi, 'uploadExtract').mockResolvedValue(REJECTED);
    render(ebWrapper(new Set(['MIG_INTAKE']))(<ExtractsPage />));
    expect(
      screen.getByText('Still needed: the data file and the control file.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Upload and Check' })).toBeDisabled();

    const table = await screen.findByRole('table');
    expect(within(table).getByRole('columnheader', { name: 'Rows Received' })).toBeInTheDocument();
    expect(within(table).getByRole('columnheader', { name: 'Rows Staged' })).toBeInTheDocument();
    expect(
      await within(table).findByText('Row count: The file has 3 rows; the control file says 4'),
    ).toBeInTheDocument();

    const inputs = document.querySelectorAll<HTMLInputElement>('input[type="file"]');
    const file = (name: string) => new File(['x'], name, { type: 'text/csv' });
    fireEvent.change(inputs[0]!, { target: { files: [file('R01_QPS_20261120_01.csv')] } });
    fireEvent.change(inputs[1]!, { target: { files: [file('R01_QPS_20261120_01_CONTROL.csv')] } });
    expect(screen.queryByText(/Still needed/)).not.toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Upload and Check' }));
    await waitFor(() => expect(upload).toHaveBeenCalled());
    const notice = await screen.findByRole('alert');
    expect(within(notice).getByText(/EXT-2026-000002 rejected/)).toBeInTheDocument();
    expect(
      within(notice).getByText(
        'Hash total: The hash total of the key column is 3; the control file says 4',
      ),
    ).toBeInTheDocument();
    expect(
      within(notice).getByText('Row count: The file has 3 rows; the control file says 4'),
    ).toBeInTheDocument();
  });
});
