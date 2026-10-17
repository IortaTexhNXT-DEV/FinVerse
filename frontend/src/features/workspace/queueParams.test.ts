import { queueParamsOf, queuePath, queueViewOf, stageChips } from './queueParams';

describe('My Work filters in the URL', () => {
  it('reads the scope, workflow, stage, overdue and search of the URL', () => {
    expect(queueViewOf(new URLSearchParams(''))).toEqual({ scope: 'ALL' });
    expect(
      queueViewOf(
        new URLSearchParams('scope=MINE&workflow=NB_ACCOUNT&stage=DRAFT&overdue=true&q=Santos'),
      ),
    ).toEqual({
      scope: 'MINE',
      workflow: 'NB_ACCOUNT',
      stage: 'DRAFT',
      overdue: true,
      text: 'Santos',
    });
    // An unknown scope falls back to all queues; a stage needs its workflow.
    expect(queueViewOf(new URLSearchParams('scope=EVERYTHING&stage=DRAFT'))).toEqual({
      scope: 'ALL',
    });
  });

  it('writes the filters back to the URL and builds the links of the dashboard figures', () => {
    const view = { scope: 'UNASSIGNED' as const, workflow: 'BCL_CLAIM', overdue: true };
    expect(queueViewOf(queueParamsOf(view))).toEqual(view);
    expect(queuePath({ scope: 'ALL' })).toBe('/my-work');
    expect(queuePath({ scope: 'ALL', overdue: true, workflow: 'BCL_CLAIM' })).toBe(
      '/my-work?workflow=BCL_CLAIM&overdue=true',
    );
  });

  it('shows the stage filter as a chip named by workflow and stage, never by code', () => {
    const clear = vi.fn();
    const counts = [
      {
        workflowCode: 'NB_ACCOUNT',
        stageCode: 'RETURNED_TO_MARKETING',
        stageName: 'Returned to marketing',
        open: 2,
        overdue: 1,
        mine: 0,
      },
    ];
    expect(stageChips({ scope: 'ALL' }, counts, clear)).toEqual([]);
    const [chip] = stageChips(
      { scope: 'ALL', workflow: 'NB_ACCOUNT', stage: 'RETURNED_TO_MARKETING' },
      counts,
      clear,
    );
    expect(chip?.label).toBe('Accounts: Returned to Marketing');
    chip?.onRemove();
    expect(clear).toHaveBeenCalledOnce();
    expect(stageChips({ workflow: 'BCL_CLAIM' }, counts, clear)[0]?.label).toBe('Claims');
  });
});
