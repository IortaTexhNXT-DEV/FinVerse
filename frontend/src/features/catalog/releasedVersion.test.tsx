import { fireEvent, render, screen, within } from '@testing-library/react';
import { productCatalogApi } from '@/api/productCatalog';
import type { Coverage } from '@/api/productCatalog';
import { ebWrapper } from '@/features/eb/testWrapper';
import { emptyTerms, newCoverage } from '@/features/productmaint/packageRequest';
import { TermsEditor } from '@/features/productmaint/TermsEditor';
import { VersionCoveragesSection } from './VersionPanelSections';
import { VersionSchemeSection } from './VersionSchemeSection';
import type { VersionForm } from './versionForm';

const FORM: VersionForm = {
  defaultRate: '1.2',
  minimumPremium: '5000',
  defaultCommissionRate: '15',
  maxSumInsured: '5000000',
  ratingBasisNote:
    'Rates as agreed with the insurers for the whole fleet, including units added mid-term',
  effectiveFrom: '2026-09-28',
  packageStartDate: '',
  packageEndDate: '2027-09-28',
  anniversaryDate: '',
  changeSummary: '',
  mancomSignoffRef: 'MC-1',
  coverages: [
    {
      coverageCode: 'OD_THEFT',
      included: true,
      optional: false,
      limitAmount: '5000000',
      deductibleAmount: '',
      deductibleText: '',
    },
  ],
  insurers: [],
  terms: [],
};

const COVERAGES: Coverage[] = [
  {
    lineCode: 'MOTOR',
    code: 'OD_THEFT',
    name: 'Own Damage and Theft',
    kind: 'COVERAGE',
    basic: true,
    sortOrder: 1,
    recordStatus: 'ACTIVE',
  } as Coverage,
];

describe('released package version and request terms', () => {
  afterEach(() => vi.restoreAllMocks());

  it('reads a released version as formatted values with a dash for an empty date', () => {
    render(
      ebWrapper(new Set())(
        <VersionSchemeSection form={FORM} errors={{}} readOnly onChange={() => undefined} />,
      ),
    );
    expect(screen.getByText('5,000.00')).toBeInTheDocument();
    expect(screen.getByText('5,000,000.00')).toBeInTheDocument();
    expect(screen.getByText('1.20')).toBeInTheDocument();
    expect(screen.getByText('28-Sep-2027')).toBeInTheDocument();
    const start = screen.getByText('Package Start').parentElement!;
    expect(within(start).getByText('—')).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  });

  it('shows the coverages of a released version by name with formatted limits', async () => {
    vi.spyOn(productCatalogApi, 'coverages').mockResolvedValue(COVERAGES);
    render(
      ebWrapper(new Set())(
        <VersionCoveragesSection
          form={FORM}
          lineCode="MOTOR"
          errors={{}}
          readOnly
          onChange={() => undefined}
        />,
      ),
    );
    expect(await screen.findByText('Own Damage and Theft')).toBeInTheDocument();
    expect(screen.getByText('5,000,000.00')).toBeInTheDocument();
    expect(screen.getByText('Yes')).toBeInTheDocument();
    expect(screen.queryByRole('checkbox')).not.toBeInTheDocument();
  });

  it('picks the coverage of a request by name from the line', async () => {
    vi.spyOn(productCatalogApi, 'coverages').mockResolvedValue(COVERAGES);
    const onChange = vi.fn();
    render(
      ebWrapper(new Set())(
        <TermsEditor
          terms={{ ...emptyTerms(), coverages: [newCoverage()] }}
          onChange={onChange}
          errors={{}}
          lineCode="MOTOR"
          withInsurers={false}
        />,
      ),
    );
    const picker = screen.getByRole('combobox', { name: 'Coverage 1' });
    await screen.findByRole('option', { name: 'Own Damage and Theft (basic)' });
    fireEvent.change(picker, { target: { value: 'OD_THEFT' } });
    expect(onChange.mock.calls[0]?.[0].coverages[0].coverageCode).toBe('OD_THEFT');
  });

  it('asks for the line before the coverages', () => {
    render(
      ebWrapper(new Set())(
        <TermsEditor
          terms={{ ...emptyTerms(), coverages: [newCoverage()] }}
          onChange={() => undefined}
          errors={{}}
          withInsurers={false}
        />,
      ),
    );
    expect(screen.getByRole('combobox', { name: 'Coverage 1' })).toBeDisabled();
    expect(screen.getByRole('option', { name: 'Choose the line first' })).toBeInTheDocument();
  });
});
