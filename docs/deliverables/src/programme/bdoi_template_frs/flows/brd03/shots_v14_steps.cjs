// The steps of shots_v14.cjs: storyboards (sb-*) and screen states (ux-*) of Product Maintenance v1.4. Each step
// returns the page to capture. The walkthrough steps and the records opened for a stage are those of the BRD-03
// recipe of the screenshot tools (tools/screenshots/packs/brd03.cjs and brd03_walkthrough.cjs).
const path = require('path');

const PACKS = path.resolve(__dirname, '../../../../../../..', 'tools/screenshots/packs');
const recipe = require(path.join(PACKS, 'brd03.cjs'));
const wt = recipe.walkthrough;

module.exports = (ctx) => {
  const go = (user, url, after) => async () => {
    const page = await ctx.pageOf(user);
    await page.goto(ctx.BASE + url);
    await ctx.settle(page);
    if (after) await after(page, ctx);
    return page;
  };
  const open = (user, key, tabName, after) => async () => {
    const url = await recipe.opens[key](ctx);
    const page = await ctx.pageOf(user);
    await page.goto(ctx.BASE + url);
    await ctx.settle(page);
    if (tabName) await ctx.openTab(page, tabName);
    if (after) await after(page, ctx);
    return page;
  };
  const step = (file, run, after) => ({ file, run, after });
  const walk = (file, key) => step(file, () => wt[key](ctx));
  const custom = (file, key) => step(file, () => recipe.custom[key](ctx));

  return [
    // ------------------------------------------------------------ SB-01 and SB-02: new package, release, pricing
    walk('sb1-01-clause', 'wt-a-01'),
    walk('sb1-02-clause-authorised', 'wt-a-02'),
    walk('sb1-03-request', 'wt-a-03'),
    walk('sb1-04-marketing', 'wt-a-04'),
    walk('sb1-05-recommend', 'wt-a-05'),
    walk('sb1-06-tsu-head', 'wt-a-06'),
    walk('sb1-07-slip', 'wt-a-07'),
    walk('sb1-08-slip-sent', 'wt-a-08'),
    walk('sb1-09-responses', 'wt-a-09'),
    walk('sb1-10-terms-final', 'wt-a-10'),
    walk('sb1-11-comparative', 'wt-a-11'),
    walk('sb1-12-requirements', 'wt-a-12'),
    walk('sb1-13-mancom-sent', 'wt-a-13'),
    walk('sb1-14-mancom-approved', 'wt-a-14'),
    walk('sb1-15-setup', 'wt-a-15'),
    walk('sb1-16-version', 'wt-a-16'),
    walk('sb1-17-released', 'wt-a-17'),
    walk('sb1-18-commission', 'wt-a-18'),
    walk('sb1-19-commission-authorised', 'wt-a-19'),
    walk('sb1-20-advisory', 'wt-a-20'),
    walk('sb2-01-calculator', 'wt-a-21'),
    walk('sb2-02-exception', 'wt-a-22'),
    walk('sb2-03-exception-approved', 'wt-a-23'),
    walk('sb2-04-quotation', 'wt-a-24'),

    // ------------------------------------------------------------ screens (UX design)
    step('ux-01-home', go('tsulead', '/product-maintenance')),
    step('ux-02-requests', go('tsulead', '/product-maintenance/requests')),
    step('ux-03-new', go('ao', '/product-maintenance/requests/new', (p) => ctx.runSteps(p, recipe.fills.new_request, ctx))),
    step('ux-03-errors', go('ao', '/product-maintenance/requests/new', async (p) => {
      await ctx.runSteps(p, recipe.fills.new_request_invalid, ctx);
      await ctx.clickButton(p, '^submit for approval$');
    })),
    step('ux-04-record', open('mkttl', 'for_mkt_approval')),
    step('ux-04-tsu-approval', open('tsuhead', 'for_tsu_approval')),
    step('ux-05-negotiation', open('tsu', 'negotiation', 'Negotiation')),
    step('ux-06-comparative', open('tsu', 'negotiation_master', 'Comparative')),
    step('ux-07-requirements', open('tsu', 'requirements_prep', 'Requirements')),
    step('ux-29-routing', open('mancom', 'for_mancom', 'Routing')),
    step('ux-08-setup', open('mbs', 'with_mbs')),
    step('ux-08-advisories', open('tsu', 'released', 'Advisories')),
    step('ux-09-tsu', go('tsulead', '/product-maintenance/tsu')),
    step('ux-10-expiry', go('tsu', '/product-maintenance/expiry')),
    step('ux-11-products', go('mbs', '/catalog/products')),
    step('ux-12-product', open('mbs', 'packaged_product')),
    step('ux-13-version', open('mbs', 'draft_version')),
    step('ux-14-validation', go('tsuhead', '/catalog/validation', async (p) => {
      await recipe.opens.version_for_validation(ctx);
      await p.reload();
      await ctx.settle(p);
    })),
    step('ux-13-version-validate', open('tsuhead', 'version_for_validation')),
    step('ux-15-coverages', go('mbs', '/catalog/coverages')),
    step('ux-15-clauses', go('mbs', '/catalog/coverages', (p) => ctx.openTab(p, 'Clause Library'))),
    step('ux-16-incentives', go('mbs', '/catalog/incentives')),
    step('ux-17-insurers', go('mbs', '/catalog/insurers')),
    step('ux-18-insurer', open('mbs', 'first_insurer')),
    step('ux-19-rates', go('mbs', '/catalog/rates')),
    step('ux-20-sales', go('badmin', '/catalog/sales-organisation', recipe.after['scr-pm-20-01-tree'])),
    step('ux-21-calculator', go('ao', '/catalog/calculator', async (p) => {
      await ctx.runSteps(p, recipe.fills.calculator_package, ctx);
      await ctx.clickButton(p, '^calculate$');
    })),
    step('ux-22-exception', open('approver', 'rate_exception_pending')),
    step('ux-23-dashboard', go('tsuhead', '/product-maintenance/dashboard')),
    custom('ux-24-deactivate', 'scr-pm-24-03-new'),
    step('ux-24-deactivations', go('mbs', '/product-maintenance/deactivations')),
    step('ux-25-transfers', go('mbs', '/product-maintenance/master-changes')),
    step('ux-26-placement', go('tsu', '/product-maintenance/placement-reports')),
    step('ux-27-audit', go('auditor', '/product-maintenance/audit-logs')),
    // The non-package proposal request with insurer terms received (New Business, Proposals).
    step('ux-28-proposal', async () => {
      const id = ctx.one("select id from npk_proposal where prf_no in ('PRF-2026-900004', 'PRF-2026-000104') order by id limit 1");
      return go('tsu', `/proposals/${id}`, (p) => ctx.openTab(p, 'Comparative Table'))();
    }),

    // ------------------------------------------------------------ SB-04: returns and refusals
    walk('sb4-01-draft', 'wt-b-01'),
    walk('sb4-02-incomplete', 'wt-b-02'),
    walk('sb4-03-marketing', 'wt-b-03'),
    walk('sb4-04-tsu-return', 'wt-b-04'),
    walk('sb4-05-own-slip', 'wt-b-05'),
    walk('sb4-06-terms-final', 'wt-b-06'),
    walk('sb4-07-mancom-return', 'wt-b-07'),
    walk('sb4-08-resubmit', 'wt-b-08'),
    walk('sb4-09-mbs-return', 'wt-b-09'),
    walk('sb4-10-version-errors', 'wt-b-10'),
    walk('sb4-11-validation-return', 'wt-b-11'),
    walk('sb4-12-mbs-again', 'wt-b-12'),
  ];
};
