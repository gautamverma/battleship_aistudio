package com.galacticfleet.game

import android.os.Bundle
import android.media.AudioManager
import android.media.ToneGenerator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GalacticFleetGame()
        }
    }
}

@Composable
fun GalacticFleetGame() {
    val engine = remember { GameEngine() }
    var playerGrid by remember { mutableStateOf(engine.createEmptyGrid()) }
    var aiGrid by remember { mutableStateOf(engine.createEmptyGrid()) }
    var playerShips by remember { mutableStateOf(engine.createInitialShips()) }
    var aiShips by remember { mutableStateOf(engine.createInitialShips()) }
    var phase by remember { mutableStateOf(GamePhase.PLACEMENT) }
    var currentTurn by remember { mutableStateOf("player") }
    var message by remember { mutableStateOf("Deploy your fleet to the grid") }
    var orientation by remember { mutableStateOf(Orientation.HORIZONTAL) }
    var selectedShipIndex by remember { mutableStateOf(0) }
    var hoveredCell by remember { mutableStateOf<Position?>(null) }
    val aiMemory = remember { AiMemory() }

    // Initialize AI
    LaunchedEffect(Unit) {
        val (grid, ships) = engine.placeShipsRandomly(engine.createInitialShips())
        aiGrid = grid
        aiShips = ships
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "GALACTIC FLEET",
                style = TextStyle(color = Color.Cyan, fontWeight = FontWeight.Black, fontSize = 24.sp)
            )
            
            if (phase == GamePhase.PLACEMENT) {
                Button(
                    onClick = { orientation = if (orientation == Orientation.HORIZONTAL) Orientation.VERTICAL else Orientation.HORIZONTAL },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                ) {
                    Text(text = orientation.name, color = Color.White, fontSize = 10.sp)
                }
            }
        }
        
        Text(text = message, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))

        // Enemy Grid (AI)
        Text("ENEMY SECTOR", color = Color.Red, fontSize = 12.sp)
        GridView(grid = aiGrid, isPlayer = false) { r, c ->
            if (phase == GamePhase.PLAYING && currentTurn == "player") {
                if (aiGrid[r][c].status == CellStatus.EMPTY || aiGrid[r][c].status == CellStatus.SHIP) {
                    val isHit = aiGrid[r][c].status == CellStatus.SHIP
                    if (isHit) {
                        val shipType = aiGrid[r][c].shipType!!
                        aiGrid.forEach { row ->
                            row.forEach { cell ->
                                if (cell.shipType == shipType) cell.status = CellStatus.HIT
                            }
                        }
                        aiShips.find { it.type == shipType }?.let { it.hits = it.length }
                        message = "CRITICAL HIT! ENEMY DESTROYED!"
                        playExplosionSound()
                    } else {
                        aiGrid[r][c].status = CellStatus.MISS
                        message = "TURBOLASER MISSED."
                        playMissSound()
                    }
                    
                    if (aiShips.all { it.isSunk }) {
                        phase = GamePhase.GAME_OVER
                        message = "VICTORY! THE EMPIRE HAS FALLEN."
                    } else {
                        currentTurn = "ai"
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Player Grid
        Text("FRIENDLY SECTOR", color = Color.Blue, fontSize = 12.sp)
        GridView(
            grid = playerGrid, 
            isPlayer = true,
            placementPreview = if (phase == GamePhase.PLACEMENT && hoveredCell != null) {
                val ship = playerShips[selectedShipIndex]
                val isValid = engine.canPlaceShip(playerGrid, hoveredCell!!.r, hoveredCell!!.c, ship.length, orientation)
                if (isValid) {
                    List(ship.length) { i ->
                        if (orientation == Orientation.HORIZONTAL) Position(hoveredCell!!.r, hoveredCell!!.c + i)
                        else Position(hoveredCell!!.r + i, hoveredCell!!.c)
                    }
                } else null
            } else null,
            onHover = { r, c -> hoveredCell = Position(r, c) }
        ) { r, c ->
            if (phase == GamePhase.PLACEMENT && selectedShipIndex < playerShips.size) {
                val ship = playerShips[selectedShipIndex]
                if (engine.canPlaceShip(playerGrid, r, c, ship.length, orientation)) {
                    val (nextGrid, nextShip) = engine.placeShipOnGrid(playerGrid, r, c, ship.copy(orientation = orientation))
                    playerGrid = nextGrid
                    val newShips = playerShips.toMutableList()
                    newShips[selectedShipIndex] = nextShip
                    playerShips = newShips
                    
                    if (selectedShipIndex < playerShips.size - 1) {
                        selectedShipIndex++
                    } else {
                        phase = GamePhase.PLAYING
                        message = "BATTLE STATIONS!"
                    }
                }
            }
        }
    }

    // AI Move Logic
    if (phase == GamePhase.PLAYING && currentTurn == "ai") {
        LaunchedEffect(currentTurn) {
            delay(1200)
            val move = engine.getSmartAiMove(playerGrid, aiMemory)
            val cell = playerGrid[move.r][move.c]
            
            if (cell.status == CellStatus.SHIP) {
                val shipType = cell.shipType!!
                playerGrid.forEach { row ->
                    row.forEach { c ->
                        if (c.shipType == shipType) c.status = CellStatus.HIT
                    }
                }
                playerShips.find { it.type == shipType }?.let { it.hits = it.length }
                message = "ALERT! OUR FLEET HAS BEEN HIT!"
                playExplosionSound()
                
                aiMemory.lastHit = move
                aiMemory.huntMode = false
                engine.getAdjacentCells(move).forEach { adj ->
                    if (playerGrid[adj.r][adj.c].status == CellStatus.EMPTY || playerGrid[adj.r][adj.c].status == CellStatus.SHIP) {
                        aiMemory.targets.add(adj)
                    }
                }
            } else {
                playerGrid[move.r][move.c].status = CellStatus.MISS
                message = "ENEMY TURBOLASER MISSED."
                playMissSound()
            }
            
            if (playerShips.all { it.isSunk }) {
                phase = GamePhase.GAME_OVER
                message = "DEFEAT! THE REBELLION IS CRUSHED."
            } else {
                currentTurn = "player"
            }
        }
    }
}

fun playExplosionSound() {
    try {
        val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        toneG.startTone(ToneGenerator.TONE_CDMA_LOW_L, 500)
    } catch (e: Exception) {}
}

fun playMissSound() {
    try {
        val toneG = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        toneG.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
    } catch (e: Exception) {}
}

@Composable
fun GridView(
    grid: Array<Array<Cell>>, 
    isPlayer: Boolean, 
    placementPreview: List<Position>? = null,
    onHover: ((Int, Int) -> Unit)? = null,
    onCellClick: (Int, Int) -> Unit
) {
    Column {
        for (r in 0 until 10) {
            Row {
                for (c in 0 until 10) {
                    val isPreview = placementPreview?.contains(Position(r, c)) == true
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f))
                            .background(
                                if (isPreview) Color.Green.copy(alpha = 0.3f) 
                                else getCellColor(grid[r][c], isPlayer)
                            )
                            .clickable { onCellClick(r, c) }
                    ) {
                        // In a real app, we'd add onPointerEvent for hover, 
                        // but for this demo we'll trigger hover on click or simplified logic
                    }
                }
            }
        }
    }
}

fun getCellColor(cell: Cell, isPlayer: Boolean): Color {
    return when (cell.status) {
        CellStatus.HIT -> Color.Red
        CellStatus.MISS -> Color.Gray
        CellStatus.SHIP -> if (isPlayer) Color.Blue.copy(alpha = 0.4f) else Color.Transparent
        CellStatus.EMPTY -> Color.Transparent
    }
}
