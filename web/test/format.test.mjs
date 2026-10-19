import { test } from 'node:test';
import assert from 'node:assert/strict';
import { duration, escapeHtml, isUuid, money, number, pageLabel } from '../.test-build/format.js';

test('active times retain millisecond precision and round average times', () => {
  assert.equal(duration(null), '—');
  assert.equal(duration(0), '0:00.000');
  assert.equal(duration(59_999), '0:59.999');
  assert.equal(duration(60_000), '1:00.000');
  assert.equal(duration(3_661_001), '61:01.001');
  assert.equal(duration(59_999.8), '1:00.000');
});

test('signed scores stay signed while cash uses game currency formatting', () => {
  assert.equal(number(-150), '-150');
  assert.equal(number(12_000), '12,000');
  assert.equal(money(12_000), '$12,000');
});

test('player UUID validation accepts canonical identifiers and rejects paths or partial values', () => {
  assert.equal(isUuid('50c03d46-b595-4eb5-97b4-b9bd127c21ea'), true);
  assert.equal(isUuid('50C03D46-B595-4EB5-97B4-B9BD127C21EA'), true);
  assert.equal(isUuid('player'), false);
  assert.equal(isUuid('../players/anything'), false);
  assert.equal(isUuid('50c03d46-b595-4eb5-97b4-b9bd127c21ea?other=true'), false);
});

test('untrusted usernames and descriptions are escaped before insertion into HTML', () => {
  assert.equal(escapeHtml('<img src=x onerror="alert(1)"> & \'test\''), '&lt;img src=x onerror=&quot;alert(1)&quot;&gt; &amp; &#39;test&#39;');
});

test('pagination reports real totals, including an empty dataset', () => {
  assert.equal(pageLabel({ items: [], offset: 0, total: 0 }), '0 results');
  assert.equal(pageLabel({ items: [], offset: 20, total: 12 }), '0 on this page · 12 total');
  assert.equal(pageLabel({ items: [{}, {}], offset: 10, total: 12 }), '11–12 of 12');
});
