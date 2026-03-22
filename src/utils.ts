/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { Cell, CellStatus, Ship, ShipType, SHIP_CONFIG } from './types';

export const GRID_SIZE = 10;

export const createEmptyGrid = (): Cell[][] => {
  const grid: Cell[][] = [];
  for (let r = 0; r < GRID_SIZE; r++) {
    const row: Cell[] = [];
    for (let c = 0; c < GRID_SIZE; c++) {
      row.push({ r, c, status: 'empty' });
    }
    grid.push(row);
  }
  return grid;
};

export const createInitialShips = (): Ship[] => {
  return Object.entries(SHIP_CONFIG).map(([type, config]) => ({
    type: type as ShipType,
    length: config.length,
    name: config.name,
    hits: 0,
    placed: false,
    orientation: 'horizontal',
    positions: [],
  }));
};

export const canPlaceShip = (
  grid: Cell[][],
  r: number,
  c: number,
  length: number,
  orientation: 'horizontal' | 'vertical'
): boolean => {
  if (orientation === 'horizontal') {
    if (c + length > GRID_SIZE) return false;
    for (let i = 0; i < length; i++) {
      if (grid[r][c + i].status !== 'empty') return false;
    }
  } else {
    if (r + length > GRID_SIZE) return false;
    for (let i = 0; i < length; i++) {
      if (grid[r + i][c].status !== 'empty') return false;
    }
  }
  return true;
};

export const placeShipOnGrid = (
  grid: Cell[][],
  r: number,
  c: number,
  ship: Ship
): { grid: Cell[][]; ship: Ship } => {
  const newGrid = grid.map((row) => row.map((cell) => ({ ...cell })));
  const positions: { r: number; c: number }[] = [];

  for (let i = 0; i < ship.length; i++) {
    const currR = ship.orientation === 'horizontal' ? r : r + i;
    const currC = ship.orientation === 'horizontal' ? c + i : c;
    newGrid[currR][currC].status = 'ship';
    newGrid[currR][currC].shipType = ship.type;
    positions.push({ r: currR, c: currC });
  }

  return {
    grid: newGrid,
    ship: { ...ship, placed: true, positions },
  };
};

export const placeShipsRandomly = (grid: Cell[][], ships: Ship[]): { grid: Cell[][]; ships: Ship[] } => {
  let currentGrid = grid.map((row) => row.map((cell) => ({ ...cell })));
  const placedShips: Ship[] = [];

  for (const ship of ships) {
    let placed = false;
    while (!placed) {
      const orientation = Math.random() > 0.5 ? 'horizontal' : 'vertical';
      const r = Math.floor(Math.random() * GRID_SIZE);
      const c = Math.floor(Math.random() * GRID_SIZE);

      if (canPlaceShip(currentGrid, r, c, ship.length, orientation)) {
        const { grid: nextGrid, ship: nextShip } = placeShipOnGrid(currentGrid, r, c, {
          ...ship,
          orientation,
        });
        currentGrid = nextGrid;
        placedShips.push(nextShip);
        placed = true;
      }
    }
  }

  return { grid: currentGrid, ships: placedShips };
};

export const getSmartAiMove = (
  grid: Cell[][],
  memory: { targets: { r: number; c: number }[]; lastHit?: { r: number; c: number }; huntMode: boolean }
): { r: number; c: number; newMemory: typeof memory } => {
  let r: number, c: number;
  let newMemory = { ...memory };

  if (newMemory.targets.length > 0) {
    // Target mode: pop a target from the list
    const target = newMemory.targets.shift()!;
    r = target.r;
    c = target.c;
    
    // If we've already hit/missed this cell, try again
    if (grid[r][c].status === 'hit' || grid[r][c].status === 'miss') {
      return getSmartAiMove(grid, newMemory);
    }
  } else {
    // Hunt mode: pick a random cell (parity strategy)
    const availableMoves: { r: number; c: number }[] = [];
    for (let row = 0; row < GRID_SIZE; row++) {
      for (let col = 0; col < GRID_SIZE; col++) {
        // Only target cells that haven't been attacked
        if (grid[row][col].status === 'empty' || grid[row][col].status === 'ship') {
          // Parity: target cells where (r + c) is even (checkerboard pattern)
          if ((row + col) % 2 === 0) {
            availableMoves.push({ r: row, c: col });
          }
        }
      }
    }

    // Fallback if no parity moves left
    if (availableMoves.length === 0) {
      for (let row = 0; row < GRID_SIZE; row++) {
        for (let col = 0; col < GRID_SIZE; col++) {
          if (grid[row][col].status === 'empty' || grid[row][col].status === 'ship') {
            availableMoves.push({ r: row, c: col });
          }
        }
      }
    }

    const move = availableMoves[Math.floor(Math.random() * availableMoves.length)];
    r = move.r;
    c = move.c;
  }

  return { r, c, newMemory };
};

export const getAdjacentCells = (r: number, c: number): { r: number; c: number }[] => {
  const adj = [];
  if (r > 0) adj.push({ r: r - 1, c });
  if (r < GRID_SIZE - 1) adj.push({ r: r + 1, c });
  if (c > 0) adj.push({ r, c: c - 1 });
  if (c < GRID_SIZE - 1) adj.push({ r, c: c + 1 });
  return adj;
};

export const getSunkShip = (ships: Ship[]): Ship | null => {
  return ships.find((ship) => ship.hits === ship.length && !ship.positions.every(p => false)) || null;
};

export const checkWin = (ships: Ship[]): boolean => {
  return ships.every((ship) => ship.hits === ship.length);
};
