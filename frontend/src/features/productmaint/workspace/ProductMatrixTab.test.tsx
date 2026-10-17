import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '@/api/client';
import { deactivationApi, pmWorkspaceApi } from '@/api/pmWorkspace';
import type { MatrixRow } from '@/api/pmWorkspace';
import { ProductMatrixTab } from './ProductMatrixTab';
import { Providers } from './testProviders';

const ROW: MatrixRow = {
  productCode: 'PAR25',
  lineCode: 'PROPERTY',
  lineOfInsurance: 'Property',
  subLine: 'Fire',
  packageName: 'Property package PAR25',
  description: 'Fire and allied perils for homes',
  productType: 'Package',
  insurers: 'Malayan Insurance',
  status: 'Expiring',
  effectiveDate: '2020-01-01',
  expiryDate: '2026-11-23',
  daysLeft: 45,
  lastUpdatedBy: 'mbs',
  lastUpdatedAt: '2026-10-01T02:00:00Z',
  versionNo: 1,
  deactivationId: null,
  deactivationNo: null,
};

describe('Product Matrix', () => {
  afterEach(() => vi.restoreAllMocks());

  it("shows BDOI's columns and the days left of an expiring package", async () => {
    vi.spyOn(api, 'get').mockResolvedValue([]);
    vi.spyOn(deactivationApi, 'settings').mockResolvedValue({
      route: 'EFFECTIVE_DATED',
      approvers: ['tsuhead'],
    });
    const matrix = vi.spyOn(pmWorkspaceApi, 'matrix').mockResolvedValue({
      page: { content: [ROW], page: 0, size: 20, totalElements: 1, totalPages: 1 },
      noticeDays: 90,
    });
    render(
      <Providers username="mbs">
        <ProductMatrixTab />
      </Providers>,
    );
    expect(await screen.findByText('Property package PAR25')).toBeInTheDocument();
    expect(screen.getByText('45 days left')).toBeInTheDocument();
    for (const header of [
      'Line of Insurance',
      'Sub-Line',
      'Package Name',
      'Package Description',
      'Insurer',
      'Status',
      'Effective Date',
      'Expiry Date',
      'Last Updated By',
      'Last Updated Date',
    ]) {
      expect(screen.getAllByText(header).length).toBeGreaterThan(0);
    }
    expect(screen.getByRole('tab', { name: 'Expiring Products (90 days)' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('tab', { name: 'Expired Products' }));
    expect(matrix).toHaveBeenLastCalledWith(expect.objectContaining({ tab: 'EXPIRED' }), 0);
  });

  it('asks for the effective date, reason and approver to deactivate a package', async () => {
    vi.spyOn(api, 'get').mockResolvedValue([]);
    vi.spyOn(deactivationApi, 'settings').mockResolvedValue({
      route: 'EFFECTIVE_DATED',
      approvers: ['tsuhead'],
    });
    vi.spyOn(pmWorkspaceApi, 'matrix').mockResolvedValue({
      page: { content: [ROW], page: 0, size: 20, totalElements: 1, totalPages: 1 },
      noticeDays: 90,
    });
    const submit = vi.spyOn(deactivationApi, 'submit');
    render(
      <Providers username="mbs">
        <ProductMatrixTab />
      </Providers>,
    );
    await userEvent.click(await screen.findByRole('button', { name: 'Actions for PAR25' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Deactivate Package' }));
    const dialog = await screen.findByRole('dialog', { name: 'Deactivate Package' });
    await userEvent.click(within(dialog).getByRole('button', { name: 'Submit for Approval' }));
    expect(within(dialog).getByText('Enter the deactivation effective date')).toBeInTheDocument();
    expect(within(dialog).getByText('Select the reason for deactivation')).toBeInTheDocument();
    expect(within(dialog).getByText('Select the approver')).toBeInTheDocument();
    expect(submit).not.toHaveBeenCalled();
  });

  it('opens a retirement request when deactivation goes through package requests', async () => {
    vi.spyOn(api, 'get').mockResolvedValue([]);
    vi.spyOn(deactivationApi, 'settings').mockResolvedValue({
      route: 'RETIRE_REQUEST',
      approvers: [],
    });
    vi.spyOn(pmWorkspaceApi, 'matrix').mockResolvedValue({
      page: { content: [ROW], page: 0, size: 20, totalElements: 1, totalPages: 1 },
      noticeDays: 90,
    });
    render(
      <Providers username="mbs">
        <ProductMatrixTab />
      </Providers>,
    );
    await userEvent.click(await screen.findByRole('button', { name: 'Actions for PAR25' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Deactivate Package' }));
    expect(screen.queryByRole('dialog', { name: 'Deactivate Package' })).not.toBeInTheDocument();
  });
});
