/**
 * The place of a record, tab or panel while it loads: grey lines in the shape of its content
 * (title, key facts, text) instead of a blank area or a lone spinner. Announced as "Loading".
 */
export function LoadingPanel({ lines = 4 }: Readonly<{ lines?: number }>) {
  return (
    <div className="loading-panel" role="status" aria-label="Loading">
      <div className="skeleton-line loading-title" />
      {Array.from({ length: lines }, (_, i) => (
        <div key={i} className={i % 2 === 0 ? 'skeleton-line wide' : 'skeleton-line'} />
      ))}
    </div>
  );
}
