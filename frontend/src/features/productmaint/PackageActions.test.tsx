import { describe, expect, it } from 'vitest';
import type { PackageRequest } from '@/api/productmaint';
import type { WorkAction } from '@/api/workflow';
import { offeredActions } from './PackageActions';

const request = { id: 7, status: 'FOR_MANCOM', requestType: 'NEW', negotiationRequired: true } as PackageRequest;
const action = (name: string): WorkAction =>
  ({ action: name, label: name, toStage: 'WITH_MBS' }) as WorkAction;

describe('offeredActions', () => {
  it('offers the direct ManCom sign-off only when any member may sign off alone', () => {
    const actions = [action('signoff'), action('return')];
    expect(offeredActions(request, actions, false).map((a) => a.action)).toEqual([
      'signoff',
      'return',
    ]);
    expect(offeredActions(request, actions, true).map((a) => a.action)).toEqual(['return']);
  });

  it('offers the set-up to every request type but a retirement', () => {
    const actions = [action('setup'), action('retire')];
    expect(offeredActions(request, actions).map((a) => a.action)).toEqual(['setup']);
    expect(
      offeredActions({ ...request, requestType: 'RETIRE' }, actions).map((a) => a.action),
    ).toEqual(['retire']);
  });
});
