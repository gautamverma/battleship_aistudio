package com.galacticfleet.game

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF10B981),
                    secondary = Color(0xFF3B82F6),
                    background = Color(0xFF050505),
                    surface = Color(0xFF111111)
                )
            ) {
                GalacticFleetGame()
            }
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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF050505))) {
        // Starfield Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val random = java.util.Random(42)
            repeat(100) {
                drawCircle(
                    color = Color.White.copy(alpha = random.nextFloat() * 0.5f),
                    radius = random.nextFloat() * 2f,
                    center = Offset(random.nextFloat() * size.width, random.nextFloat() * size.height)
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GALACTIC FLEET",
                        style = TextStyle(
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-1).sp
                        )
                    )
                    Text(
                        text = "SYSTEM ONLINE",
                        style = TextStyle(
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 2.sp
                        )
                    )
                }
                
                if (phase == GamePhase.PLACEMENT) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { orientation = if (orientation == Orientation.HORIZONTAL) Orientation.VERTICAL else Orientation.HORIZONTAL },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = orientation.name, fontSize = 10.sp)
                        }
                        Button(
                            onClick = { 
                                val (grid, ships) = engine.placeShipsRandomly(engine.createInitialShips())
                                playerGrid = grid
                                playerShips = ships
                                phase = GamePhase.PLAYING
                                message = "BATTLE STATIONS!"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(text = "AUTO", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 10.sp)
                        }
                    }
                }
            }

            // Status Message
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.6f)),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = message,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }

            // Grids
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Enemy Grid
                Column(modifier = Modifier.weight(1f)) {
                    Text("ENEMY SECTOR", color = Color.Red.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).padding(8.dp)) {
                        GridView(
                            grid = aiGrid, 
                            isPlayer = false,
                            enabled = phase == GamePhase.PLAYING && currentTurn == "player"
                        ) { r, c ->
                            if (phase == GamePhase.PLAYING && currentTurn == "player") {
                                if (aiGrid[r][c].status == CellStatus.EMPTY || aiGrid[r][c].status == CellStatus.SHIP) {
                                    val isHit = aiGrid[r][c].status == CellStatus.SHIP
                                    if (isHit) {
                                        val shipType = aiGrid[r][c].shipType!!
                                        val newGrid = aiGrid.map { row -> row.map { it.copy() }.toTypedArray() }.toTypedArray()
                                        newGrid.forEach { row ->
                                            row.forEach { cell ->
                                                if (cell.shipType == shipType) cell.status = CellStatus.HIT
                                            }
                                        }
                                        aiGrid = newGrid
                                        val newShips = aiShips.map { if (it.type == shipType) it.copy(hits = it.length) else it }
                                        aiShips = newShips
                                        message = "CRITICAL HIT! ENEMY ${shipType.shipName.uppercase()} DESTROYED!"
                                        playExplosionSound()
                                    } else {
                                        val newGrid = aiGrid.map { row -> row.map { it.copy() }.toTypedArray() }.toTypedArray()
                                        newGrid[r][c].status = CellStatus.MISS
                                        aiGrid = newGrid
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
                    }
                }

                // Player Grid
                Column(modifier = Modifier.weight(1f)) {
                    Text("FRIENDLY SECTOR", color = Color(0xFF3B82F6).copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.weight(1f).aspectRatio(1f).background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(16.dp)).border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).padding(8.dp)) {
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
                }
            }
        }

        // Game Over Overlay
        if (phase == GamePhase.GAME_OVER) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.9f)).clickable { },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Text(
                        text = if (message.contains("VICTORY")) "MISSION ACCOMPLISHED" else "FLEET DESTROYED",
                        style = TextStyle(color = Color.White, fontWeight = FontWeight.Black, fontSize = 32.sp, textAlign = TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = message,
                        style = TextStyle(color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp, textAlign = TextAlign.Center)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            playerGrid = engine.createEmptyGrid()
                            val (g, s) = engine.placeShipsRandomly(engine.createInitialShips())
                            aiGrid = g
                            aiShips = s
                            playerShips = engine.createInitialShips()
                            phase = GamePhase.PLACEMENT
                            currentTurn = "player"
                            message = "Deploy your fleet to the grid"
                            selectedShipIndex = 0
                            aiMemory.targets.clear()
                            aiMemory.huntMode = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("INITIATE NEW SORTIE", color = Color.Black, fontWeight = FontWeight.Black)
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
                val newGrid = playerGrid.map { row -> row.map { it.copy() }.toTypedArray() }.toTypedArray()
                newGrid.forEach { row ->
                    row.forEach { c ->
                        if (c.shipType == shipType) c.status = CellStatus.HIT
                    }
                }
                playerGrid = newGrid
                val newShips = playerShips.map { if (it.type == shipType) it.copy(hits = it.length) else it }
                playerShips = newShips
                message = "ALERT! OUR ${shipType.shipName.uppercase()} DESTROYED!"
                playExplosionSound()
                
                aiMemory.lastHit = move
                aiMemory.huntMode = false
                engine.getAdjacentCells(move).forEach { adj ->
                    if (playerGrid[adj.r][adj.c].status == CellStatus.EMPTY || playerGrid[adj.r][adj.c].status == CellStatus.SHIP) {
                        aiMemory.targets.add(adj)
                    }
                }
            } else {
                val newGrid = playerGrid.map { row -> row.map { it.copy() }.toTypedArray() }.toTypedArray()
                newGrid[move.r][move.c].status = CellStatus.MISS
                playerGrid = newGrid
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

@Composable
fun GridView(
    grid: Array<Array<Cell>>, 
    isPlayer: Boolean, 
    enabled: Boolean = true,
    placementPreview: List<Position>? = null,
    onHover: ((Int, Int) -> Unit)? = null,
    onCellClick: (Int, Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        for (r in 0 until 10) {
            Row(modifier = Modifier.weight(1f)) {
                for (c in 0 until 10) {
                    val cell = grid[r][c]
                    val isPreview = placementPreview?.contains(Position(r, c)) == true
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, Color.White.copy(alpha = 0.05f))
                            .background(
                                when {
                                    isPreview -> Color(0xFF10B981).copy(alpha = 0.2f)
                                    cell.status == CellStatus.HIT -> Color.Red.copy(alpha = 0.4f)
                                    cell.status == CellStatus.MISS -> Color.White.copy(alpha = 0.1f)
                                    cell.status == CellStatus.SHIP && isPlayer -> Color(0xFF3B82F6).copy(alpha = 0.2f)
                                    else -> Color.Transparent
                                }
                            )
                            .clickable(enabled = enabled) { 
                                onCellClick(r, c)
                                onHover?.invoke(r, c)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (cell.status == CellStatus.HIT) {
                            ShipIcon(type = cell.shipType!!, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(4.dp))
                        } else if (cell.status == CellStatus.SHIP && isPlayer) {
                            ShipIcon(type = cell.shipType!!, color = Color(0xFF3B82F6).copy(alpha = 0.6f), modifier = Modifier.padding(4.dp))
                        }
                        
                        if (cell.status == CellStatus.HIT) {
                            Box(modifier = Modifier.fillMaxSize().background(
                                Brush.radialGradient(
                                    colors = listOf(Color.Red.copy(alpha = 0.6f), Color.Transparent),
                                    center = Offset.Unspecified,
                                    radius = Float.POSITIVE_INFINITY
                                )
                            ))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShipIcon(type: ShipType, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val path = Path()
        when (type) {
            ShipType.CARRIER -> { // Star Destroyer (Triangle)
                path.moveTo(size.width / 2, 0f)
                path.lineTo(0f, size.height)
                path.lineTo(size.width, size.height)
                path.close()
            }
            ShipType.BATTLESHIP -> { // Millennium Falcon (Circle with bits)
                drawCircle(color = color, radius = size.minDimension / 2, style = Stroke(width = 2.dp.toPx()))
                path.moveTo(size.width / 2, 0f)
                path.lineTo(size.width / 2, size.height)
                path.moveTo(0f, size.height / 2)
                path.lineTo(size.width, size.height / 2)
            }
            ShipType.DESTROYER -> { // X-Wing (X shape)
                path.moveTo(0f, 0f)
                path.lineTo(size.width, size.height)
                path.moveTo(size.width, 0f)
                path.lineTo(0f, size.height)
            }
            ShipType.SUBMARINE -> { // Slave I (Oval)
                path.addOval(androidx.compose.ui.geometry.Rect(0f, 0f, size.width, size.height))
            }
            ShipType.PATROL_BOAT -> { // TIE Fighter (H shape)
                path.moveTo(0f, 0f)
                path.lineTo(0f, size.height)
                path.moveTo(size.width, 0f)
                path.lineTo(size.width, size.height)
                path.moveTo(0f, size.height / 2)
                path.lineTo(size.width, size.height / 2)
            }
        }
        drawPath(path = path, color = color, style = Stroke(width = 2.dp.toPx()))
    }
}

