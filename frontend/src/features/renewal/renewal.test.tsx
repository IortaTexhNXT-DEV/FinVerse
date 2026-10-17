import { fireEvent, render, screen } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { renewalApi } from '@/api/renewal';
import { workflowApi } from '@/api/workflow';
import type { WorkCaseDetail } from '@/api/workflow';
import { parseSelection, selectionValue } from '@/components/broking/multiSelect';
import { MultiSelectFilter } from '@/components/broking/MultiSelectFilter';
import { mayOpen } from '@/navigation/access';
import { NAV_GROUPS } from '@/navigation/modules';
import { BucketPill, FlagChips, OutcomeDialog } from './common/RenewalBits';
import {
  EXPIRY_TABS,
  LETTER_TABS,
  bucketTone,
  dispositionLabel,
  outcomeSummary,
  stageLink,
  tabOf,
} from './common/renewalCodes';
import ExpiryListPage from './expiry/ExpiryListPage';
import RenewalHomePage from './home/RenewalHomePage';
import { renewalModule } from './module';
import CandidatePage from './record/CandidatePage';
import {
  MATRIX_FIELDS,
  blankBucketRule,
  blankDecisionRule,
  bucketFields,
} from './setup/ruleFields';
import { numberOrNull, severityLabel, textOrNull } from './setup/setupCodes';
import { detail, row } from './renewalFixtures';
import { renewalWrapper } from './testWrapper';

const TL = new Set([
  'RNW_VIEW',
  'RNW_EXTRACT',
  'RNW_ASSIGN',
  'RNW_REVIEW',
  'RNW_OVERRIDE',
  'RNW_DISPOSE',
  'RNW_RA_GENERATE',
  'RNW_RA_SEND',
  'RNW_ACCEPT',
  'RNW_EXPORT',
]);

function openFor(permissions: readonly string[]) {
  const can = (p: string) => permissions.includes(p);
  return renewalModule.screens
    .filter((s) => s.hidden !== true && mayOpen(s, can))
    .map((s) => s.path);
}

