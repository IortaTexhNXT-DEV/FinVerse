import { Banknote, CalendarClock, FileText, Landmark, Wallet } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

export const payablesModule: FeatureModule = {
  id: 'payables',
  section: 'Payables & Cash',
  screens: [
    {
      path: '/payables/invoices',
      label: 'Supplier Invoices',
      icon: FileText,
      permission: 'JOURNAL_VIEW',
      component: lazy(() => import('./SupplierInvoicesPage')),
    },
    {
      path: '/payables/invoices/new',
      label: 'New Supplier Invoice',
      icon: FileText,
      permission: 'RECEIPT_PAYMENT_MAINTAIN',
      component: lazy(() => import('./InvoiceEntryPage')),
      hidden: true,
    },
    {
      path: '/payables/vouchers',
      label: 'Payment Vouchers',
      icon: Banknote,
      permission: 'JOURNAL_VIEW',
      component: lazy(() => import('./PaymentVouchersPage')),
    },
    {
      path: '/payables/vouchers/new',
      label: 'New Payment',
      icon: Banknote,
      permission: 'RECEIPT_PAYMENT_MAINTAIN',
      component: lazy(() => import('./PaymentEntryPage')),
      hidden: true,
    },
    {
      path: '/payables/pdc-issued',
      label: 'PDC Issued',
      icon: CalendarClock,
      permission: 'JOURNAL_VIEW',
      component: lazy(() => import('./PdcIssuedPage')),
    },
    {
      path: '/payables/petty-cash',
      label: 'Petty Cash',
      icon: Wallet,
      permission: 'JOURNAL_VIEW',
      component: lazy(() => import('./PettyCashPage')),
    },
    {
      path: '/payables/bank-accounts',
      label: 'Bank Accounts',
      icon: Landmark,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./BankAccountsPage')),
    },
  ],
};
