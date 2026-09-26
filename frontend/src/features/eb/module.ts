import {
  ClipboardCheck,
  ClipboardList,
  FilePlus2,
  FileText,
  HeartPulse,
  Receipt,
  Scale,
  Settings2,
  UsersRound,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

/**
 * Employee Benefits (BRD-8, BRID-001-030; docs/architecture/EMPLOYEE_BENEFITS_DESIGN.md section
 * 10.1): EB programmes and cycles up to placement, member servicing and SOAs. Registered by the
 * foundation (E0) in Client & Policy after Non-Package Management with every internal route of the
 * design; each route points at the page of the wave that builds it (E1-B: programmes, programme,
 * comparative, set-up; E1-C: member changes, pending items, SOA). A wave replaces its page files
 * and keeps the routes. BDOI Drop 2 has no partner portal: no portal screen or route is declared.
 * EB reports are in the Report Centre under Employee Benefits.
 */
export const ebModule: FeatureModule = {
  id: 'eb',
  section: 'Employee Benefits',
  screens: [
    {
      path: '/eb',
      label: 'EB Home',
      icon: HeartPulse,
      permission: 'EB_VIEW',
      component: lazy(() => import('./home/EbHomePage')),
    },
    {
      path: '/eb/programmes',
      label: 'Programmes',
      icon: ClipboardList,
      permission: 'EB_VIEW',
      component: lazy(() => import('./programmes/ProgrammesPage')),
    },
    {
      path: '/eb/programmes/new',
      label: 'New Programme',
      icon: FilePlus2,
      permission: 'EB_MARKET',
      component: lazy(() => import('./programmes/NewProgrammePage')),
    },
    {
      path: '/eb/programmes/:id',
      label: 'Programme',
      icon: FileText,
      permission: 'EB_VIEW',
      component: lazy(() => import('./programmes/ProgrammePage')),
      hidden: true,
    },
    {
      path: '/eb/comparatives/:id',
      label: 'Comparative',
      icon: Scale,
      permission: 'EB_VIEW',
      component: lazy(() => import('./comparative/ComparativePage')),
      hidden: true,
    },
    {
      path: '/eb/member-changes',
      label: 'Member Changes',
      icon: UsersRound,
      permission: 'EB_VIEW',
      component: lazy(() => import('./members/MemberChangesPage')),
    },
    {
      path: '/eb/pending-items',
      label: 'Pending Items',
      icon: ClipboardCheck,
      permission: 'EB_VIEW',
      component: lazy(() => import('./pending/PendingItemsPage')),
    },
    {
      path: '/eb/soa',
      label: 'SOA Register',
      icon: Receipt,
      permission: 'EB_PROCESS',
      alsoPermissions: ['EB_COLLECT'],
      component: lazy(() => import('./soa/SoaRegisterPage')),
    },
    {
      path: '/eb/setup',
      label: 'EB Setup',
      icon: Settings2,
      permission: 'EB_SETUP',
      component: lazy(() => import('./setup/EbSetupPage')),
    },
  ],
};
