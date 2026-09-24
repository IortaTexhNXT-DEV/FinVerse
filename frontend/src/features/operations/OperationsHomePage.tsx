import { useQuery } from '@tanstack/react-query';
import { ArrowRight, ExternalLink, Search } from 'lucide-react';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { opsApi } from '@/api/operations';
import type { OperationsHome } from '@/api/operations';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';
import { useCompanyId } from '@/context/workspaceContext';
import { WorkCountTiles } from './OpsParts';
import { SECTION_LABELS, SECTION_ROUTES, safeUrl } from './opsLabels';

function InvoiceFinder() {
  const navigate = useNavigate();
  const [text, setText] = useState('');
  return (
    <Card title="Find an Invoice">
      <form
        className="ops-toolbar"
        onSubmit={(e) => {
          e.preventDefault();
          void navigate(`/operations/invoices?q=${encodeURIComponent(text.trim())}`);
        }}
      >
        <label className="visually-hidden" htmlFor="ops-home-search">
          Search Invoice No.
        </label>
        <input
          id="ops-home-search"
          className="input"
          placeholder="Search Invoice No., ARN, policy or client"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <Button type="submit" icon={<Search size={14} />}>
          Search
        </Button>
      </form>
    </Card>
  );
}

function ExternalLinks({ links }: Readonly<{ links: OperationsHome['links'] }>) {
  return (
    <Card title="Integrated Applications">
      {links.length === 0 ? (
        <p className="ops-muted">
          No application links yet. The Business Administrator maintains them in the list of values
          OPS_EXTERNAL_LINK.
        </p>
      ) : (
        <div className="ops-links">
          {links.map((l) => {
            const url = safeUrl(l.url);
            return url === undefined ? (
              <span key={l.code} className="ops-muted">
                {l.name} (address to be configured)
              </span>
            ) : (
              <a
                key={l.code}
                className="btn btn-secondary btn-sm"
                href={url}
                target="_blank"
                rel="noreferrer"
              >
                <ExternalLink size={14} aria-hidden="true" /> {l.name}
              </a>
            );
          })}
        </div>
      )}
    </Card>
  );
}

/**
 * Operations home (BRQID.003): one card per team section the user works in, with the live work
 * counts of the invoice ledger and the Operations modules, an invoice finder and the links to the
 * integrated applications.
 */
export default function OperationsHomePage() {
  const companyId = useCompanyId();
  const home = useQuery({
    queryKey: ['ops', 'home', companyId],
    queryFn: () => opsApi.home(companyId),
    enabled: companyId > 0,
  });
  const sections = home.data?.sections ?? [];
  return (
    <div className="stack">
      <PageHeader
        section="Operations"
        title="Operations Home"
        description="Work waiting for your Operations teams, counted live from the invoice ledger. Open a tile to see the list behind it."
      />
      <ErrorAlert error={home.error} />
      <InvoiceFinder />
      {home.isLoading && <span className="spinner" aria-label="Loading" />}
      {!home.isLoading && sections.length === 0 && (
        <Card>
          <EmptyState message="No Operations section is assigned to your role" />
        </Card>
      )}
      <div className="ops-sections">
        {sections.map((s) => (
          <Card
            key={s.section}
            title={SECTION_LABELS[s.section]}
            actions={
              <Link className="btn btn-ghost btn-sm" to={SECTION_ROUTES[s.section]}>
                Open <ArrowRight size={14} aria-hidden="true" />
              </Link>
            }
          >
            <WorkCountTiles counts={s.counts} label={`${SECTION_LABELS[s.section]} work queues`} />
          </Card>
        ))}
      </div>
      <ExternalLinks links={home.data?.links ?? []} />
    </div>
  );
}
