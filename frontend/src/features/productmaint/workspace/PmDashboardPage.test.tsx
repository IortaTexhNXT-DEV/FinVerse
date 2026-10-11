import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '@/api/client';
import { pmWorkspaceApi } from '@/api/pmWorkspace';
import { productMaintApi } from '@/api/productmaint';
import PmDashboardPage from './PmDashboardPage';
import { Providers } from './testProviders';

const KPIS = [
  { code: 'INCOMING', label: 'Incoming Requests' },
  { code: 'IN_PROGRESS', label: 'In-Progress Requests' },
  { code: 'FOR_APPROVAL', label: 'For Approval' },
  { code: 'EXPIRING', label: 'Expiring Packages' },
  { code: 'ISSUED', label: 'Issued Proposals' },
  { code: 'DEACTIVATION', label: 'Deactivation Requests' },
] as const;

describe('Product Maintenance Dashboard', () => {
  afterEach(() => vi.restoreAllMocks());

  it("shows BDOI's KPIs and drills down into the requests of a KPI", async () => {
    vi.spyOn(api, 'get').mockResolvedValue([]);
    vi.spyOn(productMaintApi, 'counts').mockResolvedValue({
      stages: [],
      expiring: {},
      advisoriesPending: 0,
      outputsThisWeek: 0,
    });
    vi.spyOn(pmWorkspaceApi, 'officers').mockResolvedValue(['tsu']);
    vi.spyOn(pmWorkspaceApi, 'dashboard').mockResolvedValue({
      counts: {
        incoming: 12,
        inProgress: 5,
        forApproval: 3,
        expiring: 2,
        issued: 4,
        deactivation: 1,
      },
      kpis: [...KPIS],
    });
    const drill = vi.spyOn(pmWorkspaceApi, 'drillDown').mockResolvedValue({
      content: [
        {
          kind: 'DEACTIVATION',
          id: 9,
          requestNo: 'PKD-2026-000009',
          requestType: 'Package Deactivation',
          lineCode: 'MOTOR',
          productLine: 'Motor',
          requestedBy: 'mbs',
          assignee: 'tsuhead',
          status: 'Pending Approval',
          submittedAt: '2026-10-05T01:00:00Z',
          agingDays: 4,
          expiryDate: '2026-10-20',
        },
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    render(
      <Providers>
        <PmDashboardPage />
      </Providers>,
    );
    expect(await screen.findByRole('button', { name: /Incoming Requests/ })).toBeInTheDocument();
    for (const k of KPIS) {
      expect(screen.getAllByText(k.label).length).toBeGreaterThan(0);
    }
    await userEvent.click(screen.getByRole('button', { name: /Deactivation Requests/ }));
    expect(drill).toHaveBeenLastCalledWith(1, expect.anything(), 'DEACTIVATION', 0);
    expect(await screen.findByText('PKD-2026-000009')).toBeInTheDocument();
    expect(screen.getByText('4 days')).toBeInTheDocument();
    for (const header of [
      'Request Number',
      'Request Type',
      'Product Line',
      'Requested By',
      'Assigned TSU Officer',
      'Current Status',
      'Submission Date',
      'Aging',
    ]) {
      expect(screen.getAllByText(header).length).toBeGreaterThan(0);
    }
    expect(screen.getByRole('button', { name: 'Excel' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'PDF' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'CSV' })).toBeInTheDocument();
  });
});
