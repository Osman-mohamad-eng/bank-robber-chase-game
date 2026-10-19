import './style.css';
import { api, ApiError } from './api.js';
import { dateTime, duration, escapeHtml as html, isUuid, money, number, pageLabel } from './format.js';
import type { Heist, LeaderboardEntry, LeaderboardMetric, PlayerQuests, PlayerStatistics, ResultPage } from './types.js';

function element<T extends HTMLElement = HTMLElement>(id: string): T {
  const found = document.getElementById(id);
  if (!found) throw new Error(`Missing dashboard element: ${id}`);
  return found as T;
}

const STORAGE_KEY = 'bank-heist.player-id';
const input = element<HTMLInputElement>('player-input');
const profileMessage = element('profile-message');
const statsContainer = element('stats');
const questsContainer = element('quest-content');
const historyContainer = element('history-content');
const leaderboardContainer = element('leaderboard-content');
const levelFilter = element<HTMLSelectElement>('level-filter');
let playerId: string | null = null;
let profileRevision = 0;
let historyRevision = 0;
let boardRevision = 0;
let boardOffset = 0;
let selectedMetric: LeaderboardMetric = 'score';
let selectedLevel: number | undefined;

const publicError = (error: unknown): string => error instanceof ApiError
  ? error.message : 'This record could not be loaded. Please refresh and try again.';

function state(container: HTMLElement, text: string, isError = false): void {
  container.innerHTML = `<div class="empty-panel${isError ? ' error-panel' : ''}" role="${isError ? 'alert' : 'status'}">${html(text)}</div>`;
  container.setAttribute('aria-busy', 'false');
}

function loading(container: HTMLElement, text: string): void {
  state(container, text);
  container.setAttribute('aria-busy', 'true');
}

function updated(): void {
  element('updated-at').textContent = `Last updated ${new Intl.DateTimeFormat(undefined, {
    hour: 'numeric', minute: '2-digit', second: '2-digit',
  }).format(new Date())}`;
}

function renderStats(stats: PlayerStatistics | null): void {
  const cards = [
    { label: 'LIFETIME CASH', value: stats ? money(stats.totalCashCollected) : '—', detail: 'Gross cash collected', icon: '$', style: 'cash-card' },
    { label: 'HEISTS COMPLETED', value: stats ? number(stats.gamesPlayed) : '—', detail: stats ? `${number(stats.wins)} wins · ${number(stats.losses)} losses` : 'Every completed attempt', icon: '↗', style: '' },
    { label: 'WIN RATE', value: stats ? `${stats.winRate.toFixed(1)}%` : '—', detail: 'Successful escapes', icon: '◎', style: '' },
    { label: 'DIAMONDS COLLECTED', value: stats ? number(stats.totalDiamondsCollected) : '—', detail: stats ? `${number(stats.rewardPoints)} quest reward points` : 'A little extra shine', icon: '◇', style: '' },
  ];
  statsContainer.innerHTML = cards.map(card => `<article class="stat-card ${card.style}"><div class="stat-top"><span>${card.label}</span><span class="stat-icon" aria-hidden="true">${card.icon}</span></div><strong>${html(card.value)}</strong><span class="stat-detail">${html(card.detail)}</span></article>`).join('');
  const hasRuns = stats !== null && stats.gamesPlayed > 0;
  const bests = [
    ['BEST SCORE', hasRuns ? number(stats.highestScore) : '—'],
    ['BIGGEST CASH HAUL', hasRuns ? money(stats.highestCash) : '—'],
    ['FASTEST ESCAPE', duration(stats?.fastestCompletionTimeMs ?? null)],
    ['AVERAGE WIN TIME', duration(stats?.averageCompletionTimeMs ?? null)],
  ];
  element('personal-bests').innerHTML = bests.map(([label, value]) => `<div><span>${label}</span><strong>${html(value ?? '—')}</strong></div>`).join('');
}

