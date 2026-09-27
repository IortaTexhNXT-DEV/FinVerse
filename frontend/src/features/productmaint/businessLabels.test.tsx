import { render, screen } from '@testing-library/react';
import { catalogApi } from '@/api/catalog';
import type { CoverType, Insurer, ProductLine } from '@/api/catalog';
import { lovApi } from '@/api/lov';
import { productCatalogApi } from '@/api/productCatalog';
import type { Coverage } from '@/api/productCatalog';
import type { PackageRequest, PackageTerms } from '@/api/productmaint';
import { ebWrapper } from '@/features/eb/testWrapper';
import { placementChannelLabel } from '@/features/catalog/insurerForm';
import { INSURER_ROLES, insurerRoleLabel } from './packageRequest';
import { DetailsTab } from './RequestTabs';
import { TermsView } from './TermsView';

const TERMS: PackageTerms = {
  sections: [],
  coverages: [
    {
      coverageCode: 'OD_THEFT',
      included: true,
      optional: false,
      clauseCodes: [],
      limitAmount: 2000000,
    },
  ],
  scheme: { defaultRate: 1.2, minimumPremium: 30000 },
  dates: { effectiveFrom: '2026-09-27', packageStartDate: '2026-09-27' },
  insurers: [{ insurerCode: 'INS-MGIC', role: 'LEAD', minimumPremium: 30000, terms: [] }],
} as PackageTerms;

function lists() {
  vi.spyOn(lovApi, 'options').mockImplementation((type: string) =>
    Promise.resolve(
      type === 'MARKET_SEGMENT'
        ? [{ code: 'CBG', label: 'Consumer Banking Group (CBG)' }]
        : [{ code: 'NEW_PROGRAMME', label: 'New programme for a market segment' }],
    ),
  );
  vi.spyOn(catalogApi, 'lines').mockResolvedValue([
    { code: 'MOTOR', name: 'Motor' } as ProductLine,
  ]);
  vi.spyOn(catalogApi, 'coverTypes').mockResolvedValue([
    { code: 'COMPREHENSIVE', name: 'Comprehensive', lineCode: 'MOTOR' } as CoverType,
  ]);
  vi.spyOn(catalogApi, 'insurers').mockResolvedValue([
    { partyCode: 'INS-MGIC', name: 'Mabuhay General Insurance Corp.' } as Insurer,
  ]);
  vi.spyOn(productCatalogApi, 'coverages').mockResolvedValue([
    { code: 'OD_THEFT', name: 'Own Damage and Theft', lineCode: 'MOTOR' } as Coverage,
  ]);
}

describe('Package request labels', () => {
  beforeEach(lists);
  afterEach(() => vi.restoreAllMocks());

  it('shows coverage names, role labels and formatted amounts in the terms', async () => {
    render(ebWrapper(new Set())(<TermsView terms={TERMS} title="Terms" lineCode="MOTOR" />));
    expect(await screen.findByText('Own Damage and Theft')).toBeInTheDocument();
    expect(screen.queryByText('OD_THEFT')).not.toBeInTheDocument();
    expect(screen.getByText('Lead')).toBeInTheDocument();
    expect(screen.getAllByText(/30,000\.00/).length).toBeGreaterThan(0);
    expect(screen.getByText('2,000,000.00')).toBeInTheDocument();
  });

  it('shows the line, cover type, segments and reason by their list labels', async () => {
    const request = {
      requestType: 'NEW',
      scope: 'GENERIC',
      lineCode: 'MOTOR',
      coverTypeCode: 'COMPREHENSIVE',
      marketSegments: ['CBG'],
      reason: 'NEW_PROGRAMME',
      negotiationRequired: true,
      milestones: {},
    } as unknown as PackageRequest;
    render(ebWrapper(new Set())(<DetailsTab request={request} />));
    expect(await screen.findByText('Consumer Banking Group (CBG)')).toBeInTheDocument();
    expect(await screen.findByText('New programme for a market segment')).toBeInTheDocument();
    expect(await screen.findByText(/Comprehensive/)).toBeInTheDocument();
    expect(screen.getByText(/Motor/)).toBeInTheDocument();
    expect(screen.queryByText(/MOTOR|COMPREHENSIVE|NEW_PROGRAMME|New Programme$/)).toBeNull();
  });

  it('words insurer roles and placement channels as the lists do', () => {
    expect(INSURER_ROLES.map((r) => r.label)).toEqual(['Panel', 'Lead', 'Participant']);
    expect(insurerRoleLabel(undefined)).toBe('Panel');
    expect(insurerRoleLabel('PARTICIPANT')).toBe('Participant');
    expect(placementChannelLabel('EMAIL')).toBe('E-mail');
  });
});
