import { Component, signal } from '@angular/core';

type Tile = number | null;

interface ConnectionCard {
  word: string;
  group: string;
  category: string;
}

const CONNECTION_CARDS: ConnectionCard[] = [
  { word: 'BULL', group: 'market', category: 'Market animals' },
  { word: 'BEAR', group: 'market', category: 'Market animals' },
  { word: 'UNICORN', group: 'market', category: 'Market animals' },
  { word: 'DOVE', group: 'market', category: 'Market animals' },
  { word: 'CALL', group: 'options', category: 'Option types' },
  { word: 'PUT', group: 'options', category: 'Option types' },
  { word: 'SPREAD', group: 'options', category: 'Option types' },
  { word: 'STRADDLE', group: 'options', category: 'Option types' },
  { word: 'APPLE', group: 'ticker', category: 'Ticker symbols' },
  { word: 'FORD', group: 'ticker', category: 'Ticker symbols' },
  { word: 'META', group: 'ticker', category: 'Ticker symbols' },
  { word: 'SNAP', group: 'ticker', category: 'Ticker symbols' },
  { word: 'MARGIN', group: 'brokerage', category: 'Brokerage terms' },
  { word: 'DIVIDEND', group: 'brokerage', category: 'Brokerage terms' },
  { word: 'LIMIT', group: 'brokerage', category: 'Brokerage terms' },
  { word: 'YIELD', group: 'brokerage', category: 'Brokerage terms' }
];

@Component({
  selector: 'app-games',
  standalone: true,
  templateUrl: './games.component.html'
})
export class GamesComponent {
  readonly activeGame = signal<'connections' | 'puzzle'>('puzzle');
  readonly connectionCards = signal<ConnectionCard[]>([]);
  readonly selectedConnections = signal<number[]>([]);
  readonly solvedConnectionGroups = signal<string[]>([]);
  readonly connectionMistakes = signal(0);
  readonly maxConnectionMistakes = 4;

  readonly puzzleSizes = [3, 6, 8] as const;
  readonly puzzleSize = signal<number>(8);
  get tileCount(): number {
    return this.puzzleSize() * this.puzzleSize();
  }
  readonly board = signal<Tile[]>([]);
  readonly moves = signal(0);
  readonly isComplete = signal(false);

  constructor() {
    this.newConnectionsGame();
    this.newGame();
  }

  selectGame(game: 'connections' | 'puzzle'): void {
    this.activeGame.set(game);
  }

  selectPuzzleSize(size: number): void {
    if (!this.puzzleSizes.includes(size as (typeof this.puzzleSizes)[number])) return;
    this.puzzleSize.set(size);
    this.newGame();
  }

  newConnectionsGame(): void {
    this.connectionCards.set(this.shuffle([...CONNECTION_CARDS]));
    this.selectedConnections.set([]);
    this.solvedConnectionGroups.set([]);
    this.connectionMistakes.set(0);
  }

  toggleConnection(index: number): void {
    if (this.solvedConnectionGroups().includes(this.connectionCards()[index].group)) return;
    const selected = this.selectedConnections();
    if (selected.includes(index)) {
      this.selectedConnections.set(selected.filter(item => item !== index));
    } else if (selected.length < 4) {
      this.selectedConnections.set([...selected, index]);
    }
  }

  submitConnection(): void {
    if (this.selectedConnections().length !== 4) return;
    const cards = this.connectionCards();
    const groups = new Set(this.selectedConnections().map(index => cards[index].group));
    if (groups.size === 1) {
      this.solvedConnectionGroups.update(groupsFound => [...groupsFound, cards[this.selectedConnections()[0]].group]);
      this.selectedConnections.set([]);
    } else {
      this.connectionMistakes.update(mistakes => mistakes + 1);
    }
  }

  isConnectionSolved(index: number): boolean {
    return this.solvedConnectionGroups().includes(this.connectionCards()[index].group);
  }

  connectionCategory(group: string): string {
    return this.connectionCards().find(card => card.group === group)?.category || '';
  }

  connectionWords(group: string): string {
    return this.connectionCards().filter(card => card.group === group).map(card => card.word).join(' · ');
  }

  connectionComplete(): boolean {
    return this.solvedConnectionGroups().length === 4;
  }

  newGame(): void {
    const solved: Tile[] = Array.from({ length: this.tileCount - 1 }, (_, index) => index);
    solved.push(null);

    let shuffled = [...solved];
    let emptyIndex = shuffled.length - 1;
    let previousIndex = -1;
    for (let step = 0; step < this.puzzleSize() * this.puzzleSize() * 2; step++) {
      const neighbors = this.neighborsOf(emptyIndex).filter(index => index !== previousIndex);
      const nextIndex = neighbors[Math.floor(Math.random() * neighbors.length)];
      [shuffled[emptyIndex], shuffled[nextIndex]] = [shuffled[nextIndex], shuffled[emptyIndex]];
      previousIndex = emptyIndex;
      emptyIndex = nextIndex;
    }

    this.board.set(shuffled);
    this.moves.set(0);
    this.isComplete.set(false);
  }

  moveTile(index: number): void {
    const emptyIndex = this.board().indexOf(null);
    if (!this.neighborsOf(emptyIndex).includes(index)) return;

    const nextBoard = [...this.board()];
    [nextBoard[index], nextBoard[emptyIndex]] = [nextBoard[emptyIndex], nextBoard[index]];
    this.board.set(nextBoard);
    this.moves.update(value => value + 1);

    if (nextBoard.every((tile, tileIndex) => tile === (tileIndex === this.tileCount - 1 ? null : tileIndex))) {
      this.isComplete.set(true);
    }
  }

  tileLabel(tile: Tile): string {
    return tile === null ? 'Empty space' : `Puzzle tile ${tile + 1}`;
  }

  tilePosition(tile: number): string {
    const row = Math.floor(tile / this.puzzleSize());
    const column = tile % this.puzzleSize();
    const positionStep = 100 / (this.puzzleSize() - 1);
    return `${column * positionStep}% ${row * positionStep}%`;
  }

  private neighborsOf(index: number): number[] {
    const row = Math.floor(index / this.puzzleSize());
    const column = index % this.puzzleSize();
    const neighbors: number[] = [];
    if (row > 0) neighbors.push(index - this.puzzleSize());
    if (row < this.puzzleSize() - 1) neighbors.push(index + this.puzzleSize());
    if (column > 0) neighbors.push(index - 1);
    if (column < this.puzzleSize() - 1) neighbors.push(index + 1);
    return neighbors;
  }

  private shuffle<T>(items: T[]): T[] {
    for (let index = items.length - 1; index > 0; index--) {
      const swapIndex = Math.floor(Math.random() * (index + 1));
      [items[index], items[swapIndex]] = [items[swapIndex], items[index]];
    }
    return items;
  }
}
