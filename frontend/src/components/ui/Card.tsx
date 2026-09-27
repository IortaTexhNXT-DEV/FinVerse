import type { ReactNode } from 'react';

interface CardProps {
  title?: ReactNode;
  actions?: ReactNode;
  flush?: boolean;
  className?: string;
  children: ReactNode;
}

/** Content container with optional header title and actions. */
export function Card({ title, actions, flush = false, className, children }: Readonly<CardProps>) {
  return (
    <section className={`card ${className ?? ''}`}>
      {title !== undefined && (
        <header className="card-header">
          <h2>{title}</h2>
          <div className="spacer" />
          {actions}
        </header>
      )}
      <div className={flush ? 'card-body flush' : 'card-body'}>{children}</div>
    </section>
  );
}
