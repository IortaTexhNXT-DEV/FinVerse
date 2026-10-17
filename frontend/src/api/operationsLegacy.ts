import type { LedgerComponent } from './operations';
import type { RecordOriginFields } from './types';

/** Open legacy invoices migrated at cut-over in the Operations ledger (BRD-13). */

/** Origin of an invoice: BIBS, or an open legacy invoice migrated at cut-over. */
export interface InvoiceOriginFields extends RecordOriginFields {
  ledgerContext?: 'NEW' | 'LEGACY';
  legacyInvoiceNo?: string;
}

export interface LegacyPositionLine {
  component: LedgerComponent;
  booked: number;
  adjusted: number;
  paid: number;
  remitted: number;
  writtenOff: number;
  openBalance: number;
}

export interface LegacySnapshot {
  sourceSystem: string;
  legacyInvoiceNo: string;
  legacyRef?: string;
  legacyServiceInvoiceNo?: string;
  invoiceDate: string;
  dueDate?: string;
  grossPremium: number;
  commission: number;
  vatOnCommission: number;
  commissionRealised: number;
  deferredVatOpen: number;
  openBalanceMode: boolean;
  shares: string;
  migrationBatch: string;
  takenAt: string;
  lines: LegacyPositionLine[];
}
