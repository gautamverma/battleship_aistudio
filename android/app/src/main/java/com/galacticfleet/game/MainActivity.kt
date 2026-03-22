package com.galacticfleet.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
    val aiMemory = remember { AiMemory() }

    // Initialize AI
    LaunchedEffect(Unit) {
        val (grid, ships) = engine.placeShipsRandomly(engine.createInitialShips())
        aiGrid = grid
        aiShips = ships
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(16.dp)) {
        Text(
            text = "GALACTIC FLEET",
            style = TextStyle(color = Color.Cyan, fontWeight = FontWeight.Black, fontSize = 24.sp)
        )
        
        Text(text = message, color = Color.White, modifier = Modifier.padding(vertical = 8.dp))

        // Enemy Grid (AI)
        Text("ENEMY SECTOR", color = Color.Red, fontSize = 12.sp)
        GridView(grid = aiGrid, isPlayer = false) { r, c ->
            if (phase == GamePhase.PLAYING && currentTurn == "player") {
                // Handle player move logic
                if (aiGrid[r][c].status == CellStatus.EMPTY || aiGrid[r][c].status == CellStatus.SHIP) {
                    val isHit = aiGrid[r][c].status == CellStatus.SHIP
                    if (isHit) {
                        val shipType = aiGrid[r][c].shipType!!
                        // Instant destruction logic
                        aiGrid.forEach { row ->
                            row.forEach { cell ->
                                if (cell.shipType == shipType) cell.status = CellStatus.HIT
                            }
                        }
                        message = "CRITICAL HIT! ENEMY DESTROYED!"
                    } else {
                        aiGrid[r][c].status = CellStatus.MISS
                        message = "TURBOLASER MISSED."
                    }
                    currentTurn = "ai"
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Player Grid
        Text("FRIENDLY SECTOR", color = Color.Blue, fontSize = 12.sp)
        GridView(grid = playerGrid, isPlayer = true) { r, c ->
            if (phase == GamePhase.PLACEMENT) {
                // Simplified placement for demo
            }
        }
    }
}

@Composable
fun GridView(grid: Array<Array<Cell>>, isPlayer: Boolean, onCellClick: (Int, Int) -> Unit) {
    Column {
        for (r in 0 until 10) {
            Row {
                for (c in 0 until 10) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .border(0.5.dp, Color.White.copy(alpha = 0.1f))
                            .background(getCellColor(grid[r][c], isPlayer))
                            .clickable { onCellClick(r, c) }
                    )
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
