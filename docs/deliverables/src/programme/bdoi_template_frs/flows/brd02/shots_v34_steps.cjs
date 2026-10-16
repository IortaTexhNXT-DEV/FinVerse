// The steps of shots_v34.cjs: storyboards (sb*-*) and screen states (ux-*) of Operations Cashiering v3.4. Each step
// returns the page to capture. The screen states are those of the screen specifications (brd02_v34_screens.py
// --manifest, file named by MANIFEST); the records to open, the form values and the record-making helpers are those of
// the BRD-02 recipe of the screenshot tools (tools/screenshots/packs/brd02.cjs). The storyboards (brd02_v34_pages.py)
// are performed here on a fresh seed database, before the screen states. Fictitious seed values only.
const fs = require('fs');
const os = require('os');
const path = require('path');

const PACKS = path.resolve(__dirname, '../../../../../../..', 'tools/screenshots/packs');
const recipe = require(path.join(PACKS, 'brd02.cjs'));
const { postingStep } = require(path.join(PACKS, 'brd02_walkthrough.cjs'));

module.exports = (ctx) => {
  const S = ctx.state;
  const company = () => Number(ctx.one("select id from org_company where code = 'FVI'"));
  const open = async (user, route) => {
    const page = await ctx.pageOf(user);
    await page.goto(ctx.BASE + route);
    await ctx.settle(page, 1000);
    return page;
  };
  const dialog = (page) => page.locator('dialog.modal[open]').last();
  const confirm = async (page, label) => {
    await dialog(page).getByRole('button', { name: new RegExp(`^${label}$`, 'i') }).click();
    await ctx.settle(page, 1200);
  };

  /** Settings of Create AR/OR and an account with an outstanding balance. */
  async function setup() {
    if (S.setup) return S.setup;
    await postingStep(ctx, 'AR,OR');
    const companyId = company();
    const s = await ctx.api('cashier', 'GET', `/cashiering/records/settings?companyId=${companyId}`);
    const bank = (s.bankAccounts.find((b) => b.currency === 'PHP' && b.defaultForCurrency) || s.bankAccounts[0]).code;
    const head = s.arBranches.find((b) => /head office/i.test(b.name)) || s.arBranches[0];
    S.setup = { companyId, s, bank, head };
    return S.setup;
  }

  // ---------------------------------------------------------------- SB-01 premium payment to the issued AR
  async function sb1Record() {
    if (S.sb1) return S.sb1;
    const { companyId, bank, head } = await setup();
    const rows = await ctx.api('cashier', 'GET', `/cashiering/records/accounts?companyId=${companyId}&q=ARN-2026-94`);
    const acc = rows.find((r) => Number(r.outstanding) > 1000 && !r.prebooked) || rows[0];
    const amount = Math.round(Number(acc.outstanding) * 100) / 100;
    const rec = await ctx.api('cashier', 'POST', '/cashiering/records', {
      companyId, receiptKind: 'AR', receiptType: 'PREMIUM', branchId: head.id, entryType: 'CLIENT',
      clientCode: acc.clientCode, clientName: acc.assuredName, payorName: acc.assuredName, tenderType: 'CASH',
      currency: 'PHP', bankAccount: bank, amount, remarks: 'Premium paid in cash at the Head Office counter',
      accounts: [{ reference: acc.invoiceNo || acc.arn, amount }],
    });
    S.sb1 = rec.id;
    return rec.id;
  }

  // ---------------------------------------------------------------- SB-02 service fee OR
  async function sb2Record() {
    if (S.sb2) return S.sb2;
    const { companyId, s, bank, head } = await setup();
    const orType = ctx.one("select code from lov_value where type_code = 'OR_TYPE' and code = 'SERVICE_FEE'");
    const back = new Date(`${s.today}T00:00:00Z`);
    back.setUTCDate(back.getUTCDate() - 14);
    const rec = await ctx.api('cashier', 'POST', '/cashiering/records', {
      companyId, receiptKind: 'OR', receiptType: orType, branchId: (s.orBranches[0] || head).id, entryType: 'INSURER',
      insurerCode: 'INS-MGIC', insurerName: 'Mabuhay General Insurance Corp.', payorName: 'Mabuhay General Insurance Corp.',
      tenderType: 'CHECK', currency: 'PHP', bankAccount: bank, amount: 11200, vat: 1200, wtax: 0, checkNo: '0045871',
      checkDate: back.toISOString().slice(0, 10), checkBank: 'BDO Unibank', remarks: 'Risk management fee of September',
      accounts: [],
    });
    S.sb2 = rec.id;
    return rec.id;
  }

  // ---------------------------------------------------------------- SB-03 Bills Payment file
  async function sb3File() {
    if (S.sb3) return S.sb3;
    const companyId = company();
    const content = await ctx.api('cashier', 'POST',
      `/cashiering/payment-files/simulator?companyId=${companyId}&fileType=BILLS_PAYMENT&rows=4`);
    const name = `BDOI${new Date().toISOString().slice(0, 10).replace(/-/g, '')}1.txt`;
    const file = path.join(os.tmpdir(), name);
    fs.writeFileSync(file, typeof content === 'string' ? content : JSON.stringify(content));
    S.sb3 = file;
    return file;
  }

  // ---------------------------------------------------------------- SB-04 refund
  const refundItem = () => ctx.one("select id from csh_unapplied where stage = 'UNAPPLIED' and origin in ('NO_MATCH', 'EXCESS') and balance > 0 order by balance desc, id limit 1");

  // ---------------------------------------------------------------- SB-05 cancellation
  async function sb5Receipt() {
    if (S.sb5) return S.sb5;
    S.sb5 = Number(ctx.one("select r.id from csh_receipt r where r.receipt_no like 'AR-HO-%' and r.status = 'ISSUED' and not exists (select 1 from csh_receipt_record x where x.receipt_id = r.id) order by r.id desc limit 1"));
    return S.sb5;
  }
  async function sb5Record() {
    if (S.sb5rec) return S.sb5rec;
    const receiptId = await sb5Receipt();
    const reason = ctx.one("select code from lov_value where type_code = 'RECEIPT_CANCEL_REASON' and label ilike '%double%' order by sort_order limit 1");
    const made = await ctx.api('cashier', 'POST', '/cashiering/records/cancellations', {
      companyId: company(), receiptIds: [receiptId], reasonCode: reason, reasonText: 'AR issued twice for the same payment',
    });
    await ctx.api('cashier', 'POST', `/cashiering/records/${made[0].id}/submit`);
    S.sb5rec = made[0].id;
    return made[0].id;
  }

  const sb = [
    { file: 'sb1-01', run: async () => open('cashier', `/cashiering/records/${await sb1Record()}/edit`) },
    { file: 'sb1-02', run: async () => {
      const id = await sb1Record();
      await ctx.api('cashier', 'POST', `/cashiering/records/${id}/submit`);
      return open('cashier', `/cashiering/records/${id}`);
    } },
    { file: 'sb1-03', run: async () => open('cashtl', '/cashiering/posting') },
    { file: 'sb1-04', run: async () => {
      const page = await open('cashtl', `/cashiering/records/${await sb1Record()}`);
      await page.getByRole('button', { name: /^post$/i }).first().click();
      await ctx.settle(page, 600);
      S.sb1Page = page;
      return page;
    } },
    { file: 'sb1-05', run: async () => {
      const page = S.sb1Page;
      await confirm(page, 'Yes');
      await page.reload();
      await ctx.settle(page, 1200);
      return page;
    } },
    { file: 'sb1-06', run: async () => {
      const no = ctx.one(`select receipt_no from csh_receipt_record where id = ${await sb1Record()}`);
      return open('cashier', `/cashiering/receipts/${ctx.one(`select id from csh_receipt where receipt_no = '${no}'`)}`);
    } },
    { file: 'sb1-07', run: async () => open('cashier', '/cashiering/print') },

    { file: 'sb2-01', run: async () => open('cashier', `/cashiering/records/${await sb2Record()}/edit`) },
    { file: 'sb2-02', run: async () => {
      const id = await sb2Record();
      await ctx.api('cashier', 'POST', `/cashiering/records/${id}/submit`);
      await ctx.api('cashtl', 'POST', '/cashiering/records/post', { ids: [id] });
      return open('cashtl', `/cashiering/records/${id}`);
    } },
    { file: 'sb2-03', run: async () => {
      const no = ctx.one(`select receipt_no from csh_receipt_record where id = ${await sb2Record()}`);
      const page = await open('cashier', `/cashiering/receipts/${ctx.one(`select id from csh_receipt where receipt_no = '${no}'`)}`);
      await page.getByText(no).first().waitFor({ timeout: 20000 });
      await ctx.settle(page, 1500);
      return page;
    } },

    { file: 'sb3-01', run: async () => {
      const file = await sb3File();
      const page = await open('cashier', '/cashiering/payment-files');
      await page.locator('input[type=file]').first().setInputFiles(file);
      await ctx.settle(page, 600);
      S.sb3Page = page;
      return page;
    } },
    { file: 'sb3-02', run: async () => {
      const page = S.sb3Page;
      await page.getByRole('button', { name: /^upload$/i }).first().click();
      await ctx.settle(page, 4000);
      await page.reload();
      await ctx.settle(page, 1500);
      return page;
    } },
    { file: 'sb3-03', run: async () => {
      const page = await open('cashier', '/cashiering/payment-files');
      await page.locator('main table tbody tr').first().getByRole('button', { name: /^Actions for/ }).click();
      await page.getByRole('menuitem', { name: /^run report$/i }).first().click();
      await ctx.settle(page, 1200);
      return page;
    } },
    { file: 'sb3-04', run: async () => open('cashier', '/cashiering/unapplied') },

    { file: 'sb4-01', run: async () => {
      S.sb4 = refundItem();
      const page = await open('cashier', `/cashiering/unapplied/${S.sb4}`);
      await ctx.runSteps(page, recipe.fills.disposition_refund, ctx);
      return page;
    } },
    { file: 'sb4-02', run: async () => {
      const bal = Number(ctx.one(`select balance from csh_unapplied where id = ${S.sb4}`));
      const open1 = ctx.sql(`select id from csh_disposition where unapplied_id = ${S.sb4} and status in ('MONITORING','FOR_APPROVAL')`);
      if (open1.length === 0) {
        await ctx.api('cashier', 'POST', `/cashiering/unapplied/${S.sb4}/disposition`, {
          dispositionType: 'REFUND', amount: bal, payeeName: 'Carmela R. Bautista',
          remarks: 'Client paid twice; refund of the second payment',
        });
      }
      await ctx.api('cashier', 'POST', `/cashiering/unapplied/${S.sb4}/submit`).catch(() => {});
      return open('cashier', `/cashiering/unapplied/${S.sb4}`);
    } },
    { file: 'sb4-03', run: async () => {
      await ctx.api('cashtl', 'POST', `/cashiering/unapplied/${S.sb4}/approve`);
      return open('cashtl', `/cashiering/unapplied/${S.sb4}`);
    } },
    { file: 'sb4-04', run: async () => open('disb', '/operations/disbursements') },

    { file: 'sb5-01', run: async () => {
      const page = await open('cashier', `/cashiering/receipts/${await sb5Receipt()}`);
      await page.getByRole('button', { name: /^cancel receipt$/i }).first().click();
      await ctx.settle(page, 600);
      const sel = dialog(page).locator('select').first();
      const options = await sel.locator('option').allTextContents();
      const hit = options.find((o) => /double/i.test(o));
      if (hit) await sel.selectOption({ label: hit });
      await ctx.settle(page, 300);
      return page;
    } },
    { file: 'sb5-02', run: async () => { await sb5Record(); return open('cashtl', '/cashiering/posting?kind=CANCELLATION'); } },
    { file: 'sb5-03', run: async () => {
      const id = await sb5Record();
      await ctx.api('cashtl', 'POST', '/cashiering/records/post', { ids: [id] });
      return open('cashtl', `/cashiering/records/${id}`);
    } },
    { file: 'sb5-04', run: async () => open('cashier', `/cashiering/receipts/${await sb5Receipt()}`) },
  ];

  /** Brings a page to a screen state of the manifest: the recipe's own way for that state, else open the record,
   * tab, fill, select, click, then the recipe's finishing touch. */
  const OWN = new Set(['scr-op-08-01-page', 'scr-op-68-01-files', 'scr-op-68-02-report']);
  const opens = {
    ...recipe.opens,
    'open_report:daily_cash': () => '/reports/CSH-DAILY-CASH-REC',
  };
  const screen = (shot) => ({
    file: shot.slug,
    run: async () => {
      const custom = recipe.custom[shot.pack_slug];
      if (custom && !OWN.has(shot.pack_slug)) {
        const page = await custom(ctx);
        const after = recipe.after[shot.pack_slug];
        if (after) await after(page, ctx);
        return page;
      }
      const page = await ctx.pageOf(shot.user);
      const openKey = Object.keys(shot).find((k) => k.startsWith('open_'));
      let target = shot.path || shot.route;
      if (openKey) {
        const resolver = opens[`${openKey}:${shot[openKey]}`];
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
      if (shot.submit) await ctx.clickButton(page, shot.submit);
      const after = recipe.after[shot.pack_slug];
      if (after) await after(page, ctx);
      return page;
    },
  });

  const manifest = JSON.parse(fs.readFileSync(process.env.MANIFEST, 'utf8'));
  return [...sb, ...manifest.map(screen)];
};
