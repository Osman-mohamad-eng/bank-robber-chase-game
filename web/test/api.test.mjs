import { afterEach, test } from 'node:test';
import assert from 'node:assert/strict';
import { api, ApiError, pageQuery } from '../.test-build/api.js';

const originalFetch = globalThis.fetch;
afterEach(() => { globalThis.fetch = originalFetch; });

test('pagination includes the backend level parameter only when selected', () => {
  assert.equal(pageQuery(20), 'limit=10&offset=20');
  assert.equal(pageQuery(0, 3), 'limit=10&offset=0&level=3');
});

test('profile requests encode the player identifier and bypass stale HTTP caches', async () => {
  let requested;
  globalThis.fetch = async (url, options) => {
    requested = { url, options };
    return Response.json({ username: 'Vault runner' });
  };
  const stats = await api.stats('player/other?secret=true');
  assert.equal(stats.username, 'Vault runner');
  assert.equal(requested.url, '/api/v1/players/player%2Fother%3Fsecret%3Dtrue/stats');
  assert.equal(requested.options.cache, 'no-store');
  assert.equal(requested.options.headers.Accept, 'application/json');
  assert.ok(requested.options.signal instanceof AbortSignal);
});

test('history, quests, and all three boards use the shared API contract', async () => {
  const urls = [];
  globalThis.fetch = async url => { urls.push(url); return Response.json({ items: [] }); };
  await api.history('player', 30);
  await api.quests('player');
  await api.leaderboard('score');
  await api.leaderboard('money', 10, 2);
  await api.leaderboard('time', 0, 5);
  assert.deepEqual(urls, [
    '/api/v1/players/player/history?limit=10&offset=30',
    '/api/v1/players/player/quests',
    '/api/v1/leaderboards/score?limit=10&offset=0',
    '/api/v1/leaderboards/money?limit=10&offset=10&level=2',
    '/api/v1/leaderboards/time?limit=10&offset=0&level=5',
  ]);
});

test('404 responses show an actionable profile error', async () => {
  globalThis.fetch = async () => new Response('internal diagnostics', { status: 404 });
  await assert.rejects(api.stats('missing'), error => error instanceof ApiError
    && error.status === 404 && /Check the UUID/.test(error.message));
});

test('server errors do not display private response bodies', async () => {
  globalThis.fetch = async () => new Response('jdbc:postgresql://private-host secret-password', { status: 500 });
  await assert.rejects(api.leaderboard('score'), error => error instanceof ApiError
    && error.status === 500 && !/private-host|secret-password|jdbc/.test(error.message));
});

test('network failures become a recoverable message', async () => {
  globalThis.fetch = async () => { throw new TypeError('Failed to fetch'); };
  await assert.rejects(api.quests('player'), error => error instanceof ApiError
    && error.status === 0 && /Check your connection/.test(error.message));
});

test('requests are bounded by a timeout', async context => {
  context.mock.timers.enable({ apis: ['setTimeout'] });
  globalThis.fetch = async (_url, options) => new Promise((_resolve, reject) => {
    options.signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')), { once: true });
  });
  const pending = api.stats('player');
  context.mock.timers.tick(10_000);
  await assert.rejects(pending, error => error instanceof ApiError && /took too long/.test(error.message));
  context.mock.timers.reset();
});
