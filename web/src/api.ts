import type { Heist, LeaderboardEntry, LeaderboardMetric, PlayerQuests, PlayerStatistics, ResultPage } from './types.js';

const API_ROOT = '/api/v1';
export const PAGE_SIZE = 10;

export class ApiError extends Error {
  constructor(public readonly status: number, message: string) {
    super(message);
    this.name = 'ApiError';
  }
}

export function pageQuery(offset = 0, level?: number): string {
  const query = new URLSearchParams({ limit: String(PAGE_SIZE), offset: String(offset) });
  if (level !== undefined) query.set('level', String(level));
  return query.toString();
}

async function get<T>(path: string): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 10_000);
  try {
    const response = await fetch(`${API_ROOT}${path}`, {
      headers: { Accept: 'application/json' },
      signal: controller.signal,
      cache: 'no-store',
    });
    if (!response.ok) {
      // Server diagnostic bodies may contain implementation details. Show a bounded public message.
      throw new ApiError(response.status, response.status === 404
        ? 'This player could not be found. Check the UUID and try again.'
        : 'The game service could not complete this request. Please try again.');
    }
    return await response.json() as T;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    throw new ApiError(0, controller.signal.aborted
      ? 'The game service took too long to respond. Please try again.'
      : 'Cannot reach the game service. Check your connection and try again.');
  } finally {
    clearTimeout(timeout);
  }
}

export const api = {
  stats: (playerId: string) => get<PlayerStatistics>(`/players/${encodeURIComponent(playerId)}/stats`),
  history: (playerId: string, offset = 0) => get<ResultPage<Heist>>(`/players/${encodeURIComponent(playerId)}/history?${pageQuery(offset)}`),
  quests: (playerId: string) => get<PlayerQuests>(`/players/${encodeURIComponent(playerId)}/quests`),
  leaderboard: (metric: LeaderboardMetric, offset = 0, level?: number) =>
    get<ResultPage<LeaderboardEntry>>(`/leaderboards/${metric}?${pageQuery(offset, level)}`),
};
