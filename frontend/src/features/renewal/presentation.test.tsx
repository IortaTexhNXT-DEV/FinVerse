import { fireEvent, render, screen, within } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { catalogApi } from '@/api/catalog';
import { renewalApi } from '@/api/renewal';
import { CandidateList } from './common/CandidateList';
import { candidateColumns, claimsText } from './common/candidateColumns';
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
import { detail, row } from './renewalFixtures';
import CandidatePage from './record/CandidatePage';
import { ChecksTab } from './record/RecordTabs';
import { Route, Routes } from 'react-router-dom';
import RenewalSetupPage from './setup/RenewalSetupPage';
import { ApprovalCell } from './setup/setupBits';
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
  it('shows the insurer, the product and the unit by name, also without the catalogue', async () => {
    vi.spyOn(renewalApi, 'list').mockResolvedValue({
      content: [
        row({ names: { insurer: 'Luzon Assurance Co.', ownerUnit: 'Corporate Marketing Team 1' } }),
        row({ renewalRef: 'RNW-2026-000002' }),
      ],
      page: 0,
      size: 200,
      totalElements: 2,
      totalPages: 1,
    });
    vi.spyOn(catalogApi, 'insurers').mockResolvedValue([
      { partyCode: 'INS1', name: 'Mabuhay General Insurance Corp.' },
    ] as unknown as Awaited<ReturnType<typeof catalogApi.insurers>>);
    render(renewalWrapper(new Set(['RNW_VIEW']))(<CandidateList />));
    expect(await screen.findByText('Luzon Assurance Co.')).toBeInTheDocument();
    expect(screen.getByText('Corporate Marketing Team 1')).toBeInTheDocument();
    expect(await screen.findByText('Mabuhay General Insurance Corp.')).toBeInTheDocument();
    expect(screen.getAllByText('Motor')).toHaveLength(2);
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

describe('renewal lists that fit their card', () => {
  it('pairs related values in one column instead of scrolling sideways', () => {
    const headers = candidateColumns(true).map((c) => c.header);
    expect(headers).toEqual([
      'Renewal Reference',
      'Client',
      'Expiring Policy / Invoice',
      'Risk / Insurance Company',
      'Unit / Officer',
      'Expiry',
      'Status / Classification',
      'Disposition',
      'Gross Premium / Outstanding',
    ]);
    expect(candidateColumns(false)).toHaveLength(8);
  });

  it('counts the claims in words', () => {
    expect(claimsText(1)).toBe('1 claim');
    expect(claimsText(3)).toBe('3 claims');
    expect(claimsText(null)).toBe('');
  });
});

describe('the actions of a Contact Center user on a renewal', () => {
  it('shows the actions of the role when the user does not read the work queues', async () => {
    vi.spyOn(renewalApi, 'get').mockResolvedValue(detail({ stage: 'RA_SENT' }));
    render(
      renewalWrapper(
        new Set(['RNW_VIEW', 'RNW_FOLLOWUP']),
        'contactc',
        '/renewal/candidates/RNW-1',
      )(
        <Routes>
          <Route path="/renewal/candidates/:ref" element={<CandidatePage />} />
        </Routes>,
      ),
    );
    expect(await screen.findByRole('button', { name: 'Add Follow-up' })).toBeInTheDocument();
    expect(screen.queryByText(/not permitted/i)).toBeNull();
  });
});

describe('renewal record words', () => {
  it('shows the business origin and account type in words', async () => {
    const { accountTypeLabel, businessOriginLabel } = await import('./common/presentation');
    expect(businessOriginLabel('EBIX')).toBe('EBIX (migrated)');
    expect(businessOriginLabel('BANK')).toBe('Bank referral');
    expect(accountTypeLabel('CORPORATE')).toBe('Corporate');
    expect(accountTypeLabel(null)).toBe('');
  });
});

describe('renewal checks tab', () => {
  it('shows an endorsement linked without its status', async () => {
    vi.spyOn(renewalApi, 'checks').mockResolvedValue({
      results: [],
      runs: [],
      buckets: [],
      endorsements: [
        { reference: 'ENR-2026-000005', source: 'BOOKING', linkedAt: '2026-10-01T02:00:00Z' },
      ],
    });
    render(renewalWrapper(new Set(['RNW_VIEW']))(<ChecksTab detail={detail()} />));
    expect(await screen.findByText('ENR-2026-000005')).toBeInTheDocument();
    expect(screen.getByText('Booking')).toBeInTheDocument();
  });
});

describe('renewal rates', () => {
  it('shows a rate in percent', async () => {
    const { rateText } = await import('./common/presentation');
    expect(rateText(18)).toBe('18%');
    expect(rateText(0.25)).toBe('0.25%');
    expect(rateText(null)).toBe('');
  });
});

describe('renewal values not given', () => {
  it('shows nothing for claims the server does not give', () => {
    expect(claimsText(undefined)).toBe('');
    const premium = candidateColumns(true).find((c) => c.key === 'premium');
    const r = row();
    const money = { ...r.money } as Partial<typeof r.money>;
    delete money.claimCount;
    render(<>{premium?.render({ ...r, money: money as typeof r.money }, 0)}</>);
    expect(screen.queryByText(/undefined/)).toBeNull();
  });
});

describe('renewal setup status', () => {
  it('shows the maker under the status, not beside it', () => {
    const { container } = render(
      renewalWrapper(new Set(['RNW_SETUP']))(
        <ApprovalCell
          approval={
            { recordStatus: 'ACTIVE', maker: 'badmin' } as Parameters<
              typeof ApprovalCell
            >[0]['approval']
          }
        />,
      ),
    );
    expect(container.querySelector('.rnw-flags')).toBeNull();
    expect(screen.getByText('Active')).toBeInTheDocument();
  });
});

describe('renewal product name', () => {
  it('names the product from the server when the catalogue is not readable', async () => {
    const { RenewalProduct } = await import('./common/RenewalBits');
    const r = row();
    render(
      renewalWrapper(new Set(['RNW_VIEW']))(
        <RenewalProduct
          row={{
            ...r,
            policy: { ...r.policy, productCode: 'CGL01', productName: null },
            names: { insurer: null, ownerUnit: null, product: 'Comprehensive General Liability' },
          }}
        />,
      ),
    );
    expect(await screen.findByText('Comprehensive General Liability')).toBeInTheDocument();
  });
});

describe('renewal first classification', () => {
  it('shows the first classification of a renewal without a previous one', async () => {
    vi.spyOn(renewalApi, 'checks').mockResolvedValue({
      results: [],
      runs: [],
      buckets: [
        { to: 'CLEAN', cause: 'RULE', by: 'proctl', at: '2026-10-02T21:02:27Z' },
      ] as unknown as Awaited<ReturnType<typeof renewalApi.checks>>['buckets'],
      endorsements: [],
    });
    render(renewalWrapper(new Set(['RNW_VIEW']))(<ChecksTab detail={detail()} />));
    expect(await screen.findByText('Classification rules')).toBeInTheDocument();
  });
});

describe('renewal stage pills', () => {
  it('shows the long stages in their short form in a list, the full label in the tooltip', async () => {
    const { statusShortLabel } = await import('@/components/ui/statusTones');
    expect(statusShortLabel('UNASSIGNED', 'Unassigned Disposition')).toBe('Unassigned');
    expect(statusShortLabel('FOR_PLACEMENT_BOOKING', 'For Placement and Booking')).toBe(
      'For Placement',
    );
  });
});

describe('renewal acceptance dialog', () => {
  it('keeps the acknowledgement apart from the remarks under it', async () => {
    const { AcceptanceDialog } = await import('./record/ClientDialogs');
    render(
      renewalWrapper(new Set(['RNW_ACCEPT']))(
        <AcceptanceDialog entityId={1} busy={false} error={null} onClose={noop} onConfirm={noop} />,
      ),
    );
    const ack = await screen.findByText(/acknowledged the change of premium/);
    expect(ack.closest('label')?.className).toContain('rnw-ack');
  });
});

describe('renewal list without amounts', () => {
  it('has no amount column when the server leaves the amounts out', async () => {
    const r = row();
    const money = { ...r.money } as Partial<typeof r.money>;
    delete money.currency;
    vi.spyOn(renewalApi, 'list').mockResolvedValue({
      content: [{ ...r, money: money as typeof r.money }],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 25,
    } as unknown as Awaited<ReturnType<typeof renewalApi.list>>);
    render(renewalWrapper(new Set(['RNW_VIEW']))(<CandidateList />));
    expect(await screen.findByText('Client')).toBeInTheDocument();
    expect(screen.queryByText('Gross Premium / Outstanding')).toBeNull();
  });
});
