import { describe, expect, it } from 'vitest';
import type { ServiceInvoiceType } from '@/api/booking';
import { serviceInvoiceTypeName } from './serviceInvoiceLabels';

describe('serviceInvoiceTypeName', () => {
  const types = [
    { code: 'INSURER_COMMISSION', name: 'Commission invoice to the insurer' },
  ] as ServiceInvoiceType[];

  it('names a service invoice type by its set-up name', () => {
    expect(serviceInvoiceTypeName(types, 'INSURER_COMMISSION')).toBe(
      'Commission invoice to the insurer',
    );
  });

  it('writes an unknown type in words, never as its code', () => {
    expect(serviceInvoiceTypeName(undefined, 'INSURER_COMMISSION_ENDT')).not.toContain('_');
  });
});
