import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactElement } from 'react';
import { Route, Routes } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import { workflowApi } from '@/api/workflow';
import FollowupsPage from './followups/FollowupsPage';
import InsurerBatchesPage from './insurer/InsurerBatchesPage';
import LamdPage from './lamd/LamdPage';
import LettersPage from './letters/LettersPage';
import MyDispositionsPage from './mine/MyDispositionsPage';
import ProcessingPage from './processing/ProcessingPage';
import CandidatePage from './record/CandidatePage';
import { detail, row } from './renewalFixtures';
import ReviewPage from './review/ReviewPage';
import RenewalSetupPage from './setup/RenewalSetupPage';
import { renewalWrapper } from './testWrapper';
import TransfersPage from './transfers/TransfersPage';

const ALL = new Set([
  'RNW_VIEW',
  'RNW_EXTRACT',
  'RNW_ASSIGN',
  'RNW_REVIEW',
  'RNW_OVERRIDE',
  'RNW_DISPOSE',
  'RNW_PROCESS_ASSIGN',
  'RNW_PROCESS',
  'RNW_UPLOAD',
  'RNW_INSURER',
  'RNW_RA_GENERATE',
  'RNW_RA_SEND',
  'RNW_ACCEPT',
  'RNW_FOLLOWUP',
  'RNW_LAMD_UPLOAD',
  'RNW_SETUP',
  'RNW_PACKAGE_REMAP',
  'RNW_EXPORT',
]);

const page = <T,>(content: T[]) => ({
  content,
  page: 0,
  size: 200,
  totalElements: content.length,
  totalPages: 1,
});

const APPROVAL = {
  recordStatus: 'PENDING_AUTHORIZATION',
  maker: 'maker',
  authorizedBy: null,
  authorizedAt: null,
};

function mockApis() {
  vi.spyOn(renewalApi, 'list').mockResolvedValue(page([row()]));
  vi.spyOn(renewalApi, 'transfersIn').mockResolvedValue([
    {
      id: 1,
      renewalRef: 'RNW-1',
      clientName: 'Santos Trading',
      expiry: '2026-10-01',
      fromUnit: 'T-A',
      toUnit: 'T-B',
      reasonCode: null,
      remarks: 'Moved',
      status: 'REQUESTED',
      requestedBy: 'ao',
      requestedAt: '2026-09-01T00:00:00Z',
      decidedBy: null,
      decidedAt: null,
      decisionRemarks: null,
    },
  ]);
  vi.spyOn(renewalApi, 'batches').mockResolvedValue([
    {
      batchNo: 'RIB-1',
      insurerCode: 'INS1',
      expiryFrom: '2026-10-01',
      expiryTo: '2026-10-31',
      status: 'DRAFT',
      lineCount: 2,
      recipients: null,
      sentAt: null,
      sentBy: null,
      replyDue: null,
      attachmentId: null,
    },
  ]);
  vi.spyOn(renewalApi, 'batch').mockResolvedValue({
    batch: {
      batchNo: 'RIB-1',
      insurerCode: 'INS1',
      expiryFrom: '2026-10-01',
      expiryTo: '2026-10-31',
      status: 'DRAFT',
      lineCount: 1,
      recipients: null,
      sentAt: null,
      sentBy: null,
      replyDue: null,
      attachmentId: null,
    },
    headers: ['Policy No.'],
    lines: [{ renewalRef: 'RNW-1', responded: false, columns: ['POL-1'] }],
  });
  vi.spyOn(renewalApi, 'lamdReports').mockResolvedValue([
    {
      reportNo: 'LAMD-1',
      type: 'PAID_OFF',
      period: '2026-09',
      jobNo: 'J1',
      lines: 3,
      matched: 2,
      by: 'lamd',
      at: '2026-09-02T00:00:00Z',
    },
  ]);
  vi.spyOn(renewalApi, 'lamdLines').mockResolvedValue([
    {
      rowNo: 1,
      pnNo: 'PN-9',
      status: 'PAID_OFF',
      statusDate: null,
      borrower: 'Santos',
      match: 'MATCHED',
      renewalRef: 'RNW-1',
      routing: null,
      message: 'Not for Renewal proposed',
    },
  ]);
  vi.spyOn(renewalApi, 'riskCodes').mockResolvedValue([
    {
      id: 1,
      riskCode: 'CAR07',
      lineCode: null,
      reason: 'Old cars',
      effectiveFrom: '2026-01-01',
      effectiveTo: null,
      approval: APPROVAL,
    },
  ]);
  vi.spyOn(renewalApi, 'checkSettings').mockResolvedValue([
    {
      checkCode: 'CLAIMS',
      checkName: 'Claims',
      active: true,
      severity: 'FAIL_REVIEW',
      parameters: null,
      approval: APPROVAL,
    },
  ]);
  vi.spyOn(renewalApi, 'bucketRules').mockResolvedValue([
    {
      id: 1,
      versionNo: 1,
      status: 'ACTIVE',
      effectiveFrom: '2026-01-01',
      description: null,
      maker: 'badmin',
      submittedAt: null,
      approvedBy: null,
      approvedAt: null,
      decisionRemarks: null,
      rules: [
        { priority: 10, checkCode: null, severity: 'WARN', outcome: 'WARN', bucket: 'REVIEW' },
      ],
    },
  ]);
  vi.spyOn(renewalApi, 'packageMap').mockResolvedValue([]);
  vi.spyOn(renewalApi, 'pendingChoices').mockResolvedValue([]);
  vi.spyOn(renewalApi, 'source').mockResolvedValue({ connected: false });
  vi.spyOn(renewalApi, 'corrections').mockResolvedValue([]);
}

