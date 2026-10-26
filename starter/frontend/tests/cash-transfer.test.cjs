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

class EventEmitter {
  emit() {}
}

class HttpErrorResponse extends Error {
  constructor(error) {
    super('Request failed');
    this.error = error;
  }
}

function loadSource(relativePath, globals = {}) {
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
    setTimeout: () => 1,
    ...globals,
    require: name => {
      if (name === '@angular/core') {
        return {
          Component: () => type => type, Injectable: () => type => type,
          Input: () => () => {}, Output: () => () => {},
          signal, computed: value => value, EventEmitter
        };
      }
      if (name === '@angular/common/http') return { HttpErrorResponse };
      if (name === 'rxjs') return rx;
      if (name.endsWith('/environment')) return { environment: { apiUrl: '' } };
      return {};
    }
  };
  vm.runInNewContext(compiled, context);
  return context.exports;
}

const { AddCashModalComponent } = loadSource('src/app/dashboard/shared/add-cash-modal.component.ts');

test('withdrawal validates cents, positive amounts, and available cash including the exact balance', () => {
  const modal = new AddCashModalComponent({});
  modal.mode = 'withdraw';
  modal.currentBalance = 125.50;
  for (const amount of ['', '.', '0', '-1', 'abc', '1e2', '1.001', '125.51']) {
    modal.amount = amount;
    assert.equal(modal.isValid(), false, amount);
  }
  for (const amount of ['0.01', '25.50', '125.50']) {
    modal.amount = amount;
    assert.equal(modal.isValid(), true, amount);
  }
  modal.currentBalance = 0;
  assert.equal(modal.isValid(), false);
});

test('deposit default still permits adding more than the current balance', () => {
  let deposited;
  const modal = new AddCashModalComponent({ addCash: amount => { deposited = amount; return rx.of(1000); } });
  modal.amount = '1000';
  assert.equal(modal.isValid(), true);
  modal.submit();
  assert.equal(deposited, 1000);
  assert.equal(modal.success(), true);
});

test('withdrawal calls the correct API, blocks duplicates, and reports success', () => {
  const response = new rx.Subject();
  const requests = [];
  const modal = new AddCashModalComponent({
    withdrawCash: amount => { requests.push(amount); return response; }
  });
  modal.mode = 'withdraw';
  modal.currentBalance = 125.50;
  modal.amount = '25.50';
  modal.submit();
  modal.submit();
  assert.deepEqual(requests, [25.50]);
  assert.equal(modal.loading(), true);
  response.next(100);
  assert.equal(modal.loading(), false);
  assert.equal(modal.success(), true);
  assert.equal(modal.amount, '');
  modal.submit();
  assert.equal(requests.length, 1);
});

test('overdraw does not submit and server errors are shown for stale balances', () => {
  let requests = 0;
  const modal = new AddCashModalComponent({
    withdrawCash: () => {
      requests++;
      return rx.throwError(() => new HttpErrorResponse({ message: 'Insufficient cash balance.' }));
    }
  });
  modal.mode = 'withdraw';
  modal.currentBalance = 10;
  modal.amount = '10.01';
  modal.submit();
  assert.equal(requests, 0);
  assert.match(modal.error(), /exceeds/);
  modal.amount = '10';
  modal.submit();
  assert.equal(requests, 1);
  assert.equal(modal.error(), 'Insufficient cash balance.');
  assert.equal(modal.loading(), false);
  assert.equal(modal.success(), false);
  assert.equal(modal.amount, '10');
});

test('cash service posts the amount and uses the server balance without changing holdings', () => {
  let clientId = null;
  const requests = [];
  const response = new rx.Subject();
  const { PortfolioService } = loadSource('src/app/services/portfolio.service.ts', {
    localStorage: { getItem: () => clientId }
  });
  const service = new PortfolioService({
    post: (url, body) => { requests.push({ url, amount: body.amount }); return response; }
  }, { priceUpdates$: rx.EMPTY });
  clientId = '1';
  service.availableCash.set(125.50);
  service.holdings.set([{ shares: 2, current: 10 }]);
  service.withdrawCash(25.50).subscribe();
  assert.deepEqual(requests, [{ url: '/api/clients/1/withdraw', amount: 25.50 }]);
  assert.equal(service.availableCash(), 125.50);
  response.next(100);
  assert.equal(service.availableCash(), 100);
  assert.equal(service.netWorth(), 120);
  assert.equal(service.holdings().length, 1);
});

test('failed withdrawal leaves the cached balance unchanged', () => {
  let clientId = null;
  const { PortfolioService } = loadSource('src/app/services/portfolio.service.ts', {
    localStorage: { getItem: () => clientId }
  });
  const service = new PortfolioService({
    post: () => rx.throwError(() => new HttpErrorResponse({ error: 'Your account is frozen.' }))
  }, { priceUpdates$: rx.EMPTY });
  clientId = '1';
  service.availableCash.set(125.50);
  let error;
  service.withdrawCash(25.50).subscribe({ error: value => { error = value; } });
  assert.ok(error instanceof HttpErrorResponse);
  assert.equal(service.availableCash(), 125.50);
});
