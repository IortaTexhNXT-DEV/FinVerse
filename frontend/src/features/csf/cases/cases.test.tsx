import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import type { ReactElement } from 'react';
import { Route, Routes } from 'react-router-dom';
import { csfApi } from '@/api/csf';
import { csfCaseApi } from '@/api/csfCases';
import type { CaseDashboard, CaseRecord, CaseRow } from '@/api/csfCases';
import { lovApi } from '@/api/lov';
import { submittedWrapper } from '../../submitted/testWrapper';
import { account } from '../csfFixtures';
import CaseDashboardPage from './CaseDashboardPage';
import CaseRecordPage from './CaseRecordPage';
import CaseRoutingPage from './CaseRoutingPage';
import CasesPage from './CasesPage';
import { queuesFor, slaTone } from './caseCodes';
import { LogCaseDialog } from './LogCaseDialog';

const AGENT = new Set(['CSF_VIEW', 'CSF_CASE_CREATE']);
const SUPERVISOR = new Set([...AGENT, 'CSF_CASE_SUPERVISE', 'CSF_CASE_REPORT']);
const FULFILMENT = new Set(['CSF_CASE_FULFIL']);

const coded = (code: string, label = code) => ({ code, label });

function row(over: Partial<CaseRow> = {}): CaseRow {
  return {
    id: 7,
    caseNo: 'CASE-2026-000007',
    createdAt: '2026-10-12T02:00:00Z',
    client: 'CL-2026-000001 Maria Santos',
    clientId: 1,
    arn: 'ARN-1',
    policyNo: 'POL-1',
    caseType: coded('REQUEST', 'Request'),
    tatDays: 4,
    nature: coded('OR_REQUEST', 'Request for official receipt'),
    fulfilmentUnit: coded('PROCESSING', 'Processing'),
    marketSegment: coded('CONSUMER_1', 'Consumer 1'),
    priority: coded('NORMAL', 'Normal'),
    owner: 'csfagent',
    agent: 'csfagent',
    status: 'WITH_FULFILMENT',
    statusLabel: 'Open - With Fulfillment Unit',
    statusGroup: 'Open / In Progress',
    dueDate: '2026-10-16',
    sla: { ageDays: 3, status: 'APPROACHING', label: 'Approaching breach' },
    escalated: { reason: coded('PAST_TAT', 'Past TAT'), at: '2026-10-15T02:00:00Z' },
    subject: 'Copy of the official receipt',
    ...over,
  };
}

function record(over: Partial<CaseRecord> = {}): CaseRecord {
  return {
    row: row(),
    companyId: 1,
    channel: coded('PHONE', 'Phone'),
    productType: coded('MOTOR', 'Motor Car'),
    description: 'The client asks for a copy',
    callerContact: null,
    returnReason: null,
    instruction: null,
    resolution: null,
    closureReason: null,
    closedAt: null,
    closedBy: null,
    linked: [
      {
        id: 3,
        caseNo: 'CASE-2026-000003',
        relation: 'ORIGINAL',
        statusLabel: 'Closed by CSR',
        reason: 'Pending',
      },
    ],
    actions: ['RESOLVE', 'RETURN', 'CSR_ACTION', 'NOTE', 'UPLOAD'],
    ...over,
  };
}

const DASHBOARD: CaseDashboard = {
  created: 4,
  closed: 1,
  open: 3,
  averageAgeClosed: 1.5,
  slaCompliance: 100,
  breached: 1,
  notBreached: 3,
  escalated: 1,
  byStatus: [{ code: 'WITH_FULFILMENT', label: 'Open - With Fulfillment Unit', count: 3 }],
  byType: [{ code: 'REQUEST', label: 'Request', count: 4 }],
  byNature: [],
  byUnit: [{ code: 'PROCESSING', label: 'Processing', count: 4 }],
  ageing: [{ code: '0-1', label: '0-1 working days', count: 3 }],
  openCases: [row()],
};

