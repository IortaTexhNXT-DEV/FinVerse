import { describe, expect, it, vi } from 'vitest';
import { adviceActions, dispatchActions } from './issuanceRowActions';

describe('issuance row actions', () => {
  it('offers Send to the E-policy Sender only', () => {
    const send = vi.fn();
    const [action] = dispatchActions(true, send);
    expect(action?.label).toBe('Send');
    expect(action?.hidden).toBe(false);
    void action?.onSelect('');
    expect(send).toHaveBeenCalledOnce();
    expect(dispatchActions(false, send)[0]?.hidden).toBe(true);
  });

  it('offers the PDF of an Insurance Advice as Download PDF', () => {
    const download = vi.fn();
    const [action] = adviceActions(download);
    expect(action?.label).toBe('Download PDF');
    void action?.onSelect('');
    expect(download).toHaveBeenCalledOnce();
  });
});
