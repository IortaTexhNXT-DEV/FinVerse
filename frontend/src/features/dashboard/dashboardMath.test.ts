import { changePct, changeText, hasData, monthLabel, sharePct } from './dashboardMath';

describe('dashboard math', () => {
  it('labels months', () => {
    expect(monthLabel('2026-03')).toBe('Mar');
    expect(monthLabel('2026-12')).toBe('Dec');
    expect(monthLabel('oops')).toBe('oops');
  });

  it('computes the change against a comparison figure', () => {
    expect(changePct(1250, 500)).toBe(150);
    expect(changePct(90, 100)).toBe(-10);
    expect(changePct(-50, -100)).toBe(50);
    expect(changePct(10, 0)).toBeUndefined();
    expect(changeText(1250, 500, 'prior year')).toBe('+150.0% vs prior year');
    expect(changeText(90, 100, 'prior year')).toBe('-10.0% vs prior year');
    expect(changeText(5, 0, 'prior year')).toBe('no prior year figure');
  });

  it('detects data and shares', () => {
    expect(hasData([0, 0])).toBe(false);
    expect(hasData([0, -1])).toBe(true);
    expect(hasData([])).toBe(false);
    expect(sharePct(25, 100)).toBe(25);
    expect(sharePct(150, 100)).toBe(100);
    expect(sharePct(-5, 100)).toBe(0);
    expect(sharePct(5, 0)).toBe(0);
  });
});
