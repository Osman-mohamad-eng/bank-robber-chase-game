export type Outcome = 'WON' | 'LOST';
export type LeaderboardMetric = 'score' | 'money' | 'time';

export interface ResultPage<T> {
  items: T[];
  limit: number;
  offset: number;
  total: number;
}

export interface PlayerStatistics {
  playerId: string;
  username: string;
  gamesPlayed: number;
  wins: number;
  losses: number;
  winRate: number;
  totalCashCollected: number;
  totalDiamondsCollected: number;
  highestScore: number;
  highestCash: number;
  fastestCompletionTimeMs: number | null;
  averageCompletionTimeMs: number | null;
  rewardPoints: number;
}

export interface Heist {
  sessionId: string;
  playerId: string;
  username: string;
  level: number;
  cashCollected: number;
  diamondsCollected: number;
  penaltiesIncurred: number;
  finalScore: number;
  completionTimeMs: number;
  outcome: Outcome;
  completedAt: string;
  receivedAt: string;
}

export interface LeaderboardEntry {
  rank: number;
  playerId: string;
  username: string;
  sessionId: string;
  level: number;
  cashCollected: number;
  diamondsCollected: number;
  finalScore: number;
  completionTimeMs: number;
  outcome: Outcome;
  completedAt: string;
}

export interface Quest {
  id: number;
  code: string;
  title: string;
  description: string;
  metric: string;
  target: number;
  rewardPoints: number;
  progress: number;
  completed: boolean;
  completedAt: string | null;
  qualifyingTimeMs?: number | null;
}

export interface PlayerQuests {
  playerId: string;
  rewardPoints: number;
  items: Quest[];
}
