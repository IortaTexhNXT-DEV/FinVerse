import { HELP_SECTIONS, searchHelp } from './helpContent';

describe('help content', () => {
  it('covers every section with at least one screen', () => {
    expect(HELP_SECTIONS.length).toBeGreaterThan(4);
    HELP_SECTIONS.forEach((s) => expect(s.screens.length).toBeGreaterThan(0));
    const ids = HELP_SECTIONS.map((s) => s.id);
    expect(new Set(ids).size).toBe(ids.length);
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
