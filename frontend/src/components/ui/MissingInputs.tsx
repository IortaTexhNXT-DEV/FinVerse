/**
 * The one-line reason next to a disabled button: what the user still has to give ("Still needed:
 * the data file and the control file."). Renders nothing when nothing is missing, so the button is
 * enabled and the line disappears together.
 */
export function MissingInputs({
  missing,
  id,
}: Readonly<{ missing: readonly (string | false | undefined)[]; id?: string }>) {
  const items = missing.filter((m): m is string => typeof m === 'string' && m !== '');
  if (items.length === 0) {
    return null;
  }
  return (
    <span id={id} className="muted missing-inputs" role="status">
      Still needed: {joinWords(items)}.
    </span>
  );
}

/** "a", "a and b", "a, b and c". */
function joinWords(items: readonly string[]): string {
  if (items.length === 1) {
    return items[0] ?? '';
  }
  return `${items.slice(0, -1).join(', ')} and ${items[items.length - 1] ?? ''}`;
}
