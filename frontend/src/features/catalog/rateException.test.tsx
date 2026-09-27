import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Route, Routes } from 'react-router-dom';
import { productCatalogApi } from '@/api/productCatalog';
import type { RateException, RateExceptionDetail } from '@/api/productCatalog';
import { quotationsApi } from '@/api/quotations';
import { resetUserDirectory, setUserDirectory } from '@/api/users';
import { ebWrapper } from '@/features/eb/testWrapper';
import RateExceptionPage from './RateExceptionPage';
import {
  exceptionStatus,
  exceptionSubject,
  formatRate,
  mayDecide,
  rateExceptionPath,
  requestedValue,
  schemeInForce,
} from './rateException';

const PENDING: RateException = {
  id: 7,
  referenceNo: 'RSE-2026-000001',
  productCode: 'MTR30',
  purpose: 'NEW_BUSINESS',
  requestedRate: 1.1,
  transactionRef: 'QT-2026-000001',
  reason: 'Fleet of 30 vehicles with no claims in three years',
  validUntil: '2026-10-27',
  recordStatus: 'PENDING_AUTHORIZATION',
  requestedBy: 'ao',
  requestedAt: '2026-09-27T08:41:00Z',
};

const DETAIL: RateExceptionDetail = {
  exception: PENDING,
  productName: 'Motor Fleet Plus',
  currentVersionNo: 1,
  schemeRate: 1.2,
};

function renderPage(permissions: string[], username: string) {
  return render(
    ebWrapper(
      new Set(permissions),
      username,
      rateExceptionPath(PENDING.referenceNo),
    )(
      <Routes>
        <Route path="/catalog/rate-exceptions/:reference" element={<RateExceptionPage />} />
      </Routes>,
    ),
  );
}

describe('Rate exception', () => {
  beforeEach(() => {
    setUserDirectory([
      { username: 'ao', displayName: 'Aileen Account Officer' },
      { username: 'approver', displayName: 'Nora NB Approver' },
    ]);
    vi.spyOn(quotationsApi, 'search').mockResolvedValue({
      content: [
        {
          id: 42,
          quotationNo: 'QT-2026-000001',
          clientName: 'Garcia, Antonio Luis Dizon',
          grossPremium: 18488.5,
          currency: 'PHP',
        },
      ],
    } as unknown as Awaited<ReturnType<typeof quotationsApi.search>>);
  });
  afterEach(() => {
    vi.restoreAllMocks();
    resetUserDirectory();
  });

  it('shows the request against the scheme in force and approves it with a comment', async () => {
    const user = userEvent.setup();
    vi.spyOn(productCatalogApi, 'rateException').mockResolvedValue(DETAIL);
    const approve = vi
      .spyOn(productCatalogApi, 'approveRateException')
      .mockResolvedValue({ ...PENDING, recordStatus: 'ACTIVE' });
    renderPage(['PRODUCT_AUTHORIZE', 'QUOTE_VIEW'], 'approver');
    expect(await screen.findByText('MTR30 – Motor Fleet Plus')).toBeInTheDocument();
    expect(screen.getByText('Rate 1.10%')).toBeInTheDocument();
    expect(screen.getByText('Version 1, rate 1.20%')).toBeInTheDocument();
    expect(screen.getByText('Aileen Account Officer')).toBeInTheDocument();
    expect(screen.queryByText('ao')).not.toBeInTheDocument();
    expect(await screen.findByRole('link', { name: 'QT-2026-000001' })).toHaveAttribute(
      'href',
      '/quotations/42',
    );
    await user.click(screen.getByRole('button', { name: 'Approve' }));
    const dialog = screen.getByRole('dialog');
    await user.type(within(dialog).getByLabelText('Comment'), 'Loss-free fleet');
    await user.click(within(dialog).getByRole('button', { name: 'Approve' }));
    await waitFor(() => expect(approve).toHaveBeenCalledWith('RSE-2026-000001', 'Loss-free fleet'));
  });

  it('needs a reason to reject', async () => {
    const user = userEvent.setup();
    vi.spyOn(productCatalogApi, 'rateException').mockResolvedValue(DETAIL);
    const reject = vi
      .spyOn(productCatalogApi, 'rejectRateException')
      .mockResolvedValue({ ...PENDING, recordStatus: 'INACTIVE' });
    renderPage(['PRODUCT_AUTHORIZE'], 'approver');
    await user.click(await screen.findByRole('button', { name: 'Reject' }));
    const dialog = screen.getByRole('dialog');
    const confirm = within(dialog).getByRole('button', { name: 'Reject' });
    expect(confirm).toBeDisabled();
    await user.type(within(dialog).getByLabelText(/Reason for the rejection/), 'Scheme applies');
    await user.click(confirm);
    await waitFor(() => expect(reject).toHaveBeenCalledWith('RSE-2026-000001', 'Scheme applies'));
  });

  it('offers no decision to the requester nor after the decision', async () => {
    vi.spyOn(productCatalogApi, 'rateException').mockResolvedValue(DETAIL);
    const { unmount } = renderPage(['PRODUCT_AUTHORIZE', 'QUOTE_MAINTAIN'], 'ao');
    expect(await screen.findByText('MTR30 – Motor Fleet Plus')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Approve' })).not.toBeInTheDocument();
    unmount();

    vi.spyOn(productCatalogApi, 'rateException').mockResolvedValue({
      ...DETAIL,
      exception: {
        ...PENDING,
        recordStatus: 'INACTIVE',
        decidedBy: 'approver',
        decidedAt: '2026-09-27T09:00:00Z',
        decisionComment: 'Version 2 applies',
      },
    });
    renderPage(['PRODUCT_AUTHORIZE'], 'approver');
    expect(await screen.findByText('Version 2 applies')).toBeInTheDocument();
    expect(screen.getByText('Rejected by')).toBeInTheDocument();
    expect(screen.getByText('Nora NB Approver')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Reject' })).not.toBeInTheDocument();
  });

  it('words the exception and decides who may decide it', () => {
    expect(exceptionStatus({ recordStatus: 'ACTIVE' })).toBe('APPROVED');
    expect(exceptionStatus({ recordStatus: 'INACTIVE' })).toBe('REJECTED');
    expect(exceptionStatus({ recordStatus: 'PENDING_AUTHORIZATION' })).toBe(
      'PENDING_AUTHORIZATION',
    );
    expect(exceptionSubject(PENDING)).toBe('Rate 1.10% until 27-Oct-2026');
    expect(requestedValue({ requestedVersionNo: 1 })).toBe('Version 1');
    expect(schemeInForce({})).toBe('No version in force');
    expect(schemeInForce({ currentVersionNo: 2 })).toBe('Version 2, rate per insurer');
    expect(formatRate(0.235)).toBe('0.235');
    expect(formatRate(2)).toBe('2.00');
    const can = (p: string) => p === 'PRODUCT_AUTHORIZE';
    expect(mayDecide(PENDING, 'approver', can)).toBe(true);
    expect(mayDecide(PENDING, 'AO', can)).toBe(false);
    expect(mayDecide(PENDING, 'approver', () => false)).toBe(false);
    expect(mayDecide({ ...PENDING, recordStatus: 'ACTIVE' }, 'approver', can)).toBe(false);
    expect(mayDecide(PENDING, undefined, can)).toBe(false);
  });
});
