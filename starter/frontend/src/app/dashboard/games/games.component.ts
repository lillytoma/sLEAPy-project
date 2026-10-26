import { Component, ElementRef, QueryList, ViewChildren, signal } from '@angular/core';

type Tile = number | null;
type WordleLetterState = 'correct' | 'present' | 'absent' | 'empty';

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
  readonly activeGame = signal<'connections' | 'puzzle' | 'wordle'>('wordle');
  readonly connectionCards = signal<ConnectionCard[]>([]);
  readonly selectedConnections = signal<number[]>([]);
  readonly solvedConnectionGroups = signal<string[]>([]);
  readonly connectionMistakes = signal(0);
  readonly maxConnectionMistakes = 4;
  readonly wordleAnswer = 'SHARE';
  readonly wordleGuesses = signal<string[]>([]);
  readonly wordleInput = signal('');
  readonly wordleMaxGuesses = 6;
  readonly wordleMessage = signal('');
  @ViewChildren('wordleTile') private wordleTiles!: QueryList<ElementRef<HTMLInputElement>>;

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

  selectGame(game: 'connections' | 'puzzle' | 'wordle'): void {
    this.activeGame.set(game);
  }

  submitWordleGuess(): void {
    const guess = this.wordleInput().trim().toUpperCase();
    if (!/^[A-Z]{5}$/.test(guess)) {
      this.wordleMessage.set('Enter a five-letter word.');
      return;
    }
    if (/([A-Z])\1{2}/.test(guess)) {
      this.wordleMessage.set('A guess cannot repeat the same letter three times in a row.');
      return;
    }
    if (this.wordleGuesses().includes(guess)) {
      this.wordleMessage.set('You already tried that word.');
      return;
    }
    if (this.wordleGuesses().length >= this.wordleMaxGuesses || this.wordleComplete()) return;

    this.wordleGuesses.update(guesses => [...guesses, guess]);
    this.wordleInput.set('');
    this.wordleMessage.set('');
    if (!this.wordleFinished()) this.focusWordleTile(0);
  }

  resetWordle(): void {
    this.wordleGuesses.set([]);
    this.wordleInput.set('');
    this.wordleMessage.set('');
  }

  wordleComplete(): boolean {
    return this.wordleGuesses().includes(this.wordleAnswer);
  }

  wordleFinished(): boolean {
    return this.wordleComplete() || this.wordleGuesses().length >= this.wordleMaxGuesses;
  }

  wordleLetterState(guess: string, index: number): WordleLetterState {
    if (!guess[index]) return 'empty';
    if (guess[index] === this.wordleAnswer[index]) return 'correct';
    if (this.wordleAnswer.includes(guess[index])) return 'present';
    return 'absent';
  }

  wordleKeyboardState(letter: string): WordleLetterState {
    const guesses = this.wordleGuesses();
    if (guesses.some(guess => guess.split('').some((value, index) => value === letter && this.wordleLetterState(guess, index) === 'correct'))) return 'correct';
    if (guesses.some(guess => guess.split('').some((value, index) => value === letter && this.wordleLetterState(guess, index) === 'present'))) return 'present';
    if (guesses.some(guess => guess.includes(letter))) return 'absent';
    return 'empty';
  }

  onWordlePanelClick(event: MouseEvent): void {
    if (this.wordleFinished()) return;
    if (event.target instanceof Element && event.target.closest('input, button')) return;
    this.focusWordleTile(this.wordleInput().length);
  }

  focusWordleTile(index: number): void {
    const tile = this.wordleTiles.get(Math.max(0, Math.min(index, this.wordleInput().length, 4)))?.nativeElement;
    tile?.focus();
    tile?.select();
  }

  updateWordleTile(index: number, input: HTMLInputElement): void {
    if (this.wordleFinished()) return;
    const letters = input.value.replace(/[^a-z]/gi, '').toUpperCase();
    const current = this.wordleInput();
    this.wordleInput.set((current.slice(0, index) + letters + current.slice(index + Math.max(letters.length, 1))).slice(0, 5));
    input.value = this.wordleInput()[index] || '';
    this.wordleMessage.set('');
    this.focusWordleTile(letters ? index + letters.length : index);
  }

  onWordleKeydown(event: KeyboardEvent, index: number): void {
    if (this.wordleFinished() || event.ctrlKey || event.metaKey || event.altKey) return;
    if (event.key === 'Enter') {
      event.preventDefault();
      this.submitWordleGuess();
    } else if (event.key === 'Backspace' || event.key === 'Delete') {
      event.preventDefault();
      const position = event.key === 'Backspace' && !this.wordleInput()[index] ? index - 1 : index;
      if (position < 0) return;
      this.wordleInput.update(value => value.slice(0, position) + value.slice(position + 1));
      this.wordleMessage.set('');
      this.focusWordleTile(position);
    } else if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
      event.preventDefault();
      this.focusWordleTile(index + (event.key === 'ArrowLeft' ? -1 : 1));
    }
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
