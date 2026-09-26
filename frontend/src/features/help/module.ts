import { CircleHelp, UserRound } from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

export const helpModule: FeatureModule = {
  id: 'help',
  section: 'Help',
  screens: [
    {
      path: '/help',
      label: 'Help Center',
      icon: CircleHelp,
      component: lazy(() => import('./HelpPage')),
    },
    {
      path: '/profile',
      label: 'My Profile',
      icon: UserRound,
      component: lazy(() => import('@/features/profile/MyProfilePage')),
    },
  ],
};
