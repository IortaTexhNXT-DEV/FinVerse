/** An action of the row menu of a catalog record. */
export type RecordAction = 'authorize' | 'deactivate';

/**
 * The confirmation after an action, naming the record: its reference when the record has one,
 * otherwise what the menu is about (a clause by its title), never a bare "authorized".
 */
export function actionMessage(
  reference: string | null | undefined,
  label: string,
  done: RecordAction,
) {
  const name = reference?.trim() ? reference.trim() : label;
  return `${name} ${done === 'authorize' ? 'authorized' : 'deactivated'}`;
}
