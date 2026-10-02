import { render, screen, within } from '@testing-library/react';
import { productCatalogApi } from '@/api/productCatalog';
import type { RateException } from '@/api/productCatalog';
import type { Quotation } from '@/api/quotations';
import { resetUserDirectory, setUserDirectory } from '@/api/users';
import { rateDifference } from '@/features/catalog/rateException';
import { ebWrapper } from '@/features/eb/testWrapper';
import { RateExceptionsCard } from './RateExceptionsCard';

const QUOTATION = {
  id: 42,
  quotationNo: 'QT-2026-000001',
  productCode: 'MTR30',
  status: 'DRAFT',
  content: { schemeVersion: 1, schemeDeviation: true, validUntil: '2026-10-27', items: [] },
} as unknown as Quotation;

const BASE: RateException = {
  id: 1,
  referenceNo: 'RSE-2026-000001',
  productCode: 'MTR30',
  purpose: 'NEW_BUSINESS',
  requestedRate: 1.1,
  transactionRef: 'QT-2026-000001',
  reason: 'Fleet with no claims',
  validUntil: '2026-10-27',
  recordStatus: 'ACTIVE',
  requestedBy: 'ao',
  requestedAt: '2026-09-27T08:41:00Z',
  decidedBy: 'approver',
  decidedAt: '2026-09-27T09:10:00Z',
};

const PENDING: RateException = {
  ...BASE,
  id: 3,
  referenceNo: 'RSE-2026-000003',
  requestedRate: 1.05,
  recordStatus: 'PENDING_AUTHORIZATION',
  decidedBy: undefined,
  decidedAt: undefined,
};

function renderCard(permissions: string[]) {
  return render(
    ebWrapper(new Set(permissions), 'ao')(<RateExceptionsCard quotation={QUOTATION} />),
  );
}

describe('rate exceptions card', () => {
  beforeEach(() => {
    setUserDirectory([
      { username: 'ao', displayName: 'Aileen Account Officer' },
      { username: 'approver', displayName: 'Nora NB Approver' },
    ]);
    vi.spyOn(productCatalogApi, 'rateExceptions').mockResolvedValue([PENDING, BASE]);
    vi.spyOn(productCatalogApi, 'rateException').mockResolvedValue({
      exception: PENDING,
      productName: 'Motor Fleet Plus',
      currentVersionNo: 1,
      schemeRate: 1.35,
    });
  });
  afterEach(() => {
    vi.restoreAllMocks();
    resetUserDirectory();
  });

  it('gives the card, its notice and its table stable callout names for the capture recipes', async () => {
    const { container } = renderCard(['QUOTE_MAINTAIN']);
    await screen.findByRole('link', { name: 'RSE-2026-000003' });
    const card = container.querySelector('[data-callout="rate-exceptions"]');
    expect(card).not.toBeNull();
    const notice = card?.querySelector('[data-callout="rate-exceptions-notice"]');
    expect(notice).toHaveTextContent('Priced on package version');
    const table = card?.querySelector('[data-callout="rate-exceptions-table"]');
    expect(table?.querySelector('a[href*="/catalog/rate-exceptions/"]')).not.toBeNull();
  });

  it('lists the exceptions as a record table in a titled card, not in a highlighted panel', async () => {
    const { container } = renderCard(['QUOTE_MAINTAIN']);
    expect(await screen.findByRole('link', { name: 'RSE-2026-000003' })).toHaveAttribute(
      'href',
      '/catalog/rate-exceptions/RSE-2026-000003',
    );
    expect(screen.getByRole('heading', { name: 'Rate Exceptions' })).toBeInTheDocument();
    expect(container.querySelector('.alert')).toBeNull();
    const headers = screen.getAllByRole('columnheader').map((h) => h.textContent);
    expect(headers).toEqual([
      'Exception No.',
      'Requested Rate (%)',
      'Scheme Rate (%)',
      'Difference (%)',
      'Valid Until',
      'Requested By',
      'Decided By',
      'Status',
    ]);
    const rows = screen.getAllByRole('row').slice(1);
    const approved = within(rows[1]!);
    expect(approved.getByText('1.10')).toBeInTheDocument();
    expect(await approved.findByText('1.35')).toBeInTheDocument();
    expect(approved.getByText('−0.25')).toBeInTheDocument();
    expect(approved.getByText('27-Oct-2026')).toBeInTheDocument();
    expect(approved.getByText('Aileen Account Officer')).toBeInTheDocument();
    expect(approved.getByText('Nora NB Approver')).toBeInTheDocument();
    expect(approved.getByText('Approved')).toHaveClass('badge', 'success');
    const pending = within(rows[0]!);
    expect(pending.getByText('Pending Authorization')).toHaveClass('badge', 'warning');
  });

  it('puts one subtle info line above the table and the request button in the card header', async () => {
    renderCard(['QUOTE_MAINTAIN']);
    await screen.findByRole('link', { name: 'RSE-2026-000001' });
    const notice = screen.getByRole('status');
    expect(notice).toHaveClass('notice', 'notice-info', 'subtle');
    expect(notice).toHaveTextContent('submission needs an approved rate exception');
    const header = screen.getByRole('heading', { name: 'Rate Exceptions' }).closest('header');
    expect(
      within(header!).getByRole('button', { name: 'Request Rate Exception' }),
    ).toBeInTheDocument();
  });

  it('offers no request without the permission', async () => {
    renderCard([]);
    await screen.findByRole('link', { name: 'RSE-2026-000001' });
    expect(screen.queryByRole('button', { name: 'Request Rate Exception' })).toBeNull();
  });

  it('writes the difference signed in percentage points', () => {
    expect(rateDifference(1.5, 1.35)).toBe('+0.15');
    expect(rateDifference(1.35, 1.35)).toBe('0.00');
    expect(rateDifference(undefined, 1.35)).toBe('');
  });
});
