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
        description="Run any report on screen, print it or download it as PDF, Excel, ODS, CSV or XML."
        actions={
          <input
            className="input report-search"
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
          <div className="report-grid">
            {entries.map((e) => (
              <Link key={e.code} to={`/reports/${e.code}`} className="report-card">
                <FileBarChart2 size={20} aria-hidden="true" />
                <span>
                  <strong>{e.title}</strong>
                  <span className="muted">
                    {e.code} · {e.description}
                  </span>
                </span>
              </Link>
            ))}
          </div>
        </Card>
      ))}
    </div>
  );
}
