import { render, screen, within } from '@testing-library/react';
import { Route, Routes } from 'react-router-dom';
import { productCatalogApi } from '@/api/productCatalog';
import type { VersionDetail } from '@/api/productCatalog';
import { productMaintApi } from '@/api/productmaint';
import { resetUserDirectory, setUserDirectory } from '@/api/users';
import { ebWrapper } from '@/features/eb/testWrapper';
import { ValidationResultCard } from './ValidationResultCard';
import VersionEditorPage from './VersionEditorPage';
import { checksOf, hasValidationOutcome } from './validationOutcome';

const RELEASED: VersionDetail = {
  summary: {
    productCode: 'MTR30',
    productName: 'Motor Fleet Plus',
    versionNo: 1,
    status: 'RELEASED',
    effectiveFrom: '2026-09-28',
    packageEndDate: '2027-09-28',
    minimumPremium: 3500,
    sourceRequestNo: 'PKR-2026-000001',
    maker: 'mbs',
    validatedBy: 'tsuhead',
    validatedAt: '2026-09-28T05:35:00Z',
  },
  lineCode: 'MOTOR',
  scheme: {
    defaultRate: 1.2,
    minimumPremium: 3500,
    defaultCommissionRate: 15,
    manualRateAllowed: false,
  },
  coverages: [],
  insurers: [],
  insurerTerms: [],
  mancomSignoffRef: 'MC-PKR-2026-000001',
  testPremium: 13446,
  validationResult: 'PASSED',
  validationChecks: [
    {
      seq: 1,
      code: 'INSURER_TERMS',
      label: 'Every panel insurer has terms for each included coverage',
      result: 'PASSED',
      detail: '3 of 3 insurers have terms for 2 included coverages',
    },
    {
      seq: 2,
      code: 'SHARES',
      label: 'Co-insurance shares add up to 100%',
      result: 'NOT_APPLICABLE',
      detail: 'Panel insurers only; no co-insurance',
    },
  ],
};

describe('Validation of a package version', () => {
  beforeEach(() => {
    setUserDirectory([
      { username: 'tsuhead', displayName: 'Tomas TSU Head' },
      { username: 'mbs', displayName: 'Monica Business Services' },
    ]);
  });
  afterEach(() => {
    vi.restoreAllMocks();
    resetUserDirectory();
  });

  it('shows the result, validator, test premium and the checks as a table', () => {
    render(ebWrapper(new Set())(<ValidationResultCard detail={RELEASED} />));
    const facts = screen.getByLabelText('Validation result');
    expect(within(facts).getByText('Passed')).toBeInTheDocument();
    expect(within(facts).getByText('Tomas TSU Head')).toBeInTheDocument();
    expect(within(facts).getByText('PHP 13,446.00')).toBeInTheDocument();
    expect(within(facts).queryByText('Return Reason')).not.toBeInTheDocument();
    const table = screen.getByRole('table');
    expect(
      within(table).getByText('3 of 3 insurers have terms for 2 included coverages'),
    ).toBeInTheDocument();
    expect(within(table).getByText('Not Applicable')).toBeInTheDocument();
  });

  it('shows a return with its reason and reads the checklist of older versions', () => {
    const returned: VersionDetail = {
      ...RELEASED,
      summary: {
        ...RELEASED.summary,
        status: 'DRAFT',
        validatedBy: undefined,
        validatedAt: undefined,
      },
      validationResult: 'RETURNED',
      returnedBy: 'tsuhead',
      returnedAt: '2026-09-28T04:00:00Z',
      returnedReason: 'Check the minimum premium',
      validationChecks: [],
      validationChecklist: '["Test premium reviewed"]',
    };
    expect(hasValidationOutcome(returned)).toBe(true);
    expect(hasValidationOutcome({ ...returned, validationResult: undefined })).toBe(false);
    expect(checksOf({ ...returned, validationChecklist: 'not json' })).toEqual([]);
    render(ebWrapper(new Set())(<ValidationResultCard detail={returned} />));
    const facts = screen.getByLabelText('Validation result');
    expect(within(facts).getByText('Returned')).toBeInTheDocument();
    expect(within(facts).getByText('Returned By')).toBeInTheDocument();
    expect(within(facts).getByText('Check the minimum premium')).toBeInTheDocument();
    expect(screen.getByText('Test premium reviewed')).toBeInTheDocument();
    expect(screen.getByText('Confirmed by the validator')).toBeInTheDocument();
  });

  it('shows the package request as a field with a link in the header card', async () => {
    vi.spyOn(productCatalogApi, 'version').mockResolvedValue(RELEASED);
    vi.spyOn(productMaintApi, 'search').mockResolvedValue({
      content: [{ id: 5, requestNo: 'PKR-2026-000001' }],
    } as unknown as Awaited<ReturnType<typeof productMaintApi.search>>);
    render(
      ebWrapper(
        new Set(['PRODUCT_VIEW']),
        'mbs',
        '/catalog/products/MTR30/versions/1',
      )(
        <Routes>
          <Route
            path="/catalog/products/:code/versions/:versionNo"
            element={<VersionEditorPage />}
          />
        </Routes>,
      ),
    );
    const link = await screen.findByRole('link', { name: 'PKR-2026-000001' });
    expect(link).toHaveAttribute('href', '/product-maintenance/requests/5');
    expect(screen.getByText('Package Request')).toBeInTheDocument();
    expect(screen.queryByText('Request PKR-2026-000001')).not.toBeInTheDocument();
  });
});
