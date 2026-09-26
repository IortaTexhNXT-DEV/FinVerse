import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '@/api/client';
import { DataTable } from './DataTable';
import { ErrorAlert } from './ErrorAlert';
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

  it('lists the field errors of a validation failure with readable names', () => {
    const error = new ApiError(400, {
      detail: 'Invalid request',
      code: 'VALIDATION_FAILED',
      errors: { 'lines[0].amount': 'must be greater than 0', vatRate: 'must be at least 0' },
    });
    render(<ErrorAlert error={error} />);
    const items = screen.getAllByRole('listitem').map((li) => li.textContent);
    expect(items).toEqual([
      'Line 1 amount: must be greater than 0',
      'VAT rate: must be at least 0',
    ]);
    expect(screen.getByText('Invalid request')).toBeInTheDocument();
    expect(screen.getByText('Reference: VALIDATION_FAILED')).toBeInTheDocument();
  });

  it('shows plain errors without a field list and nothing without an error', () => {
    const { container, rerender } = render(<ErrorAlert error={new Error('Boom')} />);
    expect(screen.getByRole('alert')).toHaveTextContent('Boom');
    expect(screen.queryByRole('list')).toBeNull();
    rerender(<ErrorAlert error={null} />);
    expect(container).toBeEmptyDOMElement();
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
