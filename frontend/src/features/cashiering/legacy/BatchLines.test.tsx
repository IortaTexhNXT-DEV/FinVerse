import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { LegacyBatchLine } from '@/api/legacyBatches';
import { INCOME_SCREEN } from './batchScreens';
import { BatchLines } from './LegacyBatchDetail';

const LINE: LegacyBatchLine = {
  id: 7,
  lineNo: 1,
  reference: 'UNP-2026-000004',
  amount: 2500,
  status: 'PENDING',
};

describe('legacy batch lines', () => {
  it('removes a line from its row action menu, never from a button in the row', async () => {
    const onRemove = vi.fn();
    render(
      <BatchLines
        screen={INCOME_SCREEN}
        lines={[LINE]}
        loading={false}
        onRemove={onRemove}
        removing={false}
      />,
    );
    expect(screen.queryByRole('button', { name: /^remove/i })).toBeNull();
    await userEvent.click(screen.getByRole('button', { name: 'Actions for line 1' }));
    await userEvent.click(screen.getByRole('menuitem', { name: 'Remove' }));
    expect(onRemove).toHaveBeenCalledWith(7);
  });
});
