const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');
const vm = require('node:vm');
const ts = require('typescript');
const rx = require('rxjs');

function signal(value) {
  const read = () => value;
  read.set = next => { value = next; };
  read.update = update => { value = update(value); };
  return read;
}

class HttpErrorResponse extends Error {
  status = 503;
  error = { message: 'Database temporarily unavailable' };
}

function loadSource(relativePath, scheduler, errors, globals = {}) {
  const source = fs.readFileSync(path.join(__dirname, '..', relativePath), 'utf8');
  const compiled = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
      experimentalDecorators: true
    }
  }).outputText;
  const context = {
    exports: {},
    console: { error: (...args) => errors.push(args) },
    setInterval: () => 1,
    clearInterval: () => {},
    ...globals,
    require: name => {
      if (name === '@angular/core') {
        return { Injectable: () => type => type,
          Component: metadata => type => { type.componentMetadata = metadata; return type; },
          ViewChild: () => () => {}, HostListener: () => () => {},
          Optional: () => () => {}, signal, computed: value => value };
      }
      if (name === '@angular/common/http') return { HttpErrorResponse };
      if (name === 'rxjs') return { ...rx, timer: (due, interval) => rx.timer(due, interval, scheduler) };
      if (name.endsWith('/environment')) return { environment: { apiUrl: '' } };
      return {};
    }
  };
  vm.runInNewContext(compiled, context);
  return context.exports;
}

function fixture() {
  const scheduler = new rx.VirtualTimeScheduler();
  const errors = [];
  const requests = [];
  const state = { totalTrades: 1, delay: 0, failNext: false, owners: [], failOwners: false };
  const http = {
    get: (url, options) => new rx.Observable(subscriber => {
      requests.push({ url, params: options.params });
      const totalTrades = state.totalTrades;
      const ownerResponse = state.owners.map(owner => ({ ...owner }));
      return scheduler.schedule(() => {
        if (url.endsWith('/instrument-owners')) {
          if (state.failOwners) subscriber.error(new HttpErrorResponse());
          else subscriber.next(ownerResponse);
          subscriber.complete();
        } else if (state.failNext) {
          state.failNext = false;
          subscriber.error(new HttpErrorResponse());
        } else {
          subscriber.next({ totalTrades, tradeVolume: totalTrades * 10, mostActiveInstruments: [], updatedAt: null,
            portfolioHistory: { historySince: '2026-10-23T00:00:00', comparable: true, first: null,
              latest: { snapshotAt: '2026-10-23T00:00:00', portfolios: options.params.clientIds ? options.params.clientIds.split(',').length : totalTrades,
                cashBalance: totalTrades * 100, holdingsValue: totalTrades * 10, netWorth: totalTrades * 110,
                missingPrices: 0, instrumentValues: { Stock: totalTrades * 10 } } },
            activity: [{ timestamp: '2026-10-23T00:00:00', tradeVolume: totalTrades * 10,
              instrumentVolumes: { Stock: totalTrades * 10 } }] });
          subscriber.complete();
        }
      }, state.delay);
    })
  };
  const { AnalystService } = loadSource('src/app/services/analyst.service.ts', scheduler, errors);
  const service = new AnalystService(http);
  let summary;
  let error;
  service.summary$.subscribe(value => { summary = value; });
  service.error$.subscribe(value => { error = value; });
  return {
    service, scheduler, requests, state, errors,
    summary: () => summary,
    error: () => error,
    advance: milliseconds => {
      scheduler.maxFrames = scheduler.frame + milliseconds;
      scheduler.schedule(() => {}, milliseconds);
      scheduler.flush();
    }
  };
}

test('polls every 10 seconds and immediately refreshes without duplicate subscriptions', () => {
  const f = fixture();
  f.service.connect();
  f.advance(0);
  assert.equal(f.summary().totalTrades, 1);
  assert.equal(f.requests[0].params.period, 'DAY');
  f.state.totalTrades = 2;
  f.advance(9999);
  assert.equal(f.requests.length, 1);
  f.advance(1);
  assert.equal(f.summary().totalTrades, 2);
  f.service.connect();
  f.advance(0);
  assert.equal(f.requests.length, 3);
  f.advance(10000);
  assert.equal(f.requests.length, 4);
  f.service.disconnect();
});

