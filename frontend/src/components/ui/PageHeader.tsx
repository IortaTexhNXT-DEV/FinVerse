import { ChevronLeft } from 'lucide-react';
import { useEffect } from 'react';
import type { ReactNode } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { BRAND } from '@/branding';

interface PageHeaderProps {
  title: string;
  section?: string;
  description?: string;
  actions?: ReactNode;
  /** Route of the parent list; shows a back arrow before the title. */
  backTo?: string;
}

/** The browser tab title of a screen: "Unapplied Payments · BIBS". */
function documentTitle(title: string): string {
  return title ? `${title} · ${BRAND.product}` : BRAND.product;
}

/**
 * Back arrow: returns to the previous page of the session (the list with its filters and search),
 * or opens the parent list when the page was opened directly.
 */
function BackLink({ to }: Readonly<{ to: string }>) {
  const navigate = useNavigate();
  const location = useLocation();
  const hasHistory = location.key !== 'default';
  return (
    <Link
      to={to}
      className="page-back"
      aria-label="Back"
      onClick={(e) => {
        if (hasHistory) {
          e.preventDefault();
          void navigate(-1);
        }
      }}
    >
      <ChevronLeft size={28} aria-hidden="true" />
    </Link>
  );
}

/**
 * Standard page heading: back arrow, module breadcrumb, title, one short business line and the
 * page actions (secondary, primary, then the destructive action separated). Sets the browser tab
 * title.
 */
export function PageHeader({
  title,
  section,
  description,
  actions,
  backTo,
}: Readonly<PageHeaderProps>) {
  useEffect(() => {
    document.title = documentTitle(title);
  }, [title]);
  return (
    <div className="page-header">
      {backTo !== undefined && <BackLink to={backTo} />}
      <div>
        {section !== undefined && <div className="breadcrumb">{section}</div>}
        <h1>{title}</h1>
        {description !== undefined && <p title={description}>{description}</p>}
      </div>
      <div className="spacer" />
      {actions !== undefined && <div className="row">{actions}</div>}
    </div>
  );
}
