import { fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { catalogApi } from '@/api/catalog';
import { renewalApi } from '@/api/renewal';
import { CandidateList } from './common/CandidateList';
import {
  acceptanceMethodLabel,
  bucketCauseLabel,
  closedAsLabel,
  dispositionSourceLabel,
  letterTypeLabel,
  mapSourceLabel,
  overrideChange,
  overrideKindLabel,
  responseLabel,
  setupRecordActions,
  transferActions,
  versionActions,
} from './common/presentation';
import MyDispositionsPage from './mine/MyDispositionsPage';
import ProcessingPage from './processing/ProcessingPage';
import { row } from './renewalFixtures';
import RenewalSetupPage from './setup/RenewalSetupPage';
import { renewalWrapper } from './testWrapper';
import TransfersPage from './transfers/TransfersPage';

/** The screen standards on the Renewal screens: labels instead of codes, names and row menus. */

const noop = () => undefined;

afterEach(() => vi.restoreAllMocks());

describe('labels instead of codes', () => {
  it('names the insurer response, the letter and the acceptance method in words', () => {
    expect(responseLabel('RENEW_AS_IS')).toBe('Renew as is');
    expect(responseLabel('REVISE')).toBe('Renew with revised terms');
    expect(responseLabel('REJECT')).toBe('Declined');
    expect(letterTypeLabel('RA', 'FIRST')).toBe('Renewal Advice, first notice');
    expect(letterTypeLabel('NAL', null)).toBe('No Advice Letter');
    expect(letterTypeLabel('NFR', null)).toBe('Not for Renewal Letter');
    expect(acceptanceMethodLabel('SIGNED_RA')).toBe('Signed Renewal Advice');
  });

  it('names the source of a disposition, an override and a classification change', () => {
    expect(dispositionSourceLabel('MATRIX')).toBe('Decision matrix');
    expect(dispositionSourceLabel('SYSTEM_CHECK')).toBe('System check');
    expect(overrideKindLabel('OUTSTANDING_BALANCE')).toBe('Outstanding balance');
    expect(overrideChange('REVIEW', 'CLEAN')).toBe('Review to Clean');
    expect(overrideChange('FOR_RENEWAL', 'LOST_BUSINESS')).toBe('For Renewal to Lost Business');
    expect(overrideChange(null, null)).toBe('');
    expect(bucketCauseLabel('RULE')).toBe('Classification rules');
    expect(closedAsLabel('EXPIRED_UNRENEWED')).toBe('Expired without renewal');
    expect(mapSourceLabel('MIGRATION')).toBe('Data migration');
  });

  it('never shows a code for an unknown value, only its words', () => {
    expect(responseLabel('APPROVED_WITH_CHANGES')).toBe('Approved with Changes');
    expect(responseLabel(null)).toBe('');
  });
});

describe('row action menus', () => {
  it('offers Accept and Decline on an incoming transfer and Cancel Request on an outgoing one', () => {
    const on = { accept: noop, decline: noop, cancel: noop };
    expect(transferActions('incoming', 'REQUESTED', on).map((a) => a.label)).toEqual([
      'Accept',
      'Decline',
    ]);
    expect(transferActions('outgoing', 'REQUESTED', on).map((a) => a.label)).toEqual([
      'Cancel Request',
    ]);
    expect(transferActions('incoming', 'ACCEPTED', on)).toEqual([]);
  });

  it('offers Authorize while a set-up record waits and Deactivate, in red, when it is active', () => {
    const on = { authorize: noop, edit: noop, deactivate: noop };
    expect(
      setupRecordActions('PENDING_AUTHORIZATION', true, 'Risk Code', on).map((a) => a.label),
    ).toEqual(['Authorize', 'Edit']);
    const active = setupRecordActions('ACTIVE', true, 'Risk Code', on);
    expect(active.map((a) => a.label)).toEqual(['Edit', 'Deactivate']);
    expect(active[1]?.danger).toBe(true);
    expect(setupRecordActions('ACTIVE', false, 'Risk Code', on)).toEqual([]);
  });

  it('offers Submit on a draft version and Activate or Reject with a reason on a submitted one', () => {
    const on = { submit: noop, activate: noop, reject: noop };
    expect(versionActions('DRAFT', 2, true, on).map((a) => a.label)).toEqual(['Submit']);
    const submitted = versionActions('SUBMITTED', 2, true, on);
    expect(submitted.map((a) => a.label)).toEqual(['Activate', 'Reject']);
    expect(submitted[1]?.confirm?.reason).toBe('required');
    expect(versionActions('ACTIVE', 1, true, on)).toEqual([]);
  });

  it('shows the actions of a transfer in the row menu, never as buttons in the row', async () => {
    vi.spyOn(renewalApi, 'transfersIn').mockResolvedValue([
      {
        id: 1,
        renewalRef: 'RNW-1',
        clientName: 'Santos Trading',
        expiry: '2026-10-01',
        fromUnit: 'T-CBG1',
        toUnit: 'T-CORP1',
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
    vi.spyOn(catalogApi, 'salesOrganisation').mockResolvedValue({
      units: [
        { code: 'T-CBG1', name: 'CBG Team 1' },
        { code: 'T-CORP1', name: 'Corporate Team 1' },
      ],
    } as unknown as Awaited<ReturnType<typeof catalogApi.salesOrganisation>>);
    render(renewalWrapper(new Set(['RNW_ASSIGN']))(<TransfersPage />));
    const rowEl = (await screen.findByText('Moved')).closest('tr');
    expect(rowEl).not.toBeNull();
    const cells = within(rowEl as HTMLElement);
    expect(cells.queryByRole('button', { name: 'Accept' })).toBeNull();
    expect(await cells.findByText('CBG Team 1')).toBeInTheDocument();
    expect(cells.queryByText('T-CORP1')).toBeNull();
    fireEvent.click(cells.getByRole('button', { name: 'Actions for RNW-1' }));
    expect(screen.getByRole('menuitem', { name: 'Accept' })).toBeInTheDocument();
    expect(screen.getByRole('menuitem', { name: 'Decline' })).toBeInTheDocument();
  });
});

describe('names in the renewal lists', () => {
  it('shows the insurer and the product by name, the product code muted under it', async () => {
    vi.spyOn(renewalApi, 'list').mockResolvedValue({
      content: [row()],
      page: 0,
      size: 200,
      totalElements: 1,
      totalPages: 1,
    });
    vi.spyOn(catalogApi, 'insurers').mockResolvedValue([
      { partyCode: 'INS1', name: 'Mabuhay General Insurance Corp.' },
    ] as unknown as Awaited<ReturnType<typeof catalogApi.insurers>>);
    vi.spyOn(catalogApi, 'products').mockResolvedValue([
      { code: 'MTR12', name: 'Motor Car Comprehensive' },
    ] as unknown as Awaited<ReturnType<typeof catalogApi.products>>);
    render(renewalWrapper(new Set(['RNW_VIEW']))(<CandidateList />));
    expect(await screen.findByText('Mabuhay General Insurance Corp.')).toBeInTheDocument();
    expect(await screen.findByText('Motor Car Comprehensive')).toBeInTheDocument();
    expect(screen.queryByText('INS1')).toBeNull();
  });
});

describe('where each persona finds its uploads', () => {
  const lists = () => {
    vi.spyOn(renewalApi, 'list').mockResolvedValue({
      content: [],
      page: 0,
      size: 200,
      totalElements: 0,
      totalPages: 0,
    });
  };

  it('offers the dispositioned file upload on the Processing Worklist, not on My Dispositions', () => {
    lists();
    const processing = new Set(['RNW_VIEW', 'RNW_PROCESS', 'RNW_UPLOAD']);
    const { unmount } = render(renewalWrapper(processing)(<ProcessingPage />));
    expect(screen.getByRole('button', { name: 'Upload Dispositions' })).toBeInTheDocument();
    unmount();
    render(
      renewalWrapper(new Set(['RNW_VIEW', 'RNW_DISPOSE', 'RNW_UPLOAD']))(<MyDispositionsPage />),
    );
    expect(screen.queryByRole('button', { name: 'Upload Dispositions' })).toBeNull();
  });

  it('shows the Go-live tab to the Processing team for its uploads, without the set-up tabs', async () => {
    vi.spyOn(renewalApi, 'packageMap').mockResolvedValue([]);
    vi.spyOn(renewalApi, 'corrections').mockResolvedValue([]);
    vi.spyOn(renewalApi, 'source').mockResolvedValue({ connected: false });
    const processingLead = new Set(['RNW_VIEW', 'RNW_PACKAGE_REMAP', 'RNW_EXTRACT', 'RNW_RA_SEND']);
    render(renewalWrapper(processingLead)(<RenewalSetupPage />));
    expect(screen.queryByRole('tab', { name: 'Non-renewable Risk Codes' })).toBeNull();
    fireEvent.click(screen.getByRole('tab', { name: 'Go-live' }));
    expect(
      await screen.findByRole('button', { name: 'Upload Migrated Policies' }),
    ).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Upload RAs Already Sent' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Run Take-over' })).toBeNull();
  });
});
