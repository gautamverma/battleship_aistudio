package com.galacticfleet.game

import kotlin.random.Random

class GameEngine {
    val gridSize = 10

    fun createEmptyGrid() = Array(gridSize) { r ->
        Array(gridSize) { c -> Cell(r, c) }
    }

    fun createInitialShips() = ShipType.values().map { Ship(it) }

    fun canPlaceShip(grid: Array<Array<Cell>>, r: Int, c: Int, length: Int, orientation: Orientation): Boolean {
        if (orientation == Orientation.HORIZONTAL) {
            if (c + length > gridSize) return false
            for (i in 0 until length) {
                if (grid[r][c + i].status != CellStatus.EMPTY) return false
            }
        } else {
            if (r + length > gridSize) return false
            for (i in 0 until length) {
                if (grid[r + i][c].status != CellStatus.EMPTY) return false
            }
        }
        return true
    }

    fun placeShipOnGrid(grid: Array<Array<Cell>>, r: Int, c: Int, ship: Ship): Pair<Array<Array<Cell>>, Ship> {
        val positions = mutableListOf<Position>()
        for (i in 0 until ship.length) {
            val currR = if (ship.orientation == Orientation.HORIZONTAL) r else r + i
            val currC = if (ship.orientation == Orientation.HORIZONTAL) c + i else c
            grid[currR][currC].status = CellStatus.SHIP
            grid[currR][currC].shipType = ship.type
            positions.add(Position(currR, currC))
        }
        return grid to ship.copy(placed = true, positions = positions)
    }

    fun placeShipsRandomly(ships: List<Ship>): Pair<Array<Array<Cell>>, List<Ship>> {
        val grid = createEmptyGrid()
        val placedShips = mutableListOf<Ship>()
        
        ships.forEach { ship ->
            var placed = false
            while (!placed) {
                val orientation = if (Random.nextBoolean()) Orientation.HORIZONTAL else Orientation.VERTICAL
                val r = Random.nextInt(gridSize)
                val c = Random.nextInt(gridSize)
                
                if (canPlaceShip(grid, r, c, ship.length, orientation)) {
                    val (_, nextShip) = placeShipOnGrid(grid, r, c, ship.copy(orientation = orientation))
                    placedShips.add(nextShip)
                    placed = true
                }
            }
        }
        return grid to placedShips
    }

    fun getSmartAiMove(grid: Array<Array<Cell>>, memory: AiMemory): Position {
        if (memory.targets.isNotEmpty()) {
            val target = memory.targets.removeAt(0)
            if (grid[target.r][target.c].status == CellStatus.HIT || grid[target.r][target.c].status == CellStatus.MISS) {
                return getSmartAiMove(grid, memory)
            }
            return target
        }

        // Hunt mode: Parity/Checkerboard strategy
        val availableMoves = mutableListOf<Position>()
        for (r in 0 until gridSize) {
            for (c in 0 until gridSize) {
                if (grid[r][c].status == CellStatus.EMPTY || grid[r][c].status == CellStatus.SHIP) {
                    if ((r + c) % 2 == 0) availableMoves.add(Position(r, c))
                }
            }
        }

        if (availableMoves.isEmpty()) {
            // Fallback to any available cell
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    if (grid[r][c].status == CellStatus.EMPTY || grid[r][c].status == CellStatus.SHIP) {
                        availableMoves.add(Position(r, c))
                    }
                }
            }
        }

        return availableMoves.random()
    }

    fun getAdjacentCells(pos: Position): List<Position> {
        val adj = mutableListOf<Position>()
        if (pos.r > 0) adj.add(Position(pos.r - 1, pos.c))
        if (pos.r < gridSize - 1) adj.add(Position(pos.r + 1, pos.c))
        if (pos.c > 0) adj.add(Position(pos.r, pos.c - 1))
        if (pos.c < gridSize - 1) adj.add(Position(pos.r, pos.c + 1))
        return adj
    }
}
