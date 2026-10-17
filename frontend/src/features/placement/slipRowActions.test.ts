import { describe, expect, it, vi } from 'vitest';
import type { Slip } from '@/api/placement';
import { slipActions } from './slipRowActions';

const slip = (status: string) => ({ id: 5, displayNo: 'PL-2026-000005 v1', status }) as Slip;
const on = () => ({ download: vi.fn(), send: vi.fn(), regenerate: vi.fn() });
const labels = (s: Slip, manage: boolean) =>
  slipActions(s, manage, on())
    .filter((a) => !a.hidden)
    .map((a) => a.label);

describe('slipActions', () => {
  it('offers the downloads and Send for a generated slip', () => {
    expect(labels(slip('GENERATED'), true)).toEqual(['Download PDF', 'Download Excel', 'Send']);
  });

  it('offers Resend and Regenerate once the slip is sent', () => {
    expect(labels(slip('SENT'), true)).toEqual([
      'Download PDF',
      'Download Excel',
      'Resend',
      'Regenerate',
    ]);
  });

  it('offers only the downloads to a user who does not manage placement', () => {
    expect(labels(slip('SENT'), false)).toEqual(['Download PDF', 'Download Excel']);
  });

  it('downloads the format chosen', () => {
    const handlers = on();
    slipActions(slip('SENT'), true, handlers).forEach((a) => void a.onSelect(''));
    expect(handlers.download.mock.calls).toEqual([['pdf'], ['xlsx']]);
    expect(handlers.send).toHaveBeenCalledOnce();
    expect(handlers.regenerate).toHaveBeenCalledOnce();
  });
});