test('all-time and customer filters survive a trip from the dashboard to settings and back', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const dashboard = new AnalystComponent(f.service);
  dashboard.ngOnInit();
  dashboard.setPeriod('ALL_TIME');
  dashboard.setInstrument('ETF');
  dashboard.selectClient({ id: 42, username: 'customer', email: 'customer@example.invalid' });
  f.advance(0);
  dashboard.ngOnDestroy();
  const restored = new AnalystComponent(f.service);
  restored.ngOnInit();
  f.advance(0);
  assert.equal(restored.selectedPeriod(), 'ALL_TIME');
  assert.equal(restored.selectedInstrument(), 'ETF');
  assert.equal(restored.selectedClients()[0].id, 42);
  assert.equal(restored.activityBucketLabel(), 'Monthly');
  assert.equal(restored.activityAxisFormat(), 'MMM yyyy');
  assert.equal(f.requests.at(-1).params.period, 'ALL_TIME');
  assert.equal(f.requests.at(-1).params.clientIds, '42');
  restored.ngOnDestroy();
});

test('analyst Settings uses persistent theme controls without loading client account controls', () => {
  const storage = new Map([['user_id', '42']]);
  const localStorage = {
    getItem: key => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value)
  };
  const classes = new Set();
  const root = {
    dataset: {},
    classList: {
      toggle: (name, enabled) => enabled ? classes.add(name) : classes.delete(name),
      remove: (...names) => names.forEach(name => classes.delete(name)),
      add: name => classes.add(name)
    }
  };
  const globals = { localStorage, document: { documentElement: root } };
  const { ThemeService } = loadSource('src/app/services/theme.service.ts', null, [], globals);
  const { SettingsComponent } = loadSource('src/app/dashboard/settings/settings.component.ts', null, [], globals);
  const theme = new ThemeService();
  const accountRequests = [];
  const auth = { getAccountStatus: id => { accountRequests.push(id); return rx.of({ frozen: true }); } };
  const analyst = new SettingsComponent(theme, auth, {}, { snapshot: { data: { analystSettings: true } } });
  assert.equal(analyst.analystSettings, true);
  assert.deepEqual(accountRequests, []);
  analyst.themeService.toggleDark();
  analyst.themeService.setFontSize('large');
  analyst.themeService.setColorVision('tritanopia');
  assert.ok(classes.has('dark'));
  assert.ok(classes.has('color-vision-tritanopia'));
  assert.equal(root.dataset.fontSize, 'large');
  const restored = new ThemeService();
  assert.equal(restored.dark(), true);
  assert.equal(restored.fontSize(), 'large');
  assert.equal(restored.colorVision(), 'tritanopia');
  const client = new SettingsComponent(restored, auth, {}, { snapshot: { data: {} } });
  assert.equal(client.analystSettings, false);
  assert.equal(client.frozen(), true);
  assert.deepEqual(accountRequests, [42]);
  assert.match(SettingsComponent.componentMetadata.template, /@if \(!analystSettings\)/);
  assert.match(SettingsComponent.componentMetadata.template, /routerLink="\/analyst"/);
});

test('analyst profile offers only Settings and Sign out and Escape restores trigger focus', () => {
  const { AnalystProfileComponent } = loadSource('src/app/shared/analyst-profile.component.ts', null, [], {
    localStorage: { getItem: () => '' }
  });
  const profile = new AnalystProfileComponent({}, {});
  const template = AnalystProfileComponent.componentMetadata.template;
  assert.match(template, /routerLink="\/analyst\/settings"/);
  assert.match(template, /authService\.logout\(\)/);
  assert.doesNotMatch(template, /themeService|textSizes|colorModes/);
  let focused = false;
  profile.trigger = { nativeElement: { focus: () => { focused = true; } } };
  profile.open.set(true);
  profile.closeOnEscape({ preventDefault() {}, stopPropagation() {} });
  assert.equal(profile.open(), false);
  assert.equal(focused, true);
});

