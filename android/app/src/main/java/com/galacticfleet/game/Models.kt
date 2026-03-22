package com.galacticfleet.game

enum class ShipType(val length: Int, val shipName: String) {
    CARRIER(5, "Star Destroyer"),
    BATTLESHIP(4, "Millennium Falcon"),
    DESTROYER(3, "X-Wing"),
    SUBMARINE(3, "Slave I"),
    PATROL_BOAT(2, "TIE Fighter")
}

enum class CellStatus { EMPTY, SHIP, HIT, MISS }
enum class GamePhase { PLACEMENT, PLAYING, GAME_OVER }
enum class Orientation { HORIZONTAL, VERTICAL }

data class Position(val r: Int, val c: Int)

data class Ship(
    val type: ShipType,
    val length: Int = type.length,
    val name: String = type.shipName,
    var hits: Int = 0,
    var placed: Boolean = false,
    var orientation: Orientation = Orientation.HORIZONTAL,
    var positions: List<Position> = emptyList()
) {
    val isSunk: Boolean get() = hits >= length
}

data class Cell(
    val r: Int,
    val c: Int,
    var status: CellStatus = CellStatus.EMPTY,
    var shipType: ShipType? = null
)

data class AiMemory(
    val targets: MutableList<Position> = mutableListOf(),
    var lastHit: Position? = null,
    var huntMode: Boolean = true
)
