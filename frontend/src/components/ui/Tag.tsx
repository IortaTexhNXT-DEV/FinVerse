import type { ReactNode } from 'react';

export type TagTone = 'flag' | 'neutral' | 'info' | 'danger';

interface TagProps {
  children: ReactNode;
  /** Full text for the tooltip when the chip shows a short form. */
  title?: string;
  /** Gold flag chip by default (FFY, Direct Payment…); neutral, info or danger otherwise. */
  tone?: TagTone;
}

/**
 * Record flag or label chip: one height, one font and a fixed minimum width, centred text, never
 * wraps (long text is cut with an ellipsis, full text in the tooltip). Flags go in their own column
 * or in the record header's flag list, never mixed with the status pill.
 */
export function Tag({ children, title, tone = 'flag' }: Readonly<TagProps>) {
  const text = typeof children === 'string' ? children : undefined;
  return (
    <span className={tone === 'flag' ? 'tag' : `tag ${tone}`} title={title ?? text}>
      {children}
    </span>
  );
}
