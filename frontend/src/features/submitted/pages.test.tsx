import { fireEvent, render, screen } from '@testing-library/react';
import type { ReactElement } from 'react';
import { Route, Routes } from 'react-router-dom';
import { catalogApi } from '@/api/catalog';
import { lovApi } from '@/api/lov';
import { submittedApi } from '@/api/submitted';
import { workflowApi } from '@/api/workflow';
import ExtractionPage from './extraction/ExtractionPage';
import HandlingFeesPage from './fees/HandlingFeesPage';
import NoTouchPage from './fees/NoTouchPage';
import SubmittedHomePage from './home/SubmittedHomePage';
import IntakePage from './intake/IntakePage';
import LettersPage from './letters/LettersPage';
import MasterlistPage from './masterlist/MasterlistPage';
import PolicyPage from './record/PolicyPage';
import RenewalWorkListPage from './renewals/RenewalWorkListPage';
import IaafPage from './reviews/IaafPage';
import TorPage from './reviews/TorPage';
import RunsPage from './runs/RunsPage';
import SetupPage from './setup/SetupPage';
import {
  fee,
  iaaf,
  letter,
  page,
  policyDetail,
  policyRow,
  renewalRow,
  tor,
} from './submittedFixtures';
import { submittedWrapper } from './testWrapper';

const ALL = new Set([
  'SBM_VIEW',
  'SBM_MAINTAIN',
  'SBM_INTAKE',
  'SBM_PROCESS',
  'SBM_MIGRATE',
  'SBM_LETTER_SEND',
  'SBM_HANDLING_FEE',
  'SBM_RULE_MAINTAIN',
  'SBM_RULE_APPROVE',
  'IAAF_PREPARE',
  'IAAF_APPROVE',
  'TOR_PREPARE',
  'TOR_APPROVE',
  'WORK_ASSIGN',
]);

const CONTROL = {
  recordStatus: 'PENDING_AUTHORIZATION',
  maker: 'badmin',
  authorizedBy: null,
  authorizedAt: null,
};

