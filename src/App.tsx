/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useCallback, useRef } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { 
  Ship as ShipIcon, 
  Target, 
  RotateCw, 
  Play, 
  RefreshCcw, 
  Trophy, 
  Skull,
  Crosshair,
  Zap,
  Volume2,
  VolumeX,
  Info
} from 'lucide-react';
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

import { 
  Cell, 
  CellStatus, 
  Ship, 
  GamePhase, 
  SHIP_CONFIG,
  ShipType
} from './types';
import { 
  createEmptyGrid, 
  createInitialShips, 
  canPlaceShip, 
  placeShipOnGrid, 
  placeShipsRandomly, 
  getSmartAiMove,
  getAdjacentCells,
  checkWin,
  GRID_SIZE
} from './utils';

function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

// Sound URLs
const SOUNDS = {
  hit: 'https://assets.mixkit.co/active_storage/sfx/2571/2571-preview.mp3',
  miss: 'https://assets.mixkit.co/active_storage/sfx/2568/2568-preview.mp3',
  sunk: 'https://assets.mixkit.co/active_storage/sfx/2572/2572-preview.mp3',
  click: 'https://assets.mixkit.co/active_storage/sfx/2567/2567-preview.mp3',
  victory: 'https://assets.mixkit.co/active_storage/sfx/1435/1435-preview.mp3',
  defeat: 'https://assets.mixkit.co/active_storage/sfx/2535/2535-preview.mp3',
};

