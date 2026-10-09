import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '@/api/client';
import { deactivationApi, pmAuditApi } from '@/api/pmWorkspace';
import type { Deactivation } from '@/api/pmWorkspace';
import DeactivationsPage from './DeactivationsPage';
import { Providers } from './testProviders';

const REQUEST: Deactivation = {
  id: 9,
  requestNo: 'PKD-2026-000009',
  productCode: 'MTR12',
  versionNo: 1,
  packageName: 'Motor package MTR12',
  effectiveDate: '2026-10-20',
  reason: 'LOW_UPTAKE',
  remarks: 'Few accounts',
  approver: 'tsuhead',
  status: 'PENDING',
  statusLabel: 'Pending Approval',
  packageExpiryDate: '2027-01-31',
  decisionRemarks: null,
  decidedBy: null,
  decidedAt: null,
  expiryDate: null,
  requestedBy: 'mbs',
  requestedAt: '2026-10-09T01:00:00Z',
};

const page = { content: [REQUEST], page: 0, size: 20, totalElements: 1, totalPages: 1 };

describe('Deactivation Requests', () => {
  afterEach(() => vi.restoreAllMocks());

  it('lists the requests and lets the approver reject with remarks', async () => {
    vi.spyOn(api, 'get').mockImplementation((path: string) =>
      Promise.resolve(
        path.includes('/attachments/policy')
          ? { allowedExtensions: 'pdf, docx', maxSizeBytes: 10_000_000, maxFiles: 5 }
          : [],
      ),
    );
    vi.spyOn(deactivationApi, 'list').mockResolvedValue(page);
    vi.spyOn(deactivationApi, 'get').mockResolvedValue(REQUEST);
    vi.spyOn(deactivationApi, 'settings').mockResolvedValue({
      route: 'EFFECTIVE_DATED',
      approvers: ['tsuhead', 'tsulead'],
    });
    vi.spyOn(pmAuditApi, 'search').mockResolvedValue({ ...page, content: [] });
    const reject = vi
      .spyOn(deactivationApi, 'reject')
      .mockResolvedValue({ ...REQUEST, status: 'REJECTED', statusLabel: 'Rejected' });
    render(
      <Providers username="tsuhead" path="/product-maintenance/deactivations">
        <DeactivationsPage />
      </Providers>,
    );
    expect(await screen.findByText('Motor package MTR12')).toBeInTheDocument();
    for (const header of [
      'Request Number',
      'Package Name',
      'Deactivation Effective Date',
      'Approval Status',
      'Requested By',
      'Request Date',
      'Package Expiry Date',
    ]) {
      expect(screen.getAllByText(header).length).toBeGreaterThan(0);
    }
    await userEvent.click(screen.getByText('Motor package MTR12'));
    const dialog = await screen.findByRole('dialog', { name: 'Deactivation Request' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Reject' }));
    expect(
      within(dialog).getByText('Enter the approval remarks of the rejection'),
    ).toBeInTheDocument();
    expect(reject).not.toHaveBeenCalled();
    await userEvent.type(within(dialog).getByLabelText('Approval Remarks'), 'Terms renewed');
    await userEvent.click(within(dialog).getByRole('button', { name: 'Reject' }));
    expect(reject).toHaveBeenCalledWith(9, 'Terms renewed');
  });
});
