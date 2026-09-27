import { CircleAlert, CircleCheck, Info, TriangleAlert } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { useId, useState } from 'react';
import type { ReactNode } from 'react';

/** Semantic tone of a notice, a toast or a message: one colour per meaning. */
export type NoticeTone = 'error' | 'warning' | 'info' | 'success';

const ICONS: Record<NoticeTone, LucideIcon> = {
  error: CircleAlert,
  warning: TriangleAlert,
  info: Info,
  success: CircleCheck,
};

interface NoticeProps {
  /** Error red, warning amber, info blue or success green (info by default). */
  tone?: NoticeTone;
  /** Bold short title ("Cannot submit for ManCom sign-off"). */
  title?: ReactNode;
  /** The business message. */
  children?: ReactNode;
  /** Several things to fix or know, one bullet each. */
  items?: readonly ReactNode[];
  /**
   * Reference for support (unexpected system errors only, never business-rule refusals): shown
   * muted behind a Details toggle.
   */
  reference?: string;
  /** Buttons aligned right (Retry, Request Rate Exception...). */
  actions?: ReactNode;
  /** Accessible name when the notice has no title. */
  label?: string;
  className?: string;
}

/** Whether a part of a notice has something to show. */
function present(node: ReactNode): boolean {
  return node !== undefined && node !== null && node !== '' && node !== false;
}

/** The support reference of an unexpected error, behind a Details toggle. */
export function SupportReference({ reference }: Readonly<{ reference: string }>) {
  const [open, setOpen] = useState(false);
  const id = useId();
  return (
    <div className="notice-reference">
      <button
        type="button"
        className="link-button notice-details"
        aria-expanded={open}
        aria-controls={id}
        onClick={() => setOpen((v) => !v)}
      >
        {open ? 'Hide Details' : 'Details'}
      </button>
      {open && (
        <span id={id} className="muted">
          Reference for support: <span className="nowrap">{reference}</span>
        </span>
      )}
    </div>
  );
}

/**
 * The one message standard of BIBS (error, warning, info, success): a white surface with a thin
 * accent bar in the semantic colour and its icon, a bold short title, the business message and,
 * when several things are concerned, one bullet each. Notices carry short messages only; records,
 * lists and key data go in tables and definition grids. No technical codes: a support reference is
 * shown only for unexpected system errors, behind Details.
 */
export function Notice({
  tone = 'info',
  title,
  children,
  items,
  reference,
  actions,
  label,
  className,
}: Readonly<NoticeProps>) {
  const Icon = ICONS[tone];
  const shownItems = (items ?? []).filter(present);
  return (
    <div
      className={['notice', `notice-${tone}`, className].filter(Boolean).join(' ')}
      role={tone === 'error' ? 'alert' : 'status'}
      aria-label={label}
    >
      <Icon className="notice-icon" size={18} aria-hidden="true" />
      <div className="notice-body">
        {present(title) && <div className="notice-title">{title}</div>}
        {present(children) && <div className="notice-text">{children}</div>}
        {shownItems.length > 0 && (
          <ul className="notice-items">
            {shownItems.map((item, i) => (
              <li key={typeof item === 'string' ? item : `item-${String(i)}`}>{item}</li>
            ))}
          </ul>
        )}
        {present(reference) && <SupportReference reference={reference ?? ''} />}
      </div>
      {present(actions) && <div className="notice-actions">{actions}</div>}
    </div>
  );
}