function mockApis() {
  vi.spyOn(lovApi, 'options').mockResolvedValue([]);
  vi.spyOn(catalogApi, 'insurers').mockResolvedValue([]);
  vi.spyOn(submittedApi, 'users').mockResolvedValue([
    { username: 'sbmhandler', displayName: 'Susana Motor Handler' },
  ]);
  vi.spyOn(submittedApi, 'counts').mockResolvedValue({
    ALL: 21,
    FOR_VALIDATION: 1,
    CLASSIFIED: 2,
    FOR_RENEWAL: 9,
    MANUAL_DISPOSITION: 1,
    NON_RENEWAL: 5,
    FALLOUT: 3,
  });
  vi.spyOn(submittedApi, 'home').mockResolvedValue({
    iaafForApproval: 1,
    iaafApproved: 1,
    torForApproval: 1,
    renewalsPending: 0,
    lettersFailed: 2,
    feesBilled: 3,
    feesTagged: 0,
  });
  vi.spyOn(submittedApi, 'list').mockResolvedValue(page([policyRow()]));
  vi.spyOn(submittedApi, 'get').mockResolvedValue(policyDetail());
  vi.spyOn(submittedApi, 'history').mockResolvedValue([
    {
      field: 'Bucket',
      oldValue: null,
      newValue: 'For Renewal',
      source: 'RUN',
      reference: 'SBR-1',
      by: 'sanitation',
      at: '2026-09-26T00:00:00Z',
    },
  ]);
  vi.spyOn(submittedApi, 'resultsOf').mockResolvedValue({ results: [], limitChecks: [] });
  vi.spyOn(submittedApi, 'reviews').mockResolvedValue([]);
  vi.spyOn(submittedApi, 'iaafOf').mockResolvedValue([iaaf('DRAFT')]);
  vi.spyOn(submittedApi, 'torsOf').mockResolvedValue([tor()]);
  vi.spyOn(submittedApi, 'breaches').mockResolvedValue({ breaches: '' });
  vi.spyOn(submittedApi, 'renewalOf').mockResolvedValue([renewalRow()]);
  vi.spyOn(submittedApi, 'lettersOf').mockResolvedValue([letter('SENT')]);
  vi.spyOn(submittedApi, 'intakeRuns').mockResolvedValue(
    page([
      {
        id: 1,
        runNo: 'SBI-2026-000001',
        sourceCode: 'LFS_INSURANCE',
        bulkJobNo: 'BLK-1',
        fileName: 'lfs.xlsx',
        received: 11,
        created: 11,
        updated: 0,
        duplicate: 0,
        failed: 0,
        status: 'COMPLETED',
        startedAt: '2026-09-25T00:00:00Z',
        finishedAt: null,
        processingRunNo: 'SBR-2026-000001',
      },
    ]),
  );
  vi.spyOn(submittedApi, 'extractions').mockResolvedValue(page([]));
  vi.spyOn(submittedApi, 'runs').mockResolvedValue(
    page([
      {
        id: 1,
        runNo: 'SBR-2026-000001',
        trigger: 'MANUAL',
        scope: 'Seed intake',
        startedAt: '2026-09-25T00:00:00Z',
        finishedAt: '2026-09-25T00:01:00Z',
        startedBy: 'sanitation',
        total: 21,
        passed: 5,
        bucketed: 13,
        fallout: 3,
        overridden: 0,
        breaches: 1,
      },
    ]),
  );
  vi.spyOn(submittedApi, 'results').mockResolvedValue(page([]));
  vi.spyOn(submittedApi, 'iaafs').mockResolvedValue(page([iaaf()]));
  vi.spyOn(submittedApi, 'tors').mockResolvedValue(page([tor()]));
  vi.spyOn(submittedApi, 'renewals').mockResolvedValue(page([renewalRow()]));
  vi.spyOn(submittedApi, 'letters').mockResolvedValue(page([letter()]));
  vi.spyOn(submittedApi, 'printBatches').mockResolvedValue(page([]));
  vi.spyOn(submittedApi, 'fees').mockResolvedValue(page([fee()]));
  vi.spyOn(submittedApi, 'ambiguous').mockResolvedValue([]);
  vi.spyOn(submittedApi, 'noTouch').mockResolvedValue([
    {
      id: 1,
      batchNo: 'SNT-2026-000001',
      insurerCode: 'INS-MGIC',
      period: '2026-11',
      status: 'RETURNED',
      lineCount: 2,
      grossFee: 1500,
      vat: 180,
      wtax: 30,
      exportedFileId: 3,
      statementFileId: null,
      siNo: null,
      journalBatchNo: null,
      returnedAt: null,
      billedAt: null,
    },
  ]);
  vi.spyOn(submittedApi, 'noTouchLines').mockResolvedValue([]);
  vi.spyOn(submittedApi, 'ruleSets').mockResolvedValue([
    {
      id: 1,
      code: 'DISPOSITION',
      step: 'DISPOSITION',
      segment: null,
      businessType: null,
      versionNo: 1,
      status: 'ACTIVE',
      effectiveFrom: '2026-01-01',
      description: null,
      maker: 'badmin',
      submittedAt: null,
      approvedBy: 'mkttl',
      approvedAt: null,
      decisionRemarks: null,
      rules: [],
    },
  ]);
  vi.spyOn(submittedApi, 'limits').mockResolvedValue([
    {
      id: 1,
      control: CONTROL,
      limits: {
        insurerCode: 'INS-MGIC',
        segment: 'CBG_MOTOR',
        line: null,
        maxSumInsured: 5000000,
        maxVehicleAge: 10,
        attribute: null,
        attributeLimit: null,
        description: 'Motor limit',
      },
    },
  ]);
  vi.spyOn(submittedApi, 'matrix').mockResolvedValue([]);
  vi.spyOn(submittedApi, 'scopes').mockResolvedValue([]);
  vi.spyOn(workflowApi, 'byRecord').mockRejectedValue(new Error('no case'));
}

