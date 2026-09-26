import { FilePlus2, ListChecks } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { PageHeader } from '@/components/ui/PageHeader';
import { EB_SECTION } from '../EbPlaceholder';
import { EB_HOME_TILES } from './ebHomeTiles';

/**
 * EB Home (design 10.1; FR-EB-062): the work tiles of the EB cycle and servicing, each opening its
 * list. Landing screen of the foundation (E0); the counts come with waves E1-B and E1-C.
 */
export default function EbHomePage() {
  const navigate = useNavigate();
  const { can } = useAuth();
  return (
    <div className="stack">
      <PageHeader
        section={EB_SECTION}
        title="EB Home"
        description="Employee Benefits at a glance: renewal advices due, franchise and proposals outstanding, comparatives to sign off or approve, programmes with the client, member changes and overdue pending items."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<ListChecks size={16} />}
              onClick={() => void navigate('/eb/programmes')}
            >
              Open Programmes
            </Button>
            {can('EB_MARKET') && (
              <Button
                variant="primary"
                icon={<FilePlus2 size={16} />}
                onClick={() => void navigate('/eb/programmes/new')}
              >
                New Programme
              </Button>
            )}
          </>
        }
      />
      <div className="grid-4">
        {EB_HOME_TILES.map((tile) => (
          <Link key={tile.id} to={tile.to} className="card kpi" aria-label={tile.label}>
            <div className="kpi-label">{tile.label}</div>
            <div className="kpi-value">–</div>
          </Link>
        ))}
      </div>
      <Card title="My Work">
        <p className="muted">
          EB cycles, franchise requests, member changes and SOAs assigned to you appear in My Work
          and open their programme.
        </p>
      </Card>
    </div>
  );
}
