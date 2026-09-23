import {
  ArrowLeftRight,
  BadgeCheck,
  CalendarClock,
  FilePlus2,
  FileSpreadsheet,
  Landmark,
  NotebookTabs,
  ReceiptText,
  ScrollText,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const RECEIPT_VIEW = 'RECEIPT_PAYMENT_MAINTAIN';
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
      component: lazy(() => import('./ReceiptsPage')),
    },
    {
      path: '/receivables/receipts/new',
      label: 'New Receipt',
      icon: FilePlus2,
      permission: RECEIPT_VIEW,
      component: lazy(() => import('./ReceiptEntryPage')),
    },
    {
      path: '/receivables/receipts/:id',
      label: 'Receipt',
      icon: ReceiptText,
      permission: RECEIPT_VIEW,
      component: lazy(() => import('./ReceiptDetailPage')),
      hidden: true,
    },
    {
      path: '/receivables/approvals',
      label: 'Receipt Approvals',
      icon: BadgeCheck,
      permission: RECEIPT_APPROVE,
      component: lazy(() => import('./ReceiptsPage')),
    },
    {
      path: '/receivables/approvals/:id',
      label: 'Receipt',
      icon: ReceiptText,
      permission: RECEIPT_APPROVE,
      component: lazy(() => import('./ReceiptDetailPage')),
      hidden: true,
    },
    {
      path: '/receivables/deposits',
      label: 'Cheques & Deposits',
      icon: Landmark,
      permission: RECEIPT_VIEW,
      component: lazy(() => import('./DepositsPage')),
    },
    {
      path: '/receivables/pdcs',
      label: 'PDC Received',
      icon: CalendarClock,
      permission: RECEIPT_VIEW,
      component: lazy(() => import('./PdcPage')),
    },
    {
      path: '/receivables/bank-statements',
      label: 'Bank Statements',
      icon: FileSpreadsheet,
      permission: 'RECONCILIATION_MANAGE',
      component: lazy(() => import('./BankStatementsPage')),
    },
    {
      path: '/receivables/bank-reconciliation',
      label: 'Bank Reconciliation',
      icon: ArrowLeftRight,
      permission: 'RECONCILIATION_MANAGE',
      component: lazy(() => import('./BankReconciliationPage')),
    },
    {
      path: '/receivables/party-statement',
      label: 'Party Statement',
      icon: NotebookTabs,
      permission: RECEIPT_VIEW,
      component: lazy(() => import('./PartyStatementPage')),
    },
    {
      path: '/receivables/reports',
      label: 'Receivables Reports',
      icon: ScrollText,
      permission: 'REPORT_FINANCIAL',
      component: lazy(() => import('./ReceivablesReportsPage')),
    },
  ],
};
