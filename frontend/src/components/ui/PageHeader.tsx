import { ChevronLeft } from 'lucide-react';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

interface PageHeaderProps {
  title: string;
  section?: string;
  description?: string;
  actions?: ReactNode;
  /** Route of the parent list; shows a back arrow before the title. */
  backTo?: string;
}

/** Standard page heading: back arrow, module breadcrumb, title, description and page actions. */
export function PageHeader({
  title,
  section,
  description,
  actions,
  backTo,
}: Readonly<PageHeaderProps>) {
  return (
    <div className="page-header">
      {backTo !== undefined && (
        <Link to={backTo} className="page-back" aria-label="Back">
          <ChevronLeft size={28} aria-hidden="true" />
        </Link>
      )}
      <div>
        {section !== undefined && <div className="breadcrumb">{section}</div>}
        <h1>{title}</h1>
        {description !== undefined && <p>{description}</p>}
      </div>
      <div className="spacer" />
      {actions !== undefined && <div className="row">{actions}</div>}
    </div>
  );
}
