import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import * as client from '@/api/client';
import { changeHistoryApi } from '@/api/changeHistory';
import { submittedWrapper } from '../submitted/testWrapper';
import MasterChangeLogPage from './MasterChangeLogPage';

describe('Master Data Change Log', () => {
  afterEach(() => vi.restoreAllMocks());

  it('lists the changes, filters them and exports them', async () => {
    vi.spyOn(changeHistoryApi, 'types').mockResolvedValue(['InsurerProfile', 'Client']);
    vi.spyOn(changeHistoryApi, 'log').mockResolvedValue([
      {
        id: 1,
        entityType: 'InsurerProfile',
        entityId: 12,
        changeKind: 'UPDATE',
        field: 'name',
        oldValue: 'Old Mutual',
        newValue: 'New Mutual',
        maker: 'badmin',
        madeAt: '2026-10-12T02:00:00Z',
        checker: 'approver',
        checkedAt: '2026-10-12T03:00:00Z',
      },
    ]);
    const file = { blob: new Blob(['x']), fileName: 'log.xlsx' };
    vi.spyOn(changeHistoryApi, 'exportLog').mockResolvedValue(file);
    const save = vi.spyOn(client, 'saveFile').mockImplementation(() => undefined);
    render(
      <>{submittedWrapper(new Set(['AUDIT_VIEW']), 'auditor', '/')(<MasterChangeLogPage />)}</>,
    );
    expect(await screen.findByText('New Mutual')).toBeTruthy();
    await screen.findByRole('option', { name: 'Client' });
    expect(screen.getAllByText('Insurer').length).toBeGreaterThan(0);
    fireEvent.change(screen.getByLabelText('Record Type'), { target: { value: 'Client' } });
    fireEvent.change(screen.getByLabelText('Maker'), { target: { value: 'badmin' } });
    await waitFor(() =>
      expect(changeHistoryApi.log).toHaveBeenLastCalledWith({
        entityType: 'Client',
        maker: 'badmin',
      }),
    );
    fireEvent.click(screen.getByRole('button', { name: 'Export to Excel' }));
    await waitFor(() => expect(save).toHaveBeenCalledWith(file.blob, 'log.xlsx'));
  });
});
