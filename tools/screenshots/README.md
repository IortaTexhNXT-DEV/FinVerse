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
