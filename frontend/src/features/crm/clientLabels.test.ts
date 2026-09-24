import type { ClientDetail } from '@/api/clients';
import { applyQuickFilter, clientActions, describeBank } from './clientLabels';

const client = (patch: Partial<ClientDetail>): ClientDetail =>
  ({
    status: 'PROSPECT',
    onboardingStage: 'PROSPECT',
    bankClient: false,
    kyc: { status: 'NOT_STARTED' },
    lifecycle: { createdBy: 'ao', createdAt: '2026-09-01T00:00:00Z' },
    ...patch,
  }) as ClientDetail;

const grants =
  (...permissions: string[]) =>
  (p: string) =>
    permissions.includes(p);

describe('quick filters', () => {
  it('overrides status or asks for KYC reviews due', () => {
    expect(applyQuickFilter({ name: 'x', status: 'INACTIVE' }, 'PROSPECT')).toEqual({
      name: 'x',
      status: 'PROSPECT',
    });
    expect(applyQuickFilter({}, 'KYC_DUE')).toEqual({ kycDue: true });
    expect(applyQuickFilter({ name: 'x' }, 'ALL')).toEqual({ name: 'x' });
  });
});

describe('client actions', () => {
  it('lets the maker submit and another checker verify', () => {
    const maker = clientActions(client({}), grants('CLIENT_MAINTAIN'), 'ao');
    expect(maker).toEqual({
      edit: true,
      submitKyc: true,
      verifyKyc: false,
      confirm: false,
      deactivate: true,
    });
    const review = client({
      onboardingStage: 'KYC_REVIEW',
      kyc: { status: 'PENDING', submittedBy: 'ao2' },
    });
    expect(clientActions(review, grants('CLIENT_APPROVE'), 'mkttl').verifyKyc).toBe(true);
    expect(clientActions(review, grants('CLIENT_APPROVE'), 'ao2').verifyKyc).toBe(false);
    expect(clientActions(review, grants('CLIENT_APPROVE'), 'ao').verifyKyc).toBe(false);
  });

  it('confirms verified prospects and reviews confirmed clients', () => {
    const verified = client({ onboardingStage: 'KYC_VERIFIED', kyc: { status: 'VERIFIED' } });
    expect(clientActions(verified, grants('CLIENT_MAINTAIN'), 'ao').confirm).toBe(true);
    const confirmed = client({
      status: 'CONFIRMED',
      onboardingStage: 'CONFIRMED',
      kyc: { status: 'EXPIRED' },
    });
    expect(clientActions(confirmed, grants('CLIENT_APPROVE'), 'mkttl').verifyKyc).toBe(true);
    const inactive = clientActions(
      client({ status: 'INACTIVE', onboardingStage: 'INACTIVE' }),
      grants('CLIENT_MAINTAIN', 'CLIENT_APPROVE'),
      'x',
    );
    expect(Object.values(inactive).every((v) => !v)).toBe(true);
  });

  it('describes the bank relationship', () => {
    expect(describeBank(client({}))).toBe('Non-bank client');
    expect(describeBank(client({ bankClient: true }))).toBe('BDO bank client');
    expect(describeBank(client({ bankClient: true, bankCif: 'C1' }))).toBe(
      'BDO bank client · CIF C1',
    );
  });
});