const PAGES: [string, () => ReactElement, string][] = [
  ['My Dispositions', () => <MyDispositionsPage />, 'Santos Trading'],
  ['TL Review', () => <ReviewPage />, 'Santos Trading'],
  ['Processing Worklist', () => <ProcessingPage />, 'Santos Trading'],
  ['Letters', () => <LettersPage />, 'Santos Trading'],
  ['Follow-ups', () => <FollowupsPage />, 'Santos Trading'],
  ['Transfers', () => <TransfersPage />, 'Moved'],
  ['Insurer Batches', () => <InsurerBatchesPage />, 'RIB-1'],
  ['LAMD Reports', () => <LamdPage />, 'LAMD-1'],
  ['Renewal Setup', () => <RenewalSetupPage />, 'CAR07'],
];

describe('Renewal screens', () => {
  beforeEach(mockApis);
  afterEach(() => vi.restoreAllMocks());

  it.each(PAGES)('shows %s with its rows', async (title, make, text) => {
    render(renewalWrapper(ALL)(make()));
    expect(screen.getByRole('heading', { level: 1, name: title })).toBeInTheDocument();
    expect(await screen.findByText(text)).toBeInTheDocument();
  });

  it('opens an insurer batch with its lines', async () => {
    render(renewalWrapper(ALL)(<InsurerBatchesPage />));
    fireEvent.click(await screen.findByText('RIB-1'));
    expect(await screen.findByText('POL-1')).toBeInTheDocument();
    // A value of the extract stays on one line; the extract scrolls sideways as a whole.
    expect(screen.getByText('POL-1')).toHaveClass('nowrap');
    expect(screen.getByRole('button', { name: 'Send to Insurer' })).toBeInTheDocument();
  });

  it('opens the lines of a LAMD report', async () => {
    render(renewalWrapper(ALL)(<LamdPage />));
    fireEvent.click(await screen.findByText('LAMD-1'));
    expect(await screen.findByText('Not for Renewal proposed')).toBeInTheDocument();
  });

  it('shows every Setup tab', async () => {
    render(renewalWrapper(ALL)(<RenewalSetupPage />));
    fireEvent.click(screen.getByRole('tab', { name: 'Checks' }));
    expect(await screen.findByText('Claims')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Classification Rules' }));
    expect(await screen.findByText('Rules of Version 1')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'New Version' }));
    expect(screen.getByRole('button', { name: 'Save Draft' })).toBeInTheDocument();
    fireEvent.click(screen.getByRole('button', { name: 'Add Rule' }));
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    fireEvent.click(screen.getByRole('tab', { name: 'Package Map' }));
    expect(await screen.findByText('No package mappings')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Package Choices' }));
    expect(await screen.findByText('No package choices waiting')).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: 'Go-live' }));
    expect(await screen.findByText('loaded by upload')).toBeInTheDocument();
  });

  it('shows the tabs of the renewal record', async () => {
    vi.spyOn(renewalApi, 'get').mockResolvedValue(detail({ stage: 'RA_SENT' }));
    vi.spyOn(workflowApi, 'byRecord').mockRejectedValue(new Error('No workflow'));
    vi.spyOn(renewalApi, 'checks').mockResolvedValue({
      results: [],
      runs: [],
      buckets: [],
      endorsements: [],
    });
    vi.spyOn(renewalApi, 'history').mockResolvedValue({
      dispositions: [],
      assignments: [],
      overrides: [],
    });
    vi.spyOn(renewalApi, 'remarks').mockResolvedValue([]);
    vi.spyOn(renewalApi, 'followups').mockResolvedValue([]);
    vi.spyOn(renewalApi, 'letters').mockResolvedValue([]);
    vi.spyOn(renewalApi, 'acceptances').mockResolvedValue([]);
    vi.spyOn(renewalApi, 'responses').mockResolvedValue([]);
    vi.spyOn(renewalApi, 'computations').mockResolvedValue({
      renewalArn: null,
      expiring: {
        currency: 'PHP',
        netPremium: 1,
        charges: 0,
        grossPremium: 1,
        sumInsured: 1,
        commissionRate: 1,
        commission: 1,
      },
      renewal: {
        currency: 'PHP',
        netPremium: 1,
        charges: 0,
        grossPremium: 1,
        sumInsured: 1,
        commissionRate: 1,
        commission: 1,
      },
    });
    render(
      renewalWrapper(
        ALL,
        'ao',
        '/renewal/candidates/RNW-1',
      )(
        <Routes>
          <Route path="/renewal/candidates/:ref" element={<CandidatePage />} />
        </Routes>,
      ),
    );
    const expectations: [string, string][] = [
      ['Checks', 'The checks have not run yet'],
      ['Account History', 'View Account History'],
      ['Computations', 'Renewal (No Renewal Account Yet)'],
      ['Insurer', 'No response from the insurer'],
      ['Letters', 'No acceptance recorded'],
      ['Remarks & Follow-ups', 'No follow-ups'],
      ['History', 'No overrides'],
    ];
    for (const [tab, text] of expectations) {
      fireEvent.click(await screen.findByRole('tab', { name: tab }));
      expect(await screen.findByText(text)).toBeInTheDocument();
    }
  });
});
