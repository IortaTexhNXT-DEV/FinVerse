import { describe, expect, it } from 'vitest';
import { arrangeWidgets, DELIVERED_WIDGETS } from './homeWidgets';

describe('arrangeWidgets', () => {
  it('shows the delivered widgets while no role set-up is read', () => {
    const { top, grid } = arrangeWidgets(undefined);
    expect([...top, ...grid]).toEqual([...DELIVERED_WIDGETS]);
    expect(arrangeWidgets([])).toEqual(arrangeWidgets(undefined));
  });

  it('keeps the order of the role set-up within the top row and the grid', () => {
    expect(arrangeWidgets(['CASH', 'WORKLOAD', 'COLLECTIONS', 'KPIS'])).toEqual({
      top: ['WORKLOAD', 'KPIS'],
      grid: ['CASH', 'COLLECTIONS'],
    });
  });
});
