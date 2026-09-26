import { ChevronLeft, ChevronRight } from 'lucide-react';
import { pageWindow, showingText } from './pagerMath';

interface PagerProps {
  page: number;
  totalPages: number;
  total: number;
  /** Rows per page; omit when unknown (the range is then derived from the page count). */
  size?: number;
  /** Plural noun of the rows; kept for callers, the BDO wording is always "results". */
  noun?: string;
  onPage: (page: number) => void;
}

/**
 * BDO Insure table pager: "Showing 1 to n of N results" on the left and numbered pages with
 * previous / next on the right. Hidden when there is nothing to page.
 */
export function Pager({ page, totalPages, total, size, onPage }: Readonly<PagerProps>) {
  if (total <= 0 || totalPages <= 0) {
    return null;
  }
  const pageSize = size ?? Math.ceil(total / totalPages);
  return (
    <nav className="pagination" aria-label="Pages">
      <span className="pagination-summary">{showingText(page, pageSize, total)}</span>
      <div className="spacer" />
      <button
        type="button"
        className="page-button"
        aria-label="Previous page"
        disabled={page === 0}
        onClick={() => onPage(page - 1)}
      >
        <ChevronLeft size={16} aria-hidden="true" />
      </button>
      {pageWindow(page, totalPages).map((p, i) =>
        p === null ? (
          <span key={`gap-${String(i)}`} className="page-gap" aria-hidden="true">
            …
          </span>
        ) : (
          <button
            key={p}
            type="button"
            className="page-button"
            aria-current={p === page ? 'page' : undefined}
            aria-label={`Page ${String(p + 1)}`}
            onClick={() => onPage(p)}
          >
            {p + 1}
          </button>
        ),
      )}
      <button
        type="button"
        className="page-button"
        aria-label="Next page"
        disabled={page + 1 >= totalPages}
        onClick={() => onPage(page + 1)}
      >
        <ChevronRight size={16} aria-hidden="true" />
      </button>
    </nav>
  );
}

/** Pager bound to a paged API response (renders nothing until the page has loaded). */
export function PageFooter({
  data,
  noun,
  onPage,
}: Readonly<{
  data: { page: number; size?: number; totalPages: number; totalElements: number } | undefined;
  noun?: string;
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
      size={data.size}
      noun={noun}
      onPage={onPage}
    />
  );
}
