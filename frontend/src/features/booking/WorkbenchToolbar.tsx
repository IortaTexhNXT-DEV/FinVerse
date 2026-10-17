import { CalendarCheck, ListPlus, XCircle } from 'lucide-react';
import type { WorkbenchTab } from '@/api/booking';
import { WorklistToolbar } from '@/components/broking/WorklistToolbar';
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
  return (
    <WorklistToolbar onSearch={onSearch} filters={{ open: filtersOpen, onToggle: onToggleFilters }}>
      {canProcess && (
        <BulkButtons
          tab={tab}
          selected={selected}
          enqueueing={enqueueing}
          onEnqueue={onEnqueue}
          onDialog={onDialog}
        />
      )}
    </WorklistToolbar>
  );
}