test('preserves client and period filters through polling and ignores outdated responses', () => {
  const f = fixture();
  const received = [];
  const subscription = f.service.summary$.subscribe(summary => {
    if (summary) received.push(summary.totalTrades);
  });
  f.state.delay = 1000;
  f.service.connect();
  f.advance(0);
  f.state.delay = 750;
  f.state.totalTrades = 2;
  f.service.setFilters('YEAR', [42]);
  f.advance(0);
  f.state.delay = 500;
  f.state.totalTrades = 3;
  f.service.setFilters('MONTH', [7]);
  assert.equal(f.summary(), null);
  f.advance(500);
  assert.equal(f.requests.at(-1).params.period, 'MONTH');
  assert.equal(f.requests.at(-1).params.clientIds, '7');
  assert.equal(f.summary().totalTrades, 3);
  assert.deepEqual(received, [3]);
  f.state.totalTrades = 5;
  f.advance(10000);
  assert.equal(f.summary().totalTrades, 5);
  assert.deepEqual(received, [3, 5]);
  assert.equal(f.requests.at(-1).params.period, 'MONTH');
  assert.equal(f.requests.at(-1).params.clientIds, '7');
  f.service.setFilters('MONTH');
  f.advance(500);
  assert.equal(f.requests.at(-1).params.clientIds, undefined);
  f.service.disconnect();
  subscription.unsubscribe();
});

test('reports failures and recovers on the next refresh', () => {
  const f = fixture();
  f.state.failNext = true;
  f.service.connect();
  f.advance(0);
  assert.match(f.error(), /503.*Database temporarily unavailable/);
  assert.equal(f.errors.length, 1);
  assert.equal(f.summary(), null);
  f.advance(10000);
  assert.equal(f.error(), null);
  assert.equal(f.summary().totalTrades, 1);
  f.service.disconnect();
});

test('disconnect stops polling and resets filters for the next dashboard session', () => {
  const f = fixture();
  f.service.setFilters('YEAR', [42]);
  f.service.connect();
  f.advance(0);
  f.service.disconnect();
  assert.equal(f.summary(), null);
  f.advance(30000);
  assert.equal(f.requests.length, 1);
  f.service.connect();
  f.advance(0);
  assert.equal(f.requests.at(-1).params.period, 'DAY');
  assert.equal(f.requests.at(-1).params.clientIds, undefined);
  f.service.disconnect();
});

test('period buttons and client selection share filters and replace historical chart data', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const component = new AnalystComponent(f.service);
  component.ngOnInit();
  f.advance(0);
  component.setInstrument('Stock');
  assert.equal(component.periods.map(period => period.label).join(','), 'Day,Week,Month,Year,All time');
  for (const period of ['WEEK', 'MONTH', 'YEAR', 'ALL_TIME', 'DAY']) {
    component.setPeriod(period);
    assert.equal(component.activityPoints().length, 0);
    f.advance(0);
    assert.equal(f.requests.at(-1).params.period, period);
    assert.equal(component.activityPoints().length, 1);
    assert.equal(component.selectedInstrument(), 'Stock');
  }
  component.selectClient({ id: 42, username: 'customer', email: 'customer@example.invalid' });
  f.advance(0);
  f.state.totalTrades = 5;
  f.advance(10000);
  assert.equal(component.summary().totalTrades, 5);
  assert.equal(component.portfolioLatest().portfolios, 1);
  assert.equal(component.portfolioLatest().netWorth, 550);
  assert.equal(component.selectedInstrument(), 'Stock');
  assert.equal(component.activityPoints()[0].value, 50);
  assert.equal(f.requests.at(-1).params.clientIds, '42');
  component.setPeriod('MONTH');
  assert.equal(component.portfolioLatest(), null);
  f.advance(0);
  assert.equal(f.requests.at(-1).params.clientIds, '42');
  assert.equal(f.requests.at(-1).params.period, 'MONTH');
  assert.equal(component.portfolioLatest().portfolios, 1);
  component.clearClient();
  f.advance(0);
  assert.equal(f.requests.at(-1).params.clientIds, undefined);
  assert.equal(f.requests.at(-1).params.period, 'MONTH');
  assert.equal(component.portfolioLatest().portfolios, 5);
  component.ngOnDestroy();
});

