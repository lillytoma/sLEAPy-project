export const portfolioHistory = [
  { date: 'Jan', value: 112400 },
  { date: 'Feb', value: 108900 },
  { date: 'Mar', value: 115200 },
  { date: 'Apr', value: 121800 },
  { date: 'May', value: 118300 },
  { date: 'Jun', value: 129400 },
  { date: 'Jul', value: 135700 },
  { date: 'Aug', value: 148240 },
];

export interface Holding {
  symbol: string;
  name: string;
  shares: number;
  avgCost: number;
  current: number;
  sector: string;
}

export const holdings: Holding[] = [
  { symbol: 'AAPL', name: 'Apple Inc.', shares: 42, avgCost: 172.30, current: 211.42, sector: 'Technology' },
  { symbol: 'NVDA', name: 'NVIDIA Corp.', shares: 18, avgCost: 88.14, current: 124.56, sector: 'Technology' },
  { symbol: 'MSFT', name: 'Microsoft Corp.', shares: 25, avgCost: 390.50, current: 438.17, sector: 'Technology' },
  { symbol: 'JPM', name: 'JPMorgan Chase', shares: 30, avgCost: 198.40, current: 246.90, sector: 'Finance' },
  { symbol: 'AMZN', name: 'Amazon.com Inc.', shares: 12, avgCost: 178.60, current: 198.03, sector: 'Consumer' },
  { symbol: 'TSLA', name: 'Tesla Inc.', shares: 20, avgCost: 210.00, current: 182.94, sector: 'Automotive' },
];

export const DEFAULT_WATCHLIST = ['META', 'GOOG', 'BRK.B', 'V', 'NFLX'];

export const transactions = [
  { date: 'Aug 28', type: 'BUY', symbol: 'NVDA', shares: 5, price: 124.56, total: 622.80 },
  { date: 'Aug 22', type: 'SELL', symbol: 'TSLA', shares: 10, price: 191.20, total: 1912.00 },
  { date: 'Aug 15', type: 'BUY', symbol: 'JPM', shares: 8, price: 241.30, total: 1930.40 },
  { date: 'Aug 09', type: 'BUY', symbol: 'AAPL', shares: 6, price: 208.80, total: 1252.80 },
  { date: 'Jul 31', type: 'SELL', symbol: 'AMZN', shares: 4, price: 202.10, total: 808.40 },
];

export const sparklines: Record<string, number[]> = {
  AAPL: [168, 172, 170, 185, 195, 200, 207, 211],
  NVDA: [90, 95, 88, 100, 108, 115, 120, 125],
  MSFT: [388, 392, 385, 400, 415, 422, 430, 438],
  JPM: [200, 210, 205, 220, 228, 235, 242, 247],
  AMZN: [180, 175, 182, 188, 192, 195, 196, 198],
  TSLA: [215, 208, 220, 205, 195, 188, 190, 183],
};

export interface WatchlistStock {
  symbol: string;
  name: string;
  price: number;
  change: number;
  high52: number;
  low52: number;
  mktCap: string;
}

export const extendedWatchlist: WatchlistStock[] = [
  { symbol: 'META', name: 'Meta Platforms', price: 554.78, change: -0.44, high52: 604.50, low52: 414.50, mktCap: '1.41T' },
  { symbol: 'GOOG', name: 'Alphabet Inc.', price: 180.22, change: +1.05, high52: 208.70, low52: 155.20, mktCap: '2.19T' },
  { symbol: 'BRK.B', name: 'Berkshire Hathaway', price: 462.10, change: +0.22, high52: 489.00, low52: 392.20, mktCap: '1.02T' },
  { symbol: 'V', name: 'Visa Inc.', price: 310.44, change: +0.61, high52: 354.00, low52: 270.90, mktCap: '626B' },
  { symbol: 'NFLX', name: 'Netflix Inc.', price: 1041.30, change: -1.18, high52: 1105.00, low52: 562.00, mktCap: '446B' },
  { symbol: 'AMD', name: 'Advanced Micro Devices', price: 162.50, change: +2.34, high52: 227.30, low52: 117.90, mktCap: '263B' },
  { symbol: 'PYPL', name: 'PayPal Holdings', price: 68.42, change: -0.87, high52: 97.90, low52: 55.80, mktCap: '67B' },
];

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

