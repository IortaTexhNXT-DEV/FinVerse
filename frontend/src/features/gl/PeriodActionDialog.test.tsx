import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { Period } from '@/api/periods';
import { PeriodActionDialog } from './PeriodActionDialog';
import { PERIOD_ACTIONS, periodActionText, reasonProblem } from './periodActions';

const september: Period = {
  id: 9,
  fiscalYearId: 1,
  periodNo: 9,
  name: '2026-09',
  startDate: '2026-09-01',
  endDate: '2026-09-30',
  status: 'OPEN',
};

function renderDialog(action: 'close' | 'reopen', onConfirm = vi.fn(), onCancel = vi.fn()) {
  render(
    <PeriodActionDialog
      pending={{ period: september, action }}
      busy={false}
      error={null}
      onCancel={onCancel}
      onConfirm={onConfirm}
    />,
  );
  return { onConfirm, onCancel };
}

describe('period status changes', () => {
  it('asks for confirmation naming the period and the consequence before closing', async () => {
    const { onConfirm, onCancel } = renderDialog('close');
    expect(screen.getByRole('dialog', { name: 'Close 2026-09' })).toBeInTheDocument();
    expect(screen.getByText(/All postings to 2026-09 will be blocked/)).toBeInTheDocument();
    expect(onConfirm).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(onCancel).toHaveBeenCalled();
    expect(onConfirm).not.toHaveBeenCalled();
  });

  it('confirms a close with one explicit click', async () => {
    const { onConfirm } = renderDialog('close');
    await userEvent.click(screen.getByRole('button', { name: 'Close period' }));
    expect(onConfirm).toHaveBeenCalledWith('');
  });

  it('requires a non-blank reason of at most 200 characters to reopen', async () => {
    const { onConfirm } = renderDialog('reopen');
    const confirm = screen.getByRole('button', { name: 'Reopen period' });
    expect(confirm).toBeDisabled();

    const reason = screen.getByLabelText('Reason');
    await userEvent.type(reason, '   ');
    expect(confirm).toBeDisabled();
    expect(reason).toHaveAttribute('maxLength', '200');

    await userEvent.type(reason, 'Late supplier invoice  ');
    await userEvent.click(confirm);
    expect(onConfirm).toHaveBeenCalledWith('Late supplier invoice');
  });

  it('describes every offered action', () => {
    Object.values(PERIOD_ACTIONS)
      .flat()
      .forEach(({ action }) => {
        const text = periodActionText(action, september);
        expect(text.title).toContain('2026-09');
        expect(text.consequence).toContain('2026-09');
      });
    expect(periodActionText('reopen', september).needsReason).toBe(true);
    expect(periodActionText('startClosing', september).needsReason).toBe(false);
    expect(reasonProblem('x'.repeat(201))).toBe('At most 200 characters');
    expect(reasonProblem('ok')).toBeUndefined();
  });
});
