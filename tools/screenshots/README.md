# Screenshot refresh

`capture.cjs` keeps `docs/design/screenshots/` current. On every run it:

1. deletes every existing image;
2. captures each screen listed in `screens.cjs`, numbering the files in list order;
3. rewrites `docs/design/screenshots/README.md`.

Because the folder is cleared first, it never keeps outdated screens or duplicates.

Run it after every change that affects screens. Commit the refreshed folder in the same commit as the change.

## Steps

1. **Add new screens to the manifest.** Put each one at its sidebar position in `screens.cjs`. The fields are `slug`, `title`, the seed `user` and `path`, plus optional `open: 'first'` or `click`.
2. **Start the seed profile.**
   - Backend: `java -jar backend/target/brokerverse-backend.jar --spring.profiles.active=seed`, with a fresh database
     and a local signing key in the environment (`BROKERVERSE_JWT_SECRET`, any value of 32 or more characters; the
     seed profile has no default), and `BROKERVERSE_SEED_PASSWORD` set to the SIT/UAT password (the same value as
     `SEED_PASSWORD` below; it is never written into a file). The SIT/UAT seed data switches the second factor off
     (`MFA_POLICY` = `OFF`).
   - Frontend: `npm run dev` in `frontend`.
   - Open the app at `http://localhost:...`, not `127.0.0.1`. The backend accepts only the origins in
     `BROKERVERSE_ALLOWED_ORIGINS`, which defaults to `http://localhost:5173`. If Vite runs on another port, start the
     backend with that origin, for example `BROKERVERSE_ALLOWED_ORIGINS=http://localhost:5195`, and point Vite at the
     backend with `BROKERVERSE_API_URL`.
3. **Capture.** Run this from the project root, with `SEED_PASSWORD` set to the password of the SIT/UAT users:
   ```
   SEED_PASSWORD=... BASE=http://localhost:5173 API=http://localhost:8080 \
   PLAYWRIGHT_MODULE=/opt/node22/lib/node_modules/playwright \
   CHROMIUM=/opt/pw-browsers/chromium-1194/chrome-linux/chrome \
   node tools/screenshots/capture.cjs
   ```
   The two variables `PLAYWRIGHT_MODULE` and `CHROMIUM` are needed only when Playwright is not installed locally.

The script waits until `API/actuator/health/readiness` reports UP. The seed start-up runners (booking, ledger replay, Operations seed data) run after "Started" is logged, so capturing earlier would show empty Operations screens.

The script exits with code 1 and lists the screens it could not capture, for example after a route has been renamed. Fix the manifest and run it again.

## Business sign-off pack screenshots

`capture_pack.cjs` captures the screenshots of a BRD sign-off pack (`docs/deliverables/src/<BRD-nn_Name>/pack/`): every
screen state of the pack (list, empty form, filled form, validation error, record per status, approval, dialog),
the steps of the end-to-end walkthroughs, the upload error-file flow and the first page of each generated document.
The shots come from the pack itself (`signoff_pack.py --manifest`), so the FRS, the sign-off workbook and the images
name the same screens and fields. Numbered callout badges are drawn in an overlay on the page before the capture;
each badge number is the No. of the field in the screen's field table. The images are saved as optimised PNG in
the pack's screenshot folder (`meta.screenshot_dir`, for BRD-01 `docs/deliverables/src/BRD-01_New_Business/screenshots`).

How each state is reached (the record to open for a status, the values typed, the files uploaded, the walkthrough
steps and the document downloads) is in the recipe of the BRD, `packs/brdNN.cjs`. The walkthroughs create records,
so capture on a fresh seed database, after readiness is UP:

```
SEED_PASSWORD=... BASE=http://localhost:5195 API=http://localhost:8095 \
PGHOST=localhost PGDATABASE=<seed database> PGUSER=... PGPASSWORD=... \
STORAGE_ROOT=<BROKERVERSE_STORAGE_LOCAL_ROOT of the backend> \
PLAYWRIGHT_MODULE=/opt/node22/lib/node_modules/playwright CHROMIUM=/opt/pw-browsers/chromium-1194/chrome-linux/chrome \
node tools/screenshots/capture_pack.cjs brd01 [slug-regex]
```

**Sharp, cropped images.** Pages are rendered at device scale factor 2 (`SCALE`, default 2) in a 1440 x 900 window
(`WIDTH`, `HEIGHT`), so every image holds two pixels per screen pixel. Each shot is cropped to what it is about, with
a 12-pixel margin: the open dialog, otherwise the content area without the menu and the header (trimmed to what is
drawn), or the element the recipe names in `crops` (a Playwright selector; `'full'` keeps the whole window where the
menu gives the navigation context, `'main'` or `'dialog'` force those crops). A walkthrough step shows the top of its
page (the record header, the stepper and the message of the step), unless the recipe names a region for it. The crop
kind is stored in the PNG (text `bibs-crop`); the Word builder puts `full` shots on landscape pages. Callout badges are
26 pixels with bold 15-pixel digits, so they stay legible in print. Generated documents are rendered at 200 dpi
(`DOC_DPI`). The PNG keeps a 256-colour palette without dither for flat screens and full colour for photo-like images.

**Generated documents.** `packs/brd01_documents.cjs` `render` (with `doc_render.py`) turns a downloaded file into
its image. A PDF shows its first page. A workbook prints landscape at the width of the page, each column as wide as
its content: `keep` names the columns of a wide workbook to show, under the report header of the workbook (logo,
title, parameters; the logo keeps its own size), `options.rows` keeps an extract of the rows (the rows of one value
of a column, at most `max`), and `options.above: 'drop'` leaves out the guide band of a template. A text file is
drawn in a monospace font without wrapping; a file of delimited records is shown field by field. An image that is an
extract says so in its caption (`shot_caption` of documents.yaml).

**Order on a fresh database.** The walkthroughs change seed records that some screen states need, so each pack runs in
this order, BRD-01 first:

| Pack | Runs, in order |
|---|---|
| brd01 | `'^wt-(a|b)-|^wt-c-01$'`, then `'^(scr|doc)-'` (the KYC approval shot needs the client that step C-01 submits), then `'^wt-c-(0[2-9]|1)'` |
| brd03 | `'^wt-a-'`, then `'^(scr|doc)-'` (the set-up shot needs the seed request that walkthrough B returns), then `'^wt-b-'` |
| brd11 | `'^scr-ua-08-03'` (the seeded request waiting for its second approval, which walkthrough B approves), then `'^wt-'`, then the other `'^(scr|doc)-'` shots, then `'^ux-'` |

A run with a slug filter keeps the other images; a run without a filter deletes the images that are no longer in the
pack. Recipes exist for `brd01`, `brd03` and `brd11` (`packs/brdNN.cjs`). The BRD-11 recipe also signs in users that are
not seed personas (the user enrolled in walkthrough A, a user whose password the administrator set), locks a seed
subject user through failed sign-ins, and moves the browser clock (Playwright `page.clock`) to show the inactivity
warning; its walkthroughs run in order (A before B, C and D).

The script needs `psql` (read-only queries that find the records), LibreOffice and `pdftoppm` (document pages) and
Python with Pillow (PNG optimisation). It lists the shots it could not take and exits with code 1; the page at the
moment of a failure is saved as `capture-pack-failed-<slug>-<user>.png` in the temporary folder.
