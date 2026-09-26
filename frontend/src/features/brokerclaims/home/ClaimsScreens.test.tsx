import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { claimStatusApi } from '../status/api';
import type { ClaimProgress } from '../status/api';
import { ClaimStatusPanel } from '../status/ClaimStatusPanel';
import { HistoryTab } from '../status/HistoryTab';
import { diaryApi } from '../diary/api';
import { DiaryTab } from '../diary/DiaryTab';
import DiaryPage from '../diary/DiaryPage';
import WorklistPage from '../worklist/WorklistPage';
import { claimsHomeApi } from './api';
import ClaimsHomePage from './ClaimsHomePage';
import { claimsWrapper } from './testWrapper';

const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 20,
  totalElements: content.length,
  totalPages: 1,
});

const PROGRESS: ClaimProgress = {
  claimId: 7,
  claimNo: 'BCL-2026-000102',
  handler: 'clmofficer',
  status: {
    code: 'INSURER_REVIEW',
    label: "For Insurer's Review and Evaluation",
    phase: 'IN_PROGRESS',
    awaitingPremiumRemittance: false,
  },
  ages: { thisStage: 2, overall: 7 },
  settlement: { typeLabel: '' },
  followUp: { nextFollowUpDate: '2026-10-03', overridden: true, nextActionPlan: 'Chase LOA' },
};

