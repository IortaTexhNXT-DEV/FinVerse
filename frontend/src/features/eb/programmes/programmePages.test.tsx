import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ebApi } from '@/api/eb';
import { lovApi } from '@/api/lov';
import { workflowApi } from '@/api/workflow';
import { ebWrapper } from '../testWrapper';
import { CASE, PROGRAMME, ROW, page } from './fixtures';
import NewProgrammePage from './NewProgrammePage';
import ProgrammePage from './ProgrammePage';
import ProgrammesPage from './ProgrammesPage';

const AO = new Set(['EB_VIEW', 'EB_MARKET']);

describe('Employee Benefits programme screens', () => {
  beforeEach(() => {
    vi.spyOn(lovApi, 'options').mockResolvedValue([
      { code: 'BDO', label: 'BDO' },
      { code: 'HMO', label: 'Health Maintenance Organization (HMO)' },
    ]);
  });
  afterEach(() => vi.restoreAllMocks());

  it('lists the programmes and sends the renewal advice of the selection', async () => {
    const user = userEvent.setup();
    const list = vi.spyOn(ebApi, 'programmes').mockResolvedValue(page([ROW]));
    const send = vi
      .spyOn(ebApi, 'sendRa')
      .mockResolvedValue([
        { programmeId: 7, programmeNo: 'EBP-2026-000007', sent: true, cycleNo: 'EBC-2026-000011' },
      ]);
    render(ebWrapper(AO)(<ProgrammesPage />));
    expect(await screen.findByText('EBP-2026-000007')).toBeInTheDocument();
    expect(screen.getByText('Pacific Harbor Logistics Inc.')).toBeInTheDocument();
    expect(list).toHaveBeenCalledWith(1, expect.objectContaining({ tab: 'RENEWAL_DUE' }), 0);
    const sendButton = screen.getByRole('button', { name: /Send RA/ });
    expect(sendButton).toBeDisabled();
    await user.click(screen.getByRole('checkbox', { name: 'Select EBP-2026-000007' }));
    await user.click(screen.getByRole('button', { name: 'Send RA (1)' }));
    await user.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Send RA' }));
    await waitFor(() => expect(send).toHaveBeenCalledWith(1, [7]));
    expect(await screen.findByText('Renewal Advice Results')).toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: 'Lost' }));
    await waitFor(() =>
      expect(list).toHaveBeenLastCalledWith(1, expect.objectContaining({ tab: 'LOST' }), 0),
    );
  });

  it('hides the selection and New Programme without EB_MARKET', async () => {
    vi.spyOn(ebApi, 'programmes').mockResolvedValue(page([ROW]));
    render(ebWrapper(new Set(['EB_VIEW']))(<ProgrammesPage />));
    expect(await screen.findByText('EBP-2026-000007')).toBeInTheDocument();
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
    expect(screen.queryByRole('link', { name: /New Programme/ })).not.toBeInTheDocument();
  });

  it('validates the new programme before creating it', async () => {
    const user = userEvent.setup();
    const create = vi.spyOn(ebApi, 'create');
    vi.spyOn(ebApi, 'accountOfficers').mockResolvedValue(['ebao']);
    render(ebWrapper(AO)(<NewProgrammePage />));
    await user.click(screen.getByRole('button', { name: 'Create Programme' }));
    expect(await screen.findByText('Select the client')).toBeInTheDocument();
    expect(screen.getByText('Enter the programme name')).toBeInTheDocument();
    expect(screen.getByText('Select the benefit line')).toBeInTheDocument();
    expect(screen.getByText('Enter a valid e-mail address')).toBeInTheDocument();
    expect(create).not.toHaveBeenCalled();
    await user.click(screen.getByRole('button', { name: 'Add Line' }));
    expect(screen.getByText('Line 2')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Remove line 2' }));
    expect(screen.queryByText('Line 2')).not.toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Add Contact' }));
    expect(screen.getByText('Contact 2')).toBeInTheDocument();
  });

  it('shows the programme with its cycle, workflow and tabs', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebApi, 'programme').mockResolvedValue(PROGRAMME);
    vi.spyOn(ebApi, 'feedback').mockResolvedValue([
      {
        id: 1,
        cycleId: 11,
        channel: 'EMAIL',
        receivedOn: '2026-09-05',
        text: 'Renew as is',
        fileCount: 0,
        recordedBy: 'ebao',
        recordedAt: '2026-09-05T02:00:00Z',
      },
    ]);
    vi.spyOn(workflowApi, 'byRecord').mockResolvedValue(CASE);
    vi.spyOn(ebApi, 'documents').mockResolvedValue([
      {
        id: 5,
        cycleId: 11,
        cycleNo: 'EBC-2026-000011',
        documentType: 'RENEWAL_ADVICE',
        documentTypeLabel: 'Renewal advice',
        processType: 'RENEWAL_PLACEMENT',
        processLabel: 'Renewal placement',
        versionNo: 1,
        status: 'ACTIVE',
        source: 'SYSTEM',
        attachmentId: 44,
        fileName: 'EBC-2026-000011_RENEWAL_ADVICE.pdf',
        sizeBytes: 1000,
        uploadedBy: 'SYSTEM',
        uploadedAt: '2026-09-01T02:00:00Z',
      },
    ]);
    vi.spyOn(ebApi, 'bors').mockResolvedValue([]);
    vi.spyOn(ebApi, 'accounts').mockResolvedValue([]);
    vi.spyOn(ebApi, 'activity').mockResolvedValue([
      {
        id: 3,
        cycleNo: 'EBC-2026-000011',
        activity: 'RENEWAL_ADVICE',
        receivedAt: '2026-09-01T02:00:00Z',
        releasedAt: '2026-09-01T02:00:00Z',
        actor: 'SYSTEM',
        remarks: 'Sent to hr@client.example',
      },
    ]);
    vi.spyOn(ebApi, 'items').mockResolvedValue(page([]));
    render(ebWrapper(AO, 'ebao', '/eb/programmes/7')(<ProgrammePage />));
    expect(
      await screen.findByRole('heading', { level: 1, name: 'Group Health' }),
    ).toBeInTheDocument();
    expect(await screen.findByText('Renew as is')).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Record Feedback' })).toBeInTheDocument();
    expect(screen.getByText('Renewal Advice – EBC-2026-000011')).toBeInTheDocument();

    await user.click(screen.getByRole('tab', { name: 'Lines' }));
    expect(await screen.findByText('HMO-1')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Contacts' }));
    expect(await screen.findByText('hr@client.example')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Documents' }));
    expect(await screen.findByText('EBC-2026-000011_RENEWAL_ADVICE.pdf')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'BOR' }));
    expect(await screen.findByText('No Broker on Record uploaded')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Accounts' }));
    expect(await screen.findByText(/No accounts yet/)).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'Pending Items' }));
    expect(await screen.findByText('No pending items to display')).toBeInTheDocument();
    await user.click(screen.getByRole('tab', { name: 'History' }));
    expect(await screen.findByText('Sent to hr@client.example')).toBeInTheDocument();
  });

  it('records the feedback of the current cycle', async () => {
    const user = userEvent.setup();
    vi.spyOn(ebApi, 'programme').mockResolvedValue(PROGRAMME);
    vi.spyOn(ebApi, 'feedback').mockResolvedValue([]);
    vi.spyOn(workflowApi, 'byRecord').mockResolvedValue(CASE);
    const record = vi.spyOn(ebApi, 'recordFeedback').mockResolvedValue({
      id: 2,
      cycleId: 11,
      channel: 'EMAIL',
      receivedOn: '2026-09-05',
      fileCount: 0,
      recordedBy: 'ebao',
      recordedAt: '2026-09-05T02:00:00Z',
    });
    render(ebWrapper(AO, 'ebao', '/eb/programmes/7')(<ProgrammePage />));
    await user.click(await screen.findByRole('button', { name: 'Record Feedback' }));
    const dialog = screen.getByRole('dialog');
    await user.click(within(dialog).getByRole('button', { name: 'Record Feedback' }));
    expect(
      await within(dialog).findByText('Enter the feedback or attach a file'),
    ).toBeInTheDocument();
    await user.type(within(dialog).getByLabelText('Feedback'), 'Renew with the same benefits');
    await user.click(within(dialog).getByRole('button', { name: 'Record Feedback' }));
    await waitFor(() =>
      expect(record).toHaveBeenCalledWith(
        1,
        11,
        expect.objectContaining({ text: 'Renew with the same benefits', channel: 'EMAIL' }),
        [],
      ),
    );
  });
});