function show(ui: ReactElement, route = '/', perms: ReadonlySet<string> = ALL, user = 'sbmtl') {
  render(<>{submittedWrapper(perms, user, route)(ui)}</>);
}

beforeEach(() => {
  mockApis();
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe('Submitted Policies screens', () => {
  it('shows the home tiles', async () => {
    show(<SubmittedHomePage />);
    expect(await screen.findByText('IAAF for Approval')).toBeInTheDocument();
    expect(screen.getByText('Fallout')).toBeInTheDocument();
  });

  it('lists the masterlist with its tabs and bulk actions', async () => {
    show(<MasterlistPage />);
    expect(await screen.findByText('SBM-2026-000007')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Run Processing/ })).toBeDisabled();
    fireEvent.click(screen.getByLabelText(/Select all rows shown/));
    expect(screen.getByRole('button', { name: /Run Processing/ })).toBeEnabled();
    fireEvent.click(screen.getByRole('button', { name: /Assign Handler/ }));
    expect(await screen.findByText('1 record selected.')).toBeInTheDocument();
  });

  it('opens a record with its tabs', async () => {
    show(
      <Routes>
        <Route path="/submitted/policies/:id" element={<PolicyPage />} />
      </Routes>,
      '/submitted/policies/7',
    );
    expect(await screen.findByText('Emilio Santos', { selector: 'h1' })).toBeInTheDocument();
    for (const tab of [
      'Rule Results',
      'Review & IAAF',
      'TOR',
      'Renewal',
      'Letters',
      'History',
      'Details',
    ]) {
      fireEvent.click(screen.getByRole('tab', { name: new RegExp(tab) }));
    }
    fireEvent.click(screen.getByRole('tab', { name: /Review & IAAF/ }));
    expect(await screen.findByText(/IAAF IAAF-2026-000003/)).toBeInTheDocument();
    fireEvent.click(screen.getByRole('tab', { name: /^TOR/ }));
    expect(await screen.findByText(/TOR TOR-2026-000004/)).toBeInTheDocument();
  });

  it.each([
    ['intake', <IntakePage key="i" />, 'SBI-2026-000001'],
    ['runs', <RunsPage key="r" />, 'SBR-2026-000001'],
    ['iaaf', <IaafPage key="a" />, 'IAAF-2026-000003'],
    ['tor', <TorPage key="t" />, 'TOR-2026-000004'],
    ['renewals', <RenewalWorkListPage key="w" />, 'RNW-2026-000031'],
    ['letters', <LettersPage key="l" />, 'SBL-2026-000011'],
    ['fees', <HandlingFeesPage key="f" />, 'SHF-2026-000012'],
    ['no touch', <NoTouchPage key="n" />, 'SNT-2026-000001'],
    ['setup', <SetupPage key="s" />, 'DISPOSITION'],
  ])('shows the %s screen', async (_name, ui, text) => {
    show(ui);
    expect(await screen.findByText(text)).toBeInTheDocument();
  });

  it('shows the extraction review', async () => {
    show(<ExtractionPage />);
    expect(await screen.findByText('No proposal to confirm')).toBeInTheDocument();
  });

  it('lets a checker approve an IAAF but not its preparer', async () => {
    show(<IaafPage />, '/', ALL, 'sbmchecker');
    expect(await screen.findByRole('button', { name: 'Approve' })).toBeInTheDocument();
  });

  it('shows the insurer limits of Setup with the approval', async () => {
    show(<SetupPage />, '/?tab=limits', ALL, 'mkttl');
    expect(await screen.findByText('Motor limit')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Approve' })).toBeInTheDocument();
  });
});
