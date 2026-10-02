import { render, screen } from '@testing-library/react';
import type { Premium } from '@/api/underwriting';
import { PremiumSummary } from './PremiumSummary';

const premium: Premium = {
  sumInsured: 10_000_000,
  ourSumInsured: 10_000_000,
  grossPremium: 100_000,
  discountAmount: 10_000,
  loadingAmount: 5_000,
  netPremium: 95_000,
  ourGrossPremium: 100_000,
  ourDiscount: 10_000,
  ourLoading: 5_000,
  ourNetPremium: 95_000,
  coinsurerPremium: 0,
  billedPremium: 95_000,
  dst: 11_875,
  vat: 11_400,
  lgt: 712.5,
  fst: 1_900,
  premiumTax: 0,
  policyFee: 250,
  taxesAndCharges: 26_137.5,
  totalDue: 121_137.5,
  commissionRate: 20,
  commission: 19_000,
  withholdingRate: 10,
  withholdingTax: 1_900,
  netCommission: 17_100,
};

describe('PremiumSummary', () => {
  it('shows taxes, total due and commission and hides zero lines', () => {
    render(<PremiumSummary premium={premium} currency="PHP" />);
    expect(screen.getByText('Documentary stamp tax')).toBeInTheDocument();
    expect(screen.getByText('121,137.50')).toBeInTheDocument();
    expect(screen.getByText('Commission (20%)')).toBeInTheDocument();
    expect(screen.queryByText('Premium tax')).not.toBeInTheDocument();
    expect(screen.getByText('(10,000.00)')).toBeInTheDocument();
  });
});
