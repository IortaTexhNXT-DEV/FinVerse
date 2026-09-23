import { useQuery } from '@tanstack/react-query';
import { systemApi } from '@/api/system';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { Kpi } from '@/components/ui/Kpi';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatusBadge } from '@/components/ui/StatusBadge';
import { formatDateTime } from '@/utils/format';

/** Uptime as "2d 3h 4m". */
function uptime(seconds: number): string {
  const days = Math.floor(seconds / 86_400);
  const hours = Math.floor((seconds % 86_400) / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return `${days}d ${hours}h ${minutes}m`;
}

/** Application information: version and build, database migration level and health. */
export default function ApplicationInfoPage() {
  const info = useQuery({ queryKey: ['system', 'info'], queryFn: systemApi.info });
  const i = info.data;
  return (
    <div className="stack">
      <PageHeader
        section="Administration"
        title="Application Info"
        description="Version, build, database migration level and health of this FinVerse installation."
      />
      <ErrorAlert error={info.error} />
      {i === undefined ? (
        info.isLoading && <span className="spinner" aria-label="Loading" />
      ) : (
        <>
          <div className="grid-4">
            <Kpi
              label="Version"
              value={i.about.version}
              hint={`Built ${formatDateTime(i.about.buildTime) || 'locally'}`}
              accent
            />
            <Kpi
              label="Health"
              value={<StatusBadge status={i.health === 'UP' ? 'ACTIVE' : 'REJECTED'} />}
              hint={i.health}
            />
            <Kpi
              label="Database migration"
              value={`V${i.migrationVersion}`}
              hint={`${i.migrationsApplied} migrations applied`}
            />
            <Kpi
              label="Uptime"
              value={uptime(i.uptimeSeconds)}
              hint={`Since ${formatDateTime(i.startedAt)}`}
            />
          </div>
          <Card title="Details">
            <dl className="form-grid" style={{ margin: 0 }}>
              {[
                ['Product', `${i.about.product} by ${i.about.vendor}`],
                ['Latest migration', i.migrationDescription],
                ['Java runtime', i.javaVersion],
                ['Database', i.databaseVersion],
                ['Active profiles', i.activeProfiles.join(', ') || 'default'],
              ].map(([label, value]) => (
                <div key={label}>
                  <dt className="muted">{label}</dt>
                  <dd style={{ margin: 0, fontWeight: 600 }}>{value}</dd>
                </div>
              ))}
            </dl>
          </Card>
        </>
      )}
    </div>
  );
}
