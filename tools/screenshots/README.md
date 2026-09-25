# Screenshot refresh

`capture.cjs` keeps `docs/design/screenshots/` current. On every run it:

1. deletes every existing image;
2. captures each screen listed in `screens.cjs`, numbering the files in list order;
3. rewrites `docs/design/screenshots/README.md`.

Because the folder is cleared first, it never keeps outdated screens or duplicates.

Run it after every change that affects screens. Commit the refreshed folder in the same commit as the change.

## Steps

1. **Add new screens to the manifest.** Put each one at its sidebar position in `screens.cjs`. The fields are `slug`, `title`, the demo `user` and `path`, plus optional `open: 'first'` or `click`.
2. **Start the demo.**
   - Backend: `java -jar backend/target/brokerverse-backend.jar --spring.profiles.active=demo`, with a fresh database.
   - Frontend: `npm run dev` in `frontend`.
   - Open the app at `http://localhost:...`, not `127.0.0.1`. The origin check rejects `127.0.0.1`.
3. **Capture.** Run this from the repository root:
   ```
   BASE=http://localhost:5173 \
   PLAYWRIGHT_MODULE=/opt/node22/lib/node_modules/playwright \
   CHROMIUM=/opt/pw-browsers/chromium-1194/chrome-linux/chrome \
   node tools/screenshots/capture.cjs
   ```
   The two variables `PLAYWRIGHT_MODULE` and `CHROMIUM` are needed only when Playwright is not installed locally.

The script exits with code 1 and lists the screens it could not capture, for example after a route has been renamed. Fix the manifest and run it again.
