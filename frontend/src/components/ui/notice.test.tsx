import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ApiError } from '@/api/client';
import { ErrorAlert } from './ErrorAlert';
import { FormErrorSummary } from './FormErrorSummary';
import { messageParts, plainMessage } from './errorView';
import { Notice } from './Notice';
import { ToastProvider } from './ToastProvider';
import { useToast } from './toastContext';

describe('notice standard', () => {
  it.each([
    ['error', 'alert'],
    ['warning', 'status'],
    ['info', 'status'],
    ['success', 'status'],
  ] as const)('renders a %s notice with its accent class, icon and title', (tone, role) => {
    const { container } = render(
      <Notice tone={tone} title="Short title" items={['First', 'Second']}>
        The business message.
      </Notice>,
    );
    const notice = screen.getByRole(role);
    expect(notice).toHaveClass('notice', `notice-${tone}`);
    expect(container.querySelector('svg.notice-icon')).not.toBeNull();
    expect(screen.getByText('Short title')).toHaveClass('notice-title');
    expect(screen.getByText('The business message.')).toBeInTheDocument();
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'First',
      'Second',
    ]);
  });

  it('shows the support reference only behind Details', async () => {
    render(<Notice tone="error" title="Failed" reference="abc-123" />);
    expect(screen.queryByText(/abc-123/)).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Details' }));
    expect(screen.getByText(/Reference for support/)).toHaveTextContent('abc-123');
  });

  it('shows a business-rule refusal with the dialog title and one bullet per missing item', () => {
    render(
      <ErrorAlert
        title="Cannot submit for ManCom sign-off"
        error={
          new ApiError(422, {
            code: 'REQUIREMENTS_INCOMPLETE',
            detail: 'Complete the requirements: the signed package slip; the MBS set-up form',
            correlationId: 'e5f0a2b4-1c2d-4e5f-8a9b-0c1d2e3f4a5b',
          })
        }
      />,
    );
    const notice = screen.getByRole('alert');
    expect(screen.getByText('Cannot submit for ManCom sign-off')).toHaveClass('notice-title');
    expect(screen.getByText('Complete the requirements')).toBeInTheDocument();
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'the signed package slip',
      'the MBS set-up form',
    ]);
    expect(notice).not.toHaveTextContent(/REQUIREMENTS_INCOMPLETE|e5f0a2b4|Reference|Details/);
    expect(notice).not.toHaveClass('danger');
  });

  it('keeps the reference of an unexpected server error behind Details, not in the message', async () => {
    render(
      <ErrorAlert
        error={
          new ApiError(500, {
            code: 'INTERNAL_ERROR',
            detail:
              'An unexpected error occurred. Quote reference 0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0 to support.',
            reference: '0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0',
          })
        }
        onRetry={() => undefined}
      />,
    );
    const notice = screen.getByRole('alert');
    expect(notice).toHaveTextContent('The system could not complete the request');
    expect(notice).not.toHaveTextContent(/0f1e2d3c|INTERNAL_ERROR/);
    expect(screen.getByRole('button', { name: 'Retry' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('button', { name: 'Details' }));
    expect(notice).toHaveTextContent('Reference for support: 0f1e2d3c');
  });

  it('removes technical codes and identifiers from messages', () => {
    expect(plainMessage('Blocked REQUIREMENTS_INCOMPLETE-6d1f (BRPM.015).')).toBe('Blocked.');
    expect(plainMessage('Quote reference 0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0 to support.')).toBe(
      '',
    );
    expect(messageParts('No list here: just one thing')).toEqual({
      lead: 'No list here: just one thing',
      items: [],
    });
  });

  it('shows toasts in the semantic colours with their icon', async () => {
    function Buttons() {
      const toast = useToast();
      return (
        <>
          <button type="button" onClick={() => toast.success('Saved')}>
            ok
          </button>
          <button type="button" onClick={() => toast.error('Not saved')}>
            ko
          </button>
          <button type="button" onClick={() => toast.warning('Check')}>
            warn
          </button>
        </>
      );
    }
    const { container } = render(
      <ToastProvider>
        <Buttons />
      </ToastProvider>,
    );
    await userEvent.click(screen.getByRole('button', { name: 'ok' }));
    await userEvent.click(screen.getByRole('button', { name: 'ko' }));
    await userEvent.click(screen.getByRole('button', { name: 'warn' }));
    expect(screen.getByText('Saved').parentElement).toHaveClass('toast', 'success');
    expect(screen.getByText('Not saved').parentElement).toHaveClass('toast', 'error');
    expect(screen.getByText('Check').parentElement).toHaveClass('toast', 'warning');
    expect(container.querySelectorAll('.toast svg.notice-icon')).toHaveLength(3);
  });

  it('summarises the field errors of a long form at the top, each field once', async () => {
    const { rerender } = render(
      <>
        <FormErrorSummary errors={{ tin: 'Use the format 000-000-000-000', email: undefined }} />
        <input aria-label="TIN" aria-invalid="true" />
      </>,
    );
    expect(screen.getByText('Correct the highlighted fields')).toHaveClass('notice-title');
    expect(screen.getAllByRole('listitem').map((li) => li.textContent)).toEqual([
      'TIN: Use the format 000-000-000-000',
    ]);
    await userEvent.click(screen.getByRole('button', { name: 'Go to First Field' }));
    expect(screen.getByLabelText('TIN')).toHaveFocus();
    rerender(<FormErrorSummary errors={{}} />);
    expect(screen.queryByRole('alert')).toBeNull();
  });
});
