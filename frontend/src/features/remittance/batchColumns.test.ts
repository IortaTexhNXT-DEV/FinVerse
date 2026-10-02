import { describe, expect, it } from 'vitest';
import { BATCH_COLUMNS } from './batchColumns';

describe('remittance batch columns', () => {
  it('keeps the processor on one line and the status as a pill column, so the list fits its card', () => {
    const processor = BATCH_COLUMNS.find((c) => c.header === 'Processor');
    expect(processor?.truncate).toBe(true);
    expect(processor?.width).toBe('180px');
    expect(BATCH_COLUMNS.find((c) => c.header === 'Status')?.kind).toBe('status');
  });
});