function renderQuests(quests: PlayerQuests): void {
  questsContainer.setAttribute('aria-busy', 'false');
  const completed = quests.items.filter(quest => quest.completed).length;
  element('quest-count').textContent = `${completed}/${quests.items.length} COMPLETE · ${number(quests.rewardPoints)} POINTS`;
  if (quests.items.length === 0) {
    state(questsContainer, 'No quests are available yet. Your next challenge is on its way.');
    return;
  }
  questsContainer.innerHTML = quests.items.map((quest, index) => {
    const progress = Math.min(quest.target, Math.max(0, quest.progress));
    return `<article class="quest-card${quest.completed ? ' quest-completed' : ''}">
      <div class="quest-top"><span class="quest-number">${String(index + 1).padStart(2, '0')}</span><span class="reward-tag">+${number(quest.rewardPoints)} PTS</span></div>
      <h3>${html(quest.title)}</h3><p>${html(quest.description)}</p>
      <div class="quest-progress-label"><label for="quest-${quest.id}">${quest.completed ? '✓ Complete' : 'In progress'}</label><span>${number(progress)} / ${number(quest.target)}</span></div>
      <progress id="quest-${quest.id}" value="${progress}" max="${quest.target}">${number(progress)} / ${number(quest.target)}</progress>
    </article>`;
  }).join('');
}

function pagination<T>(container: HTMLElement, page: ResultPage<T>, onPage: (offset: number) => void): void {
  container.innerHTML = `<span>${html(pageLabel(page))}</span><div><button class="page-button" type="button" data-page="previous" ${page.offset === 0 ? 'disabled' : ''}>← Previous</button><button class="page-button" type="button" data-page="next" ${page.offset + page.items.length >= page.total ? 'disabled' : ''}>Next →</button></div>`;
  container.querySelector('[data-page="previous"]')?.addEventListener('click', () => onPage(Math.max(0, page.offset - page.limit)));
  container.querySelector('[data-page="next"]')?.addEventListener('click', () => onPage(page.offset + page.limit));
}

const outcomeBadge = (outcome: string): string => `<span class="outcome ${outcome === 'WON' ? 'won' : 'lost'}">${outcome === 'WON' ? 'Escaped' : 'Lost'}</span>`;

function renderHistory(page: ResultPage<Heist>): void {
  historyContainer.setAttribute('aria-busy', 'false');
  if (page.items.length === 0) {
    state(historyContainer, page.total === 0 ? 'A clean slate. Finish a heist in the desktop game to start your record.' : 'No heists on this page. Return to the previous page.');
  } else {
    historyContainer.innerHTML = `<div class="table-scroll" tabindex="0" role="region" aria-label="Recent heist results"><table class="history-table"><caption class="sr-only">Completed heists, most recent first. Times show active gameplay.</caption><thead><tr><th>Outcome</th><th>Level</th><th>Final score</th><th>Gross cash</th><th>Diamonds</th><th>Penalties</th><th>Active time</th><th>Completed</th></tr></thead><tbody>${page.items.map(heist => `<tr><td>${outcomeBadge(heist.outcome)}</td><td><span class="level-chip">L${heist.level}</span></td><td class="numeric score-cell">${number(heist.finalScore)}</td><td class="numeric">${money(heist.cashCollected)}</td><td class="numeric">${number(heist.diamondsCollected)}</td><td class="numeric">${money(heist.penaltiesIncurred)}</td><td class="numeric time-cell">${duration(heist.completionTimeMs)}</td><td class="date-cell"><time datetime="${html(heist.completedAt)}">${html(dateTime(heist.completedAt))}</time></td></tr>`).join('')}</tbody></table></div>`;
  }
  pagination(element('history-pagination'), page, offset => void loadHistory(offset));
}

async function loadHistory(offset = 0, expectedProfile = profileRevision): Promise<void> {
  if (!playerId) return;
  const revision = ++historyRevision;
  loading(historyContainer, 'Opening the heist log…');
  element('history-pagination').replaceChildren();
  try {
    const page = await api.history(playerId, offset);
    if (revision !== historyRevision || expectedProfile !== profileRevision) return;
    renderHistory(page);
  } catch (error) {
    if (revision === historyRevision && expectedProfile === profileRevision) state(historyContainer, publicError(error), true);
  }
}

