import { describe, expect, it, vi } from 'vitest';
import { issuedCertificateActions, receivedCertificateActions } from './rowActions';

describe('certificate row menus', () => {
  it('cancels a recorded received certificate, in red', () => {
    const cancel = vi.fn();
    const actions = receivedCertificateActions({ status: 'RECORDED' }, true, cancel);
    expect(actions.map((a) => a.label)).toEqual(['Cancel Certificate']);
    expect(actions[0]?.danger).toBe(true);
    expect(receivedCertificateActions({ status: 'CANCELLED' }, true, cancel)).toEqual([]);
    expect(receivedCertificateActions({ status: 'RECORDED' }, false, cancel)).toEqual([]);
  });

  it('downloads an issued 2307 and cancels it last, after a confirmation', () => {
    const on = { download: vi.fn(), cancel: vi.fn() };
    const actions = issuedCertificateActions({ status: 'ISSUED', certificateNo: 'C-1' }, true, on);
    expect(actions.map((a) => a.label)).toEqual(['Download PDF', 'Cancel Certificate']);
    expect(actions[1]?.confirm?.destructive).toBe(true);
    actions[0]?.onSelect('');
    expect(on.download).toHaveBeenCalled();
    expect(issuedCertificateActions({ status: 'CANCELLED' }, true, on).map((a) => a.label)).toEqual(
      ['Download PDF'],
    );
  });
});