test('chart uses historical buckets with period-specific axes, zero intervals and instrument filters', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const component = new AnalystComponent(f.service);
  const formats = { DAY: 'ha', WEEK: 'MMM d', MONTH: 'd', YEAR: 'MMM', ALL_TIME: 'MMM yyyy' };
  for (const [period, format] of Object.entries(formats)) {
    component.selectedPeriod.set(period);
    assert.equal(component.activityAxisFormat(), format);
  }
  component.summary.set({
    totalTrades: 3, tradeVolume: 150, updatedAt: null, mostActiveInstruments: [],
    activity: [
      { timestamp: '2026-10-01T00:00:00', tradeVolume: 0, instrumentVolumes: {} },
      { timestamp: '2026-10-02T00:00:00', tradeVolume: 150, instrumentVolumes: { Stock: 100, ETF: 50 } },
      { timestamp: '2026-10-03T00:00:00', tradeVolume: 0, instrumentVolumes: {} }
    ]
  });
  component.selectedPeriod.set('MONTH');
  assert.deepEqual(Array.from(component.activityPoints(), point => point.value), [0, 150, 0]);
  const chart = component.activityChart();
  assert.equal(chart.coords[0].x, 0);
  assert.equal(chart.coords[1].x, 300);
  assert.equal(chart.coords[2].x, 600);
  assert.ok(!/NaN|Infinity/.test(chart.linePath));
  assert.ok(chart.coords.every(point => point.y >= 0 && point.y <= chart.height));
  component.setInstrument('Stock');
  assert.deepEqual(Array.from(component.activityPoints(), point => point.value), [0, 100, 0]);
  component.setInstrument('Bond');
  assert.deepEqual(Array.from(component.activityPoints(), point => point.value), [0, 0, 0]);
  assert.ok(!/NaN|Infinity/.test(component.activityChart().linePath));
});

test('toolbar instrument filter applies to metrics, type mix and most-active list', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const component = new AnalystComponent(f.service);
  component.summary.set({
    totalTrades: 10, tradeVolume: 1000, updatedAt: null,
    orderDirections: [
      { direction: 'BUY', instrumentType: 'CRYTPO', totalTrades: 5, tradeVolume: 500 },
      { direction: 'SELL', instrumentType: 'EQUITY', totalTrades: 3, tradeVolume: 300 },
      { direction: 'BUY', instrumentType: 'FUND', totalTrades: 2, tradeVolume: 200 }
    ],
    mostActiveInstruments: [
      { symbol: 'BTC', symbolName: 'Bitcoin', instrumentType: 'CRYTPO', totalTrades: 5, tradeVolume: 500 },
      { symbol: 'AAPL', symbolName: 'Apple', instrumentType: 'EQUITY', totalTrades: 3, tradeVolume: 300 },
      { symbol: 'SPY', symbolName: 'SPDR', instrumentType: 'FUND', totalTrades: 2, tradeVolume: 200 }
    ],
    activity: [{ timestamp: '2026-10-23T00:00:00', tradeVolume: 1000,
      instrumentVolumes: { CRYTPO: 500, EQUITY: 300, FUND: 200 } }]
  });
  assert.equal(component.totalTrades(), 10);
  assert.equal(component.instrumentActivity().find(item => item.type === 'Crypto').share, 50);
  assert.equal(component.instrumentActivity().find(item => item.type === 'Stock').share, 30);

  for (const [type, symbol, trades, volume] of [
    ['Stock', 'AAPL', 3, 300], ['Crypto', 'BTC', 5, 500], ['ETF', 'SPY', 2, 200]
  ]) {
    component.hoveredType.set('Crypto');
    component.hoveredActivityPoint.set({});
    component.setInstrument(type);
    assert.equal(component.hoveredType(), null);
    assert.equal(component.hoveredActivityPoint(), null);
    assert.equal(component.totalTrades(), trades);
    assert.equal(component.kpis()[0].value, `$${volume.toLocaleString()}`);
    assert.equal(component.kpis()[1].value, String(trades));
    assert.equal(component.kpis()[2].value, '1');
    assert.equal(component.kpis()[3].value, '$100');
    assert.equal(component.activityPoints()[0].value, volume);
    assert.equal(component.instrumentActivity()[0].type, type);
    assert.equal(component.instrumentActivity()[0].volume, volume);
    assert.equal(component.instrumentActivity()[0].share, 100);
    assert.equal(component.topStocks()[0].symbol, symbol);
  }
  component.setInstrument('Bond');
  assert.equal(component.totalTrades(), 0);
  assert.equal(component.kpis()[0].value, '$0');
  assert.equal(component.activityPoints()[0].value, 0);
  assert.equal(component.instrumentActivity()[0].share, 0);
  assert.equal(component.topStocks().length, 0);
  component.setInstrument('All instruments');
  assert.equal(component.totalTrades(), 10);
  assert.equal(component.activityPoints()[0].value, 1000);
  assert.equal(component.instrumentActivity().length, 4);
});

