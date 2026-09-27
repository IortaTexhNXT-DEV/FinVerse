import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DataTable } from '@/components/ui/DataTable';
import { ItemResultsDialog } from './ItemResultsDialog';
import { selectionColumn, useRowSelection } from './rowSelection';
import { WorkTiles } from './WorkTiles';

interface Row {
  arn: string;
}

function SelectableList({
  rows,
  onKeys,
}: Readonly<{ rows: Row[]; onKeys: (keys: string[]) => void }>) {
  const selection = useRowSelection();
  onKeys(selection.keys);
  return (
    <>
      <DataTable<Row>
        rows={rows}
        rowKey={(r) => r.arn}
        columns={[
          selectionColumn(
            rows,
            (r) => r.arn,
            selection,
            (r) => r.arn,
          ),
          { key: 'arn', header: 'ARN', render: (r) => r.arn },
        ]}
      />
      <button type="button" onClick={selection.clear}>
        Clear
      </button>
    </>
  );
}

describe('work list building blocks', () => {
  it('selects rows one by one or all at once', async () => {
    const user = userEvent.setup();
    let keys: string[] = [];
    render(
      <SelectableList
        rows={[{ arn: 'ARN-1' }, { arn: 'ARN-2' }]}
        onKeys={(k) => {
          keys = k;
        }}
      />,
    );
    await user.click(screen.getByLabelText('Select ARN-1'));
    expect(keys).toEqual(['ARN-1']);
    await user.click(screen.getByLabelText('Select all rows shown'));
    expect(keys).toEqual(['ARN-1', 'ARN-2']);
    await user.click(screen.getByLabelText('Select ARN-2'));
    expect(keys).toEqual(['ARN-1']);
    await user.click(screen.getByLabelText('Select all rows shown'));
    await user.click(screen.getByLabelText('Select all rows shown'));
    expect(keys).toEqual([]);
    await user.click(screen.getByLabelText('Select ARN-2'));
    await user.click(screen.getByText('Clear'));
    expect(keys).toEqual([]);
  }, 20_000);

  it('shows tiles that open their list and flags those needing attention', async () => {
    const user = userEvent.setup();
    const opened: string[] = [];
    render(
      <WorkTiles
        label="Status"
        tiles={[
          { key: 'a', label: 'Placed', value: 3, active: true, onClick: () => opened.push('a') },
          { key: 'b', label: 'Returned', value: 1, alert: true, onClick: () => opened.push('b') },
        ]}
      />,
    );
    expect(screen.getByRole('button', { name: /Placed/ })).toHaveAttribute('aria-pressed', 'true');
    expect(screen.getByText('Needs attention')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: /Returned/ }));
    expect(opened).toEqual(['b']);
  });

  it('lists the outcome of a bulk action', async () => {
    const user = userEvent.setup();
    let closed = false;
    render(
      <ItemResultsDialog
        title="Cancel Placement"
        results={[
          { reference: 'ARN-1', ok: true, message: 'Placement cancelled' },
          { reference: 'ARN-2', ok: false, message: 'Not allowed' },
        ]}
        onClose={() => {
          closed = true;
        }}
      />,
    );
    expect(screen.getByText('1 of 2 done.')).toBeInTheDocument();
    expect(screen.getByText('Not allowed')).toBeInTheDocument();
    await user.click(screen.getAllByRole('button', { name: 'Close' })[1]!);
    expect(closed).toBe(true);
  });
});
