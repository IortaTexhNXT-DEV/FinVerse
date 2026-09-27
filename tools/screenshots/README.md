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
   - Backend: `java -jar backend/target/brokerverse-backend.jar --spring.profiles.active=seed`, with a fresh database.
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

Recipes exist for `brd01`, `brd03` and `brd11` (`packs/brdNN.cjs`). The BRD-11 recipe also signs in users that are
not seed personas (the user enrolled in walkthrough A, a user whose password the administrator set), locks a seed
subject user through failed sign-ins, and moves the browser clock (Playwright `page.clock`) to show the inactivity
warning; its walkthroughs run in order (A before B, C and D).

The script needs `psql` (read-only queries that find the records), LibreOffice and `pdftoppm` (document pages) and
Python with Pillow (PNG optimisation). It lists the shots it could not take and exits with code 1; the page at the
moment of a failure is saved as `capture-pack-failed-<slug>-<user>.png` in the temporary folder.
