import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { api } from '@/api/client';
import { pmRoutingApi } from '@/api/pmRouting';
import type { RoutingView } from '@/api/pmRouting';
import type { PackageRequest } from '@/api/productmaint';
import { RoutingTab } from './RoutingTab';
import { Providers } from './testProviders';

const ROUTING: RoutingView = {
  source: 'INSURER',
  details: {
    estimatedPolicies: 25,
    estimatedPremium: 6_000_000,
    incentiveEligible: true,
    incentiveRate: 2.5,
  },
  marketing: {
    chained: true,
    levelsGiven: 1,
    history: [
      {
        level: 1,
        approver: 'mkttl',
        decision: 'APPROVED',
        remarks: 'ok',
        decidedAt: '2026-10-08T02:00:00Z',
      },
    ],
  },
  mancom: {
    selectedRouting: true,
    president: 'mancompres',
    members: ['mancom', 'mancompres'],
    tasks: [
      {
        round: 1,
        approver: 'mancom',
        president: false,
        status: 'PENDING',
        remarks: null,
        decidedAt: null,
      },
      {
        round: 1,
        approver: 'mancompres',
        president: true,
        status: 'WAITING',
        remarks: null,
        decidedAt: null,
      },
    ],
  },
  deployment: { mode: 'MANUAL', number: null, requestedAt: null, requestedBy: null },
};

const request = { id: 5, requestNo: 'PKR-2026-000005', status: 'FOR_MANCOM' } as PackageRequest;

describe('Routing & Approvals of a package request', () => {
  afterEach(() => vi.restoreAllMocks());

  it('shows the source, the Marketing approvals and lets a ManCom approver return with remarks', async () => {
    vi.spyOn(api, 'get').mockResolvedValue([]);
    vi.spyOn(pmRoutingApi, 'routing').mockResolvedValue(ROUTING);
    const decide = vi.spyOn(pmRoutingApi, 'decideManCom').mockResolvedValue(ROUTING);
    render(
      <Providers username="mancom" permissions={['PKG_MANCOM_SIGNOFF']}>
        <RoutingTab request={request} />
      </Providers>,
    );
    expect(await screen.findByText('Insurer')).toBeInTheDocument();
    expect(screen.getByText('Marketing Team Leader')).toBeInTheDocument();
    expect(screen.getByText('Pending Approval')).toBeInTheDocument();
    expect(screen.getByText('Waiting for the others')).toBeInTheDocument();
    expect(screen.getByText('President')).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Return for Revision' }));
    expect(
      screen.getByText('Enter the remarks of the rejection or the return'),
    ).toBeInTheDocument();
    expect(decide).not.toHaveBeenCalled();
    await userEvent.type(screen.getByLabelText('Remarks'), 'Attach the signed slip');
    await userEvent.click(screen.getByRole('button', { name: 'Return for Revision' }));
    expect(decide).toHaveBeenCalledWith(5, 'RETURN', 'Attach the signed slip');
  });

  it('offers Request for Deployment once ManCom approved', async () => {
    vi.spyOn(api, 'get').mockResolvedValue([]);
    vi.spyOn(pmRoutingApi, 'routing').mockResolvedValue({
      ...ROUTING,
      mancom: { ...ROUTING.mancom, tasks: [] },
    });
    const deploy = vi.spyOn(pmRoutingApi, 'requestDeployment').mockResolvedValue({
      ...ROUTING,
      deployment: {
        mode: 'MANUAL',
        number: 'PDR-2026-000001',
        requestedAt: null,
        requestedBy: 'tsu',
      },
    });
    render(
      <Providers username="tsu" permissions={['PKG_NEGOTIATE']}>
        <RoutingTab request={{ ...request, status: 'MANCOM_APPROVED' }} />
      </Providers>,
    );
    await userEvent.click(await screen.findByRole('button', { name: 'Request for Deployment' }));
    expect(deploy).toHaveBeenCalledWith(5);
  });
});
