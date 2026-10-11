import {
  BookOpen,
  BookUser,
  CalendarRange,
  FilePlus2,
  ListTree,
  Search,
  Upload,
} from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

const JournalEntryPage = lazyWithRetry(() => import('./JournalEntryPage'));

export const glModule: FeatureModule = {
  id: 'gl',
  section: 'General Ledger',
  screens: [
    {
      path: '/gl/journals',
      label: 'Journals',
      icon: BookOpen,
      permission: 'JOURNAL_VIEW',
      component: lazyWithRetry(() => import('./JournalsPage')),
    },
    {
      path: '/gl/journals/new',
      label: 'New Journal',
      icon: FilePlus2,
      permission: 'JOURNAL_CREATE',
      component: JournalEntryPage,
    },
    {
      path: '/gl/journals/:id',
      label: 'Journal',
      icon: BookOpen,
      permission: 'JOURNAL_VIEW',
      component: lazyWithRetry(() => import('./JournalDetailPage')),
      hidden: true,
    },
    {
      path: '/gl/journals/:id/edit',
      label: 'Edit Journal',
      icon: BookOpen,
      permission: 'JOURNAL_CREATE',
      component: JournalEntryPage,
      hidden: true,
    },
    {
      path: '/gl/inquiry',
      label: 'Account Inquiry',
      icon: Search,
      permission: 'JOURNAL_VIEW',
      component: lazyWithRetry(() => import('./AccountInquiryPage')),
    },
    {
      path: '/gl/party-statement',
      label: 'Party Statement',
      icon: BookUser,
      permission: 'JOURNAL_VIEW',
      component: lazyWithRetry(() => import('./PartyLedgerPage')),
    },
    {
      path: '/gl/accounts',
      label: 'Chart of Accounts',
      icon: ListTree,
      permission: 'MASTER_VIEW',
      component: lazyWithRetry(() => import('./ChartOfAccountsPage')),
    },
    {
      path: '/gl/accounts/upload',
      label: 'Chart Upload',
      icon: Upload,
      permission: 'COA_UPLOAD',
      component: lazyWithRetry(() => import('./ChartUploadPage')),
    },
    {
      path: '/gl/periods',
      label: 'Financial Periods',
      icon: CalendarRange,
      permission: 'MASTER_VIEW',
      component: lazyWithRetry(() => import('./PeriodsPage')),
    },
  ],
};
