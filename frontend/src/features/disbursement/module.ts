import {
  ArrowRightLeft,
  Building2,
  CalendarCheck,
  FileBarChart,
  FilePlus2,
  FileText,
  Landmark,
  Upload,
  UserRound,
  Users,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const PROCESSORS = ['DISB_PROCESS', 'DISB_REVIEW', 'DISB_APPROVE'];
const PAYEE_ROLES = ['DISB_PAYEE_MAINTAIN', 'DISB_PAYEE_AUTHORIZE'];
const FUNDING_ROLES = ['DISB_FUNDING_REQUEST', 'DISB_FUNDING_VERIFY', 'DISB_FUNDING_APPROVE'];

/**
 * Disbursement (BRD-5, DIS 2.2-3.28; docs/architecture/ACCOUNTING_DISBURSEMENT_DESIGN.md section
 * 11): payment requests, disbursement vouchers, instruments, end of day, payees and account
 * funding. Registered by the foundation (A0); wave A1-DSB adds its screens (routes under
 * /disbursement).
 */
export const disbursementModule: FeatureModule = {
  id: 'disbursement',
  section: 'Disbursement',
  screens: [
    {
      path: '/disbursement',
      label: 'Disbursement Workbench',
      icon: Landmark,
      permission: 'DISB_VIEW',
      alsoPermissions: [...PROCESSORS, ...PAYEE_ROLES, ...FUNDING_ROLES, 'DISB_EOD'],
      component: lazy(() => import('./DisbursementHomePage')),
    },
    {
      path: '/disbursement/requests/new',
      label: 'Encode Payment Request',
      icon: FilePlus2,
      permission: 'DISB_PROCESS',
      component: lazy(() => import('./EncodeRequestPage')),
      hidden: true,
    },
    {
      path: '/disbursement/vouchers/:id',
      label: 'Disbursement Voucher',
      icon: FileText,
      permission: 'DISB_VIEW',
      alsoPermissions: PROCESSORS,
      component: lazy(() => import('./VoucherPage')),
      hidden: true,
    },
    {
      path: '/disbursement/payees',
      label: 'Payees',
      icon: Users,
      permission: 'DISB_VIEW',
      alsoPermissions: PAYEE_ROLES,
      component: lazy(() => import('./PayeesPage')),
    },
    {
      path: '/disbursement/payees/:id',
      label: 'Payee',
      icon: UserRound,
      permission: 'DISB_VIEW',
      alsoPermissions: PAYEE_ROLES,
      component: lazy(() => import('./PayeePage')),
      hidden: true,
    },
    {
      path: '/disbursement/uploads',
      label: 'Disbursement Uploads',
      icon: Upload,
      permission: 'DISB_UPLOAD',
      alsoPermissions: ['DISB_PAYEE_MAINTAIN'],
      component: lazy(() => import('./UploadsPage')),
    },
    {
      path: '/disbursement/eod',
      label: 'Disbursement End of Day',
      icon: CalendarCheck,
      permission: 'DISB_EOD',
      alsoPermissions: ['DISB_VIEW'],
      component: lazy(() => import('./EodPage')),
    },
    {
      path: '/disbursement/funding',
      label: 'Account Funding',
      icon: ArrowRightLeft,
      permission: 'DISB_VIEW',
      alsoPermissions: FUNDING_ROLES,
      component: lazy(() => import('./FundingPage')),
    },
    {
      path: '/disbursement/funding/:id',
      label: 'Funding Request',
      icon: ArrowRightLeft,
      permission: 'DISB_VIEW',
      alsoPermissions: FUNDING_ROLES,
      component: lazy(() => import('./FundingDetailPage')),
      hidden: true,
    },
    {
      path: '/disbursement/banks',
      label: 'Bank Accounts and Checks',
      icon: Building2,
      permission: 'DISB_VIEW',
      alsoPermissions: ['DISB_REVIEW', 'DISB_APPROVE'],
      component: lazy(() => import('./BanksPage')),
    },
    {
      path: '/disbursement/reports',
      label: 'Disbursement Reports',
      icon: FileBarChart,
      permission: 'DISB_REPORT_VIEW',
      alsoPermissions: ['DISB_VIEW'],
      component: lazy(() => import('./ReportsPage')),
    },
  ],
};
