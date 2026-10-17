import { describe, expect, it, vi } from 'vitest';
import type { QuotationRequest } from '@/api/quotations';
import { requestActions } from './requestActions';

const request = (over: Partial<QuotationRequest>): QuotationRequest => ({
  id: 1,
  requestNo: 'QR-2026-000001',
  channel: 'EMAIL',
  receivedAt: '2026-09-27T01:00:00Z',
  status: 'NEW',
  createdBy: 'ao',
  ...over,
});

const labels = (r: QuotationRequest, canMaintain = true) =>
  requestActions(r, canMaintain, { open: vi.fn(), prospect: vi.fn(), close: vi.fn() })
    .filter((a) => !a.hidden)
    .map((a) => a.label);

describe('requestActions', () => {
  it('offers Create Prospect and Close for a new request without a client', () => {
    expect(labels(request({}))).toEqual(['Create Prospect', 'Close']);
  });

  it('offers Create Quotation and Close for a new request of a client', () => {
    expect(labels(request({ clientId: 7 }))).toEqual(['Create Quotation', 'Close']);
  });

  it('offers Open Quotation once quoted, and nothing to a user who may not maintain requests', () => {
    expect(labels(request({ status: 'QUOTED', quotationId: 9 }))).toEqual(['Open Quotation']);
    expect(labels(request({}), false)).toEqual([]);
    expect(labels(request({ status: 'CLOSED' }))).toEqual([]);
  });

  it('opens the quotation of a quoted request and marks Close as the reversing action', () => {
    const open = vi.fn();
    const quoted = requestActions(request({ status: 'QUOTED', quotationId: 9 }), true, {
      open,
      prospect: vi.fn(),
      close: vi.fn(),
    });
    void quoted[0]?.onSelect('');
    expect(open).toHaveBeenCalledWith('/quotations/9');
    const fresh = requestActions(request({}), true, { open, prospect: vi.fn(), close: vi.fn() });
    expect(fresh.find((a) => a.label === 'Close')?.danger).toBe(true);
  });
});
