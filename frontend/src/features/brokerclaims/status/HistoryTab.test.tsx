import { render, screen } from '@testing-library/react';
import { setUserDirectory } from '@/api/users';
import { ebWrapper } from '@/features/eb/testWrapper';
import { claimStatusApi } from './api';
import { HistoryTab } from './HistoryTab';

describe('claim history', () => {
  afterEach(() => vi.restoreAllMocks());

  it('names the users of status and field changes by display name, never by login', async () => {
    setUserDirectory([{ username: 'clmofficer', displayName: 'Carla Claims Officer' }]);
    vi.spyOn(claimStatusApi, 'history').mockResolvedValue({
      statusChanges: [
        {
          toStatus: 'DOCS_COMPLETE',
          toLabel: 'Documents complete',
          toPhase: 'IN_PROGRESS',
          stamp: { by: 'clmofficer', at: '2026-09-25T02:00:00Z' },
        },
      ],
      fieldChanges: [
        {
          field: 'ACTION_PLAN',
          newValue: 'Call the adjuster',
          stamp: { by: 'clmofficer', at: '2026-09-25T03:00:00Z' },
        },
      ],
    });
    render(ebWrapper(new Set())(<HistoryTab claimId={5} companyId={1} />));
    expect(await screen.findAllByText('Carla Claims Officer')).toHaveLength(2);
    expect(screen.queryByText('clmofficer')).not.toBeInTheDocument();
  });
});
