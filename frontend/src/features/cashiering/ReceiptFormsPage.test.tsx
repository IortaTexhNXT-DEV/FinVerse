import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ebWrapper } from '@/features/eb/testWrapper';
import type { ReceiptFormVersion } from './printApi';
import { printApi } from './printApi';
import ReceiptFormsPage from './ReceiptFormsPage';
import { settlementOrsApi } from './settlementOrsApi';
import SettlementOrsPage from './SettlementOrsPage';

const PENDING: ReceiptFormVersion = {
  id: 7,
  formKind: 'AR',
  versionNo: 2,
  text: { footer1: 'THIS IS A SYSTEM GENERATED RECEIPT.' },
  effectiveFrom: '2026-10-09',
  status: 'PENDING_APPROVAL',
  createdBy: 'mbs',
  createdAt: '2026-10-09T01:00:00Z',
};

describe('AR and OR Forms (FRS.CSH.02.06.01)', () => {
  afterEach(() => vi.restoreAllMocks());

  it('approves a version waiting for approval from its row action menu', async () => {
    vi.spyOn(printApi, 'forms').mockResolvedValue([PENDING]);
    const approve = vi
      .spyOn(printApi, 'approveForm')
      .mockResolvedValue({ ...PENDING, status: 'APPROVED' });
    render(ebWrapper(new Set(['MASTER_AUTHORIZE']), 'checker')(<ReceiptFormsPage />));
    await userEvent.click(await screen.findByRole('button', { name: 'Actions for version 2' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Approve' }));
    await waitFor(() => expect(approve).toHaveBeenCalledWith(7));
  });

  it('asks for the reason of a rejection', async () => {
    vi.spyOn(printApi, 'forms').mockResolvedValue([PENDING]);
    const reject = vi
      .spyOn(printApi, 'rejectForm')
      .mockResolvedValue({ ...PENDING, status: 'REJECTED' });
    render(ebWrapper(new Set(['MASTER_AUTHORIZE']), 'checker')(<ReceiptFormsPage />));
    await userEvent.click(await screen.findByRole('button', { name: 'Actions for version 2' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Reject' }));
    const dialog = screen.getByText('Reject AR Form Version 2').closest<HTMLElement>('.modal')!;
    await userEvent.type(
      within(dialog).getByLabelText(/Reason/),
      'Footer line not approved by Legal',
    );
    await userEvent.click(within(dialog).getByRole('button', { name: 'Reject Version' }));
    await waitFor(() =>
      expect(reject).toHaveBeenCalledWith(7, 'Footer line not approved by Legal'),
    );
  });
});

describe('Settlement ORs (FRS.CSH.07.01.01)', () => {
  afterEach(() => vi.restoreAllMocks());

  it('lists the ORs waiting for Disbursement with their batch and status', async () => {
    vi.spyOn(settlementOrsApi, 'list').mockResolvedValue({
      content: [
        {
          id: 1,
          sourceModule: 'REMITTANCE',
          sourceRef: 'RMB-INS-MGIC-000012:COMMISSION',
          awaitRef: 'RMB-INS-MGIC-000012',
          orType: 'COMMISSION',
          payeeName: 'Mabuhay General Insurance Corp.',
          currency: 'PHP',
          amount: 1200,
          status: 'PENDING',
          createdAt: '2026-10-09T01:00:00Z',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    } as never);
    render(ebWrapper(new Set(['CASH_RECEIPT']))(<SettlementOrsPage />));
    expect(await screen.findByText('Waiting for Disbursement approval')).toBeInTheDocument();
    expect(screen.getAllByText('RMB-INS-MGIC-000012').length).toBeGreaterThan(0);
  });
});
