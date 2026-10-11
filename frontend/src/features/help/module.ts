import { CircleHelp, UserRound } from 'lucide-react';
import { lazyWithRetry } from '@/app/loadRecovery';
import type { FeatureModule } from '@/navigation/types';

export const helpModule: FeatureModule = {
  id: 'help',
  section: 'Help',
  screens: [
    {
      path: '/help',
      label: 'Help Center',
      icon: CircleHelp,
      component: lazyWithRetry(() => import('./HelpPage')),
    },
    {
      path: '/profile',
      label: 'My Profile',
      icon: UserRound,
      component: lazyWithRetry(() => import('@/features/profile/MyProfilePage')),
    },
  ],
};