async function loadProfile(id: string): Promise<void> {
  id = id.trim().toLowerCase();
  if (!isUuid(id)) {
    profileMessage.textContent = 'Enter a complete player UUID, such as the one shown in the desktop services window.';
    profileMessage.classList.add('error-message');
    input.setAttribute('aria-invalid', 'true');
    input.focus();
    return;
  }
  const revision = ++profileRevision;
  playerId = id;
  input.value = id;
  input.removeAttribute('aria-invalid');
  profileMessage.classList.remove('error-message');
  profileMessage.textContent = 'Loading player record…';
  element('player-name').textContent = 'Loading player…';
  element('player-id').textContent = id;
  element('avatar').textContent = '…';
  element('quest-count').textContent = 'QUEST PROGRESS';
  renderStats(null);
  loading(questsContainer, 'Checking the objectives…');
  const statsRequest = api.stats(id).then(stats => {
    if (revision !== profileRevision) return;
    element('player-name').textContent = stats.username;
    element('avatar').textContent = stats.username.slice(0, 2).toUpperCase();
    renderStats(stats);
    profileMessage.textContent = stats.gamesPlayed > 0 ? 'Player record loaded.' : 'Player found. Your first completed heist starts the story.';
    try { localStorage.setItem(STORAGE_KEY, id); } catch { /* Storage may be disabled; the URL still works. */ }
    const url = new URL(window.location.href);
    url.searchParams.set('player', id);
    window.history.replaceState(null, '', url);
    updated();
  }).catch(error => {
    if (revision !== profileRevision) return;
    element('player-name').textContent = 'Record unavailable';
    element('avatar').textContent = '?';
    profileMessage.textContent = publicError(error);
    profileMessage.classList.add('error-message');
  });
  const questRequest = api.quests(id).then(quests => {
    if (revision === profileRevision) renderQuests(quests);
  }).catch(error => {
    if (revision === profileRevision) state(questsContainer, publicError(error), true);
  });
  await Promise.allSettled([statsRequest, questRequest, loadHistory(0, revision)]);
}

const boardDescriptions: Record<LeaderboardMetric, string> = {
  score: "Ranked by each player's highest final score, including rewards and penalties.",
  money: "Ranked by each player's biggest gross cash haul in a single completed heist.",
  time: "Ranked by each player's fastest successful escape. Losses do not qualify.",
};

function renderLeaderboard(page: ResultPage<LeaderboardEntry>): void {
  leaderboardContainer.setAttribute('aria-busy', 'false');
  if (page.items.length === 0) {
    state(leaderboardContainer, page.total === 0 ? 'The vault is open. No qualifying heists yet — finish a run to set the pace.' : 'No rankings on this page. Return to the previous page.');
  } else {
    const metricTitle = selectedMetric === 'score' ? 'Final score' : selectedMetric === 'money' ? 'Gross cash' : 'Active time';
    const value = (entry: LeaderboardEntry): string => selectedMetric === 'score' ? number(entry.finalScore)
      : selectedMetric === 'money' ? money(entry.cashCollected) : duration(entry.completionTimeMs);
    leaderboardContainer.innerHTML = `<div class="table-scroll" tabindex="0" role="region" aria-label="Global rankings"><table class="leaderboard-table"><caption class="sr-only">${html(boardDescriptions[selectedMetric])} Select a player to view their record.</caption><thead><tr><th class="rank-cell">Rank</th><th>Player</th><th>${metricTitle}</th><th>Level</th><th class="secondary-column">Outcome</th><th class="secondary-column">Completed</th></tr></thead><tbody>${page.items.map(entry => `<tr${entry.playerId === playerId ? ' class="selected-player"' : ''}><td class="rank-cell"><span class="rank-number${entry.rank <= 3 ? ' podium' : ''}">${String(entry.rank).padStart(2, '0')}</span></td><td><button type="button" class="player-link" data-player="${html(entry.playerId)}" aria-label="View ${html(entry.username)}'s player record"><span class="mini-avatar" aria-hidden="true">${html(entry.username.slice(0, 1).toUpperCase())}</span><span>${html(entry.username)}<small>${entry.playerId === playerId ? 'YOUR SELECTED PLAYER' : 'View player record ↗'}</small></span></button></td><td class="numeric board-value">${value(entry)}</td><td><span class="level-chip">L${entry.level}</span></td><td class="secondary-column">${outcomeBadge(entry.outcome)}</td><td class="secondary-column date-cell"><time datetime="${html(entry.completedAt)}">${html(dateTime(entry.completedAt))}</time></td></tr>`).join('')}</tbody></table></div>`;
    leaderboardContainer.querySelectorAll<HTMLButtonElement>('[data-player]').forEach(button => {
      button.addEventListener('click', () => {
        const id = button.dataset['player'];
        if (!id) return;
        void loadProfile(id);
        element('overview').scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' });
        input.focus({ preventScroll: true });
      });
    });
  }
  pagination(element('leaderboard-pagination'), page, offset => void loadLeaderboard(offset));
}

