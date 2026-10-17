import { describe, expect, it } from 'vitest';
import { itemColumns } from './cycleItemColumns';

describe('reconciliation item columns', () => {
  it('keeps the AO on one line and the status as a pill column, so the list fits its card', () => {
    const columns = itemColumns([], undefined, () => undefined);
    const ao = columns.find((c) => c.header === 'AO');
    expect(ao?.truncate).toBe(true);
    expect(ao?.width).toBe('180px');
    expect(columns.find((c) => c.header === 'Status')?.kind).toBe('status');
  });
});
