import type { ReactNode } from 'react';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';

interface WidgetCardProps {
  title: string;
  loading: boolean;
  error: unknown;
  /** True when the widget has nothing to show; a friendly message replaces the content. */
  empty: boolean;
  emptyMessage: string;
  children: ReactNode;
}

/** Card of one dashboard widget with its loading, error and no-data states. */
export function WidgetCard({
  title,
  loading,
  error,
  empty,
  emptyMessage,
  children,
}: Readonly<WidgetCardProps>) {
  let body: ReactNode = children;
  if (error !== null && error !== undefined) {
    body = <ErrorAlert error={error} />;
  } else if (loading) {
    body = <p className="muted">Loading…</p>;
  } else if (empty) {
    body = <p className="muted">{emptyMessage}</p>;
  }
  return <Card title={title}>{body}</Card>;
}
