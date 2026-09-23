import { useQuery } from '@tanstack/react-query';
import { FileBarChart2 } from 'lucide-react';
import { useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { reportApi } from '@/api/reports';
import type { CatalogueEntry } from '@/api/reports';
import { Card } from '@/components/ui/Card';
import { ErrorAlert } from '@/components/ui/ErrorAlert';
import { PageHeader } from '@/components/ui/PageHeader';

function groupByCategory(entries: CatalogueEntry[]): [string, CatalogueEntry[]][] {
  const groups = new Map<string, CatalogueEntry[]>();
  entries.forEach((e) => {
    groups.set(e.categoryLabel, [...(groups.get(e.categoryLabel) ?? []), e]);
  });
  return [...groups.entries()];
}

/** Report centre: every report the user may run, grouped by menu category. */
export default function ReportsPage() {
  const catalogue = useQuery({ queryKey: ['report-catalogue'], queryFn: reportApi.catalogue });
  const [search, setSearch] = useState('');
  const groups = useMemo(() => {
    const term = search.trim().toLowerCase();
    const entries = (catalogue.data ?? []).filter(
      (e) =>
        term === '' || e.title.toLowerCase().includes(term) || e.code.toLowerCase().includes(term),
    );
    return groupByCategory(entries);
  }, [catalogue.data, search]);

  return (
    <div className="stack">
      <PageHeader
        section="Reports"
        title="Report Centre"
        description="Run any report on screen or export it to PDF, Excel or CSV."
        actions={
          <input
            className="input"
            style={{ width: 280 }}
            aria-label="Search reports"
            placeholder="Search by title or code"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        }
      />
      <ErrorAlert error={catalogue.error} />
      {catalogue.isLoading && <span className="spinner" aria-label="Loading" />}
      {groups.map(([category, entries]) => (
        <Card key={category} title={category}>
          <div className="grid-2">
            {entries.map((e) => (
              <Link
                key={e.code}
                to={`/reports/${e.code}`}
                className="card"
                style={{ padding: 16, textDecoration: 'none', color: 'inherit' }}
              >
                <div className="row">
                  <FileBarChart2 size={18} color="#0033A0" aria-hidden="true" />
                  <strong style={{ color: '#00205B' }}>{e.title}</strong>
                </div>
                <div className="muted" style={{ fontSize: 12, marginTop: 4 }}>
                  {e.code} · {e.description}
                </div>
              </Link>
            ))}
          </div>
        </Card>
      ))}
    </div>
  );
}
