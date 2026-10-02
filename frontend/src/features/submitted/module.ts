import {
  BadgeDollarSign,
  ClipboardCheck,
  FileSearch,
  FileSignature,
  FileText,
  Inbox,
  ListChecks,
  Mails,
  PlayCircle,
  ReceiptText,
  RefreshCcw,
  Settings2,
  ShieldCheck,
  Upload,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Submitted Policies (BRD-12; docs/architecture/SUBMITTED_POLICIES_DESIGN.md section 12): the
 * masterlist of the bank-submitted policies from intake and processing to the reviews, IAAF and
 * TOR, the hand-off to Renewal, the letters, the handling fees and the No Touch billing, in Client
 * & Policy after Renewal. The reports are in the Report Centre under Submitted Policies.
 */
export const submittedModule: FeatureModule = {
  id: 'submitted',
  section: 'Submitted Policies',
  screens: [
    {
      path: '/submitted',
      label: 'Submitted Policies Home',
      icon: Inbox,
      permission: 'SBM_VIEW',
      component: lazy(() => import('./home/SubmittedHomePage')),
    },
    {
      path: '/submitted/masterlist',
      label: 'Masterlist',
      icon: ListChecks,
      permission: 'SBM_VIEW',
      component: lazy(() => import('./masterlist/MasterlistPage')),
    },
    {
      path: '/submitted/intake',
      label: 'Upload & Intake',
      icon: Upload,
      permission: 'SBM_INTAKE',
      alsoPermissions: ['SBM_MIGRATE'],
      component: lazy(() => import('./intake/IntakePage')),
    },
    {
      path: '/submitted/extractions',
      label: 'Extraction Review',
      icon: FileSearch,
      permission: 'SBM_MAINTAIN',
      component: lazy(() => import('./extraction/ExtractionPage')),
    },
    {
      path: '/submitted/runs',
      label: 'Processing Runs',
      icon: PlayCircle,
      permission: 'SBM_VIEW',
      component: lazy(() => import('./runs/RunsPage')),
    },
    {
      path: '/submitted/reviews',
      label: 'Reviews & IAAF',
      icon: ClipboardCheck,
      permission: 'SBM_VIEW',
      component: lazy(() => import('./reviews/IaafPage')),
    },
    {
      path: '/submitted/tors',
      label: 'Terms of Reference',
      icon: FileSignature,
      permission: 'SBM_VIEW',
      alsoPermissions: ['TOR_APPROVE'],
      component: lazy(() => import('./reviews/TorPage')),
    },
    {
      path: '/submitted/renewals',
      label: 'Renewal Work List',
      icon: RefreshCcw,
      permission: 'SBM_VIEW',
      component: lazy(() => import('./renewals/RenewalWorkListPage')),
    },
    {
      path: '/submitted/letters',
      label: 'Letters & Print Batches',
      icon: Mails,
      permission: 'SBM_LETTER_SEND',
      component: lazy(() => import('./letters/LettersPage')),
    },
    {
      path: '/submitted/fees',
      label: 'Handling Fees',
      icon: BadgeDollarSign,
      permission: 'SBM_HANDLING_FEE',
      component: lazy(() => import('./fees/HandlingFeesPage')),
    },
    {
      path: '/submitted/no-touch',
      label: 'No Touch Billing',
      icon: ReceiptText,
      permission: 'SBM_HANDLING_FEE',
      component: lazy(() => import('./fees/NoTouchPage')),
    },
    {
      path: '/submitted/setup',
      label: 'Submitted Policies Setup',
      icon: Settings2,
      permission: 'SBM_RULE_MAINTAIN',
      alsoPermissions: ['SBM_RULE_APPROVE'],
      component: lazy(() => import('./setup/SetupPage')),
    },
    {
      path: '/submitted/policies/:id',
      label: 'Submitted Policy',
      icon: FileText,
      permission: 'SBM_VIEW',
      component: lazy(() => import('./record/PolicyPage')),
      hidden: true,
    },
  ],
};

/** Icon of the approval inbox entries of IAAF and TOR. */
export const SUBMITTED_APPROVAL_ICON = ShieldCheck;
