import { describe, expect, it, vi } from 'vitest';
import type { BillingBatch } from '@/api/placement';
import { batchActions, lineActions } from './batchActions';

const batch = (status: string) => ({ id: 3, batchNo: 'CLPC-2026-000003', status }) as BillingBatch;

describe('batchActions', () => {
  it('offers the two billing files and the report upload of an open batch', () => {
    const download = vi.fn();
    const upload = vi.fn();
    const actions = batchActions(batch('SENT'), { download, upload }).filter((a) => !a.hidden);
    expect(actions.map((a) => a.label)).toEqual([
      'Download Excel',
      'Download ODS',
      'Upload Report',
    ]);
    actions.forEach((a) => void a.onSelect(''));
    expect(download.mock.calls).toEqual([['XLSX'], ['ODS']]);
    expect(upload).toHaveBeenCalledOnce();
  });

  it('offers no upload once the batch is closed', () => {
    const labels = batchActions(batch('CLOSED'), { download: vi.fn(), upload: vi.fn() })
      .filter((a) => !a.hidden)
      .map((a) => a.label);
    expect(labels).toEqual(['Download Excel', 'Download ODS']);
  });
});

describe('lineActions', () => {
  it('offers Match while the report is reviewed, nothing afterwards', () => {
    const match = vi.fn();
    const [action] = lineActions(true, match);
    expect(action?.label).toBe('Match');
    void action?.onSelect('');
    expect(match).toHaveBeenCalledOnce();
    expect(lineActions(false, match).filter((a) => !a.hidden)).toEqual([]);
  });
});
