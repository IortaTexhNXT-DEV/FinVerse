import { Button } from './Button';

interface PagerProps {
  page: number;
  totalPages: number;
  total: number;
  /** Plural noun of the rows, e.g. "accounts". */
  noun: string;
  onPage: (page: number) => void;
}

/** Previous / next pager under a paged table (hidden when everything fits on one page). */
export function Pager({ page, totalPages, total, noun, onPage }: Readonly<PagerProps>) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <div className="pagination">
      <span className="muted">
        Page {page + 1} of {totalPages} · {total} {noun}
      </span>
      <div className="spacer" />
      <Button size="sm" variant="secondary" disabled={page === 0} onClick={() => onPage(page - 1)}>
        Previous
      </Button>
      <Button
        size="sm"
        variant="secondary"
        disabled={page + 1 >= totalPages}
        onClick={() => onPage(page + 1)}
      >
        Next
      </Button>
    </div>
  );
}

/** Pager bound to a paged API response (renders nothing until the page has loaded). */
export function PageFooter({
  data,
  noun,
  onPage,
}: Readonly<{
  data: { page: number; totalPages: number; totalElements: number } | undefined;
  noun: string;
  onPage: (page: number) => void;
}>) {
  if (data === undefined) {
    return null;
  }
  return (
    <Pager
      page={data.page}
      totalPages={data.totalPages}
      total={data.totalElements}
      noun={noun}
      onPage={onPage}
    />
  );
}
