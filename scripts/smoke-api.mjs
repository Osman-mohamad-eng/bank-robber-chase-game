import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
import { readFile, writeFile } from 'node:fs/promises';

// Exercises a running API without deleting or depending on existing player data.
const base = (process.env.API_URL ?? 'http://localhost:8080').replace(/\/$/, '') + '/api/v1';
async function request(path, { method = 'GET', body, expected = 200 } = {}) {
  const response = await fetch(base + path, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : {},
    body: body ? JSON.stringify(body) : undefined,
    signal: AbortSignal.timeout(10_000),
  });
  const data = await response.json();
  assert.equal(response.status, expected, `${method} ${path}: ${JSON.stringify(data)}`);
  return data;
}

if (process.env.SMOKE_VERIFY_STATE) {
  const saved = JSON.parse(await readFile(process.env.SMOKE_VERIFY_STATE, 'utf8'));
  const persisted = await request(`/players/${saved.playerId}/stats`);
  assert.equal(persisted.gamesPlayed, 2);
  assert.equal(persisted.totalCashCollected, 2300);
  assert.equal(persisted.rewardPoints, 425);
  console.log('Previously accepted sessions and rewards survived service restart.');
  process.exit(0);
}

const playerId = randomUUID();
const username = `Smoke_${playerId.slice(0, 8)}`;
await request('/players', { method: 'POST', body: { playerId, username }, expected: 201 });
await request('/players', { method: 'POST', body: { playerId, username } });
const result = {
  sessionId: randomUUID(), playerId, level: 1, cashCollected: 2300, diamondsCollected: 3,
  penaltiesIncurred: 150, finalScore: 3650, completionTimeMs: 45_000,
  outcome: 'WON', completedAt: new Date().toISOString(),
};
await request('/heists', { method: 'POST', body: result, expected: 201 });
const beforeRetry = await request(`/players/${playerId}/quests`);
await request('/heists', { method: 'POST', body: result });
await request('/heists', { method: 'POST', body: { ...result, finalScore: 3651 }, expected: 409 });
const afterRetry = await request(`/players/${playerId}/quests`);
assert.deepEqual(afterRetry, beforeRetry, 'Duplicate attempts must not change quest progress or rewards');
assert.equal(afterRetry.rewardPoints, 425);
const loss = {
  ...result, sessionId: randomUUID(), cashCollected: 0, diamondsCollected: 0,
  penaltiesIncurred: 150, finalScore: -150, completionTimeMs: 1000, outcome: 'LOST',
};
await request('/heists', { method: 'POST', body: loss, expected: 201 });
const stats = await request(`/players/${playerId}/stats`);
assert.equal(stats.gamesPlayed, 2);
assert.equal(stats.wins, 1);
assert.equal(stats.losses, 1);
assert.equal(stats.totalCashCollected, 2300);
assert.equal(stats.totalDiamondsCollected, 3);
assert.equal(stats.fastestCompletionTimeMs, 45_000);
assert.equal(stats.averageCompletionTimeMs, 45_000);
const history = await request(`/players/${playerId}/history?limit=1&offset=1`);
assert.equal(history.total, 2);
assert.equal(history.items.length, 1);
assert.equal((await request(`/heists/${result.sessionId}`)).finalScore, 3650);
for (const metric of ['score', 'money', 'time']) {
  const board = await request(`/leaderboards/${metric}?level=1&limit=100`);
  assert.equal(new Set(board.items.map(item => item.playerId)).size, board.items.length);
  assert.ok(board.items.every(item => item.level === 1));
  if (metric === 'time') assert.ok(board.items.every(item => item.outcome === 'WON'));
  const field = metric === 'score' ? 'finalScore' : metric === 'money' ? 'cashCollected' : 'completionTimeMs';
  for (let i = 1; i < board.items.length; i++) {
    const previous = board.items[i - 1][field], current = board.items[i][field];
    assert.ok(metric === 'time' ? previous <= current : previous >= current);
  }
}
console.log(`API smoke checks passed. Created player ${username} (${playerId}).`);
if (process.env.SMOKE_SAVE_STATE) {
  await writeFile(process.env.SMOKE_SAVE_STATE, JSON.stringify({ playerId, username }));
}
