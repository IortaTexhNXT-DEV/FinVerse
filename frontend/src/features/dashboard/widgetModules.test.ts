import { showsWidget } from './widgetModules';

describe('dashboard widgets of product modules', () => {
  const off = new Set(['BUDGET']);
  const can = (p: string) => p.startsWith('MODULE_OFF:') && off.has(p.slice('MODULE_OFF:'.length));

  it('are hidden when their module is switched off', () => {
    expect(showsWidget('budget', can)).toBe(false);
  });

  it('are shown when their module is in use', () => {
    expect(showsWidget('payables', can)).toBe(true);
  });
});
