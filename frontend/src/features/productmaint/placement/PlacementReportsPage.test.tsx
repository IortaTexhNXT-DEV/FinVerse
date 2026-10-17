import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { pmPlacementApi } from '@/api/pmPlacement';
import type { PlacementReport } from '@/api/pmPlacement';
import { Providers } from '../workspace/testProviders';
import PlacementReportsPage from './PlacementReportsPage';

const REPORT: PlacementReport = {
  id: 3,
  reportDate: '2026-10-15',
  periodFrom: '2026-10-09',
  periodTo: '2026-10-15',
  fileName: 'PlacementUpdate_10152026.xlsx',
  recordCount: 1,
  scope: 'Quotation requests',
  trigger: 'Scheduled',
  generatedBy: 'System (scheduled)',
  generatedAt: '2026-10-15T00:05:00Z',
};

describe('Placement Update Reports', () => {
  afterEach(() => vi.restoreAllMocks());

  it('lists the repository with the schedule and previews the rows of a report', async () => {
    vi.spyOn(pmPlacementApi, 'settings').mockResolvedValue({
      day: 'THURSDAY',
      time: '08:00',
      periodDays: 7,
      scope: 'QUOTATION',
      nextRun: '2026-10-22T08:00:00',
    });
    vi.spyOn(pmPlacementApi, 'list').mockResolvedValue({
      content: [REPORT],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });
    vi.spyOn(pmPlacementApi, 'preview').mockResolvedValue({
      report: REPORT,
      columns: [
        { key: 'itemNo', label: 'Item No.' },
        { key: 'insured', label: "Insured's Name" },
        { key: 'status', label: 'Status' },
      ],
      rows: [{ itemNo: '1', insured: 'Pacific Harbor Logistics', status: 'Under Negotiation' }],
    });
    const generate = vi.spyOn(pmPlacementApi, 'generate').mockResolvedValue(REPORT);
    render(
      <Providers permissions={['PKG_REPORT_VIEW']}>
        <PlacementReportsPage />
      </Providers>,
    );
    expect(await screen.findByText('PlacementUpdate_10152026.xlsx')).toBeInTheDocument();
    expect(
      screen.getByText('Every Thursday at 08:00, reporting period of 7 days'),
    ).toBeInTheDocument();
    await userEvent.click(
      screen.getByRole('button', { name: 'Actions for PlacementUpdate_10152026.xlsx' }),
    );
    await userEvent.click(screen.getByRole('menuitem', { name: 'Preview' }));
    const dialog = await screen.findByRole('dialog');
    expect(await within(dialog).findByText('Pacific Harbor Logistics')).toBeInTheDocument();
    expect(within(dialog).getByText("Insured's Name")).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Generate Now' }));
    expect(generate).toHaveBeenCalledWith(1);
  });
});
