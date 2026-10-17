import {
  Award,
  CalendarClock,
  FileSpreadsheet,
  Inbox,
  Library,
  Receipt,
  ScrollText,
  Settings2,
  Users,
} from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

const VIEW = 'TAX_VIEW';

/** Tax & Statutory: BIR returns and worksheets, 2307 certificates and the BIR forms and books. */
export const taxModule: FeatureModule = {
  id: 'tax',
  section: 'Tax & Statutory',
  screens: [
    {
      path: '/tax/calendar',
      label: 'Tax Calendar',
      icon: CalendarClock,
      permission: VIEW,
      component: lazyWithRetry(() => import('./TaxCalendarPage')),
    },
    {
      path: '/tax/vat',
      label: 'VAT Worksheet',
      icon: Receipt,
      permission: VIEW,
      component: lazyWithRetry(() => import('./VatWorksheetPage')),
    },
    {
      path: '/tax/ewt',
      label: 'Withholding Tax',
      icon: FileSpreadsheet,
      permission: VIEW,
      component: lazyWithRetry(() => import('./EwtWorksheetPage')),
    },
    {
      path: '/tax/returns',
      label: 'Tax Returns',
      icon: ScrollText,
      permission: VIEW,
      component: lazyWithRetry(() => import('./TaxReturnsPage')),
    },
    {
      path: '/tax/2307',
      label: 'BIR Form 2307',
      icon: Award,
      permission: VIEW,
      component: lazyWithRetry(() => import('./Certificates2307Page')),
    },
    {
      path: '/tax/received-certificates',
      label: 'Certificates Received',
      icon: Inbox,
      permission: VIEW,
      alsoPermissions: ['DISB_TAG'],
      component: lazyWithRetry(() => import('./ReceivedCertificatesPage')),
    },
    {
      path: '/tax/bir-outputs',
      label: 'BIR Forms & Books',
      icon: Library,
      permission: VIEW,
      component: lazyWithRetry(() => import('./BirOutputsPage')),
    },
    {
      path: '/tax/codes',
      label: 'Tax Codes & Forms',
      icon: Settings2,
      permission: VIEW,
      component: lazyWithRetry(() => import('./TaxCodesPage')),
    },
    {
      path: '/tax/profiles',
      label: 'Party Tax Profiles',
      icon: Users,
      permission: VIEW,
      component: lazyWithRetry(() => import('./PartyProfilesPage')),
    },
  ],
};
