import { Search, SlidersHorizontal } from 'lucide-react';
import { useId, useState } from 'react';
import type { ReactNode } from 'react';
import { Button } from '@/components/ui/Button';

interface WorklistToolbarProps {
  /** Placeholder and accessible name of the search box (BDO: "Search Proposal No."). */
  placeholder?: string;
  /** Initial search text. */
  initial?: string;
  onSearch: (text: string) => void;
  /** Shows the Filters toggle when given. */
  filters?: { open: boolean; onToggle: () => void };
  /** Controls between the search and the actions (e.g. an "Only mine" checkbox). */
  extra?: ReactNode;
  /** Bulk actions on the right, enabled by the row selection. */
  children?: ReactNode;
}

/**
 * Work list toolbar of the BDO Insure design: search box with its Search button, the Filters
 * toggle, then the bulk actions right-aligned, on one line.
 */
export function WorklistToolbar({
  placeholder = 'Search Proposal No.',
  initial = '',
  onSearch,
  filters,
  extra,
  children,
}: Readonly<WorklistToolbarProps>) {
  const id = useId();
  const [text, setText] = useState(initial);
  return (
    <div className="worklist-toolbar">
      <form
        className="worklist-search"
        role="search"
        onSubmit={(e) => {
          e.preventDefault();
          onSearch(text.trim());
        }}
      >
        <label className="visually-hidden" htmlFor={id}>
          {placeholder}
        </label>
        <input
          id={id}
          className="input"
          placeholder={placeholder}
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <Button type="submit" icon={<Search size={16} />}>
          Search
        </Button>
      </form>
      {filters !== undefined && (
        <Button
          variant="ghost"
          icon={<SlidersHorizontal size={16} />}
          aria-expanded={filters.open}
          onClick={filters.onToggle}
        >
          Filters
        </Button>
      )}
      {extra}
      {children !== undefined && <div className="worklist-actions">{children}</div>}
    </div>
  );
}
