# Player headquarters

A responsive TypeScript dashboard for the Bank Heist Spring Boot API. It shows player statistics, personal bests, completed heists, quest progress, and three global leaderboards. It does not run the Swing game or calculate scores, rankings, or quest rewards.

Use Node.js 24 LTS and npm:

```sh
cd web
npm install
npm run dev
```

Open `http://localhost:5173`. Vite proxies `/api` to `http://localhost:8080`; start the backend first. A player can be selected with the UUID from the desktop services window, a leaderboard entry, or a shareable `?player=<uuid>` URL. The last successfully loaded player is remembered on this browser. This is player selection, not authentication.

```sh
npm run typecheck
npm test
npm run build
```

The tests use Node's built-in test runner. `npm run build` runs type checking and tests before producing `dist/`. Development dependencies are pinned in `package.json`; generate and retain the npm lockfile after a successful install.

The container builds the app and serves it with unprivileged nginx on port 8080. nginx forwards `/api` to the Compose service `api:8080` and exposes `/healthz` for container health checks. The browser uses the same API origin in development and Compose, so a separate browser API URL or CORS allowance is unnecessary.

The generated TeaVM browser demo under `../docs/` is a separate legacy artifact and remains unchanged.
