import type { LucideIcon } from 'lucide-react';
import type { ComponentType, LazyExoticComponent } from 'react';

/** A screen reachable from the sidebar (or only by URL when `hidden`). */
export interface ScreenDef {
  path: string;
  label: string;
  icon: LucideIcon;
  /** Backend permission required to see and open the screen. */
  permission?: string;
  /** Other permissions that also open the screen (e.g. the checker of a maker screen). */
  alsoPermissions?: string[];
  /**
   * Permissions the user must hold as well (all of them).
   */
  requiresAll?: string[];
  /**
   * Product module of the screen (set from its path by `withProductModules`); the screen is not
   * shown while the module is switched off in the deployment.
   */
  productModule?: string;
  component: LazyExoticComponent<ComponentType>;
  /** Detail/edit screens reached from a list are not shown in the menu. */
  hidden?: boolean;
  /**
   * The screen is the landing page after sign-in of the users holding any of these permissions
   * (the work permissions of the module's roles), when no general landing screen applies.
   */
  landingFor?: readonly string[];
}

/**
 * A functional module (sidebar section). Each feature folder exports one FeatureModule;
 * `navigation/modules.ts` lists them in menu order.
 */
export interface FeatureModule {
  id: string;
  section: string;
  screens: ScreenDef[];
}

/**
 * A collapsible sidebar group following the BDOI navigation (Client & Policy, Finance, Claims &
 * Reports ...). A group without a title is always open and shown first (Dashboard, My Work).
 */
export interface NavGroup {
  id: string;
  title?: string;
  modules: FeatureModule[];
}
