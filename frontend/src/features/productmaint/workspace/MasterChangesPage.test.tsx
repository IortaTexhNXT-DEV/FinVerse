import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '@/api/client';
import { pmRoutingApi } from '@/api/pmRouting';
import MasterChangesPage from './MasterChangesPage';
import { Providers } from './testProviders';

describe('Product Master Transfers', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows the changes with their status and sends the waiting ones', async () => {
    vi.spyOn(api, 'get').mockResolvedValue([]);
    vi.spyOn(pmRoutingApi, 'masterChanges').mockResolvedValue({
      target: 'Receiving system simulator',
      connected: true,
      pending: 1,
      failed: 1,
      changes: {
        content: [
          {
            id: 3,
            productCode: 'MTR29',
            versionNo: 1,
            changeKind: 'RETIRED',
            effectiveDate: '2026-11-23',
            sourceRequestNo: 'PKD-2026-900003',
            status: 'FAILED',
            attempts: 1,
            fileName: 'ProductMaster_20261009120000.csv',
            sentAt: null,
            error: 'The folder cannot be reached',
            recordedAt: '2026-10-09T01:00:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      },
    });
    const send = vi.spyOn(pmRoutingApi, 'transferNow').mockResolvedValue({
      fileName: 'ProductMaster_20261009123000.csv',
      sent: 1,
      waiting: 0,
      target: 'Receiving system simulator',
    });
    const again = vi.spyOn(pmRoutingApi, 'reprocess').mockResolvedValue({
      fileName: 'ProductMaster_20261009124000.csv',
      sent: 1,
      waiting: 0,
      target: 'Receiving system simulator',
    });
    render(
      <Providers username="mbs" permissions={['PRODUCT_MAINTAIN']}>
        <MasterChangesPage />
      </Providers>,
    );
    expect(await screen.findByText('Package deactivated')).toBeInTheDocument();
    expect(screen.getByText('The folder cannot be reached')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Send Now' }));
    expect(send).toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Actions for MTR29' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Send Again' }));
    expect(again).toHaveBeenCalledWith(3);
  });
});
