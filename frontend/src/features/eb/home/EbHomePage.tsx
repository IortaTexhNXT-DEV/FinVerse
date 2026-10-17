import { useQuery } from '@tanstack/react-query';
import { ClipboardCheck, FilePlus2, ListChecks } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { ebApi } from '@/api/eb';
import { useAuth } from '@/auth/authContext';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { EB_SECTION } from '../EbPlaceholder';
import { EB_HOME_TILES, tileValue } from './ebHomeTiles';

/**
 * EB Home (design 10.1; FR-EB-062): the work tiles of the EB cycle and servicing with their
 * counts, each opening its list, and the shortcuts to the programmes and pending items.
 */
export default function EbHomePage() {
  const navigate = useNavigate();
  const { can } = useAuth();
  const companyId = useCompanyId();
  const counts = useQuery({
    queryKey: ['eb', 'home', companyId],
    queryFn: () => ebApi.home(companyId),
    enabled: companyId > 0,
  });
  return (
    <div className="stack">
      <PageHeader
        section={EB_SECTION}
        title="EB Home"
        description="Employee Benefits at a glance."
        actions={
          <>
            <Button
              variant="secondary"
              icon={<ClipboardCheck size={16} />}
              onClick={() => void navigate('/eb/pending-items')}
            >
              Pending Items
            </Button>
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
      <ErrorAlert error={counts.error} onRetry={() => void counts.refetch()} />
      <div className="grid-4">
        {EB_HOME_TILES.map((tile) => {
          const value = tileValue(counts.data, tile.id);
          return (
            <Link key={tile.id} to={tile.to} className="card kpi" aria-label={tile.label}>
              <div className="kpi-label">{tile.label}</div>
              <div className="kpi-value">{value}</div>
              {tile.alert === true && value !== '0' && value !== '–' && (
                <div className="kpi-hint">Needs attention</div>
              )}
            </Link>
          );
        })}
      </div>
      <Card title="My Work">
        <p className="muted">
          The cycles assigned to you are in My Work; the follow-ups of pending items and the renewal
          advices run every morning.
        </p>
      </Card>
    </div>
  );
}
