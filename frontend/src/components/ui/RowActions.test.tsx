import { fireEvent, render, screen, within } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { RowActions } from './RowActions';

describe('RowActions', () => {
  it('opens a menu of the row actions, destructive ones last, and runs the choice', () => {
    const paid = vi.fn();
    const ret = vi.fn();
    const row = vi.fn();
    render(
      <table>
        <thead>
          <tr>
            <th scope="col">Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr onClick={row}>
            <td>
              <RowActions
                record="DSQ-2026-000004"
                actions={[
                  { label: 'Return', onSelect: ret, danger: true },
                  { label: 'Mark Paid', onSelect: paid },
                  { label: 'Acknowledge', onSelect: vi.fn(), hidden: true },
                ]}
              />
            </td>
          </tr>
        </tbody>
      </table>,
    );
    expect(screen.queryByRole('menu')).toBeNull();
    fireEvent.click(screen.getByRole('button', { name: 'Actions for DSQ-2026-000004' }));
    const items = screen.getAllByRole('menuitem').map((i) => i.textContent);
    expect(items).toEqual(['Mark Paid', 'Return']);
    fireEvent.click(screen.getByRole('menuitem', { name: 'Mark Paid' }));
    expect(paid).toHaveBeenCalledOnce();
    expect(row).not.toHaveBeenCalled();
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('leaves the action cell empty when no action is open to the user', () => {
    const { container } = render(
      <RowActions record="X" actions={[{ label: 'A', onSelect: vi.fn(), hidden: true }]} />,
    );
    expect(container).toBeEmptyDOMElement();
    expect(screen.queryByText('—')).toBeNull();
    expect(screen.queryByRole('button')).toBeNull();
  });

  it('closes on Escape', () => {
    render(<RowActions record="X" actions={[{ label: 'A', onSelect: vi.fn() }]} />);
    fireEvent.click(screen.getByRole('button', { name: 'Actions for X' }));
    fireEvent.keyDown(screen.getByRole('menu'), { key: 'Escape' });
    expect(screen.queryByRole('menu')).toBeNull();
  });

  it('asks first when the action needs a confirmation and runs it from the dialog', async () => {
    const cancel = vi.fn().mockResolvedValue(undefined);
    render(
      <RowActions
        record="CPU-1"
        actions={[
          {
            label: 'Cancel Check Pick-up',
            danger: true,
            confirm: { title: 'Cancel Check Pick-up', effect: 'The pick-up is cancelled.' },
            onSelect: cancel,
          },
        ]}
      />,
    );
    fireEvent.click(screen.getByRole('button', { name: 'Actions for CPU-1' }));
    fireEvent.click(screen.getByRole('menuitem', { name: 'Cancel Check Pick-up' }));
    expect(cancel).not.toHaveBeenCalled();
    const dialog = await screen.findByRole('dialog');
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel Check Pick-up' }));
    expect(cancel).toHaveBeenCalledWith('');
  });
});
