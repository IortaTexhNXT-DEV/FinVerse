import { activeMenuPath } from './activeMenu';
import { MODULES } from './modules';

const MENU = ['/', '/gl/journals', '/gl/journals/new', '/claims', '/claims/new', '/claims/lpos'];

describe('active menu entry', () => {
  it('highlights only the most specific entry', () => {
    expect(activeMenuPath('/gl/journals/new', MENU)).toBe('/gl/journals/new');
    expect(activeMenuPath('/gl/journals', MENU)).toBe('/gl/journals');
    expect(activeMenuPath('/claims/lpos', MENU)).toBe('/claims/lpos');
    expect(activeMenuPath('/claims/new', MENU)).toBe('/claims/new');
  });

  it('keeps the list entry active on its detail and edit pages', () => {
    expect(activeMenuPath('/gl/journals/12', MENU)).toBe('/gl/journals');
    expect(activeMenuPath('/gl/journals/12/edit', MENU)).toBe('/gl/journals');
    expect(activeMenuPath('/claims/7', MENU)).toBe('/claims');
  });

  it('matches the dashboard only exactly and ignores look-alike prefixes', () => {
    expect(activeMenuPath('/', MENU)).toBe('/');
    expect(activeMenuPath('/claimsx', MENU)).toBeUndefined();
    expect(activeMenuPath('/unknown', MENU)).toBeUndefined();
  });

  it('never highlights two entries of the real menu for any menu screen', () => {
    const menu = MODULES.flatMap((m) => m.screens.filter((s) => s.hidden !== true)).map(
      (s) => s.path,
    );
    menu.forEach((path) => {
      expect(activeMenuPath(path, menu)).toBe(path);
    });
  });
});
