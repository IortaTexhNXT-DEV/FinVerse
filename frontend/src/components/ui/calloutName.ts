import type { ReactNode } from 'react';

/** The default callout name of a card: its text title in lower case, words joined by dashes. */
export function calloutName(title: ReactNode): string | undefined {
  if (typeof title !== 'string') {
    return undefined;
  }
  const name = title
    .toLowerCase()
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '');
  return name === '' ? undefined : name;
}
