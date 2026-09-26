import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { SectionLanding } from '@/components/broking/SectionLanding';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { useCompanyId } from '@/context/workspaceContext';
import { formatDateTime, humanize } from '@/utils/format';
import { casesApi } from './cases/api';
import { homeTiles } from './cases/caseLogic';

/**
 * Screening home (SNSRP-402, 405): open cases per stage, SLA due today and breached, potential
 * matches not yet cased and the last watchlist run; each tile opens its list.
 */
export default function ScreeningHomePage() {
  const companyId = useCompanyId();
  const navigate = useNavigate();
  const tiles = useQuery({
    queryKey: ['screening', 'tiles', companyId],
    queryFn: () => casesApi.tiles(companyId),
    enabled: companyId > 0,
  });
  const data = tiles.data;
  return (
    <SectionLanding
      section="Client & Policy"
      title="Screening Home"
      description="Sanction and PEP screening of clients: open cases by stage, SLA due and breached, potential matches to review and the status of the last watchlist run."
      cardTitle="Screening Work"
      emptyMessage="No screening cases to show yet"
    >
      <ErrorAlert error={tiles.error} />
      {data === undefined ? (
        tiles.isLoading && <span className="spinner" aria-label="Loading" />
      ) : (
        <div className="stack">
          <WorkTiles label="Screening work" tiles={homeTiles(data, (to) => void navigate(to))} />
          <p className="muted">
            Last screening run:{' '}
            {data.lastRunNo
              ? `${data.lastRunNo} · ${humanize(data.lastRunStatus ?? '')} · ${formatDateTime(data.lastRunAt)}`
              : 'none yet'}
          </p>
        </div>
      )}
    </SectionLanding>
  );
}
