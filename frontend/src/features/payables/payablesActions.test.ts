import type { Invoice, Voucher } from '@/api/payables';
import { listQuery } from '@/api/payables';
import {
  firstError,
  invoiceActions,
  invoiceListStart,
  linkedInvoiceId,
  voucherActions,
} from './payablesActions';

const everyone = () => true;
const makerOnly = (p: string) => p === 'RECEIPT_PAYMENT_MAINTAIN';

const invoice = (status: Invoice['status']) => ({ status }) as Invoice;
const voucher = (patch: Partial<Voucher>) =>
  ({ status: 'APPROVED', paymentMode: 'CHEQUE', ...patch }) as Voucher;

describe('document actions', () => {
  it('follows the maker-checker lifecycle of invoices', () => {
    expect(invoiceActions(invoice('DRAFT'), makerOnly).map((a) => a.id)).toEqual([
      'submit',
      'cancel',
    ]);
    expect(invoiceActions(invoice('PENDING_APPROVAL'), everyone).map((a) => a.id)).toEqual([
      'approve',
      'reject',
      'cancel',
    ]);
    expect(invoiceActions(invoice('APPROVED'), everyone)).toEqual([]);
  });

  it('offers presentation and void only for open cheques', () => {
    expect(voucherActions(voucher({}), everyone).map((a) => a.id)).toEqual(['presented', 'void']);
    expect(voucherActions(voucher({}), makerOnly).map((a) => a.id)).toEqual(['presented']);
    expect(voucherActions(voucher({ presentedOn: '2026-09-01' }), everyone)).toEqual([]);
    expect(voucherActions(voucher({ paymentMode: 'PDC' }), everyone)).toEqual([]);
  });

  it('returns the first error set', () => {
    const err = new Error('x');
    expect(firstError(null, undefined, err)).toBe(err);
    expect(firstError(null, undefined)).toBeUndefined();
  });

  it('builds repeated list parameters', () => {
    expect(listQuery(7, 'types', ['SUPPLIER', 'GARAGE'])).toBe(
      '?companyId=7&types=SUPPLIER&types=GARAGE',
    );
  });
});

describe('invoice drill-down link', () => {
  it('reads the invoice id of a ?invoice= link', () => {
    expect(linkedInvoiceId('48')).toBe(48);
    expect(linkedInvoiceId(null)).toBeNull();
    expect(linkedInvoiceId('abc')).toBeNull();
    expect(linkedInvoiceId('0')).toBeNull();
    expect(linkedInvoiceId('1.5')).toBeNull();
    expect(invoiceListStart('48')).toEqual({ status: '', selected: 48 });
    expect(invoiceListStart(null)).toEqual({ status: 'PENDING_APPROVAL', selected: null });
  });
});