export const MARKET_STOCKS: MarketStock[] = [
  { symbol: 'FLYE', name: 'Fly-E Group, Inc.', chg: +58.82, price: 2.16, vol: '43.66M', relVol: '6,323', mktCap: '3.53M', pe: '—', eps: -11.53, epsGrowth: +46.40, divYield: '0.00%', sector: 'Consumer durables', rating: 'No rating' },
  { symbol: 'SSM', name: 'Sono Group N.V.', chg: +46.27, price: 3.92, vol: '17.61M', relVol: '22.60', mktCap: '9.51M', pe: '—', eps: -6.39, epsGrowth: -115.20, divYield: '0.00%', sector: 'Consumer durables', rating: 'No rating' },
  { symbol: 'BIAF', name: 'bioAffinity Technologies', chg: +42.98, price: 6.52, vol: '31.27M', relVol: '25.28', mktCap: '3.86M', pe: '—', eps: -103.29, epsGrowth: +67.18, divYield: '0.00%', sector: 'Health technology', rating: 'No rating' },
  { symbol: 'DAIC', name: 'CID HoldCo, Inc.', chg: +25.14, price: 4.36, vol: '5.26M', relVol: '0.16', mktCap: '8.55M', pe: '—', eps: -59.67, epsGrowth: -384.23, divYield: '0.00%', sector: 'Miscellaneous', rating: 'No rating' },
  { symbol: 'FRVO', name: 'Fervo Energy Company', chg: +19.39, price: 18.36, vol: '10.03M', relVol: '2.52', mktCap: '5.41B', pe: '—', eps: 0, epsGrowth: 0, divYield: '0.00%', sector: 'Utilities', rating: 'Strong buy' },
  { symbol: 'INBS', name: 'Intelligent Bio Solutions', chg: +12.08, price: 2.69, vol: '4.07M', relVol: '53.18', mktCap: '8.14M', pe: '—', eps: -9.45, epsGrowth: +54.22, divYield: '0.00%', sector: 'Health services', rating: 'Strong buy' },
  { symbol: 'AAPL', name: 'Apple Inc.', chg: +1.84, price: 211.42, vol: '61.20M', relVol: '1.12', mktCap: '3.18T', pe: '33', eps: 6.43, epsGrowth: +12.40, divYield: '0.44%', sector: 'Technology', rating: 'Buy' },
  { symbol: 'MSFT', name: 'Microsoft Corporation', chg: +0.63, price: 438.17, vol: '18.90M', relVol: '0.88', mktCap: '3.26T', pe: '35', eps: 12.93, epsGrowth: +18.20, divYield: '0.72%', sector: 'Technology', rating: 'Strong buy' },
  { symbol: 'NVDA', name: 'NVIDIA Corporation', chg: +3.27, price: 124.56, vol: '278.4M', relVol: '1.43', mktCap: '3.06T', pe: '55', eps: 2.27, epsGrowth: +168.0, divYield: '0.03%', sector: 'Technology', rating: 'Strong buy' },
  { symbol: 'JPM', name: 'JPMorgan Chase & Co.', chg: +0.38, price: 246.90, vol: '9.14M', relVol: '0.92', mktCap: '701B', pe: '13', eps: 18.22, epsGrowth: +8.30, divYield: '2.11%', sector: 'Finance', rating: 'Buy' },
  { symbol: 'TSLA', name: 'Tesla, Inc.', chg: -2.11, price: 182.94, vol: '99.30M', relVol: '1.21', mktCap: '585B', pe: '60', eps: 3.05, epsGrowth: -22.70, divYield: '0.00%', sector: 'Automotive', rating: 'Hold' },
  { symbol: 'META', name: 'Meta Platforms, Inc.', chg: -0.44, price: 554.78, vol: '14.22M', relVol: '0.76', mktCap: '1.41T', pe: '26', eps: 21.09, epsGrowth: +53.40, divYield: '0.36%', sector: 'Technology', rating: 'Strong buy' },
  { symbol: 'AMZN', name: 'Amazon.com, Inc.', chg: +0.91, price: 198.03, vol: '35.10M', relVol: '1.05', mktCap: '2.09T', pe: '44', eps: 4.47, epsGrowth: +91.30, divYield: '0.00%', sector: 'Consumer', rating: 'Strong buy' },
  { symbol: 'GOOG', name: 'Alphabet Inc.', chg: +1.05, price: 180.22, vol: '22.80M', relVol: '0.96', mktCap: '2.19T', pe: '21', eps: 8.58, epsGrowth: +29.60, divYield: '0.46%', sector: 'Technology', rating: 'Strong buy' },
];

export const PRESET_FILTERS = [
  'All stocks', 'Top gainers', 'Biggest losers', 'Large-cap', 'Small-cap',
  'Largest employers', 'High-dividend', 'Highest net income', 'Highest cash',
  'Most active', 'Pre-market gainers', 'Pre-market losers', 'After-hours gainers',
  'After-hours losers', 'Unusual volume', 'Most volatile', 'High beta',
  'Best performing', 'Highest revenue', 'Penny stocks', 'Overbought',
  'Oversold', 'All-time high', 'All-time low', '52-week high', '52-week low',
];

export const ADD_FILTER_OPTIONS = [
  { key: 'sector', label: 'Sector', options: ['Technology', 'Finance', 'Healthcare', 'Consumer', 'Energy', 'Utilities', 'Real Estate'] },
  { key: 'mktcap', label: 'Market Cap', options: ['Mega (>$200B)', 'Large ($10B–$200B)', 'Mid ($2B–$10B)', 'Small ($300M–$2B)', 'Micro (<$300M)'] },
  { key: 'change', label: '% Change', options: ['>5%', '>2%', '>0%', '<0%', '<-2%', '<-5%'] },
  { key: 'volume', label: 'Volume', options: ['>10M', '>5M', '>1M', '>500K'] },
  { key: 'peRatio', label: 'P/E Ratio', options: ['<10', '10–20', '20–30', '30–50', '>50'] },
  { key: 'divYield', label: 'Dividend Yield', options: ['>5%', '>3%', '>1%', 'No dividend'] },
  { key: 'analyst', label: 'Analyst Rating', options: ['Strong buy', 'Buy', 'Hold', 'Sell', 'Strong sell'] },
];
