import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { renewalApprovalApi } from '@/api/renewalApproval';
import type { ApprovalState } from '@/api/renewalApproval';
import type { CandidateDetail } from '@/api/renewal';
import { renewalWrapper } from '../testWrapper';
import BillingFilesPage from '../billing/BillingFilesPage';
import { ApprovalTab } from './ApprovalTab';

const STATE: ApprovalState = {
  client: { status: 'PENDING', remarks: null, by: 'ao', at: '2026-10-01T02:00:00Z' },
  approval: {
    required: true,
    status: 'RETURNED',
    remarks: 'Wrong premium',
    by: 'mkttl',
    at: '2026-10-02T02:00:00Z',
    paymentConfirmation: null,
  },
  bookingOnly: {
    tagged: true,
    policyNo: null,
    orNo: null,
    override: null,
    overrideReason: null,
    missing: ['Policy Number', 'OR Number', 'full payment'],
  },
  directToInsurer: { option: 'UNIT_HEAD', status: 'PENDING', decidedBy: null, reason: null },
  payment: { status: 'PARTIALLY_PAID', premium: 10000, paid: 4000, outstanding: 6000 },
  amortized: false,
};

const DETAIL = { row: { renewalRef: 'RNW-2026-000101' } } as unknown as CandidateDetail;

describe('Acceptance and approval', () => {
  it('shows the payment status, what For Booking Only lacks and resubmits a returned account', async () => {
    vi.spyOn(renewalApprovalApi, 'state').mockResolvedValue(STATE);
    const resubmit = vi.spyOn(renewalApprovalApi, 'resubmit').mockResolvedValue({});
    const wrap = renewalWrapper(new Set(['RNW_DISPOSE', 'RNW_VIEW']));
    render(wrap(<ApprovalTab detail={DETAIL} />));
    expect(await screen.findByText('Partially Paid')).toBeInTheDocument();
    expect(screen.getByText(/Policy Number, OR Number, full payment/)).toBeInTheDocument();
    expect(screen.getByText('Pending Unit Head Approval')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Resubmit for Approval' }));
    await waitFor(() => expect(resubmit).toHaveBeenCalledWith(1, 'RNW-2026-000101'));
  });

  it('requires remarks to record a rejection by the client', async () => {
    vi.spyOn(renewalApprovalApi, 'state').mockResolvedValue(STATE);
    const save = vi.spyOn(renewalApprovalApi, 'clientStatus').mockResolvedValue({});
    const wrap = renewalWrapper(new Set(['RNW_DISPOSE']));
    render(wrap(<ApprovalTab detail={DETAIL} />));
    fireEvent.change(await screen.findByLabelText(/^Status/), { target: { value: 'REJECTED' } });
    expect(screen.getByRole('button', { name: 'Save Status' })).toBeDisabled();
    fireEvent.change(screen.getByLabelText(/^Remarks/), { target: { value: 'Moved abroad' } });
    fireEvent.click(screen.getByRole('button', { name: 'Save Status' }));
    await waitFor(() =>
      expect(save).toHaveBeenCalledWith(1, 'RNW-2026-000101', 'REJECTED', 'Moved abroad'),
    );
  });

  it('lists the billing files with their delivery and LMS upload', async () => {
    vi.spyOn(renewalApprovalApi, 'billingFiles').mockResolvedValue([
      {
        id: 1,
        fileName: 'PROPERTY_CLPC Billing Built in_10102026.csv',
        lineCode: 'PROPERTY',
        builtIn: true,
        runKind: 'CBG_HOME',
        accounts: 3,
        attachmentId: 5,
        deliveryStatus: 'FAILED',
        deliveryError: 'No recipient of the billing files is set',
        lmsStatus: 'UPLOADED',
        generatedBy: 'SYSTEM',
        generatedAt: '2026-10-10T06:15:00Z',
      },
    ]);
    const wrap = renewalWrapper(new Set(['RNW_PROCESS']));
    render(wrap(<BillingFilesPage />));
    expect(
      await screen.findByText('PROPERTY_CLPC Billing Built in_10102026.csv'),
    ).toBeInTheDocument();
    expect(screen.getByText('Built-in')).toBeInTheDocument();
    expect(screen.getByText('No recipient of the billing files is set')).toBeInTheDocument();
    expect(screen.getByText('Uploaded')).toBeInTheDocument();
  });
});
