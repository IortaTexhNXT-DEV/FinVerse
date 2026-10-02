/** An action of the row menu of a catalog record. */
export type RecordAction = 'authorize' | 'deactivate';

/**
 * The confirmation after an action, naming the record by what the menu is about (a clause by its
 * title, a product by its name); the reference only when there is no such name. A message drops
 * codes (FLEET_REPAIR), so a reference alone would leave a bare "authorized".
 */
export function actionMessage(
  reference: string | null | undefined,
  label: string,
  done: RecordAction,
) {
  const name = label.trim() || (reference?.trim() ?? '');
  return `${name} ${done === 'authorize' ? 'authorized' : 'deactivated'}`;
}
