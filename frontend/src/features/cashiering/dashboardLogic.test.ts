import { describe, expect, it } from 'vitest';
import type { DashboardGroup } from './dashboardLogic';
import { NAVIGATION, currenciesOf, visibleGroups } from './dashboardLogic';

const BASE = 'PHP';
const FX = 'USD';

const groups: DashboardGroup[] = [
  {
    code: 'ISSUANCE',
    label: 'AR/OR for Posting',
    items: [
      {
        code: 'ISSUANCE_AR_PREMIUM',
        label: 'AR - Premium',
        count: 2,
        amounts: { [BASE]: 1500, [FX]: 20 },
        link: '/cashiering/posting?kind=CREATION',
      },
    ],
  },
  { code: 'UNAPPLIED', label: 'Unapplied Payments for Disposition', items: [] },
  { code: 'FILES', label: 'For Auto Payment Processing', items: [] },
];

describe('Cashiering dashboard (FRS.CSH.01.03)', () => {
  it('shows only the groups of the user role', () => {
    const cashier = (p: string) => ['CASH_RECEIPT', 'CASH_UPLOAD', 'CASH_DISPOSITION'].includes(p);
    expect(visibleGroups(groups, cashier).map((g) => g.code)).toEqual(['UNAPPLIED', 'FILES']);
    const teamLeader = (p: string) => p.startsWith('CASH_');
    expect(visibleGroups(groups, teamLeader)).toHaveLength(3);
  });

  it('lists the amounts per currency with the base currency first', () => {
    expect(currenciesOf(groups, BASE)).toEqual([BASE, FX]);
  });

  it('links every facility of the main navigation', () => {
    expect(NAVIGATION.map((n) => n.label)).toContain('Post - Cancellation');
    expect(NAVIGATION.every((n) => n.to.startsWith('/'))).toBe(true);
  });
});
