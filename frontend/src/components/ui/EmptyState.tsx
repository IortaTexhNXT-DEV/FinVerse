/** Table-and-magnifier illustration used by the BDO Insure empty states. */
function EmptyIllustration() {
  return (
    <svg
      width="128"
      height="88"
      viewBox="0 0 128 88"
      aria-hidden="true"
      className="empty-illustration"
    >
      <rect
        x="6"
        y="10"
        width="92"
        height="64"
        rx="6"
        fill="var(--color-surface)"
        stroke="var(--color-border-strong)"
        strokeWidth="2"
      />
      <path d="M6 28h92M6 46h92M36 10v64M68 10v64" stroke="var(--color-border)" strokeWidth="2" />
      <circle
        cx="92"
        cy="50"
        r="18"
        fill="var(--brand-blue-050)"
        stroke="var(--brand-blue)"
        strokeWidth="5"
      />
      <path d="M105 63l14 14" stroke="var(--brand-blue)" strokeWidth="7" strokeLinecap="round" />
    </svg>
  );
}

/** Empty list or search result: illustration and message (BDO Insure pattern). */
export function EmptyState({ message = 'No items to display' }: Readonly<{ message?: string }>) {
  return (
    <div className="empty-state">
      <EmptyIllustration />
      <span>{message}</span>
    </div>
  );
}
