import {
  Building2,
  CalendarDays,
  Coins,
  FileSpreadsheet,
  IdCard,
  Landmark,
  Split,
  Tags,
  Users,
} from 'lucide-react';
import { lazy } from 'react';
import type { FeatureModule } from '@/navigation/types';

export const setupModule: FeatureModule = {
  id: 'setup',
  section: 'Setup',
  screens: [
    {
      path: '/setup/companies',
      label: 'Companies',
      icon: Landmark,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./CompaniesPage')),
    },
    {
      path: '/setup/branches',
      label: 'Branches',
      icon: Building2,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./BranchesPage')),
    },
    {
      path: '/setup/currencies',
      label: 'Currencies & Rates',
      icon: Coins,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./CurrencyRatesPage')),
    },
    {
      path: '/setup/dimensions',
      label: 'Dimensions',
      icon: Tags,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./DimensionsPage')),
    },
    {
      path: '/setup/parties',
      label: 'Business Partners',
      icon: Users,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./PartiesPage')),
    },
    {
      path: '/setup/employees',
      label: 'Employees',
      icon: IdCard,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./EmployeesPage')),
    },
    {
      path: '/setup/cost-centre-rules',
      label: 'Cost-Centre Rules',
      icon: Split,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./CostCentreRulesPage')),
    },
    {
      path: '/setup/statement-layouts',
      label: 'Bank Statement Layouts',
      icon: FileSpreadsheet,
      permission: 'RECONCILIATION_MANAGE',
      component: lazy(() => import('./StatementLayoutsPage')),
    },
    {
      path: '/setup/holidays',
      label: 'Holiday Calendar',
      icon: CalendarDays,
      permission: 'MASTER_VIEW',
      component: lazy(() => import('./HolidaysPage')),
    },
  ],
};
