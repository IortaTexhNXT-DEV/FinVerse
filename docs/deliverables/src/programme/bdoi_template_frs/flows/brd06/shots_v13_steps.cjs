// The steps of shots_v13.cjs: storyboards (sb*-*) and screen states (ux-*) of Renewal v1.3. Each step returns the page
// to capture. The walkthrough steps, the records opened for a stage and the form values are those of the BRD-06 recipe
// of the screenshot tools (tools/screenshots/packs/brd06.cjs and brd06_walkthrough.cjs); the screen states are those of
// the screen specifications (brd06_v13_screens.py --manifest, file named by MANIFEST).
const fs = require('fs');
const path = require('path');

const PACKS = path.resolve(__dirname, '../../../../../../..', 'tools/screenshots/packs');
const recipe = require(path.join(PACKS, 'brd06.cjs'));
const wt = recipe.walkthrough;

// Storyboard steps: SB-01 to SB-05 are the walkthroughs A to E of the recipe.
const STORYBOARDS = { a: 8, b: 6, c: 4, d: 4, e: 3 };

module.exports = (ctx) => {
  const walk = (sb, letter, n) => ({
    file: `sb${sb}-${String(n).padStart(2, '0')}`,
    run: () => wt[`wt-${letter}-${String(n).padStart(2, '0')}`](ctx),
  });
  const storyboard = (letter) => {
    const sb = Object.keys(STORYBOARDS).indexOf(letter) + 1;
    return Array.from({ length: STORYBOARDS[letter] }, (_, i) => walk(sb, letter, i + 1));
  };

  /** Brings a page to a screen state of the manifest: open the record, tab, fill, select, click, then the recipe's
   * finishing touch for that state. */
  const screen = (shot) => ({
    file: shot.slug,
    run: async () => {
      const page = await ctx.pageOf(shot.user);
      const openKey = Object.keys(shot).find((k) => k.startsWith('open_'));
      let target = shot.route;
      if (openKey) {
        const resolver = recipe.opens[`${openKey}:${shot[openKey]}`];
        if (!resolver) throw new Error(`no resolver for ${openKey}: ${shot[openKey]}`);
        target = await resolver(ctx, shot);
      }
      if (target.includes(':')) throw new Error(`route ${target} needs a record`);
      await page.goto(ctx.BASE + target);
      await ctx.settle(page);
      if (shot.tab) await ctx.openTab(page, ctx.escapeRx(shot.tab));
      if (shot.fill) await ctx.runSteps(page, recipe.fills[shot.fill], ctx);
      if (shot.select) {
        await recipe.selects[shot.select](page, ctx);
        await ctx.settle(page, 300);
      }
      if (shot.click) await ctx.clickButton(page, shot.click);
      const after = recipe.after[shot.pack_slug];
      if (after) await after(page, ctx);
      return page;
    },
  });

  const manifest = JSON.parse(fs.readFileSync(process.env.MANIFEST, 'utf8'));
  return [
    ...storyboard('a'),
    ...storyboard('b'),
    ...manifest.map(screen),
    ...storyboard('c'),
    ...storyboard('d'),
    ...storyboard('e'),
  ];
};