describe('Renewal module', () => {
  afterEach(() => vi.restoreAllMocks());

  it('comes right after Placement & Booking in Client & Policy, every screen on an RNW permission', () => {
    const group = NAV_GROUPS.find((g) => g.id === 'client-policy');
    const ids = group?.modules.map((m) => m.id) ?? [];
    expect(ids[ids.indexOf('renewal') - 1]).toBe('booking');
    renewalModule.screens.forEach((s) => expect(s.permission).toMatch(/^RNW_/));
    expect(renewalModule.screens.filter((s) => s.hidden === true).map((s) => s.path)).toEqual([
      '/renewal/candidates/:ref',
    ]);
  });

  it('opens the Contact Center screens only for the follow-up role', () => {
    expect(openFor(['RNW_VIEW', 'RNW_FOLLOWUP'])).toEqual([
      '/renewal',
      '/renewal/expiry',
      '/renewal/followups',
    ]);
    expect(openFor(['RNW_VIEW', 'RNW_PACKAGE_REMAP'])).toContain('/renewal/setup');
  });

  it('gives the TSU group the Renewal TSU queue in its menu', () => {
    expect(openFor(['TSU_PROCESS', 'TSU_APPROVE', 'RNW_TSU_QUEUE'])).toEqual([
      '/renewal/tsu-requests',
    ]);
    expect(openFor(['TSU_PROCESS', 'TSU_APPROVE'])).toEqual([]);
  });

  it('maps the tabs, tones, labels and batch outcomes', () => {
    expect(tabOf(EXPIRY_TABS, 'EXCEPTIONS')).toBe('EXCEPTIONS');
    expect(tabOf(EXPIRY_TABS, 'nope')).toBe('UNASSIGNED');
    expect(tabOf([], null)).toBe('ALL');
    expect(bucketTone('CLEAN')).toBe('success');
    expect(bucketTone('REVIEW')).toBe('warning');
    expect(bucketTone('EXCEPTION')).toBe('danger');
    expect(bucketTone(null)).toBe('neutral');
    expect(dispositionLabel('NOT_FOR_RENEWAL')).toBe('Not for Renewal');
    expect(dispositionLabel('OTHER')).toBe('OTHER');
    expect(dispositionLabel(null)).toBe('');
    expect(outcomeSummary({ done: ['a'], refused: {} }, 'posted')).toBe('1 posted');
    expect(outcomeSummary({ done: [], refused: { b: 'x' } }, 'posted')).toBe('0 posted, 1 refused');
    expect(stageLink('WITH_INSURER')).toBe('/renewal/processing?tab=WITH_INSURER');
    expect(stageLink(LETTER_TABS[0]?.id ?? '')).toBe('/renewal/letters?tab=RA_READY');
    expect(stageLink('EXTRACTED')).toBe('/renewal/expiry?tab=EXTRACTED');
    expect(stageLink('RENEWED')).toBe('/renewal/expiry?tab=ALL');
  });

  it('reads and writes the multi-select wire value, all except included', () => {
    expect(parseSelection('!A,B')).toEqual({ except: true, codes: ['A', 'B'] });
    expect(parseSelection('')).toEqual({ except: false, codes: [] });
    expect(selectionValue(true, ['A'])).toBe('!A');
    expect(selectionValue(false, [])).toBe('');
  });

  it('edits the rules of the Setup versions', () => {
    const fields = bucketFields([
      {
        checkCode: 'CLAIMS',
        checkName: 'Claims',
        active: true,
        severity: 'FAIL_REVIEW',
        parameters: null,
        approval: { recordStatus: 'ACTIVE', maker: null, authorizedBy: null, authorizedAt: null },
      },
    ]);
    let rule = blankBucketRule(10);
    for (const f of fields) {
      rule = f.set(rule, f.key === 'priority' ? '20' : (f.options?.[1]?.code ?? ''));
    }
    expect(rule).toMatchObject({ priority: 20, checkCode: 'CLAIMS', severity: 'FAIL_EXCEPTION' });
    let decision = blankDecisionRule(5);
    for (const f of MATRIX_FIELDS) {
      decision = f.set(decision, f.numeric === true ? '7' : (f.options?.[1]?.code ?? 'X'));
    }
    expect(decision.criteria).toMatchObject({ mortgaged: true, daysFrom: 7, segment: 'X' });
    expect(MATRIX_FIELDS.map((f) => f.get(decision)).every((v) => v !== '')).toBe(true);
    expect(severityLabel('WARN')).toBe('Warning');
    expect(severityLabel(null)).toBe('Any');
    expect(numberOrNull(' ')).toBeNull();
    expect(textOrNull(' a ')).toBe('a');
  });

  it('shows the Classification pill, the flag chips and the refused renewals', () => {
    const onClose = vi.fn();
    render(
      <>
        <BucketPill bucket="EXCEPTION" />
        <FlagChips row={row()} />
        <OutcomeDialog
          title="Post"
          outcome={{ done: ['A'], refused: { B: 'Blocked' } }}
          onClose={onClose}
        />
      </>,
    );
    expect(screen.getByText('Exception')).toBeInTheDocument();
    expect(screen.getByText('Urgent')).toBeInTheDocument();
    expect(screen.getByText('Outstanding')).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'B' })).toBeInTheDocument();
    expect(screen.getByRole('cell', { name: 'Blocked' })).toBeInTheDocument();
    fireEvent.click(screen.getAllByRole('button', { name: 'Close' })[0]!);
    expect(onClose).toHaveBeenCalled();
  });

  it('filters by several codes or all except them', () => {
    const onChange = vi.fn();
    render(
      <MultiSelectFilter
        label="Segment"
        options={[
          { code: 'A', label: 'Alpha' },
          { code: 'B', label: 'Beta' },
        ]}
        value="!A"
        onChange={onChange}
      />,
    );
    expect(screen.getByText('Segment')).toBeInTheDocument();
  });

  it('shows Renewal Home with its tiles and counts', async () => {
    vi.spyOn(renewalApi, 'home').mockResolvedValue({
      stages: [{ code: 'FOR_DISPOSITION', label: 'For Disposition', count: 3 }],
      buckets: [{ code: 'CLEAN', label: 'Clean', count: 2 }],
      tiles: {
        due30: 11,
        due60: 12,
        due90: 13,
        due140: 14,
        atRisk: 1,
        urgent: 2,
        returned: 0,
        nrns: 0,
        insurerOverdue: 0,
        lettersFailed: 0,
      },
      workload: [{ username: 'ao', role: 'AO', count: 5 }],
    });
    render(renewalWrapper(TL)(<RenewalHomePage />));
    expect(screen.getByRole('heading', { level: 1, name: 'Renewal Home' })).toBeInTheDocument();
    expect(await screen.findByText('14')).toBeInTheDocument();
    expect(screen.getByText('For Disposition')).toBeInTheDocument();
  });

  it('lists the expiring renewals with the actions of a Team Leader', async () => {
    vi.spyOn(renewalApi, 'list').mockResolvedValue({
      content: [row()],
      page: 0,
      size: 200,
      totalElements: 1,
      totalPages: 1,
    });
    vi.spyOn(renewalApi, 'runs').mockResolvedValue({
      content: [],
      page: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });
    render(renewalWrapper(TL)(<ExpiryListPage />));
    expect(await screen.findByText('Santos Trading')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Generate Expiry List' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Assign Disposition' })).toBeDisabled();
    fireEvent.click(screen.getByRole('button', { name: 'Generate Expiry List' }));
    expect(screen.getByRole('button', { name: 'Generate' })).toBeDisabled();
  });

  it('shows the renewal record with its blocking checks and the disposition action', async () => {
    vi.spyOn(renewalApi, 'get').mockResolvedValue(detail());
    vi.spyOn(workflowApi, 'byRecord').mockResolvedValue({
      item: {
        id: 1,
        workflowCode: 'RNW_CASE',
        stageCode: 'FOR_DISPOSITION',
        stageName: 'For Disposition',
        entityType: 'RenewalCandidate',
        entityId: '7',
        reference: 'RNW-2026-000001',
        title: 'Renewal',
        stageEnteredAt: '2026-09-01T00:00:00Z',
        overdue: false,
      },
      actions: [],
      history: [],
      stageTerminal: false,
    } as unknown as WorkCaseDetail);
    render(
      renewalWrapper(
        TL,
        'ao',
        '/renewal/candidates/RNW-2026-000001',
      )(
        <Routes>
          <Route path="/renewal/candidates/:ref" element={<CandidatePage />} />
        </Routes>,
      ),
    );
    expect(
      await screen.findByText('Premium outstanding on the expiring invoice', { exact: false }),
    ).toBeInTheDocument();
    expect(await screen.findByRole('button', { name: 'Set Disposition' })).toBeInTheDocument();
    expect(screen.getByText('Expiring Account')).toBeInTheDocument();
  });
});
