import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '@/api/client';
import { ConfirmButton } from './ConfirmButton';

describe('ConfirmButton', () => {
  it('never acts at once: the action runs from the confirmation, which then closes', async () => {
    const run = vi.fn().mockResolvedValue(undefined);
    render(
      <ConfirmButton
        confirm={{ title: 'Approve Receipt', record: 'OR-1', effect: 'The receipt is posted.' }}
        onConfirm={run}
      >
        Approve
      </ConfirmButton>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Approve' }));
    expect(run).not.toHaveBeenCalled();
    expect(screen.getByText('OR-1')).toBeInTheDocument();
    expect(screen.getByText('The receipt is posted.')).toBeInTheDocument();
    await userEvent.click(screen.getAllByRole('button', { name: 'Approve' }).at(-1)!);
    expect(run).toHaveBeenCalledWith('');
    await waitFor(() => expect(screen.queryByText('Approve Receipt')).toBeNull());
  });

  it('shows a refusal in the dialog, titled after the action, and keeps it open', async () => {
    const run = vi
      .fn()
      .mockRejectedValue(
        new ApiError(422, { code: 'X_Y', detail: 'The receipt is already posted' }),
      );
    render(
      <ConfirmButton
        variant="danger"
        confirm={{ title: 'Cancel Slip', effect: 'The slip is cancelled.', destructive: true }}
        onConfirm={run}
      >
        Cancel Slip
      </ConfirmButton>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'Cancel Slip' }));
    await userEvent.click(screen.getAllByRole('button', { name: 'Cancel Slip' }).at(-1)!);
    expect(await screen.findByText('Cannot cancel slip')).toHaveClass('notice-title');
    expect(screen.getByText('The receipt is already posted')).toBeInTheDocument();
    expect(screen.getByText('The slip is cancelled.')).toBeInTheDocument();
  });
});
