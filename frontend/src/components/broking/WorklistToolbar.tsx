import { Search, SlidersHorizontal } from 'lucide-react';
import { useEffect, useId, useRef, useState } from 'react';
import type { ReactNode } from 'react';
import { useInRouterContext, useSearchParams } from 'react-router-dom';
import { Button } from '@/components/ui/Button';
import { FilterChips } from '@/components/ui/FilterChips';

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

interface ToolbarViewProps extends WorklistToolbarProps {
  /** The search kept in the page URL (so Back returns to the same list), when routed. */
  urlSearch?: string;
  onUrlSearch?: (text: string) => void;
}

/** URL parameter of the work list search. */
const SEARCH_PARAM = 'q';

function ToolbarView({
  placeholder = 'Search Proposal No.',
  initial = '',
  onSearch,
  filters,
  extra,
  children,
  urlSearch,
  onUrlSearch,
}: Readonly<ToolbarViewProps>) {
  const id = useId();
  const [text, setText] = useState(urlSearch ?? initial);
  const [active, setActive] = useState(urlSearch ?? '');
  const restored = useRef(false);
  const search = (value: string) => {
    setActive(value);
    onSearch(value);
    onUrlSearch?.(value);
  };
  useEffect(() => {
    // A search kept in the URL (Back from a record) is applied once when the list opens.
    if (!restored.current && urlSearch) {
      restored.current = true;
      onSearch(urlSearch);
    }
  }, [urlSearch, onSearch]);
  return (
    <div className="worklist-toolbar">
      <form
        className="worklist-search"
        role="search"
        onSubmit={(e) => {
          e.preventDefault();
          search(text.trim());
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
      <FilterChips
        filters={
          active === ''
            ? []
            : [
                {
                  key: 'search',
                  label: `Search: ${active}`,
                  onRemove: () => {
                    setText('');
                    search('');
                  },
                },
              ]
        }
      />
    </div>
  );
}

/** The toolbar of a routed list: the search is kept in the URL (?q=). */
function RoutedToolbar(props: Readonly<WorklistToolbarProps>) {
  const [params, setParams] = useSearchParams();
  return (
    <ToolbarView
      {...props}
      urlSearch={params.get(SEARCH_PARAM) ?? undefined}
      onUrlSearch={(text) =>
        setParams(
          (current) => {
            const next = new URLSearchParams(current);
            if (text === '') {
              next.delete(SEARCH_PARAM);
            } else {
              next.set(SEARCH_PARAM, text);
            }
            return next;
          },
          { replace: true },
        )
      }
    />
  );
}

/**
 * Work list toolbar of the BDO Insure design: search box with its Search button, the Filters
 * toggle, then the bulk actions right-aligned, on one line. The active search shows as a
 * removable chip and is kept in the page URL, so Back from a record returns to the same list.
 */
export function WorklistToolbar(props: Readonly<WorklistToolbarProps>) {
  return useInRouterContext() ? <RoutedToolbar {...props} /> : <ToolbarView {...props} />;
}
