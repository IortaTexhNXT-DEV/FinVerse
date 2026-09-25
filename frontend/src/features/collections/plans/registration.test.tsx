import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import { BILLING_HELP } from '../billing/helpEntries';
import { BILLING_SCREENS } from '../billing/screens';
import { ESCALATION_HELP } from '../escalations/helpEntries';
import { ESCALATION_SCREENS } from '../escalations/screens';
import { PLAN_HELP } from './helpEntries';
import { ReasonDialog } from './Parts';
import { PLAN_SCREENS } from './screens';

describe('collections plans, escalations and billing registration', () => {
  const screens = [...PLAN_SCREENS, ...ESCALATION_SCREENS, ...BILLING_SCREENS];
  const help = [...PLAN_HELP, ...ESCALATION_HELP, ...BILLING_HELP];

  it('has one help entry per menu screen, in menu order', () => {
    const menu = screens.filter((s) => s.hidden !== true).map((s) => s.path);
    expect(help.map((h) => h.path)).toEqual(menu);
    expect(screens.every((s) => s.path.startsWith('/collections/'))).toBe(true);
    expect(screens.every((s) => s.permission !== undefined)).toBe(true);
  });

  it('asks for a reason before confirming', async () => {
    const onConfirm = vi.fn();
    render(
      <ReasonDialog
        title="Cancel Plan"
        confirmLabel="Cancel Plan"
        busy={false}
        error={undefined}
        onClose={() => undefined}
        onConfirm={onConfirm}
      />,
    );
    const confirm = screen.getAllByRole('button', { name: 'Cancel Plan' }).at(-1);
    expect(confirm).toBeDefined();
    await userEvent.click(confirm!);
    expect(onConfirm).not.toHaveBeenCalled();
    expect(screen.getByText('Enter the reason')).toBeInTheDocument();
    await userEvent.type(screen.getByLabelText(/Reason/), 'Replaced by a new plan');
    await userEvent.click(confirm!);
    expect(onConfirm).toHaveBeenCalledWith('Replaced by a new plan');
  });
});
