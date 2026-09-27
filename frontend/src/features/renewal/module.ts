import {
  ArrowLeftRight,
  CalendarClock,
  ClipboardCheck,
  FileText,
  Headset,
  Landmark,
  ListChecks,
  Mails,
  RefreshCcw,
  Settings2,
  UserCheck,
  Workflow,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Renewal (BRD-6; docs/architecture/RENEWAL_DESIGN.md section 12): the expiring accounts from
 * extraction and sanitation to the disposition, the Team Leader review, processing with the
 * insurer, the Renewal Advice, the acceptance and the booking of the renewal, in Client & Policy
 * after Placement & Booking. Renewal reports are in the Report Centre under Renewal.
 */
export const renewalModule: FeatureModule = {
  id: 'renewal',
  section: 'Renewal',
  screens: [
    {
      path: '/renewal',
      label: 'Renewal Home',
      icon: RefreshCcw,
      permission: 'RNW_VIEW',
      component: lazy(() => import('./home/RenewalHomePage')),
    },
    {
      path: '/renewal/expiry',
      label: 'Expiry List',
      icon: CalendarClock,
      permission: 'RNW_VIEW',
      component: lazy(() => import('./expiry/ExpiryListPage')),
    },
    {
      path: '/renewal/mine',
      label: 'My Dispositions',
      icon: UserCheck,
      permission: 'RNW_DISPOSE',
      component: lazy(() => import('./mine/MyDispositionsPage')),
    },
    {
      path: '/renewal/review',
      label: 'TL Review',
      icon: ClipboardCheck,
      permission: 'RNW_REVIEW',
      component: lazy(() => import('./review/ReviewPage')),
    },
    {
      path: '/renewal/transfers',
      label: 'Transfers',
      icon: ArrowLeftRight,
      permission: 'RNW_ASSIGN',
      component: lazy(() => import('./transfers/TransfersPage')),
    },
    {
      path: '/renewal/processing',
      label: 'Processing Worklist',
      icon: Workflow,
      permission: 'RNW_PROCESS',
      component: lazy(() => import('./processing/ProcessingPage')),
    },
    {
      path: '/renewal/insurer',
      label: 'Insurer Batches',
      icon: Landmark,
      permission: 'RNW_INSURER',
      component: lazy(() => import('./insurer/InsurerBatchesPage')),
    },
    {
      path: '/renewal/letters',
      label: 'Letters',
      icon: Mails,
      permission: 'RNW_RA_GENERATE',
      component: lazy(() => import('./letters/LettersPage')),
    },
    {
      path: '/renewal/followups',
      label: 'Follow-ups',
      icon: Headset,
      permission: 'RNW_FOLLOWUP',
      component: lazy(() => import('./followups/FollowupsPage')),
    },
    {
      path: '/renewal/lamd',
      label: 'LAMD Reports',
      icon: ListChecks,
      permission: 'RNW_LAMD_UPLOAD',
      component: lazy(() => import('./lamd/LamdPage')),
    },
    {
      path: '/renewal/setup',
      label: 'Renewal Setup',
      icon: Settings2,
      permission: 'RNW_SETUP',
      alsoPermissions: ['RNW_PACKAGE_REMAP'],
      component: lazy(() => import('./setup/RenewalSetupPage')),
    },
    {
      path: '/renewal/candidates/:ref',
      label: 'Renewal',
      icon: FileText,
      permission: 'RNW_VIEW',
      component: lazy(() => import('./record/CandidatePage')),
      hidden: true,
    },
  ],
};