function mockApis() {
  vi.spyOn(lovApi, 'options').mockResolvedValue([{ code: 'X', label: 'Value X' }]);
  vi.spyOn(csfApi, 'accounts').mockResolvedValue([account()]);
  vi.spyOn(csfCaseApi, 'queue').mockResolvedValue([row()]);
  vi.spyOn(csfCaseApi, 'get').mockResolvedValue(record());
  vi.spyOn(csfCaseApi, 'notes').mockResolvedValue([
    { note: 'Called the client', author: 'csfagent', createdAt: '2026-10-13T02:00:00Z' },
  ]);
  vi.spyOn(csfCaseApi, 'history').mockResolvedValue([
    {
      at: '2026-10-13T02:00:00Z',
      user: 'csfsup',
      action: 'UPDATE',
      summary: 'Reassigned',
      from: 'csfagent',
      to: 'csfagent2',
    },
  ]);
  vi.spyOn(csfCaseApi, 'documents').mockResolvedValue([
    {
      id: 5,
      fileName: 'receipt.pdf',
      documentType: 'OFFICIAL_RECEIPT',
      sizeBytes: 10,
      uploadedBy: 'csfagent',
      uploadedAt: '2026-10-13T02:00:00Z',
    },
  ]);
  vi.spyOn(csfCaseApi, 'act').mockResolvedValue(record());
  vi.spyOn(csfCaseApi, 'addNote').mockResolvedValue([]);
  vi.spyOn(csfCaseApi, 'log').mockResolvedValue(record());
  vi.spyOn(csfCaseApi, 'propose').mockResolvedValue({ fulfilmentUnit: 'X' });
  vi.spyOn(csfCaseApi, 'turnaroundTimes').mockResolvedValue([
    { caseType: 'X', tatDays: 3, autoEscalate: true },
  ]);
  vi.spyOn(csfCaseApi, 'saveTurnaroundTime').mockResolvedValue([]);
  vi.spyOn(csfCaseApi, 'routes').mockResolvedValue([
    {
      id: 1,
      caseType: '',
      marketSegment: '',
      nature: '',
      fulfilmentUnit: 'X',
      escalationGroup: null,
      active: true,
    },
  ]);
  vi.spyOn(csfCaseApi, 'saveRoute').mockResolvedValue([]);
  vi.spyOn(csfCaseApi, 'members').mockResolvedValue([{ id: 1, unitCode: 'X', username: 'csffu' }]);
  vi.spyOn(csfCaseApi, 'addMember').mockResolvedValue([]);
  vi.spyOn(csfCaseApi, 'dashboard').mockResolvedValue(DASHBOARD);
}

function show(ui: ReactElement, route = '/', perms: ReadonlySet<string> = AGENT) {
  render(<>{submittedWrapper(perms, 'csfagent', route)(ui)}</>);
}

