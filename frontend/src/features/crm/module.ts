import { CalendarClock, Contact, UserPen, UserPlus, Users } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

/** Clients (crm): client master, onboarding, KYC reviews (BRNB.029-032/046-049/090/091/099/110). */
export const crmModule: FeatureModule = {
  id: 'crm',
  section: 'Clients',
  screens: [
    {
      path: '/crm/clients',
      label: 'Clients',
      icon: Users,
      permission: 'CLIENT_VIEW',
      component: lazyWithRetry(() => import('./ClientsPage')),
    },
    {
      path: '/crm/clients/new',
      label: 'New Client',
      icon: UserPlus,
      permission: 'CLIENT_MAINTAIN',
      component: lazyWithRetry(() => import('./ClientFormPage')),
    },
    {
      path: '/crm/kyc-reviews',
      label: 'KYC Reviews Due',
      icon: CalendarClock,
      permission: 'CLIENT_VIEW',
      component: lazyWithRetry(() => import('./KycReviewsPage')),
    },
    {
      path: '/crm/clients/:id',
      label: 'Client',
      icon: Contact,
      permission: 'CLIENT_VIEW',
      component: lazyWithRetry(() => import('./ClientDetailPage')),
      hidden: true,
    },
    {
      path: '/crm/clients/:id/edit',
      label: 'Edit Client',
      icon: UserPen,
      permission: 'CLIENT_MAINTAIN',
      component: lazyWithRetry(() => import('./ClientFormPage')),
      hidden: true,
    },
  ],
};
