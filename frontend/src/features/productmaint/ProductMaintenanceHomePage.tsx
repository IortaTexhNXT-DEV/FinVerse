import { useQuery } from '@tanstack/react-query';
import { ArrowRight, FileStack, Hourglass, Plus, Workflow } from 'lucide-react';
import { Link, useNavigate } from 'react-router-dom';
import { productMaintApi } from '@/api/productmaint';
import { useAuth } from '@/auth/authContext';
import { WorkTiles } from '@/components/broking/WorkTiles';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { homeTiles } from './packageRequest';

const EXPIRY_WINDOWS = [30, 60, 90];

/**
 * Product Maintenance home (BRPM.019): package requests by stage with their SLA state, packages
 * expiring within 30 / 60 / 90 days, versions waiting for validation, advisories pending and the
 * comparative outputs of the week; every tile opens its list.
 */
export default function ProductMaintenanceHomePage() {
  const companyId = useCompanyId();
  const { can } = useAuth();
  const navigate = useNavigate();
  const counts = useQuery({
    queryKey: ['package-requests', 'counts', companyId],
    queryFn: () => productMaintApi.counts(companyId),
    enabled: companyId > 0,
  });
  const tiles = homeTiles(counts.data);
  const expiring = counts.data?.expiring ?? {};
  return (
    <div className="stack">
      <PageHeader
        section="Product Maintenance"
        title="Product Maintenance Home"
        description="Where every package request stands, which packages end soon and what waits for validation or an advisory."
        actions={
          can('PKG_REQUEST') && (
            <Link className="btn btn-accent" to="/product-maintenance/requests/new">
              <Plus size={16} aria-hidden="true" /> New Package Request
            </Link>
          )
        }
      />
      <ErrorAlert error={counts.error} />
      <Card title="Package requests by stage">
        <WorkTiles
          label="Package requests by stage"
          tiles={tiles.map((t) => ({
            key: t.tab,
            label: t.overdue > 0 ? `${t.label} · ${t.overdue} overdue` : t.label,
            value: t.total,
            alert: t.overdue > 0 || t.dueSoon > 0,
            onClick: () => void navigate(`/product-maintenance/requests?tab=${t.tab}`),
          }))}
        />
      </Card>
      <Card title="Packages ending soon">
        <WorkTiles
          label="Packages ending soon"
          tiles={EXPIRY_WINDOWS.map((days) => ({
            key: String(days),
            label: `Within ${days} days`,
            value: expiring[String(days)] ?? 0,
            alert: days === EXPIRY_WINDOWS[0],
            onClick: () => void navigate(`/product-maintenance/expiry?within=${days}`),
          }))}
        />
      </Card>
      <div className="tile-grid">
        <Link className="action-tile" to="/product-maintenance/requests?tab=validation">
          <span className="action-tile-title">Versions for validation</span>
          <span className="action-tile-meta">
            {tiles.find((t) => t.tab === 'validation')?.total ?? 0} set up by MBS, waiting for the
            checkpoint
          </span>
          <ArrowRight className="action-tile-go" size={16} aria-hidden="true" />
        </Link>
        <Link className="action-tile" to="/product-maintenance/requests?tab=released">
          <span className="action-tile-title">Advisories pending</span>
          <span className="action-tile-meta">
            {counts.data?.advisoriesPending ?? 0} drafted, not sent yet
          </span>
          <FileStack className="action-tile-go" size={16} aria-hidden="true" />
        </Link>
        <Link className="action-tile" to="/product-maintenance/requests?tab=negotiation">
          <span className="action-tile-title">Comparative outputs this week</span>
          <span className="action-tile-meta">
            {counts.data?.outputsThisWeek ?? 0} master and client outputs generated
          </span>
          <Workflow className="action-tile-go" size={16} aria-hidden="true" />
        </Link>
        <Link className="action-tile" to="/product-maintenance/expiry">
          <span className="action-tile-title">Package Expiry</span>
          <span className="action-tile-meta">Monitor end dates and generate renewal requests</span>
          <Hourglass className="action-tile-go" size={16} aria-hidden="true" />
        </Link>
      </div>
    </div>
  );
}
