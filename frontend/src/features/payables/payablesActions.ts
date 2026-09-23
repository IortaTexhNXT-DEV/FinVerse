import type { Invoice, Voucher } from '@/api/payables';

/** An action button offered for a document in its current status. */
export interface DocumentAction<T extends string> {
  id: T;
  label: string;
  variant: 'primary' | 'accent' | 'secondary' | 'danger';
}

export type InvoiceActionId = 'submit' | 'approve' | 'reject' | 'cancel';
export type VoucherActionId = InvoiceActionId | 'presented' | 'void';

const MAINTAIN = 'RECEIPT_PAYMENT_MAINTAIN';
const AUTHORIZE = 'RECEIPT_PAYMENT_AUTHORIZE';

type Can = (permission: string) => boolean;

/** Maker-checker actions shared by invoices and vouchers (draft → pending → approved). */
function approvalActions(status: string, can: Can): DocumentAction<InvoiceActionId>[] {
  const actions: DocumentAction<InvoiceActionId>[] = [];
  if (status === 'DRAFT' && can(MAINTAIN)) {
    actions.push({ id: 'submit', label: 'Submit', variant: 'primary' });
  }
  if (status === 'PENDING_APPROVAL' && can(AUTHORIZE)) {
    actions.push(
      { id: 'approve', label: 'Approve & post', variant: 'accent' },
      { id: 'reject', label: 'Reject', variant: 'secondary' },
    );
  }
  if (status === 'DRAFT' || status === 'PENDING_APPROVAL') {
    actions.push({ id: 'cancel', label: 'Cancel', variant: 'danger' });
  }
  return actions;
}

/** Actions available on a supplier invoice. */
export function invoiceActions(invoice: Invoice, can: Can): DocumentAction<InvoiceActionId>[] {
  return approvalActions(invoice.status, can);
}

/** Actions available on a payment voucher (cheques can be confirmed or voided once approved). */
export function voucherActions(voucher: Voucher, can: Can): DocumentAction<VoucherActionId>[] {
  const actions: DocumentAction<VoucherActionId>[] = approvalActions(voucher.status, can);
  const openCheque =
    voucher.status === 'APPROVED' &&
    voucher.paymentMode === 'CHEQUE' &&
    voucher.presentedOn === undefined;
  if (openCheque) {
    actions.push({ id: 'presented', label: 'Cheque presented', variant: 'secondary' });
    if (can(AUTHORIZE)) {
      actions.push({ id: 'void', label: 'Void cheque', variant: 'danger' });
    }
  }
  return actions;
}

/** First error that is set (lets a screen show one alert for several queries and mutations). */
export function firstError(...errors: unknown[]): unknown {
  return errors.find((e) => e !== null && e !== undefined);
}
