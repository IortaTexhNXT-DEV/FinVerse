import type { ReactNode } from 'react';

interface PageHeaderProps {
  title: string;
  section?: string;
  description?: string;
  actions?: ReactNode;
}

/** Standard page heading: module breadcrumb, title, description and page actions. */
export function PageHeader({ title, section, description, actions }: Readonly<PageHeaderProps>) {
  return (
    <div className="page-header">
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
