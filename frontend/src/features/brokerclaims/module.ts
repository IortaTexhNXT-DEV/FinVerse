import {
  BarChart3,
  ClipboardList,
  FilePlus2,
  FileText,
  ListChecks,
  MapPin,
  NotebookPen,
  Settings2,
  ShieldCheck,
} from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

/**
 * Claims Handling (BRD-7, BRCLM.001-043; docs/architecture/CLAIMS_BROKING_DESIGN.md section 11):
 * the broker's claim case files. Registered by the foundation (CL0) first in the Claims
 * group with every route of the design; each route points at the page of the wave that builds it
 * (CL1-A: record, cover, location; CL1-B: home, worklist, diary, setup, reports). A wave replaces
 * its page files and keeps the routes. The insurer-side Claims module (`features/claims`) is
 * unrelated and stays hidden from BDOI roles.
 */
export const brokerClaimsModule: FeatureModule = {
  id: 'brokerclaims',
  section: 'Claims Handling',
  screens: [
    {
      path: '/claims-handling',
      label: 'Claims Home',
      icon: ClipboardList,
      permission: 'BCL_VIEW',
      component: lazyWithRetry(() => import('./home/ClaimsHomePage')),
    },
    {
      path: '/claims-handling/worklist',
      label: 'Claims Worklist',
      icon: ListChecks,
      permission: 'BCL_VIEW',
      component: lazyWithRetry(() => import('./worklist/WorklistPage')),
    },
    {
      path: '/claims-handling/new',
      label: 'Record Claim',
      icon: FilePlus2,
      permission: 'BCL_RECORD',
      component: lazyWithRetry(() => import('./record/RecordClaimPage')),
    },
    {
      path: '/claims-handling/:id',
      label: 'Claim',
      icon: FileText,
      permission: 'BCL_VIEW',
      component: lazyWithRetry(() => import('./record/ClaimPage')),
      hidden: true,
    },
    {
      path: '/claims-handling/covers',
      label: 'Cover Lookup',
      icon: ShieldCheck,
      permission: 'BCL_COVER_VIEW',
      component: lazyWithRetry(() => import('./cover/CoverLookupPage')),
    },
    {
      path: '/claims-handling/diary',
      label: 'My Diary',
      icon: NotebookPen,
      permission: 'BCL_VIEW',
      component: lazyWithRetry(() => import('./diary/DiaryPage')),
    },
    {
      path: '/claims-handling/location-refs',
      label: 'Insurer Location References',
      icon: MapPin,
      permission: 'BCL_LOCATION_REF_MAINTAIN',
      component: lazyWithRetry(() => import('./location/LocationRefsPage')),
    },
    {
      path: '/claims-handling/reports',
      label: 'Claims Reports',
      icon: BarChart3,
      permission: 'BCL_REPORT_VIEW',
      alsoPermissions: ['BCL_DATA_EXTRACT'],
      component: lazyWithRetry(() => import('./reports/ClaimsReportsPage')),
    },
    {
      path: '/claims-handling/setup',
      label: 'Claims Setup',
      icon: Settings2,
      permission: 'BCL_SETUP',
      component: lazyWithRetry(() => import('./setup/ClaimsSetupPage')),
    },
  ],
};