describe('Claims Handling screens', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows the home tiles and opens Record Claim for a recorder', async () => {
    vi.spyOn(claimsHomeApi, 'home').mockResolvedValue({
      tiles: [{ key: 'mine', label: 'My Open Claims', value: 3, alert: false, link: '/x' }],
      byStatus: [
        { phase: 'NEW', statusCode: 'NEW_COMPLETE_DOCS', statusLabel: 'Newly Filed', claims: 3 },
      ],
      ageing: [
        { bucket: '0-30', claims: 3 },
        { bucket: '31-60', claims: 1 },
      ],
    });
    render(claimsWrapper(new Set(['BCL_VIEW', 'BCL_RECORD']))(<ClaimsHomePage />));
    expect(await screen.findByText('My Open Claims')).toBeInTheDocument();
    expect(screen.getByText('Newly Filed')).toBeInTheDocument();
    expect(screen.getByText('31-60')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Record Claim' })).toBeInTheDocument();
  });

  it('hides Record Claim without BCL_RECORD', async () => {
    vi.spyOn(claimsHomeApi, 'home').mockResolvedValue({ tiles: [], byStatus: [], ageing: [] });
    render(claimsWrapper(new Set(['BCL_VIEW']))(<ClaimsHomePage />));
    expect(await screen.findByText('No outstanding claims')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Record Claim' })).not.toBeInTheDocument();
  });

  it('lists the worklist and reassigns the selection with WORK_ASSIGN', async () => {
    const user = userEvent.setup();
    vi.spyOn(claimsHomeApi, 'worklist').mockResolvedValue(
      page([
        {
          id: 5,
          claimNo: 'BCL-2026-000101',
          cover: {
            arn: 'ARN-1',
            policyYear: 2026,
            assuredName: 'Juan',
            insurerClaimNos: 'C-INSA-9001',
          },
          handling: { handler: 'clmofficer', phase: 'NEW', statusLabel: 'Newly Filed' },
          dates: {
            reportedDate: '2026-09-01',
            lossDate: '2026-08-31',
            ageThisStage: 1,
            ageOverall: 25,
            followUpOverdue: true,
          },
          currency: 'PHP',
        },
      ]),
    );
    vi.spyOn(claimsHomeApi, 'assignees').mockResolvedValue([
      { username: 'clmofficer2', unitCode: 'NON_MOTOR_HO' },
    ]);
    const reassign = vi.spyOn(claimsHomeApi, 'reassign').mockResolvedValue({ moved: 1 });
    render(
      claimsWrapper(
        new Set(['BCL_VIEW', 'WORK_ASSIGN']),
        'clmtl',
        '/claims-handling/worklist?tab=ALL&flag=OVERDUE',
      )(<WorklistPage />),
    );
    expect(await screen.findByText('C-INSA-9001')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Follow-up overdue/ })).toBeInTheDocument();
    await user.click(screen.getByRole('checkbox', { name: 'Select BCL-2026-000101' }));
    await user.click(screen.getByRole('button', { name: 'Reassign' }));
    await user.click(screen.getAllByRole('button', { name: 'Reassign' }).at(-1)!);
    expect(await screen.findByText('Select the new handler')).toBeInTheDocument();
    await user.selectOptions(await screen.findByLabelText(/New handler/), 'clmofficer2');
    await user.click(screen.getAllByRole('button', { name: 'Reassign' }).at(-1)!);
    await waitFor(() => expect(reassign).toHaveBeenCalledWith(1, [5], 'clmofficer2', undefined));
  });

  it('shows the status panel with the actions of a Team Lead', async () => {
    vi.spyOn(claimStatusApi, 'progress').mockResolvedValue(PROGRESS);
    render(
      claimsWrapper(
        new Set([
          'BCL_VIEW',
          'BCL_STATUS_UPDATE',
          'BCL_SETTLEMENT_UPDATE',
          'BCL_FOLLOW_UP_OVERRIDE',
          'BCL_ACTION_PLAN',
        ]),
        'clmtl',
      )(<ClaimStatusPanel claimId={7} companyId={1} />),
    );
    expect(await screen.findByText('Follow-up overridden')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Change Status' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Set Settlement' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reopen' })).not.toBeInTheDocument();
    expect(screen.getByDisplayValue('Chase LOA')).toBeInTheDocument();
  });

  it('offers only Reopen on a closed claim', async () => {
    vi.spyOn(claimStatusApi, 'progress').mockResolvedValue({
      ...PROGRESS,
      status: { ...PROGRESS.status, phase: 'CLOSED', closureKind: 'PERMANENT' },
    });
    const user = userEvent.setup();
    render(
      claimsWrapper(
        new Set(['BCL_VIEW', 'BCL_STATUS_UPDATE', 'BCL_REOPEN']),
        'clmth',
      )(<ClaimStatusPanel claimId={7} companyId={1} />),
    );
    await user.click(await screen.findByRole('button', { name: 'Reopen' }));
    expect(screen.getByRole('dialog')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Change Status' })).not.toBeInTheDocument();
  });

  it('shows the history with the days in the previous status', async () => {
    vi.spyOn(claimStatusApi, 'history').mockResolvedValue({
      statusChanges: [
        {
          fromStatus: 'ADJUSTER_REVIEW',
          fromLabel: 'For Adjuster Review',
          fromPhase: 'IN_PROGRESS',
          toStatus: 'INSURER_REVIEW',
          toLabel: 'For Insurer Review',
          toPhase: 'IN_PROGRESS',
          stamp: { by: 'clmtl', at: '2026-09-20T01:00:00Z', remark: 'Sent' },
          daysInPrevious: 5,
        },
      ],
      fieldChanges: [
        {
          field: 'FOLLOW_UP',
          oldValue: '2026-09-27',
          newValue: '2026-10-03',
          stamp: { by: 'clmtl', at: '2026-09-20T01:00:00Z' },
        },
      ],
    });
    render(claimsWrapper(new Set(['BCL_VIEW']))(<HistoryTab claimId={7} companyId={1} />));
    expect(await screen.findByText('For Adjuster Review (In progress)')).toBeInTheDocument();
    expect(screen.getByText('5')).toBeInTheDocument();
    expect(screen.getByText('Next follow-up date')).toBeInTheDocument();
  });

  it('adds a diary entry and lists my diary', async () => {
    const user = userEvent.setup();
    vi.spyOn(diaryApi, 'entries').mockResolvedValue([
      {
        id: 1,
        claimId: 7,
        entryType: 'CALL',
        typeLabel: 'Call',
        entryDate: '2026-09-25',
        assignee: 'clmofficer',
        text: 'Called the insurer',
        createdBy: 'clmtl',
        createdAt: '2026-09-25T01:00:00Z',
      },
    ]);
    render(
      claimsWrapper(new Set(['BCL_VIEW', 'BCL_RECORD']))(<DiaryTab claimId={7} companyId={1} />),
    );
    expect(await screen.findByText('Called the insurer')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Mark Done' })).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Add Diary Entry' }));
    await user.click(screen.getByRole('button', { name: 'Add Entry' }));
    expect(await screen.findByText('Enter the diary text')).toBeInTheDocument();

    vi.spyOn(diaryApi, 'mine').mockResolvedValue(
      page([
        {
          id: 2,
          claimId: 7,
          claimNo: 'BCL-2026-000102',
          entryType: 'FOLLOW_UP',
          typeLabel: 'Follow-up',
          entryDate: '2026-09-20',
          dueDate: '2026-09-21',
          text: 'Chase the LOA',
          createdBy: 'clmtl',
          overdue: true,
        },
      ]),
    );
    render(claimsWrapper(new Set(['BCL_VIEW']))(<DiaryPage />));
    expect(await screen.findByText('Chase the LOA')).toBeInTheDocument();
  });
});
