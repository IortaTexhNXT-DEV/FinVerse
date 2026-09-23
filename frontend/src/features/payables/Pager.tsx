import type { PageResponse } from '@/api/types';
import { Button } from '@/components/ui/Button';

interface PagerProps {
  data: PageResponse<unknown> | undefined;
  noun: string;
  onPage: (page: number) => void;
}

/** Previous / next pagination bar for paged lists (hidden when there is a single page). */
export function Pager({ data, noun, onPage }: Readonly<PagerProps>) {
  if (data === undefined || data.totalPages <= 1) {
    return null;
  }
  return (
    <div className="pagination">
      <span className="muted">
        Page {data.page + 1} of {data.totalPages} · {data.totalElements} {noun}
      </span>
      <div className="spacer" />
      <Button
        size="sm"
        variant="secondary"
        disabled={data.page === 0}
        onClick={() => onPage(data.page - 1)}
      >
        Previous
      </Button>
      <Button
        size="sm"
        variant="secondary"
        disabled={data.page + 1 >= data.totalPages}
        onClick={() => onPage(data.page + 1)}
      >
        Next
      </Button>
    </div>
  );
}
