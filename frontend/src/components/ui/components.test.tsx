import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { DataTable } from './DataTable';
import { Modal } from './Modal';
import { StatusBadge } from './StatusBadge';

describe('ui components', () => {
  it('renders a status badge with tone and human label', () => {
    render(<StatusBadge status="PENDING_APPROVAL" />);
    const badge = screen.getByText('Pending Approval');
    expect(badge).toHaveClass('badge', 'warning');
  });

  it.each([
    ['LOCKED', 'Locked', 'danger'],
    ['FROZEN', 'Frozen', 'danger'],
    ['SUCCEEDED', 'Succeeded', 'success'],
    ['POSTED', 'Posted', 'success'],
    ['UP', 'Up', 'success'],
    ['ACTIVE', 'Active', 'success'],
    ['RUNNING', 'Running', 'warning'],
    ['FAILED', 'Failed', 'danger'],
  ])('keeps the label of %s and only shares its colour', (status, label, tone) => {
    render(<StatusBadge status={status} />);
    expect(screen.getByText(label)).toHaveClass('badge', tone);
  });

  it('renders an unknown status in the neutral info tone', () => {
    render(<StatusBadge status="SOMETHING_NEW" />);
    expect(screen.getByText('Something New').className).toBe('badge ');
  });

  it('renders table rows and handles row clicks', async () => {
    const onClick = vi.fn();
    render(
      <DataTable
        rows={[{ id: 1, name: 'Cash' }]}
        rowKey={(r) => r.id}
        onRowClick={onClick}
        columns={[{ key: 'n', header: 'Name', render: (r) => r.name }]}
      />,
    );
    await userEvent.click(screen.getByText('Cash'));
    expect(onClick).toHaveBeenCalledWith({ id: 1, name: 'Cash' });
  });

  it('shows the empty message when there are no rows', () => {
    render(<DataTable rows={[]} rowKey={() => 1} columns={[]} emptyMessage="Nothing here" />);
    expect(screen.getByText('Nothing here')).toBeInTheDocument();
  });

  it('closes a modal on Escape', async () => {
    const onClose = vi.fn();
    render(
      <Modal title="Test" open onClose={onClose}>
        body
      </Modal>,
    );
    await userEvent.keyboard('{Escape}');
    expect(onClose).toHaveBeenCalled();
  });
});
