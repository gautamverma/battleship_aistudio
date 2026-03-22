# Battle Grid: Galactic Fleet

A Star Wars-themed Battleship game built with React, TypeScript, and Tailwind CSS for the web, and Jetpack Compose for Android.

## 🚀 Features

- **Star Wars Thematic Integration**: 
  - Ships are modeled after iconic Star Wars vessels:
    - **Star Destroyer** (Carrier - 5 units)
    - **Millennium Falcon** (Battleship - 4 units)
    - **X-Wing** (Destroyer - 3 units)
    - **Slave I** (Submarine - 3 units)
    - **TIE Fighter** (Patrol Boat - 2 units)
  - Dark, neon-accented UI with a dynamic starfield background.
- **Instant Destruction Mechanic**: A unique gameplay twist where hitting any single segment of a ship instantly destroys the entire vessel, making every shot high-stakes.
- **Smart AI Opponent**:
  - **Hunt Mode**: Uses a parity/checkerboard strategy to efficiently scan the grid.
  - **Target Mode**: Once a hit is registered, the AI intelligently targets adjacent cells to finish off remaining ships.
- **Flexible Ship Placement**:
  - **Manual Placement**: Drag or click to place ships with orientation switching (Horizontal/Vertical).
  - **Auto-Placement**: Instantly randomize ship positions for a quick start.
- **Immersive Audio**: High-quality sound effects for hits, misses, ship destruction, and game outcomes (Victory/Defeat).
- **Responsive UI**: Fully optimized for both desktop browsers and mobile devices.
- **Visual Feedback**:
  - Placement previews showing valid/invalid positions.
  - Highlighting of the last move made by the AI.
  - Ship status bars tracking the health of your fleet.

## 🎮 Rules of the Game

1. **Grid Size**: The game is played on a 10x10 grid (A-J, 1-10).
2. **Fleet Composition**: Each player has 5 ships of varying lengths (2 to 5 units).
3. **Placement Phase**:
   - Ships must be placed entirely within the grid.
   - Ships cannot overlap each other.
   - Ships can be oriented horizontally or vertically.
4. **Combat Phase**:
   - Players take turns selecting a coordinate on the opponent's hidden grid.
   - **Hit**: If a ship occupies the cell, it is marked as a hit. Due to the "Instant Destruction" rule, all segments of that ship are immediately destroyed.
   - **Miss**: If the cell is empty, it is marked as a miss.
5. **Winning**: The first player to destroy all 5 of the opponent's ships is declared the winner.

## 🎨 UI Details

- **Theme**: Galactic Dark Mode (`#050505` background) with neon emerald (Player) and neon red (Enemy) accents.
- **Background**: A custom-drawn starfield using CSS/Canvas for depth and motion.
- **HUD Elements**:
  - **System Header**: Displays game status (e.g., "SYSTEM ONLINE", "TARGET ACQUIRED").
  - **Ship Status Panel**: Shows which ships are still active and which have been destroyed.
  - **Combat Log**: Real-time feedback on hits and misses.
- **Animations**: Smooth entrance and exit transitions for modals and UI elements using `motion/react`.
- **Icons**: 
  - **Lucide React**: Used for utility icons (Mute, Refresh, Info).
  - **Custom SVGs**: Unique, stylized icons for each ship type to enhance the Star Wars feel.

## 🛠 Technical Stack

### Web Version
- **Framework**: React 19
- **Build Tool**: Vite
- **Styling**: Tailwind CSS 4
- **Language**: TypeScript
- **Animations**: Motion (formerly Framer Motion)
- **Icons**: Lucide React

### Android Version
- **Framework**: Jetpack Compose
- **Language**: Kotlin
- **Theme**: Material 3 (Dark Theme)
- **Graphics**: Custom `Canvas` drawing for starfields and ship icons.

## 📂 Project Structure

- `/src`: React source code (Web).
  - `App.tsx`: Main game component and state management.
  - `utils.ts`: Core game logic (AI, placement, win checks).
  - `types.ts`: TypeScript interfaces and constants.
- `/android`: Native Android project.
  - `MainActivity.kt`: Main UI and game loop using Jetpack Compose.
  - `GameEngine.kt`: Mirrored game logic for the Android environment.
  - `Models.kt`: Kotlin data classes for game state.

---

*May the Force be with your fleet!*
