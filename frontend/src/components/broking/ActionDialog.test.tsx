import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { ActionDialog } from './ActionDialog';

describe('ActionDialog', () => {
  it('keeps the confirm button disabled until a required comment is entered', () => {
    const onConfirm = vi.fn();
    render(
      <ActionDialog
        title="Close QR-1"
        confirmLabel="Close Request"
        commentRequired
        commentLabel="Reason for Closing"
        onConfirm={onConfirm}
        onClose={vi.fn()}
      />,
    );
    const confirm = screen.getByRole('button', { name: 'Close Request' });
    expect(confirm).toBeDisabled();
    fireEvent.change(screen.getByLabelText(/Reason for Closing/), {
      target: { value: '  Client withdrew  ' },
    });
    expect(confirm).toBeEnabled();
    fireEvent.click(confirm);
    expect(onConfirm).toHaveBeenCalledWith({ reasonCode: undefined, comment: 'Client withdrew' });
  });

  it('allows an empty comment when it is optional', () => {
    render(
      <ActionDialog title="Return" confirmLabel="Return" onConfirm={vi.fn()} onClose={vi.fn()} />,
    );
    expect(screen.getByRole('button', { name: 'Return' })).toBeEnabled();
  });
});
