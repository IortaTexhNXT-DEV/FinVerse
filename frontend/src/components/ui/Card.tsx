import type { ReactNode } from 'react';
import { titleCase } from '@/utils/format';
import { calloutName } from './calloutName';

interface CardProps {
  title?: ReactNode;
  actions?: ReactNode;
  flush?: boolean;
  className?: string;
  /**
   * Stable name of the card for callouts and capture recipes (data-callout); by default the title
   * in lower case with dashes ("Rate Exceptions" is rate-exceptions).
   */
  callout?: string;
  children: ReactNode;
}

/**
 * Content container with optional header title (in Title Case, acronyms kept) and actions. The
 * callout name stays the title as written.
 */
export function Card({
  title,
  actions,
  flush = false,
  className,
  callout,
  children,
}: Readonly<CardProps>) {
  return (
    <section className={`card ${className ?? ''}`} data-callout={callout ?? calloutName(title)}>
      {title !== undefined && (
        <header className="card-header">
          <h2>{typeof title === 'string' ? titleCase(title) : title}</h2>
          <div className="spacer" />
          {actions}
        </header>
      )}
      <div className={flush ? 'card-body flush' : 'card-body'}>{children}</div>
    </section>
  );
}
