import { render, screen } from '@testing-library/react';
import { changeHistoryApi } from '@/api/changeHistory';
import type { FieldChange } from '@/api/changeHistory';
import { ebWrapper } from '@/features/eb/testWrapper';
import { ChangeHistory } from './ChangeHistory';
import { checkerState, fieldLabel } from './changeLabels';

const change = (over: Partial<FieldChange>): FieldChange => ({
  id: 1,
  entityType: 'InsurerProfile',
  entityId: 7,
  changeKind: 'UPDATE',
  field: 'defaultCreditDays',
  oldValue: '30',
  newValue: '45',
  maker: 'badmin',
  madeAt: '2026-10-01T02:00:00Z',
  checker: null,
  checkedAt: null,
  ...over,
});

describe('ChangeHistory', () => {
  it('labels fields and creations', () => {
    expect(fieldLabel(change({}))).toBe('Default credit days');
    expect(fieldLabel(change({ changeKind: 'CREATE', field: 'record' }))).toBe('Record created');
  });

  it('states the authorization of a change', () => {
    expect(checkerState(change({ checker: 'approver' }), true)).toBe('approver');
    expect(checkerState(change({}), true)).toBe('Pending authorization');
    expect(checkerState(change({}), false)).toBe('Not subject to authorization');
  });

  it('lists the old and new values of the record', async () => {
    const spy = vi
      .spyOn(changeHistoryApi, 'history')
      .mockResolvedValue([
        change({}),
        change({ id: 2, field: 'name', oldValue: 'A', newValue: 'B' }),
      ]);
    const wrap = ebWrapper(new Set(['MASTER_VIEW']));
    render(wrap(<ChangeHistory entityType="InsurerProfile" entityId={7} />));

    expect(await screen.findByText('Default credit days')).toBeInTheDocument();
    expect(screen.getByText('45')).toBeInTheDocument();
    expect(screen.getAllByText('Pending authorization')).toHaveLength(2);
    expect(spy).toHaveBeenCalledWith('InsurerProfile', 7);
  });
});
