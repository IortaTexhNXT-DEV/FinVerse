import {
  ArrowLeftRight,
  BadgeCheck,
  CalendarClock,
  FilePlus2,
  FileSpreadsheet,
  Landmark,
  ReceiptText,
  ScrollText,
} from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

const RECEIPT_VIEW = 'JOURNAL_VIEW';
const RECEIPT_APPROVE = 'RECEIPT_PAYMENT_AUTHORIZE';

export const receivablesModule: FeatureModule = {
  id: 'receivables',
  section: 'Receivables & Banking',
  screens: [
    {
      path: '/receivables/receipts',
      label: 'Receipts',
      icon: ReceiptText,
      permission: RECEIPT_VIEW,
      component: lazyWithRetry(() => import('./ReceiptsPage')),
    },
    {
      path: '/receivables/receipts/new',
      label: 'New Receipt',
      icon: FilePlus2,
      permission: 'RECEIPT_PAYMENT_MAINTAIN',
      component: lazyWithRetry(() => import('./ReceiptEntryPage')),
    },
    {
      path: '/receivables/receipts/:id',
      label: 'Receipt',
      icon: ReceiptText,
      permission: RECEIPT_VIEW,
      component: lazyWithRetry(() => import('./ReceiptDetailPage')),
      hidden: true,
    },
    {
      path: '/receivables/approvals',
      label: 'Receipt Approvals',
      icon: BadgeCheck,
      permission: RECEIPT_APPROVE,
      component: lazyWithRetry(() => import('./ReceiptsPage')),
    },
    {
      path: '/receivables/approvals/:id',
      label: 'Receipt',
      icon: ReceiptText,
      permission: RECEIPT_APPROVE,
      component: lazyWithRetry(() => import('./ReceiptDetailPage')),
      hidden: true,
    },
    {
      path: '/receivables/deposits',
      label: 'Cheques & Deposits',
      icon: Landmark,
      permission: RECEIPT_VIEW,
      component: lazyWithRetry(() => import('./DepositsPage')),
    },
    {
      path: '/receivables/pdcs',
      label: 'PDC Received',
      icon: CalendarClock,
      permission: RECEIPT_VIEW,
      component: lazyWithRetry(() => import('./PdcPage')),
    },
    {
      path: '/receivables/bank-statements',
      label: 'Bank Statements',
      icon: FileSpreadsheet,
      permission: 'RECONCILIATION_MANAGE',
      component: lazyWithRetry(() => import('./BankStatementsPage')),
    },
    {
      path: '/receivables/bank-reconciliation',
      label: 'Bank Reconciliation',
      icon: ArrowLeftRight,
      permission: 'RECONCILIATION_MANAGE',
      component: lazyWithRetry(() => import('./BankReconciliationPage')),
    },
    {
      path: '/receivables/reports',
      label: 'Receivables Reports',
      icon: ScrollText,
      permission: 'REPORT_FINANCIAL',
      component: lazyWithRetry(() => import('./ReceivablesReportsPage')),
    },
  ],
};