describe('case management', () => {
  beforeEach(mockApis);
  afterEach(() => vi.restoreAllMocks());

  it('offers the queues of the permissions and the SLA tones', () => {
    const can = (p: string) => FULFILMENT.has(p);
    expect(queuesFor(can).map((q) => q.label)).toEqual(['Fulfilment Unit Queue']);
    expect(slaTone(row().sla)).toBe('flag');
    expect(slaTone({ ageDays: 9, status: 'BREACHED', label: 'Breached' })).toBe('danger');
    expect(slaTone({ ageDays: 0, status: 'WITHIN_TAT', label: 'Within TAT' })).toBe('neutral');
  });

  it('lists the cases of a queue and logs a caller case', async () => {
    show(<CasesPage />, '/csf/cases?queue=TEAM', SUPERVISOR);
    expect(await screen.findByText('CASE-2026-000007')).toBeTruthy();
    expect(screen.getByRole('tab', { name: 'Team Queue' })).toBeTruthy();
    fireEvent.click(screen.getByRole('tab', { name: 'Returned to Me' }));
    await waitFor(() => expect(csfCaseApi.queue).toHaveBeenCalledWith(1, 'RETURNED'));
    fireEvent.click(screen.getByRole('button', { name: 'Log Case' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText(/Caller Name/), { target: { value: 'Ana' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Submit to Fulfilment Unit' }));
    await waitFor(() => expect(csfCaseApi.log).toHaveBeenCalled());
  });

  it('logs a client case with the account and resolves it on the call', async () => {
    show(
      <LogCaseDialog companyId={1} client={{ id: 1, name: 'Maria Santos' }} onClose={vi.fn()} />,
    );
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText(/Subject/), { target: { value: 'S' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Resolve and Close' }));
    await waitFor(() =>
      expect(csfCaseApi.log).toHaveBeenCalledWith(1, expect.objectContaining({ resolveNow: true })),
    );
  });

  it('shows the case record with its tabs and resolves it', async () => {
    show(
      <Routes>
        <Route path="/csf/cases/:id" element={<CaseRecordPage />} />
      </Routes>,
      '/csf/cases/7',
      FULFILMENT,
    );
    expect(await screen.findByText('Case CASE-2026-000007')).toBeTruthy();
    expect(screen.getAllByText('Escalated').length).toBeGreaterThan(0);
    for (const tab of ['Notes', 'Documents', 'Linked Cases', 'History']) {
      fireEvent.click(screen.getByRole('tab', { name: tab }));
    }
    expect(await screen.findByText('Reassigned')).toBeTruthy();
    fireEvent.click(screen.getByRole('tab', { name: 'Notes' }));
    expect(await screen.findByText('Called the client')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Note'), { target: { value: 'Follow up' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add Note' }));
    await waitFor(() => expect(csfCaseApi.addNote).toHaveBeenCalledWith(1, 7, 'Follow up'));
    fireEvent.click(screen.getByRole('tab', { name: 'Linked Cases' }));
    expect(await screen.findByText('CASE-2026-000003')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Return to Agent' }));
    const dialog = await screen.findByRole('dialog');
    fireEvent.change(within(dialog).getByLabelText(/Comment/), { target: { value: 'Dates' } });
    fireEvent.click(within(dialog).getByRole('button', { name: 'Return to Agent' }));
    await waitFor(() =>
      expect(csfCaseApi.act).toHaveBeenCalledWith(
        1,
        7,
        'return',
        expect.objectContaining({ comment: 'Dates' }),
      ),
    );
  });

  it('offers the supervisor actions with their inputs', async () => {
    vi.spyOn(csfCaseApi, 'get').mockResolvedValue(
      record({
        actions: ['CLOSE', 'REASSIGN', 'ESCALATE', 'CHANGE_CATEGORY', 'FOLLOW_UP', 'CSR_ACTION'],
      }),
    );
    show(
      <Routes>
        <Route path="/csf/cases/:id" element={<CaseRecordPage />} />
      </Routes>,
      '/csf/cases/7',
      SUPERVISOR,
    );
    for (const [button, path] of [
      ['Reassign', 'reassign'],
      ['Escalate', 'escalate'],
      ['Change Category', 'category'],
      ['Close Case', 'close'],
      ['Create Follow-up Case', 'follow-up'],
      ['Request CSR Action', 'csr-action'],
    ] as const) {
      fireEvent.click(await screen.findByRole('button', { name: button }));
      const dialog = await screen.findByRole('dialog');
      fireEvent.click(within(dialog).getByRole('button', { name: button }));
      await waitFor(() =>
        expect(csfCaseApi.act).toHaveBeenCalledWith(1, 7, path, expect.any(Object)),
      );
      await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull());
    }
  });

  it('shows the dashboard figures', async () => {
    show(<CaseDashboardPage />, '/csf/case-dashboard', SUPERVISOR);
    expect(await screen.findByText('Approaching Breach')).toBeTruthy();
    expect(screen.getByText('100%')).toBeTruthy();
    fireEvent.change(screen.getByLabelText('Status'), { target: { value: 'CLOSED_BY_CSR' } });
    await waitFor(() =>
      expect(csfCaseApi.dashboard).toHaveBeenCalledWith(
        1,
        expect.objectContaining({ status: 'CLOSED_BY_CSR' }),
      ),
    );
  });

  it('maintains the turnaround times, the routing table and the members', async () => {
    show(<CaseRoutingPage />, '/broking-setup/case-routing', new Set(['CSF_CASE_ROUTING']));
    expect(await screen.findByText('Turnaround Times')).toBeTruthy();
    fireEvent.change(await screen.findByLabelText('Working days of X'), { target: { value: '5' } });
    fireEvent.click(screen.getAllByRole('button', { name: 'Save' })[0]!);
    await waitFor(() =>
      expect(csfCaseApi.saveTurnaroundTime).toHaveBeenCalledWith(
        expect.objectContaining({ tatDays: 5 }),
      ),
    );
    fireEvent.click(await screen.findByRole('button', { name: 'Deactivate' }));
    await waitFor(() => expect(csfCaseApi.saveRoute).toHaveBeenCalled());
    fireEvent.change(screen.getByLabelText(/^User ID/), { target: { value: 'csffu2' } });
    expect(screen.getByText('csffu')).toBeTruthy();
  });
});
