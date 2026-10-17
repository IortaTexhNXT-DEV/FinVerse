import { describe, expect, it } from 'vitest';
import { recordDescription } from './recordDescription';

describe('recordDescription', () => {
  it('names the product, the record and the client', () => {
    expect(recordDescription('Fire and Lightning Package', 'quotation', 'Bautista, Carmela')).toBe(
      'Fire and Lightning Package quotation for Bautista, Carmela',
    );
  });

  it('reads well while the product name is not known', () => {
    expect(recordDescription('', 'proposal request', 'Pacific Harbor Logistics Inc.')).toBe(
      'proposal request for Pacific Harbor Logistics Inc.',
    );
  });
});
