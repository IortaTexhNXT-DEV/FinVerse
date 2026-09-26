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
  component: LazyExoticComponent<ComponentType>;
  /** Detail/edit screens reached from a list are not shown in the menu. */
  hidden?: boolean;
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
