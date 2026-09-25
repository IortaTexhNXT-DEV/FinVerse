import {
  Award,
  BookOpenCheck,
  CalendarClock,
  FileSpreadsheet,
  Inbox,
  Landmark,
  Library,
  Receipt,
  ScrollText,
  Settings2,
  Stamp,
  Users,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

const VIEW = 'TAX_VIEW';

/** Tax & Statutory: BIR / LGU / BFP returns, 2307 certificates and Insurance Commission schedules. */
export const taxModule: FeatureModule = {
  id: 'tax',
  section: 'Tax & Statutory',
  screens: [
    {
      path: '/tax/calendar',
      label: 'Tax Calendar',
      icon: CalendarClock,
      permission: VIEW,
      component: lazy(() => import('./TaxCalendarPage')),
    },
    {
      path: '/tax/vat',
      label: 'VAT Worksheet',
      icon: Receipt,
      permission: VIEW,
      component: lazy(() => import('./VatWorksheetPage')),
    },
    {
      path: '/tax/ewt',
      label: 'Withholding Tax',
      icon: FileSpreadsheet,
      permission: VIEW,
      component: lazy(() => import('./EwtWorksheetPage')),
    },
    {
      path: '/tax/dst',
      label: 'Documentary Stamp Tax',
      icon: Stamp,
      permission: VIEW,
      component: lazy(() => import('./DstWorksheetPage')),
    },
    {
      path: '/tax/premium-tax',
      label: 'Premium Tax, LGT & FST',
      icon: Landmark,
      permission: VIEW,
      component: lazy(() => import('./PremiumTaxWorksheetPage')),
    },
    {
      path: '/tax/returns',
      label: 'Tax Returns',
      icon: ScrollText,
      permission: VIEW,
      component: lazy(() => import('./TaxReturnsPage')),
    },
    {
      path: '/tax/2307',
      label: 'BIR Form 2307',
      icon: Award,
      permission: VIEW,
      component: lazy(() => import('./Certificates2307Page')),
    },
    {
      path: '/tax/received-certificates',
      label: 'Certificates Received',
      icon: Inbox,
      permission: VIEW,
      alsoPermissions: ['DISB_TAG'],
      component: lazy(() => import('./ReceivedCertificatesPage')),
    },
    {
      path: '/tax/bir-outputs',
      label: 'BIR Forms & Books',
      icon: Library,
      permission: VIEW,
      component: lazy(() => import('./BirOutputsPage')),
    },
    {
      path: '/tax/ic-schedules',
      label: 'IC Schedules',
      icon: BookOpenCheck,
      permission: VIEW,
      component: lazy(() => import('./IcSchedulesPage')),
    },
    {
      path: '/tax/codes',
      label: 'Tax Codes & Forms',
      icon: Settings2,
      permission: VIEW,
      component: lazy(() => import('./TaxCodesPage')),
    },
    {
      path: '/tax/profiles',
      label: 'Party Tax Profiles',
      icon: Users,
      permission: VIEW,
      component: lazy(() => import('./PartyProfilesPage')),
    },
    {
      path: '/tax/ic-mapping',
      label: 'IC Mapping',
      icon: Settings2,
      permission: VIEW,
      component: lazy(() => import('./IcMappingPage')),
    },
  ],
};
