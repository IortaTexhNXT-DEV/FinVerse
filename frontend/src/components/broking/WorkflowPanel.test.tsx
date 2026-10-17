import { render, screen, waitFor } from '@testing-library/react';
import { workflowApi } from '@/api/workflow';
import type { WorkCaseDetail } from '@/api/workflow';
import { ebWrapper } from '@/features/eb/testWrapper';
import { WorkflowPanel } from './WorkflowPanel';

const STAGES = [
  { code: 'DRAFT', name: 'Draft', initial: true, terminal: false },
  { code: 'SUBMITTED', name: 'Submitted to Processing', initial: false, terminal: false },
  { code: 'VALIDATED', name: 'Validated', initial: false, terminal: true },
];

function detail(stageCode: string, stageName: string): WorkCaseDetail {
  return {
    item: {
      id: 7,
      workflowCode: 'NB_ACCOUNT',
      stageCode,
      stageName,
      entityType: 'Account',
      entityId: '42',
      reference: 'ARN-42',
      title: 'Account ARN-42',
      stageEnteredAt: '2026-09-27T08:00:00Z',
      overdue: false,
      createdBy: 'marketing',
      createdAt: '2026-09-27T08:00:00Z',
    },
    stageTerminal: false,
    actions: [],
    history: [],
    stages: STAGES,
  };
}

describe('workflow panel and record header', () => {
  afterEach(() => vi.restoreAllMocks());

  it('reloads the stepper when the header status changes and refreshes the record on a new stage', async () => {
    const byRecord = vi
      .spyOn(workflowApi, 'byRecord')
      .mockResolvedValueOnce(detail('DRAFT', 'Draft'))
      .mockResolvedValue(detail('SUBMITTED', 'Submitted to Processing'));
    const onChanged = vi.fn();
    const wrap = ebWrapper(new Set());
    const panel = (status: string) => (
      <WorkflowPanel
        entityType="Account"
        entityId={42}
        recordStatus={status}
        onChanged={onChanged}
        showHistory={false}
      />
    );
    const { rerender } = render(wrap(panel('DRAFT')));
    expect(await screen.findByText('Draft', { selector: 'dd' })).toBeInTheDocument();
    expect(onChanged).not.toHaveBeenCalled();

    rerender(wrap(panel('SUBMITTED')));
    expect(
      await screen.findByText('Submitted to Processing', { selector: 'dd' }),
    ).toBeInTheDocument();
    expect(byRecord).toHaveBeenCalledTimes(2);
    await waitFor(() => expect(onChanged).toHaveBeenCalledTimes(1));
  });

  it('does not reload while the header status stays the same', async () => {
    const byRecord = vi.spyOn(workflowApi, 'byRecord').mockResolvedValue(detail('DRAFT', 'Draft'));
    const wrap = ebWrapper(new Set());
    const panel = <WorkflowPanel entityType="Account" entityId={42} recordStatus="DRAFT" />;
    const { rerender } = render(wrap(panel));
    await screen.findByText('Draft', { selector: 'dd' });
    rerender(wrap(panel));
    await screen.findByText('Draft', { selector: 'dd' });
    expect(byRecord).toHaveBeenCalledTimes(1);
  });

  it('shows an action that closes the record off its path in the danger style', async () => {
    vi.spyOn(workflowApi, 'byRecord').mockResolvedValue({
      ...detail('DRAFT', 'Draft'),
      actions: [
        { action: 'return', label: 'Return', toStage: 'RETURNED', generic: true },
        {
          action: 'not_proceeded',
          label: 'Not proceeded',
          toStage: 'NOT_PROCEEDED',
          generic: true,
        },
      ],
    });
    render(ebWrapper(new Set())(<WorkflowPanel entityType="Account" entityId={42} />));
    const exit = await screen.findByRole('button', { name: 'Not Proceeded' });
    expect(exit).toHaveClass('btn-danger');
    expect(screen.getByRole('button', { name: 'Return' })).toHaveClass('btn-secondary');
  });
});
