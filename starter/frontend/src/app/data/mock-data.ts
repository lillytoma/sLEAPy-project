export interface MarketStock {
  symbol: string;
  name: string;
  chg: number;
  price: number;
  vol: string;
  relVol: string;
  mktCap: string;
  pe: string;
  eps: number;
  epsGrowth: number;
  divYield: string;
  sector: string;
  rating: string;
}

export interface WatchlistStock {
  symbol: string;
  name: string;
  price: number;
  change: number;
  high52: number;
  low52: number;
  mktCap: string;
}

export interface Holding {
  symbol: string;
  name: string;
  shares: number;
  avgCost: number;
  current: number;
  sector: string;
}

export interface PortfolioPoint {
  date: string;
  value: number;
}

export interface TransactionItem {
  symbol: string;
  type: 'BUY' | 'SELL';
  shares: number;
  total: number;
  date: string;
}

export const MARKET_STOCKS: MarketStock[] = [
  {
    symbol: 'AAPL',
    name: 'Apple Inc.',
    chg: 1.24,
    price: 198.21,
    vol: '52.1M',
    relVol: '1.08x',
    mktCap: '$3.1T',
    pe: '31.5',
    eps: 6.31,
    epsGrowth: 8.2,
    divYield: '0.50%',
    sector: 'Technology',
    rating: 'Buy'
  },
  {
    symbol: 'MSFT',
    name: 'Microsoft Corp.',
    chg: 0.86,
    price: 427.93,
    vol: '21.4M',
    relVol: '0.97x',
    mktCap: '$3.2T',
    pe: '36.4',
    eps: 11.75,
    epsGrowth: 14.0,
    divYield: '0.72%',
    sector: 'Technology',
    rating: 'Strong buy'
  },
  {
    symbol: 'NVDA',
    name: 'NVIDIA Corp.',
    chg: 2.91,
    price: 132.46,
    vol: '180.2M',
    relVol: '1.34x',
    mktCap: '$3.3T',
    pe: '70.1',
    eps: 1.89,
    epsGrowth: 81.2,
    divYield: '0.03%',
    sector: 'Technology',
    rating: 'Strong buy'
  },
  {
    symbol: 'GOOG',
    name: 'Alphabet Inc.',
    chg: -0.42,
    price: 183.12,
    vol: '24.5M',
    relVol: '0.92x',
    mktCap: '$2.3T',
    pe: '27.8',
    eps: 6.58,
    epsGrowth: 12.4,
    divYield: '0.00%',
    sector: 'Communication Services',
    rating: 'Buy'
  },
  {
    symbol: 'META',
    name: 'Meta Platforms',
    chg: -1.11,
    price: 541.77,
    vol: '12.2M',
    relVol: '1.16x',
    mktCap: '$1.4T',
    pe: '29.3',
    eps: 18.48,
    epsGrowth: 18.1,
    divYield: '0.40%',
    sector: 'Communication Services',
    rating: 'Buy'
  },
  {
    symbol: 'NFLX',
    name: 'Netflix Inc.',
    chg: 0.53,
    price: 731.22,
    vol: '3.8M',
    relVol: '0.88x',
    mktCap: '$315B',
    pe: '44.6',
    eps: 16.39,
    epsGrowth: 19.9,
    divYield: '0.00%',
    sector: 'Communication Services',
    rating: 'Hold'
  },
  {
    symbol: 'V',
    name: 'Visa Inc.',
    chg: 0.21,
    price: 287.91,
    vol: '6.3M',
    relVol: '0.83x',
    mktCap: '$588B',
    pe: '32.2',
    eps: 8.95,
    epsGrowth: 11.3,
    divYield: '0.76%',
    sector: 'Financials',
    rating: 'Buy'
  },
  {
    symbol: 'BRK.B',
    name: 'Berkshire Hathaway',
    chg: -0.17,
    price: 457.38,
    vol: '4.1M',
    relVol: '0.79x',
    mktCap: '$993B',
    pe: '9.1',
    eps: 50.12,
    epsGrowth: 4.2,
    divYield: '0.00%',
    sector: 'Financials',
    rating: 'Hold'
  }
];

export const extendedWatchlist: WatchlistStock[] = MARKET_STOCKS.map((s) => ({
  symbol: s.symbol,
  name: s.name,
  price: s.price,
  change: s.chg,
  high52: s.price * 1.12,
  low52: s.price * 0.82,
  mktCap: s.mktCap
}));

export const holdings: Holding[] = [
  { symbol: 'AAPL', name: 'Apple Inc.', shares: 12, avgCost: 182.2, current: 198.21, sector: 'Technology' },
  { symbol: 'MSFT', name: 'Microsoft Corp.', shares: 6, avgCost: 401.4, current: 427.93, sector: 'Technology' },
  { symbol: 'V', name: 'Visa Inc.', shares: 10, avgCost: 274.1, current: 287.91, sector: 'Financials' }
];

export const portfolioHistory: PortfolioPoint[] = [
  { date: 'Mon', value: 9800 },
  { date: 'Tue', value: 10050 },
  { date: 'Wed', value: 10120 },
  { date: 'Thu', value: 9980 },
  { date: 'Fri', value: 10340 },
  { date: 'Sat', value: 10410 },
  { date: 'Sun', value: 10590 }
];

export const transactions: TransactionItem[] = [
  { symbol: 'AAPL', type: 'BUY', shares: 4, total: 760.4, date: '2026-10-10 09:31' },
  { symbol: 'MSFT', type: 'BUY', shares: 2, total: 845.0, date: '2026-10-10 10:05' },
  { symbol: 'V', type: 'SELL', shares: 1, total: 286.2, date: '2026-10-10 13:47' }
];

export const sparklines: Record<string, number[]> = {
  AAPL: [183, 186, 185, 189, 191, 194, 198],
  MSFT: [404, 407, 409, 412, 420, 425, 428],
  V: [271, 273, 274, 276, 279, 283, 288]
};