test('type selection restores the corresponding most-active list with pagination', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const component = new AnalystComponent(f.service);
  component.summary.set({
    totalTrades: 8, tradeVolume: 1000, updatedAt: null, activity: [],
    mostActiveInstruments: [10, 60, 500, 200, 100, 80, 50, 0].map((tradeVolume, index) => ({
      symbol: `S${index}`, symbolName: `Symbol ${index}`, instrumentType: 'Stock', totalTrades: 1, tradeVolume
    }))
  });
  assert.equal(component.instrumentActivity()[0].share, 100);
  assert.equal(component.topStocks().map(item => item.symbol).join(','), 'S2,S3,S4,S5,S1');
  assert.equal(component.totalSymbolPages(), 2);
  component.nextSymbolPage();
  assert.equal(component.topStocks().length, 3);
  component.selectInstrumentType('ETF');
  assert.equal(component.symbolPage(), 0);
  assert.equal(component.topStocks().length, 0);
  component.selectInstrumentType('Stock');
  assert.equal(component.topStocks().length, 5);
  component.setInstrument('ETF');
  assert.equal(component.instrumentActivity()[0].share, 0);
  assert.equal(component.topStocks().length, 0);
});

test('portfolio summary shows real recorded history, type holdings, and missing data explicitly', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const component = new AnalystComponent(f.service);
  assert.equal(component.portfolioLatest(), null);
  assert.equal(component.portfolioChange(), null);
  const first = { snapshotAt: '2026-10-23T10:00:00', portfolios: 2,
    cashBalance: 100, holdingsValue: 200, netWorth: 300, missingPrices: 0,
    instrumentValues: { EQUITY: 150, ETF: 50 } };
  const latest = { ...first, snapshotAt: '2026-10-23T11:00:00', holdingsValue: 250, netWorth: 350,
    instrumentValues: { EQUITY: 200, ETF: 50 } };
  component.summary.set({ portfolioHistory: { comparable: true, historySince: first.snapshotAt, first, latest } });
  assert.equal(component.portfolioChange(), 50);
  assert.equal(component.portfolioMetrics()[0].value, '$350');
  component.setInstrument('Stock');
  assert.equal(component.portfolioMetrics()[0].value, '$350');
  assert.equal(component.portfolioMetrics().at(-1).value, '$200');
  component.summary.set({ portfolioHistory: { comparable: false, first, latest } });
  assert.equal(component.portfolioChange(), null);
  component.summary.set({ portfolioHistory: { historySince: first.snapshotAt, first,
    latest: { ...latest, holdingsValue: null, netWorth: null, missingPrices: 1,
      instrumentValues: { EQUITY: null, ETF: 50 } } } });
  assert.equal(component.portfolioChange(), null);
  assert.equal(component.portfolioMetrics()[0].value, 'Unavailable');
  assert.equal(component.portfolioMetrics().at(-1).value, 'Unavailable');
  component.setInstrument('ETF');
  assert.equal(component.portfolioMetrics().at(-1).value, '$50');
  component.summary.set({ portfolioHistory: { historySince: first.snapshotAt, first, latest: first } });
  assert.equal(component.portfolioChange(), null);
  component.summary.set({ portfolioHistory: { historySince: first.snapshotAt, first: null, latest: null } });
  assert.equal(component.portfolioLatest(), null);
  assert.equal(component.portfolioMetrics().length, 0);
});

