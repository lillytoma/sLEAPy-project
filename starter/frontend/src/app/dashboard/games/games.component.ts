import { Component, signal } from '@angular/core';

type Tile = number | null;

@Component({
  selector: 'app-games',
  standalone: true,
  templateUrl: './games.component.html'
})
export class GamesComponent {
  readonly gridSize = 3;
  readonly tileCount = this.gridSize * this.gridSize;
  readonly board = signal<Tile[]>([]);
  readonly moves = signal(0);
  readonly isComplete = signal(false);

  constructor() {
    this.newGame();
  }

  newGame(): void {
    const solved: Tile[] = Array.from({ length: this.tileCount - 1 }, (_, index) => index);
    solved.push(null);

    let shuffled = [...solved];
    let emptyIndex = shuffled.length - 1;
    let previousIndex = -1;
    for (let step = 0; step < 80; step++) {
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
    const row = Math.floor(tile / this.gridSize);
    const column = tile % this.gridSize;
    return `${column * 50}% ${row * 50}%`;
  }

  private neighborsOf(index: number): number[] {
    const row = Math.floor(index / this.gridSize);
    const column = index % this.gridSize;
    const neighbors: number[] = [];
    if (row > 0) neighbors.push(index - this.gridSize);
    if (row < this.gridSize - 1) neighbors.push(index + this.gridSize);
    if (column > 0) neighbors.push(index - 1);
    if (column < this.gridSize - 1) neighbors.push(index + 1);
    return neighbors;
  }
}
