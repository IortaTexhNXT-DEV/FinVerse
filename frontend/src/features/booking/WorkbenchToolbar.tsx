import { CalendarCheck, ListPlus, Search, SlidersHorizontal, XCircle } from 'lucide-react';
import { useState } from 'react';
import type { WorkbenchTab } from '@/api/booking';
import { Button } from '@/components/ui/Button';
import { actionsOf } from './bookingForm';

export type WorkbenchDialog = 'bookNow' | 'confirmBatch' | 'cancelBatch';

interface Props {
  tab: WorkbenchTab;
  selected: number;
  canProcess: boolean;
  filtersOpen: boolean;
  enqueueing: boolean;
  onSearch: (text: string) => void;
  onToggleFilters: () => void;
  onEnqueue: () => void;
  onDialog: (dialog: WorkbenchDialog) => void;
}

function BulkButtons({
  tab,
  selected,
  enqueueing,
  onEnqueue,
  onDialog,
}: Readonly<Pick<Props, 'tab' | 'selected' | 'enqueueing' | 'onEnqueue' | 'onDialog'>>) {
  const actions = actionsOf(tab);
  const none = selected === 0;
  return (
    <>
      {actions.addToBatch && (
        <Button
          variant="secondary"
          icon={<ListPlus size={16} />}
          disabled={none}
          busy={enqueueing}
          onClick={onEnqueue}
        >
          Add to Batch
        </Button>
      )}
      {actions.confirmBatch && (
        <>
          <Button
            variant="secondary"
            icon={<XCircle size={16} />}
            onClick={() => onDialog('cancelBatch')}
          >
            Cancel Batch
          </Button>
          <Button variant="secondary" disabled={none} onClick={() => onDialog('confirmBatch')}>
            Confirm Batch
          </Button>
        </>
      )}
      {actions.bookNow && (
        <Button
          icon={<CalendarCheck size={16} />}
          disabled={none}
          onClick={() => onDialog('bookNow')}
        >
          Book Now
        </Button>
      )}
    </>
  );
}

/**
 * Work-list toolbar of the Booking Workbench: search by proposal number, filters, and the bulk
 * actions of the tab on the right, enabled by the row selection.
 */
export function WorkbenchToolbar({
  tab,
  selected,
  canProcess,
  filtersOpen,
  enqueueing,
  onSearch,
  onToggleFilters,
  onEnqueue,
  onDialog,
}: Readonly<Props>) {
  const [text, setText] = useState('');
  return (
    <div className="booking-toolbar">
      <form
        className="row"
        onSubmit={(e) => {
          e.preventDefault();
          onSearch(text.trim());
        }}
      >
        <label className="visually-hidden" htmlFor="booking-search">
          Search Proposal No.
        </label>
        <input
          id="booking-search"
          className="input"
          placeholder="Search Proposal No."
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <Button type="submit" variant="secondary" icon={<Search size={14} />}>
          Search
        </Button>
      </form>
      <Button
        variant="ghost"
        icon={<SlidersHorizontal size={14} />}
        aria-expanded={filtersOpen}
        onClick={onToggleFilters}
      >
        Filters
      </Button>
      <div className="spacer" />
      {canProcess && (
        <BulkButtons
          tab={tab}
          selected={selected}
          enqueueing={enqueueing}
          onEnqueue={onEnqueue}
          onDialog={onDialog}
        />
      )}
    </div>
  );
}
