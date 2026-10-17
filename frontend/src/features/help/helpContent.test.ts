import { MODULES } from '@/navigation/modules';
import { HELP_SECTIONS, searchHelp } from './helpContent';

/** Route → index of its sidebar module, for every screen shown in the menu. */
const menuScreens = new Map(
  MODULES.flatMap((module, index) =>
    module.screens.filter((s) => s.hidden !== true).map((s) => [s.path, index] as const),
  ),
);

const helpPaths = HELP_SECTIONS.flatMap((section) =>
  section.screens.flatMap((screen) => (screen.path === undefined ? [] : [screen.path])),
);

describe('help content', () => {
  it('covers every section with at least one screen', () => {
    expect(HELP_SECTIONS.length).toBeGreaterThan(4);
    HELP_SECTIONS.forEach((s) => expect(s.screens.length).toBeGreaterThan(0));
    const ids = HELP_SECTIONS.map((s) => s.id);
    expect(new Set(ids).size).toBe(ids.length);
  });

  it('gives screens unique names within a section', () => {
    HELP_SECTIONS.forEach((section) => {
      const names = section.screens.map((s) => s.name);
      expect(new Set(names).size, section.id).toBe(names.length);
    });
  });

  it('searches screens and modules', () => {
    expect(searchHelp('')).toBe(HELP_SECTIONS);
    const recurring = searchHelp('auto-reverse');
    expect(recurring).toHaveLength(1);
    expect(recurring[0]?.screens.map((s) => s.name)).toEqual(['Recurring Journals']);
    expect(searchHelp('administration')[0]?.screens.length).toBeGreaterThan(3);
    expect(searchHelp('no-such-topic-xyz')).toEqual([]);
  });
});

describe('help coverage of the menu', () => {
  it('has exactly one help entry for every screen shown in the menu', () => {
    const paths = [...menuScreens.keys()];
    expect(paths.length).toBeGreaterThan(80);
    expect(paths.filter((p) => !helpPaths.includes(p))).toEqual([]);
    expect(paths.filter((p) => helpPaths.filter((h) => h === p).length > 1)).toEqual([]);
  });

  it('links only to screens shown in the menu', () => {
    expect(helpPaths.filter((p) => !menuScreens.has(p))).toEqual([]);
  });

  it('keeps each section within one module and the sections in sidebar order', () => {
    const order = HELP_SECTIONS.map((section) => {
      const modules = new Set(
        section.screens.flatMap((s) => (s.path === undefined ? [] : [menuScreens.get(s.path)])),
      );
      expect(modules.size, section.id).toBe(1);
      return [...modules][0] ?? -1;
    });
    expect(order).toEqual([...order].sort((a, b) => a - b));
  });
});
