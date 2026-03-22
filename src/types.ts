/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export type ShipType = 'carrier' | 'battleship' | 'destroyer' | 'submarine' | 'patrolBoat';

export interface Ship {
  type: ShipType;
  length: number;
  name: string;
  hits: number;
  placed: boolean;
  orientation: 'horizontal' | 'vertical';
  positions: { r: number; c: number }[];
}

export type CellStatus = 'empty' | 'ship' | 'hit' | 'miss';

export interface Cell {
  r: number;
  c: number;
  status: CellStatus;
  shipType?: ShipType;
}

export type GamePhase = 'placement' | 'playing' | 'gameOver';

export interface GameState {
  playerBoard: Cell[][];
  aiBoard: Cell[][];
  playerShips: Ship[];
  aiShips: Ship[];
  currentTurn: 'player' | 'ai';
  phase: GamePhase;
  winner?: 'player' | 'ai';
  lastAiMove?: { r: number; c: number; status: CellStatus };
  aiMemory: {
    targets: { r: number; c: number }[];
    lastHit?: { r: number; c: number };
    huntMode: boolean;
  };
}

export const SHIP_CONFIG: Record<ShipType, { length: number; name: string }> = {
  carrier: { length: 5, name: 'Star Destroyer' },
  battleship: { length: 4, name: 'Millennium Falcon' },
  destroyer: { length: 3, name: 'X-Wing' },
  submarine: { length: 3, name: 'Slave I' },
  patrolBoat: { length: 2, name: 'TIE Fighter' },
};
