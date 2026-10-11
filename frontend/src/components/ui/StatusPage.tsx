import { FileQuestion, ShieldAlert } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { Button } from './Button';

const PAGES = {
  forbidden: {
    icon: ShieldAlert,
    title: 'No Access to This Screen',
    text: 'Your role does not include this function. Ask your team lead or the User Access administrator if you need it.',
  },
  notFound: {
    icon: FileQuestion,
    title: 'Page Not Found',
    text: 'The page or record you asked for does not exist or is no longer available.',
  },
} as const;

/**
 * Themed 403 and 404 pages (BDO): what happened in business language and a way back, never an
 * HTTP code or a blank screen.
 */
export function StatusPage({ kind }: Readonly<{ kind: keyof typeof PAGES }>) {
  const navigate = useNavigate();
  const page = PAGES[kind];
  const Icon = page.icon;
  return (
    <div className="status-page" role="alert">
      <Icon size={48} aria-hidden="true" />
      <h1>{page.title}</h1>
      <p>{page.text}</p>
      <div className="row">
        <Button variant="secondary" onClick={() => void navigate(-1)}>
          Go Back
        </Button>
        <Link className="btn btn-primary" to="/">
          Back to Homepage
        </Link>
      </div>
    </div>
  );
}
