import type { FeatureModule, NavGroup, ScreenDef } from '@/navigation/types';

/** A sidebar group reduced to the sections and screens the user may open. */
export interface VisibleGroup {
  id: string;
  title?: string;
  sections: { module: FeatureModule; screens: ScreenDef[] }[];
  /** Menu paths inside the group, to open the group holding the current page. */
  paths: string[];
}

/**
 * Filters the navigation to what a user may open: hidden screens and screens without permission
 * are dropped, then empty sections and empty groups.
 */
export function visibleGroups(
  groups: readonly NavGroup[],
  allowed: (screen: ScreenDef) => boolean,
): VisibleGroup[] {
  return groups
    .map((group) => {
      const sections = group.modules
        .map((module) => ({
          module,
          screens: module.screens.filter((s) => s.hidden !== true && allowed(s)),
        }))
        .filter((section) => section.screens.length > 0);
      return {
        id: group.id,
        title: group.title,
        sections,
        paths: sections.flatMap((s) => s.screens.map((screen) => screen.path)),
      };
    })
    .filter((group) => group.sections.length > 0);
}
