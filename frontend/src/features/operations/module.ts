import {
  Bell,
  Cable,
  FileArchive,
  FileSearch,
  Handshake,
  LayoutGrid,
  ReceiptText,
  Wallet,
} from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

const VIEW = 'OPS_VIEW';

/**
 * Operations (BRD-2) foundation screens: the Operations home, the invoice ledger (search and
 * invoice 360), the in-app Disbursement queue, interfaces, hand-offs and extracts, the report
 * archive and notification settings (docs/architecture/OPERATIONS_DESIGN.md section 12). The team
 * modules (cashiering, remittance, prodrecon, adjustment, commission) keep their own sections.
 */
export const operationsModule: FeatureModule = {
  id: 'operations',
  section: 'Operations',
  screens: [
    {
      path: '/operations',
      label: 'Operations Home',
      icon: LayoutGrid,
      permission: VIEW,
      component: lazyWithRetry(() => import('./OperationsHomePage')),
    },
    {
      path: '/operations/invoices',
      label: 'Invoice Search',
      icon: FileSearch,
      permission: VIEW,
      component: lazyWithRetry(() => import('./InvoiceSearchPage')),
    },
    {
      path: '/operations/invoices/:no',
      label: 'Invoice 360',
      icon: ReceiptText,
      permission: VIEW,
      component: lazyWithRetry(() => import('./Invoice360Page')),
      hidden: true,
    },
    {
      path: '/operations/disbursements',
      label: 'Disbursement Queue',
      icon: Wallet,
      permission: 'DISB_PROCESS',
      component: lazyWithRetry(() => import('./DisbursementQueuePage')),
    },
    {
      path: '/operations/handoffs',
      label: 'Hand-offs and Extracts',
      icon: Handshake,
      permission: VIEW,
      component: lazyWithRetry(() => import('./HandoffsPage')),
    },
    {
      path: '/operations/interfaces',
      label: 'Interfaces',
      icon: Cable,
      permission: 'FLOWIN_MANAGE',
      component: lazyWithRetry(() => import('./InterfacesPage')),
    },
    {
      path: '/operations/report-archive',
      label: 'Report Archive',
      icon: FileArchive,
      permission: 'OPS_REPORT_VIEW',
      component: lazyWithRetry(() => import('./ReportArchivePage')),
    },
    {
      path: '/operations/notifications',
      label: 'Notification Settings',
      icon: Bell,
      permission: VIEW,
      component: lazyWithRetry(() => import('./NotificationSettingsPage')),
    },
  ],
};
