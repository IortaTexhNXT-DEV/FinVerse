import { useQuery } from '@tanstack/react-query';
import { catalogApi } from '@/api/catalog';
import { Button } from '@/components/ui/Button';
import { Field } from '@/components/ui/Field';

/** Filters of the Booking Workbench: the product line (the search box covers ARN and client). */
export function WorkbenchFilters({
  line,
  onLine,
}: Readonly<{ line: string; onLine: (line: string) => void }>) {
  const lines = useQuery({ queryKey: ['catalog', 'lines'], queryFn: catalogApi.lines });
  return (
    <div className="row">
      <Field label="Product line">
        {(id) => (
          <select id={id} className="select" value={line} onChange={(e) => onLine(e.target.value)}>
            <option value="">All product lines</option>
            {(lines.data ?? []).map((l) => (
              <option key={l.code} value={l.code}>
                {l.name}
              </option>
            ))}
          </select>
        )}
      </Field>
      {line !== '' && (
        <Button variant="ghost" size="sm" onClick={() => onLine('')}>
          Clear Filters
        </Button>
      )}
    </div>
  );
}
