import { fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { useState } from 'react';
import { RowActionMenu } from './RowActionMenu';
import { TreeTable } from './TreeTable';
import { branchKeys, treeKey, visibleRows } from './treeRows';
import type { TreeNode } from './treeRows';

interface Item {
  name: string;
  note?: string;
}

const NODES: TreeNode<Item>[] = [
  {
    key: 'a',
    row: { name: 'Alpha' },
    children: [
      { key: 'a1', row: { name: 'Alpha One', note: 'first' } },
      { key: 'a2', row: { name: 'Alpha Two' }, children: [{ key: 'a2x', row: { name: 'Deep' } }] },
    ],
  },
  { key: 'b', row: { name: 'Bravo' } },
];

const COLUMNS = [
  { key: 'name', header: 'Name', render: (r: Item) => r.name },
  { key: 'note', header: 'Note', render: (r: Item) => r.note },
];

function Harness({ onActivate }: Readonly<{ onActivate?: (row: Item) => void }>) {
  const [expanded, setExpanded] = useState<Set<string>>(new Set(['a']));
  return (
    <TreeTable
      caption="Items"
      columns={COLUMNS}
      nodes={NODES}
      expanded={expanded}
      onActivate={onActivate}
      onToggle={(key) =>
        setExpanded((current) => {
          const next = new Set(current);
          if (next.has(key)) {
            next.delete(key);
          } else {
            next.add(key);
          }
          return next;
        })
      }
    />
  );
}

describe('TreeTable', () => {
  it('lists the rows of the expanded branches as a treegrid with levels', () => {
    render(<Harness />);
    const grid = screen.getByRole('treegrid', { name: 'Items' });
    const rows = within(grid).getAllByRole('row').slice(1);
    expect(rows.map((r) => r.getAttribute('aria-level'))).toEqual(['1', '2', '2', '1']);
    expect(rows[0]).toHaveAttribute('aria-expanded', 'true');
    expect(rows[2]).toHaveAttribute('aria-expanded', 'false');
    expect(rows[3]).not.toHaveAttribute('aria-expanded');
    expect(rows[0]).toHaveAttribute('tabindex', '0');
    expect(rows[1]).toHaveAttribute('tabindex', '-1');
    // An empty cell is one muted dash.
    expect(within(rows[0]!).getByText('—')).toBeInTheDocument();
  });

  it('expands and collapses with the chevron and the arrow keys', async () => {
    const user = userEvent.setup();
    const activate = vi.fn();
    render(<Harness onActivate={activate} />);
    await user.click(screen.getAllByRole('button', { name: 'Expand' })[0]!);
    expect(screen.getByText('Deep')).toBeInTheDocument();
    const alpha = screen.getByText('Alpha').closest('tr') as HTMLElement;
    fireEvent.keyDown(alpha, { key: 'ArrowLeft' });
    expect(screen.queryByText('Alpha One')).not.toBeInTheDocument();
    fireEvent.keyDown(alpha, { key: 'ArrowRight' });
    expect(screen.getByText('Alpha One')).toBeInTheDocument();
    fireEvent.keyDown(alpha, { key: 'ArrowDown' });
    expect(screen.getByText('Alpha One').closest('tr')).toHaveFocus();
    fireEvent.keyDown(screen.getByText('Alpha One').closest('tr') as HTMLElement, {
      key: 'ArrowLeft',
    });
    expect(alpha).toHaveFocus();
    fireEvent.keyDown(alpha, { key: 'Enter' });
    expect(activate).toHaveBeenCalledWith({ name: 'Alpha' });
  });

  it('shows skeleton rows while loading and the empty state without rows', () => {
    const { rerender } = render(
      <TreeTable
        caption="Items"
        columns={COLUMNS}
        nodes={[]}
        expanded={new Set()}
        onToggle={vi.fn()}
        loading
      />,
    );
    expect(screen.getByRole('treegrid')).toHaveAttribute('aria-busy', 'true');
    rerender(
      <TreeTable
        caption="Items"
        columns={COLUMNS}
        nodes={[]}
        expanded={new Set()}
        onToggle={vi.fn()}
        emptyMessage="Nothing here"
      />,
    );
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });

  it('works out the visible rows, the branches and the key moves', () => {
    const rows = visibleRows(NODES, new Set(['a', 'a2']));
    expect(rows.map((r) => r.node.key)).toEqual(['a', 'a1', 'a2', 'a2x', 'b']);
    expect(rows[3]?.parentKey).toBe('a2');
    expect(branchKeys(NODES)).toEqual(['a', 'a2']);
    const expanded = new Set(['a', 'a2']);
    expect(treeKey('Home', rows, 3, expanded)).toEqual({ focus: 'a' });
    expect(treeKey('End', rows, 0, expanded)).toEqual({ focus: 'b' });
    expect(treeKey('ArrowUp', rows, 1, expanded)).toEqual({ focus: 'a' });
    expect(treeKey('ArrowRight', rows, 0, expanded)).toEqual({ focus: 'a1' });
    expect(treeKey('ArrowRight', rows, 1, expanded)).toBeNull();
    expect(treeKey('ArrowLeft', rows, 4, expanded)).toBeNull();
    expect(treeKey('x', rows, 0, expanded)).toBeNull();
    expect(treeKey('Enter', rows, 9, expanded)).toBeNull();
  });
});

describe('RowActionMenu', () => {
  it('lists the actions with the destructive one last and closes on Escape', async () => {
    const user = userEvent.setup();
    const deactivate = vi.fn();
    const view = vi.fn();
    render(
      <RowActionMenu
        label="T-CBG1"
        actions={[
          { label: 'Deactivate', danger: true, onSelect: deactivate },
          { label: 'View / Edit', onSelect: view },
          { label: 'Authorize', onSelect: vi.fn(), disabled: true },
        ]}
      />,
    );
    const button = screen.getByRole('button', { name: 'Actions for T-CBG1' });
    expect(button).toHaveAttribute('aria-expanded', 'false');
    await user.click(button);
    const menu = screen.getByRole('menu', { name: 'Actions for T-CBG1' });
    const items = within(menu).getAllByRole('menuitem');
    expect(items.map((i) => i.textContent)).toEqual(['View / Edit', 'Authorize', 'Deactivate']);
    expect(items[0]).toHaveFocus();
    expect(items[2]).toHaveClass('danger', 'separated');
    fireEvent.keyDown(menu, { key: 'ArrowDown' });
    expect(items[2]).toHaveFocus();
    fireEvent.keyDown(menu, { key: 'ArrowUp' });
    expect(items[0]).toHaveFocus();
    fireEvent.keyDown(menu, { key: 'End' });
    fireEvent.keyDown(menu, { key: 'Home' });
    fireEvent.keyDown(menu, { key: 'Escape' });
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    expect(button).toHaveFocus();
    fireEvent.keyDown(button, { key: 'ArrowDown' });
    await user.click(screen.getByRole('menuitem', { name: 'Deactivate' }));
    expect(deactivate).toHaveBeenCalled();
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
    await user.click(button);
    fireEvent(window, new Event('resize'));
    expect(screen.getByRole('menu')).toBeInTheDocument();
    fireEvent.mouseDown(document.body);
    expect(screen.queryByRole('menu')).not.toBeInTheDocument();
  });

  it('renders nothing without actions', () => {
    const { container } = render(<RowActionMenu label="x" actions={[]} />);
    expect(container).toBeEmptyDOMElement();
  });
});
