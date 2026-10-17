import { MODULE_OFF } from '@/navigation/productModules';

/** Dashboard widgets that belong to a product module (the others are platform widgets). */
export const WIDGET_MODULES = {
  payables: 'PAYABLES',
  budget: 'BUDGET',
} as const;

/** Whether a widget of a product module is shown: its module is in use in this deployment. */
export function showsWidget(
  widget: keyof typeof WIDGET_MODULES,
  can: (permission: string) => boolean,
): boolean {
  return !can(`${MODULE_OFF}${WIDGET_MODULES[widget]}`);
}