async function loadLeaderboard(offset = 0): Promise<void> {
  const revision = ++boardRevision;
  boardOffset = offset;
  loading(leaderboardContainer, 'Finding the best in the business…');
  element('leaderboard-pagination').replaceChildren();
  try {
    const page = await api.leaderboard(selectedMetric, offset, selectedLevel);
    if (revision !== boardRevision) return;
    renderLeaderboard(page);
    updated();
  } catch (error) {
    if (revision === boardRevision) state(leaderboardContainer, publicError(error), true);
  }
}

function chooseMetric(metric: LeaderboardMetric): void {
  selectedMetric = metric;
  document.querySelectorAll<HTMLButtonElement>('[data-metric]').forEach(button => {
    const active = button.dataset['metric'] === metric;
    button.setAttribute('aria-selected', String(active));
    button.tabIndex = active ? 0 : -1;
  });
  leaderboardContainer.setAttribute('aria-labelledby', `tab-${metric}`);
  element('leaderboard-description').textContent = boardDescriptions[metric];
  void loadLeaderboard();
}

element<HTMLFormElement>('player-form').addEventListener('submit', event => {
  event.preventDefault();
  void loadProfile(input.value);
});

document.querySelectorAll<HTMLButtonElement>('[data-metric]').forEach(button => {
  button.addEventListener('click', () => chooseMetric(button.dataset['metric'] as LeaderboardMetric));
  button.addEventListener('keydown', event => {
    const metrics: LeaderboardMetric[] = ['score', 'money', 'time'];
    const current = metrics.indexOf(selectedMetric);
    const index = event.key === 'ArrowRight' ? (current + 1) % 3 : event.key === 'ArrowLeft' ? (current + 2) % 3
      : event.key === 'Home' ? 0 : event.key === 'End' ? 2 : null;
    if (index === null) return;
    event.preventDefault();
    const metric = metrics[index];
    if (metric) { chooseMetric(metric); element<HTMLButtonElement>(`tab-${metric}`).focus(); }
  });
});

levelFilter.addEventListener('change', () => {
  selectedLevel = levelFilter.value === '' ? undefined : Number(levelFilter.value);
  void loadLeaderboard();
});

element<HTMLButtonElement>('refresh').addEventListener('click', () => {
  if (playerId) void loadProfile(playerId);
  void loadLeaderboard(boardOffset);
});

window.addEventListener('popstate', () => {
  const id = new URL(window.location.href).searchParams.get('player');
  if (id) void loadProfile(id);
});

const navigation = [...document.querySelectorAll<HTMLAnchorElement>('.nav-link')];
const sectionObserver = new IntersectionObserver(entries => {
  const current = entries.find(entry => entry.isIntersecting);
  if (!current) return;
  navigation.forEach(link => {
    const active = link.hash === `#${current.target.id}`;
    link.classList.toggle('active', active);
    if (active) link.setAttribute('aria-current', 'location');
    else link.removeAttribute('aria-current');
  });
}, { rootMargin: '-10% 0px -65% 0px' });
['overview', 'quests', 'history', 'rankings'].forEach(id => sectionObserver.observe(element(id)));

renderStats(null);
let initialPlayer = new URL(window.location.href).searchParams.get('player');
if (!initialPlayer) {
  try { initialPlayer = localStorage.getItem(STORAGE_KEY); } catch { /* No persistence in private/blocked storage. */ }
}
if (initialPlayer) void loadProfile(initialPlayer);
void loadLeaderboard();