export default function App() {
  const [playerGrid, setPlayerGrid] = useState<Cell[][]>(createEmptyGrid());
  const [aiGrid, setAiGrid] = useState<Cell[][]>(createEmptyGrid());
  const [playerShips, setPlayerShips] = useState<Ship[]>(createInitialShips());
  const [aiShips, setAiShips] = useState<Ship[]>(createInitialShips());
  const [phase, setPhase] = useState<GamePhase>('placement');
  const [currentTurn, setCurrentTurn] = useState<'player' | 'ai'>('player');
  const [winner, setWinner] = useState<'player' | 'ai' | null>(null);
  const [orientation, setOrientation] = useState<'horizontal' | 'vertical'>('horizontal');
  const [selectedShipIndex, setSelectedShipIndex] = useState<number>(0);
  const [message, setMessage] = useState<string>('Deploy your fleet to the grid');
  const [lastAiMove, setLastAiMove] = useState<{ r: number; c: number; status: CellStatus } | null>(null);
  const [isMuted, setIsMuted] = useState(false);
  const [hoveredCell, setHoveredCell] = useState<{ r: number; c: number } | null>(null);
  const [aiMemory, setAiMemory] = useState<{ targets: { r: number; c: number }[]; lastHit?: { r: number; c: number }; huntMode: boolean }>({
    targets: [],
    huntMode: true
  });

  const audioRefs = useRef<Record<string, HTMLAudioElement>>({});

  // Preload sounds
  useEffect(() => {
    Object.entries(SOUNDS).forEach(([key, url]) => {
      const audio = new Audio(url);
      audio.preload = 'auto';
      audioRefs.current[key] = audio;
    });
  }, []);

  const playSound = (key: keyof typeof SOUNDS) => {
    if (isMuted) return;
    try {
      const audio = new Audio(SOUNDS[key]);
      audio.volume = 0.6;
      audio.play().catch((err) => console.warn('Audio playback blocked or failed:', err));
    } catch (err) {
      console.warn('Audio initialization failed:', err);
    }
  };

  // Initialize AI ships
  useEffect(() => {
    const { grid, ships } = placeShipsRandomly(createEmptyGrid(), createInitialShips());
    setAiGrid(grid);
    setAiShips(ships);
  }, []);

  const handleCellClick = (r: number, c: number) => {
    if (phase === 'placement') {
      handlePlacement(r, c);
    } else if (phase === 'playing' && currentTurn === 'player') {
      handlePlayerMove(r, c);
    }
  };

  const handlePlacement = (r: number, c: number) => {
    if (selectedShipIndex >= playerShips.length) return;

    const ship = playerShips[selectedShipIndex];
    if (canPlaceShip(playerGrid, r, c, ship.length, orientation)) {
      playSound('click');
      const { grid: nextGrid, ship: nextShip } = placeShipOnGrid(playerGrid, r, c, {
        ...ship,
        orientation,
      });
      
      const newShips = [...playerShips];
      newShips[selectedShipIndex] = nextShip;
      setPlayerShips(newShips);
      setPlayerGrid(nextGrid);
      
      if (selectedShipIndex < playerShips.length - 1) {
        setSelectedShipIndex(selectedShipIndex + 1);
      } else {
        setPhase('playing');
        setMessage('BATTLE STATIONS! Attack the enemy grid.');
      }
    } else {
      setMessage('Invalid position!');
      setTimeout(() => setMessage('Deploy your fleet to the grid'), 1500);
    }
  };

  const handlePlayerMove = (r: number, c: number) => {
    if (aiGrid[r][c].status === 'hit' || aiGrid[r][c].status === 'miss') return;

    const cell = aiGrid[r][c];
    const isHit = cell.status === 'ship';
    
    if (isHit) {
      const shipType = cell.shipType!;
      
      // Instant destruction: mark all ship positions as hit
      setAiGrid(prev => prev.map((row) => 
        row.map((col) => 
          col.shipType === shipType ? { ...col, status: 'hit' as CellStatus } : col
        )
      ));

      setAiShips(prev => prev.map(s => 
        s.type === shipType ? { ...s, hits: s.length } : s
      ));
      
      const shipName = SHIP_CONFIG[shipType].name;
      playSound('sunk');
      setMessage(`CRITICAL HIT! ENEMY ${shipName.toUpperCase()} DESTROYED!`);

      // Check win condition using the updated ships
      setAiShips(currentShips => {
        const updatedShips = currentShips.map(s => 
          s.type === shipType ? { ...s, hits: s.length } : s
        );
        if (checkWin(updatedShips)) {
          setPhase('gameOver');
          setWinner('player');
          playSound('victory');
          setMessage('VICTORY! THE EMPIRE HAS FALLEN.');
        }
        return updatedShips;
      });
    } else {
      setAiGrid(prev => prev.map((row, ri) => 
        row.map((col, ci) => 
          ri === r && ci === c ? { ...col, status: 'miss' as CellStatus } : col
        )
      ));
      playSound('miss');
      setMessage('TURBOLASER MISSED.');
    }

    setCurrentTurn('ai');
  };

  const handleAiMove = useCallback(() => {
    if (phase !== 'playing' || currentTurn !== 'ai') return;

    setTimeout(() => {
      const { r, c, newMemory } = getSmartAiMove(playerGrid, aiMemory);
      const cell = playerGrid[r][c];
      const isHit = cell.status === 'ship';

      if (isHit) {
        const shipType = cell.shipType!;
        const shipName = SHIP_CONFIG[shipType].name;

        // Instant destruction: mark all ship positions as hit
        setPlayerGrid(prev => prev.map((row) => 
          row.map((col) => 
            col.shipType === shipType ? { ...col, status: 'hit' as CellStatus } : col
          )
        ));
        setLastAiMove({ r, c, status: 'hit' });

        playSound('sunk');
        setMessage(`ALERT! OUR ${shipName.toUpperCase()} HAS BEEN DESTROYED!`);

        setPlayerShips(prev => {
          const updatedShips = prev.map(s => 
            s.type === shipType ? { ...s, hits: s.length } : s
          );
          if (checkWin(updatedShips)) {
            setPhase('gameOver');
            setWinner('ai');
            playSound('defeat');
            setMessage('DEFEAT! THE REBELLION IS CRUSHED.');
          }
          return updatedShips;
        });

        // AI Memory update for hit
        const adj = getAdjacentCells(r, c).filter(p => playerGrid[p.r][p.c].status === 'empty' || playerGrid[p.r][p.c].status === 'ship');
        newMemory.targets = [...newMemory.targets, ...adj];
        newMemory.lastHit = { r, c };
        newMemory.huntMode = false;
      } else {
        setPlayerGrid(prev => prev.map((row, ri) => 
          row.map((col, ci) => 
            ri === r && ci === c ? { ...col, status: 'miss' as CellStatus } : col
          )
        ));
        setLastAiMove({ r, c, status: 'miss' });
        playSound('miss');
        setMessage('ENEMY TURBOLASER MISSED.');
      }

      setAiMemory(newMemory);
      setCurrentTurn('player');
    }, 1200);
  }, [playerGrid, playerShips, phase, currentTurn, aiMemory]);



  useEffect(() => {
    if (currentTurn === 'ai') {
      handleAiMove();
    }
  }, [currentTurn, handleAiMove]);

  const resetGame = () => {
    playSound('click');
    setPlayerGrid(createEmptyGrid());
    const { grid: aiG, ships: aiS } = placeShipsRandomly(createEmptyGrid(), createInitialShips());
    setAiGrid(aiG);
    setAiShips(aiS);
    setPlayerShips(createInitialShips());
    setPhase('placement');
    setCurrentTurn('player');
    setWinner(null);
    setSelectedShipIndex(0);
    setMessage('Deploy your fleet to the grid');
    setLastAiMove(null);
    setAiMemory({ targets: [], huntMode: true });
  };

  const autoPlace = () => {
    playSound('click');
    const { grid, ships } = placeShipsRandomly(createEmptyGrid(), createInitialShips());
    setPlayerGrid(grid);
    setPlayerShips(ships);
    setPhase('playing');
    setMessage('BATTLE STATIONS! Attack the enemy grid.');
  };

  return (
    <div className="min-h-screen bg-[#050505] text-white font-sans selection:bg-emerald-500/30 overflow-x-hidden">
      {/* HUD Background */}
      <div className="fixed inset-0 pointer-events-none">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_50%,rgba(16,185,129,0.05),transparent_70%)]" />
        <div className="absolute inset-0 bg-[linear-gradient(to_right,rgba(255,255,255,0.02)_1px,transparent_1px),linear-gradient(to_bottom,rgba(255,255,255,0.02)_1px,transparent_1px)] bg-[size:40px_40px]" />
      </div>

      {/* Header */}
      <header className="border-b border-white/5 bg-black/80 backdrop-blur-xl sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-6 h-20 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <motion.div 
              animate={{ rotate: 360 }}
              transition={{ duration: 20, repeat: Infinity, ease: "linear" }}
              className="w-12 h-12 bg-emerald-500/10 rounded-full flex items-center justify-center border border-emerald-500/30"
            >
              <Crosshair className="text-emerald-500 w-6 h-6" />
            </motion.div>
            <div>
              <h1 className="text-2xl font-black tracking-tighter italic">GALACTIC FLEET</h1>
              <div className="flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse" />
                <p className="text-[10px] text-emerald-500/70 font-mono tracking-[0.3em] uppercase">System Online</p>
              </div>
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-1">
              <button 
                onClick={() => setIsMuted(!isMuted)}
                className="p-3 bg-white/5 hover:bg-white/10 rounded-xl border border-white/10 transition-all"
                title={isMuted ? "Unmute" : "Mute"}
              >
                {isMuted ? <VolumeX className="w-5 h-5 text-red-400" /> : <Volume2 className="w-5 h-5 text-emerald-400" />}
              </button>
              {!isMuted && (
                <button 
                  onClick={() => playSound('click')}
                  className="p-3 text-[10px] font-bold text-white/40 hover:text-white transition-colors uppercase tracking-widest"
                >
                  Test Sound
                </button>
              )}
            </div>
            <div className="hidden md:flex items-center gap-3 px-6 py-3 bg-white/5 rounded-2xl border border-white/10">
              <div className={cn("w-2 h-2 rounded-full", currentTurn === 'player' ? "bg-emerald-500 animate-pulse" : "bg-red-500")} />
              <span className="text-xs font-bold uppercase tracking-widest font-mono">
                {phase === 'placement' ? 'Deployment' : `${currentTurn} ACTIVE`}
              </span>
            </div>
            <button 
              onClick={resetGame}
              className="p-3 bg-white/5 hover:bg-white/10 rounded-xl border border-white/10 transition-all group"
            >
              <RefreshCcw className="w-5 h-5 text-white/60 group-hover:rotate-180 transition-transform duration-500" />
            </button>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-8 relative">
        {/* Status Message */}
        <div className="mb-10 flex justify-center">
          <AnimatePresence mode="wait">
            <motion.div
              key={message}
              initial={{ opacity: 0, scale: 0.95, y: 10 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 1.05, y: -10 }}
              className="relative group"
            >
              <div className="absolute -inset-1 bg-emerald-500/20 blur-lg rounded-2xl group-hover:bg-emerald-500/30 transition-all" />
              <div className="relative px-10 py-5 bg-black/60 border border-emerald-500/30 rounded-2xl backdrop-blur-md">
                <p className="text-xl font-bold text-emerald-400 tracking-tight flex items-center gap-3">
                  <Info className="w-5 h-5" />
                  {message}
                </p>
              </div>
            </motion.div>
          </AnimatePresence>
        </div>

        <div className="grid lg:grid-cols-2 gap-12 xl:gap-20 items-start">
          {/* Player Board */}
          <section className="space-y-6">
            <div className="flex items-center justify-between px-2">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-xl bg-blue-500/10 flex items-center justify-center border border-blue-500/20">
                  <ShipIcon className="w-5 h-5 text-blue-400" />
                </div>
                <div>
                  <h2 className="text-lg font-bold tracking-tight">FRIENDLY SECTOR</h2>
                  <p className="text-[10px] text-blue-400/60 font-mono uppercase tracking-widest">Grid Alpha-1</p>
                </div>
              </div>
              {phase === 'placement' && (
                <div className="flex items-center gap-3">
                  <button
                    onClick={() => { playSound('click'); setOrientation(o => o === 'horizontal' ? 'vertical' : 'horizontal'); }}
                    className="flex items-center gap-2 px-5 py-2.5 bg-white/5 hover:bg-white/10 border border-white/10 rounded-xl transition-all active:scale-95"
                  >
                    <RotateCw className="w-4 h-4 text-emerald-400" />
                    <span className="text-xs font-bold uppercase tracking-wider">{orientation}</span>
                  </button>
                  <button
                    onClick={autoPlace}
                    className="flex items-center gap-2 px-5 py-2.5 bg-emerald-500 text-black hover:bg-emerald-400 rounded-xl transition-all active:scale-95 shadow-lg shadow-emerald-500/20"
                  >
                    <Zap className="w-4 h-4 fill-current" />
                    <span className="text-xs font-black uppercase tracking-wider">Auto</span>
                  </button>
                </div>
              )}
            </div>

            <div className="relative aspect-square bg-black/40 rounded-[2rem] p-6 border border-white/5 shadow-2xl backdrop-blur-sm">
              {/* Grid Labels */}
              <div className="absolute -left-8 top-6 bottom-6 flex flex-col justify-between text-[10px] font-mono text-white/30">
                {Array.from({ length: 10 }).map((_, i) => <span key={i}>{i + 1}</span>)}
              </div>
              <div className="absolute -top-8 left-6 right-6 flex justify-between text-[10px] font-mono text-white/30">
                {['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'].map(l => <span key={l}>{l}</span>)}
              </div>

              <div className="grid grid-cols-10 gap-1.5 h-full">
                {playerGrid.map((row, r) => 
                  row.map((cell, c) => (
                    <CellView 
                      key={`${r}-${c}`} 
                      cell={cell} 
                      onClick={() => handleCellClick(r, c)}
                      onHover={() => setHoveredCell({ r, c })}
                      onLeave={() => setHoveredCell(null)}
                      isPlayer
                      isLastMove={lastAiMove?.r === r && lastAiMove?.c === c}
                      placementPreview={
                        phase === 'placement' && 
                        hoveredCell?.r === r && 
                        hoveredCell?.c === c && 
                        canPlaceShip(playerGrid, r, c, playerShips[selectedShipIndex].length, orientation)
                          ? { length: playerShips[selectedShipIndex].length, orientation }
                          : undefined
                      }
                    />
                  ))
                )}
              </div>
            </div>

            {/* Ship Status */}
            <div className="grid grid-cols-5 gap-3">
              {playerShips.map((ship, i) => (
                <div 
                  key={ship.type}
                  className={cn(
                    "p-4 rounded-2xl border transition-all relative overflow-hidden",
                    phase === 'placement' && selectedShipIndex === i ? "bg-emerald-500/10 border-emerald-500/40" : "bg-white/5 border-white/10",
                    ship.hits === ship.length ? "opacity-30 grayscale" : "opacity-100"
                  )}
                >
                  {ship.hits === ship.length && (
                    <div className="absolute inset-0 bg-red-500/10 flex items-center justify-center">
                      <span className="text-[8px] font-black text-red-500 uppercase tracking-widest rotate-12">Destroyed</span>
                    </div>
                  )}
                  <div className="flex items-center gap-2 mb-2">
                    <ShipIconComponent type={ship.type} className="w-4 h-4 text-white/40" />
                    <p className="text-[9px] uppercase tracking-widest font-black text-white/30">{ship.name}</p>
                  </div>
                  <div className="flex gap-1.5">
                    {Array.from({ length: ship.length }).map((_, j) => (
                      <div 
                        key={j} 
                        className={cn(
                          "h-1.5 flex-1 rounded-full transition-colors",
                          j < ship.hits ? "bg-red-500" : "bg-white/10"
                        )} 
                      />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>

          {/* AI Board */}
          <section className="space-y-6">
            <div className="flex items-center gap-3 px-2">
              <div className="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center border border-red-500/20">
                <Target className="w-5 h-5 text-red-400" />
              </div>
              <div>
                <h2 className="text-lg font-bold tracking-tight">ENEMY WATERS</h2>
                <p className="text-[10px] text-red-400/60 font-mono uppercase tracking-widest">Sector X-Ray</p>
              </div>
            </div>

            <div className={cn(
              "relative aspect-square bg-black/40 rounded-[2rem] p-6 border border-white/5 shadow-2xl backdrop-blur-sm transition-all",
              currentTurn === 'player' && phase === 'playing' ? "ring-2 ring-emerald-500/30" : "opacity-60"
            )}>
              {/* Grid Labels */}
              <div className="absolute -left-8 top-6 bottom-6 flex flex-col justify-between text-[10px] font-mono text-white/30">
                {Array.from({ length: 10 }).map((_, i) => <span key={i}>{i + 1}</span>)}
              </div>
              <div className="absolute -top-8 left-6 right-6 flex justify-between text-[10px] font-mono text-white/30">
                {['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J'].map(l => <span key={l}>{l}</span>)}
              </div>

              <div className="grid grid-cols-10 gap-1.5 h-full">
                {aiGrid.map((row, r) => 
                  row.map((cell, c) => (
                    <CellView 
                      key={`${r}-${c}`} 
                      cell={cell} 
                      onClick={() => handleCellClick(r, c)}
                      disabled={phase !== 'playing' || currentTurn !== 'player'}
                    />
                  ))
                )}
              </div>
            </div>

            {/* AI Ship Status */}
            <div className="grid grid-cols-5 gap-2">
              {aiShips.map((ship) => (
                <div 
                  key={ship.type}
                  className={cn(
                    "p-3 rounded-xl border transition-all relative overflow-hidden",
                    ship.hits === ship.length ? "bg-red-500/10 border-red-500/40 shadow-[0_0_15px_rgba(239,68,68,0.1)]" : "bg-white/5 border-white/10"
                  )}
                >
                  <div className="flex flex-col items-center gap-2 mb-2">
                    {ship.hits === ship.length ? (
                      <ShipIconComponent type={ship.type} className="w-5 h-5 text-red-500" />
                    ) : (
                      <Target className="w-5 h-5 text-white/10" />
                    )}
                    <p className={cn(
                      "text-[7px] sm:text-[9px] uppercase tracking-tighter font-black text-center truncate w-full",
                      ship.hits === ship.length ? "text-red-500" : "text-white/20"
                    )}>
                      {ship.hits === ship.length ? ship.name : 'Unknown'}
                    </p>
                  </div>
                  <div className="flex gap-0.5 sm:gap-1">
                    {Array.from({ length: ship.length }).map((_, j) => (
                      <div 
                        key={j} 
                        className={cn(
                          "h-1 flex-1 rounded-full transition-colors",
                          j < ship.hits ? "bg-red-500" : "bg-white/5"
                        )} 
                      />
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>
      </main>

      {/* Game Over Modal */}
      <AnimatePresence>
        {phase === 'gameOver' && (
          <motion.div 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 z-[100] flex items-center justify-center bg-black/90 backdrop-blur-2xl p-6"
          >
            <motion.div 
              initial={{ scale: 0.8, opacity: 0, y: 40 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              className="max-w-lg w-full bg-white/5 border border-white/10 rounded-[3rem] p-16 text-center shadow-2xl relative overflow-hidden"
            >
              <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-right from-transparent via-emerald-500 to-transparent" />
              
              <div className={cn(
                "w-28 h-28 mx-auto rounded-[2rem] flex items-center justify-center mb-10 relative",
                winner === 'player' ? "bg-emerald-500 shadow-2xl shadow-emerald-500/40" : "bg-red-500 shadow-2xl shadow-red-500/40"
              )}>
                {winner === 'player' ? <Trophy className="w-14 h-14 text-black" /> : <Skull className="w-14 h-14 text-black" />}
                <motion.div 
                  animate={{ scale: [1, 1.2, 1] }}
                  transition={{ duration: 2, repeat: Infinity }}
                  className="absolute -inset-4 border border-white/20 rounded-[2.5rem]"
                />
              </div>

              <h2 className="text-5xl font-black mb-6 tracking-tighter italic">
                {winner === 'player' ? 'MISSION ACCOMPLISHED' : 'FLEET DESTROYED'}
              </h2>
              
              <p className="text-white/50 mb-12 text-lg leading-relaxed font-medium">
                {winner === 'player' 
                  ? 'Command, the enemy fleet has been completely neutralized. Superior tactics secured our victory.' 
                  : 'Critical failure. All assets lost. The enemy has established total dominance in this sector.'}
              </p>

              <button
                onClick={resetGame}
                className="w-full py-6 bg-white text-black font-black rounded-2xl hover:bg-emerald-400 transition-all active:scale-95 flex items-center justify-center gap-4 text-xl tracking-tight"
              >
                <Play className="w-6 h-6 fill-current" />
                INITIATE NEW SORTIE
              </button>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}

function ShipIconComponent({ type, className }: { type: ShipType; className?: string }) {
  switch (type) {
    case 'carrier': // Star Destroyer
      return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
          <path d="M12 2L2 22h20L12 2z" />
          <path d="M12 2v20" />
          <path d="M7 12h10" />
        </svg>
      );
    case 'battleship': // Millennium Falcon
      return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
          <circle cx="12" cy="12" r="10" />
          <path d="M12 2v20" />
          <path d="M2 12h20" />
          <circle cx="12" cy="12" r="4" />
        </svg>
      );
    case 'destroyer': // X-Wing
      return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
          <path d="M12 2L2 7l10 5 10-5-10-5z" />
          <path d="M2 17l10 5 10-5" />
          <path d="M2 12l10 5 10-5" />
        </svg>
      );
    case 'submarine': // Slave I
      return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
          <path d="M12 2a8 8 0 0 0-8 8v4a8 8 0 0 0 16 0v-4a8 8 0 0 0-8-8z" />
          <path d="M12 18v4" />
          <path d="M8 14h8" />
        </svg>
      );
    case 'patrolBoat': // TIE Fighter
      return (
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" className={className}>
          <circle cx="12" cy="12" r="3" />
          <path d="M3 3v18" />
          <path d="M21 3v18" />
          <path d="M3 12h6" />
          <path d="M15 12h6" />
        </svg>
      );
    default:
      return <ShipIcon className={className} />;
  }
}

function CellView({ 
  cell, 
  onClick, 
  onHover,
  onLeave,
  isPlayer = false, 
  disabled = false,
  isLastMove = false,
  placementPreview
}: { 
  key?: string;
  cell: Cell; 
  onClick: () => void; 
  onHover?: () => void;
  onLeave?: () => void;
  isPlayer?: boolean;
  disabled?: boolean;
  isLastMove?: boolean;
  placementPreview?: { length: number; orientation: 'horizontal' | 'vertical' };
}) {
  const getStatusColor = () => {
    switch (cell.status) {
      case 'hit': return 'bg-red-500 shadow-[0_0_20px_rgba(239,68,68,0.6)]';
      case 'miss': return 'bg-white/20';
      case 'ship': return isPlayer ? 'bg-blue-500/40 border border-blue-500/50' : 'bg-transparent';
      default: return 'bg-transparent';
    }
  };

  return (
    <button
      onClick={onClick}
      onMouseEnter={onHover}
      onMouseLeave={onLeave}
      disabled={disabled}
      className={cn(
        "relative w-full h-full rounded-md transition-all duration-200 group overflow-hidden flex items-center justify-center",
        cell.status === 'empty' && !disabled && "hover:bg-white/10",
        isLastMove && "ring-2 ring-white animate-pulse z-10 shadow-[0_0_15px_white]"
      )}
    >
      <div className={cn(
        "absolute inset-[15%] rounded-sm transition-all duration-500",
        getStatusColor(),
        cell.status === 'hit' && "scale-110",
        cell.status === 'miss' && "scale-50"
      )} />

      {/* Ship Icon for Player Board */}
      {isPlayer && cell.status === 'ship' && cell.shipType && (
        <ShipIconComponent type={cell.shipType} className="w-4 h-4 text-blue-400 z-10 opacity-80" />
      )}

      {/* Ship Icon for Hit Ships on AI Board (Revealed) */}
      {!isPlayer && cell.status === 'hit' && cell.shipType && (
        <ShipIconComponent type={cell.shipType} className="w-4 h-4 text-white z-10 opacity-50" />
      )}

      {/* Placement Preview */}
      {placementPreview && (
        <div className="absolute inset-0 bg-emerald-500/20 border border-emerald-500/50 animate-pulse" />
      )}
      
      {/* Target Crosshair on Hover */}
      {!isPlayer && cell.status === 'empty' && !disabled && (
        <div className="absolute inset-0 opacity-0 group-hover:opacity-100 flex items-center justify-center transition-opacity">
          <div className="w-full h-full border border-emerald-500/30 flex items-center justify-center">
            <Crosshair className="w-4 h-4 text-emerald-500/50" />
          </div>
        </div>
      )}

      {/* Grid Lines */}
      <div className="absolute inset-0 border border-white/[0.03] pointer-events-none" />
    </button>
  );
}
