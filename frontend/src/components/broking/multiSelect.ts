/**
 * Wire value of a multi-select criterion: '' for all, "A,B" for a list, "!A,B" for all except
 * the list.
 */

/** Parses the wire value of a multi-select criterion. */
export function parseSelection(value: string): { except: boolean; codes: string[] } {
  const except = value.startsWith('!');
  const body = except ? value.slice(1) : value;
  return { except, codes: body.split(',').filter((c) => c !== '') };
}

/** The wire value of a selection ('' when nothing is chosen). */
export function selectionValue(except: boolean, codes: readonly string[]): string {
  if (codes.length === 0) {
    return '';
  }
  return (except ? '!' : '') + codes.join(',');
}
