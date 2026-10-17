import { describe, expect, it, vi } from 'vitest';
import type { InsurerResponse } from '@/api/proposals';
import { archivedSlipActions, responseActions } from './responseActions';

const response = (over: Partial<InsurerResponse>): InsurerResponse => ({
  id: 1,
  insurerCode: 'INS-MGIC',
  insurerName: 'Mabuhay General Insurance Corp.',
  status: 'RECEIVED',
  recommended: false,
  revision: 1,
  ...over,
});

const shown = (r: InsurerResponse, editable: boolean) =>
  responseActions(r, editable, vi.fn(), vi.fn())
    .filter((a) => !a.hidden)
    .map((a) => a.label);

describe('responseActions', () => {
  it('offers Terms and Recommend in the row menu for terms received', () => {
    expect(shown(response({}), true)).toEqual(['Terms', 'Recommend']);
  });

  it('offers no Recommend for a declined or already recommended insurer', () => {
    expect(shown(response({ status: 'DECLINED' }), true)).toEqual(['Terms']);
    expect(shown(response({ recommended: true }), true)).toEqual(['Terms']);
  });

  it('offers nothing once the terms are closed or to a user without TSU rights', () => {
    expect(shown(response({}), false)).toEqual([]);
  });

  it('runs the chosen action on its response', () => {
    const terms = vi.fn();
    const recommend = vi.fn();
    const r = response({});
    responseActions(r, true, terms, recommend).forEach((a) => void a.onSelect(''));
    expect(terms).toHaveBeenCalledWith(r);
    expect(recommend).toHaveBeenCalledWith(r);
  });
});

describe('archivedSlipActions', () => {
  it('offers the download of an archived slip version in the row menu', () => {
    const download = vi.fn();
    const [action] = archivedSlipActions(download);
    expect(action?.label).toBe('Download');
    void action?.onSelect('');
    expect(download).toHaveBeenCalledOnce();
  });
});