test('multiple customers are added once, removed individually, and preserved through polling', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const component = new AnalystComponent(f.service);
  component.ngOnInit();
  f.advance(0);
  const first = { id: 42, username: 'first', email: 'first@example.invalid' };
  const second = { id: 7, username: 'second', email: 'second@example.invalid' };
  component.selectClient(first);
  component.selectClient(second);
  component.selectClient(first);
  f.advance(0);
  assert.equal(component.selectedClients().length, 2);
  assert.equal(component.clientSearch(), '');
  assert.equal(component.customerScope(), 'first, second');
  assert.equal(f.requests.at(-1).params.clientIds, '42,7');
  assert.equal(component.portfolioLatest().portfolios, 2);
  component.setPeriod('YEAR');
  component.setInstrument('Stock');
  f.advance(10000);
  assert.equal(f.requests.at(-1).params.clientIds, '42,7');
  assert.equal(f.requests.at(-1).params.period, 'YEAR');
  component.ngOnDestroy();
  const details = new AnalystComponent(f.service);
  details.ngOnInit();
  f.advance(0);
  assert.equal(details.selectedInstrument(), 'Stock');
  assert.equal(details.selectedPeriod(), 'YEAR');
  assert.equal(details.selectedClients().length, 2);
  assert.equal(f.requests.at(-1).params.clientIds, '42,7');
  details.removeClient(42);
  f.advance(0);
  assert.equal(details.customerScope(), 'second');
  assert.equal(f.requests.at(-1).params.clientIds, '7');
  details.removeClient(7);
  f.advance(0);
  assert.equal(details.customerScope(), 'All customers');
  assert.equal(f.requests.at(-1).params.clientIds, undefined);
  details.ngOnDestroy();
});

test('most-active list ranks by order count then value and responds to donut type selection', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const component = new AnalystComponent(f.service);
  const instruments = [
    ['LOW', 'Stock', 1], ['BIG', 'Stock', 500], ['FUND', 'ETF', 1000],
    ['B', 'Stock', 100], ['A', 'Stock', 100], ['C', 'Stock', 50], ['D', 'Stock', 10]
  ].map(([symbol, instrumentType, tradeVolume]) => ({
    symbol, symbolName: symbol, instrumentType, totalTrades: 1, tradeVolume
  }));
  component.summary.set({ mostActiveInstruments: instruments });
  assert.equal(component.activeInstruments().map(item => item.symbol).join(','), 'BIG,A,B,C,D,LOW');
  component.summary.set({ mostActiveInstruments: instruments.map(item =>
    item.symbol === 'LOW' ? { ...item, totalTrades: 10 } : item) });
  assert.equal(component.topStocks()[0].symbol, 'LOW');
  component.selectInstrumentType('ETF');
  assert.equal(component.topStocks()[0].symbol, 'FUND');
  assert.equal(instruments[0].symbol, 'LOW');
  component.setInstrument('Stock');
  assert.equal(component.activeInstruments().map(item => item.symbol).join(','), 'LOW,BIG,A,B,C,D');
  assert.equal(component.instrumentActivity().length, 1);
  assert.equal(component.instrumentActivity()[0].type, 'Stock');
});

