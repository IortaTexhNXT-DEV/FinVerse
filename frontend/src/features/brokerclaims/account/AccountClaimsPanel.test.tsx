import { render, screen } from '@testing-library/react';
import { claimsHomeApi } from '../home/api';
import type { ClaimExperience } from '../home/api';
import { claimsWrapper } from '../home/testWrapper';
import { AccountClaimsPanel } from './AccountClaimsPanel';
import { claimLink, experienceSummary } from './accountClaimsLogic';

const EXPERIENCE: ClaimExperience = {
  arn: 'ARN-2026-940001',
  claimCount: 2,
  openCount: 1,
  paid: 850000,
  outstanding: 118000,
  total: 968000,
  statuses: { 'For Adjuster Review': 1, 'Settled - LOA Issued': 1 },
  claims: [
    {
      claimId: 12,
      claimNo: 'BCL-2026-000012',
      policyYear: 1,
      lossDate: '2026-09-23',
      statusCode: 'ADJUSTER_REVIEW',
      statusLabel: "For Adjuster's Review and Evaluation",
      phase: 'IN_PROGRESS',
      currency: 'PHP',
      paid: 0,
      outstanding: 118000,
    },
    {
      claimId: 9,
      claimNo: 'BCL-2026-000009',
      policyYear: 1,
      lossDate: '2026-09-10',
      statusCode: 'INSURER_LOA_ISSUANCE',
      statusLabel: 'With Insurer - For Issuance of LOA',
      phase: 'CLOSED',
      currency: 'PHP',
      paid: 850000,
      outstanding: 0,
    },
  ],
};

describe('account page Claims tab', () => {
  afterEach(() => vi.restoreAllMocks());

  it('summarises the claims of a cover', () => {
    expect(experienceSummary(EXPERIENCE)).toBe(
      '2 claims, 1 open · paid 850,000.00 · outstanding 118,000.00',
    );
    expect(experienceSummary({ ...EXPERIENCE, claimCount: 1 })).toMatch(/^1 claim, /);
    expect(experienceSummary({ ...EXPERIENCE, claimCount: 0, claims: [] })).toBe(
      'No claim recorded on this account',
    );
    expect(claimLink(12)).toBe('/claims-handling/12');
  });

  it('lists the claims read-only with a link to each claim record', async () => {
    const spy = vi.spyOn(claimsHomeApi, 'experience').mockResolvedValue(EXPERIENCE);
    render(claimsWrapper(new Set(['BCL_VIEW']))(<AccountClaimsPanel arn="ARN-2026-940001" />));
    const link = await screen.findByRole('link', { name: 'BCL-2026-000012' });
    expect(link).toHaveAttribute('href', '/claims-handling/12');
    expect(screen.getByRole('link', { name: 'BCL-2026-000009' })).toHaveAttribute(
      'href',
      '/claims-handling/9',
    );
    expect(screen.getByText('With Insurer - For Issuance of LOA')).toBeInTheDocument();
    expect(screen.getByText(/2 claims, 1 open/)).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(spy).toHaveBeenCalledWith('ARN-2026-940001');
  });

  it('shows the empty state for an account without claims', async () => {
    vi.spyOn(claimsHomeApi, 'experience').mockResolvedValue({
      ...EXPERIENCE,
      claimCount: 0,
      openCount: 0,
      claims: [],
    });
    render(claimsWrapper(new Set(['BCL_VIEW']))(<AccountClaimsPanel arn="ARN-X" />));
    expect(await screen.findAllByText('No claim recorded on this account')).not.toHaveLength(0);
  });
});