test('donut switches from type distribution to top five symbols ranked by orders with volume labels', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const component = new AnalystComponent(f.service);
  const mostActiveInstruments = [
    ['A', 'Stock', 10, 10], ['B', 'Stock', 5, 5000], ['C', 'Stock', 4, 200],
    ['D', 'Stock', 3, 100], ['E', 'Stock', 2, 200], ['F', 'Stock', 1, 500],
    ['BTC', 'Crypto', 5, 500]
  ].map(([symbol, instrumentType, totalTrades, tradeVolume]) => ({
    symbol, symbolName: symbol, instrumentType, totalTrades, tradeVolume
  }));
  component.summary.set({ totalTrades: 30, mostActiveInstruments });
  assert.equal(component.donutItems().find(item => item.type === 'Stock').trades, 25);
  component.selectDonutItem('Crypto');
  assert.equal(component.topStocks()[0].symbol, 'BTC');
  component.setInstrument('Stock');
  assert.equal(component.donutItems().map(item => item.type).join(','), 'A,B,C,D,E');
  assert.equal(component.donutItems()[0].volume, 10);
  assert.equal(component.donutTotal(), 24);
  assert.ok(Math.abs(component.donutItems().reduce((sum, item) => sum + item.share, 0) - 100) < 1e-10);
  assert.equal(component.selectedSymbol(), 'A');
  component.selectDonutItem('B');
  assert.equal(component.selectedSymbol(), 'B');
  component.setInstrument('All instruments');
  assert.equal(component.selectedSymbol(), null);
  assert.equal(component.donutItems().length, 4);
  component.ngOnDestroy();
});

test('owner requests retain customer filters, paginate, cancel stale selections, recover and stop on teardown', () => {
  const f = fixture();
  const { AnalystComponent } = loadSource(
    'src/app/dashboard/analyst/analyst.component.ts', f.scheduler, f.errors);
  const component = new AnalystComponent(f.service);
  component.summary.set({ totalTrades: 2, mostActiveInstruments: [
    { symbol: 'A', instrumentType: 'Stock', totalTrades: 1, tradeVolume: 10 },
    { symbol: 'B', instrumentType: 'Stock', totalTrades: 1, tradeVolume: 5 }
  ] });
  component.selectedClients.set([{ id: 42, username: 'first' }, { id: 7, username: 'second' }]);
  f.state.delay = 1000;
  f.state.owners = [{ clientId: 1, username: 'outdated', shares: 100 }];
  component.setInstrument('Stock');
  f.advance(0);
  f.state.delay = 100;
  f.state.owners = Array.from({ length: 7 }, (_, index) => ({
    clientId: index + 1, username: `owner-${index}`, shares: 100 - index
  }));
  component.selectDonutItem('B');
  f.advance(100);
  assert.equal(f.requests.at(-1).params.symbol, 'B');
  assert.equal(f.requests.at(-1).params.clientIds, '42,7');
  assert.equal(component.owners().length, 7);
  assert.equal(component.totalOwnerPages(), 2);
  component.nextOwnerPage();
  assert.equal(component.visibleOwners().length, 2);
  f.advance(1000);
  assert.equal(component.owners()[0].username, 'owner-0');
  f.state.failOwners = true;
  f.advance(10000);
  assert.match(component.ownerError(), /Unable to load/);
  assert.equal(component.owners().length, 0);
  f.state.failOwners = false;
  f.state.owners = [{ clientId: 42, username: 'first', shares: 3 }];
  f.advance(10000);
  assert.equal(component.ownerError(), null);
  assert.equal(component.owners()[0].username, 'first');
  assert.equal(component.ownerPage(), 0);
  component.removeClient(42);
  assert.equal(component.selectedSymbol(), null);
  assert.equal(component.owners().length, 0);
  component.ngOnDestroy();
  const count = f.requests.length;
  f.advance(30000);
  assert.equal(f.requests.length, count);
});
