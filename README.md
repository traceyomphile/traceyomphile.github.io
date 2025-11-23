Solo-levelling Dungeon Hunter — Parallel Java implementation
=========================================================

**Overview**
- **Project**: Parallel implementation of a hill-climbing / Monte Carlo style search to locate a global maximum ("Dungeon Master") on a generated 2D power (mana) surface.
- **Authors**: Original algorithm adapted from Michelle Kuttel (2025) and parallelized by Tracey Letlape.
- **Language**: Java (ForkJoin concurrency).

**Files**
- **`DungeonHunterParallel.java`**: Main driver and entry point. Parses command-line arguments, constructs the `DungeonMapParallel`, seeds a set of `HuntParallel` searches, and runs them using a `ForkJoinPool` and the `HuntTask` (a `RecursiveTask`). After the search finishes it prints summary information and writes two PNG visualisations: `visualiseSearch.png` and `visualiseSearchPath.png`.
- **`DungeonMapParallel.java`**: Represents the dungeon grid and computes the "mana" (power) at grid points. Key features:
  - Constants: `PRECISION` (fixed-point scaling) and `RESOLUTION` (grid resolution).
  - Randomly places a boss peak and computes a multi-component analytic function to produce a complex surface.
  - Caches computed mana values in `manaMap[][]` and tracks visits with `visit[][]`.
  - `getManaLevel(int x, int y)` computes and caches the value; `getNextStepDirection(int x,int y)` returns the neighbour direction with the highest mana.
  - `visualisePowerMap(String filename, boolean path)` produces a PNG visualisation using a ForkJoin `VisualisePowerTask` (a `RecursiveAction`) to set image pixels in parallel.
- **`HuntParallel.java`**: Single hunter/search class. Each `HuntParallel` instance performs hill-climbing from a random start cell, using `getNextStepDirection(...)` to move toward higher mana until it reaches a local peak or a previously visited cell. Exposes getters for `id`, final position, steps and stopped status.
- **`Makefile`**: Targets to compile (`make` / `javac`) and run (`make run`). Note: `Makefile` uses `SRC=SoloLevellingParallel` and lists classes under that folder; in this workspace sources appear at project root — run manual `javac` commands if needed (see "Build & Run").
- **`LocalMachineSpecs.html`, `ServerSpecs.html`, `SequentialCutoff.html`, `Validation.html`**: Documentation/auxiliary HTML files included in the submission (machine specs, server specs, sequential cutoff analysis and validation results).
- **`Report.pdf`**: Written report for the assignment.

**Command-line / Build & Run**
- Java source files in this workspace: `DungeonHunterParallel.java`, `DungeonMapParallel.java`, `HuntParallel.java`.
- Compile (from the project directory, for bash on Windows):
  ```bash
  javac DungeonMapParallel.java HuntParallel.java DungeonHunterParallel.java
  ```
- Run with three arguments:
  ```bash
  java DungeonHunterParallel <gridSize> <numSearchesMultiplier> <randomSeed>
  ```
  - `<gridSize>` (integer): half-size of the dungeon in coordinate units; code uses `gateSize` and builds a square grid from `-gateSize` to `+gateSize`.
  - `<numSearchesMultiplier>` (floating point): multiplier used to compute total searches as
    `numSearches = (int)(multiplier * (gateSize*2) * (gateSize*2) * DungeonMapParallel.RESOLUTION)`.
  - `<randomSeed>` (integer): seed for reproducible boss placement and search start locations; use `0` for non-deterministic runs or a positive integer for repeatable behaviour.

+- Example (default-like values used in the `Makefile`):
  ```bash
  javac DungeonMapParallel.java HuntParallel.java DungeonHunterParallel.java
  java DungeonHunterParallel 20 0.2 0
  ```

**Expected Output & Artifacts**
- Console output: dungeon size, rows/columns, search count, execution time, number of grid points evaluated, and the found Dungeon Master (mana and coordinates).
- Images written by the program:
  - `visualiseSearch.png` — power map showing visited/unvisited areas.
  - `visualiseSearchPath.png` — power map overlaid with the search path (if path=true when visualising).

**Concurrency Notes**
- The program uses Java `ForkJoinPool` to parallelise the work:
  - `DungeonHunterParallel` constructs a `HuntTask` (`RecursiveTask<SearchResult>`) to split the array of `HuntParallel` searches among worker threads.
  - `DungeonMapParallel.VisualisePowerTask` (`RecursiveAction`) parallelises image pixel assignment when writing the visualisation.
- Shared state: `manaMap` and `visit` arrays are used by multiple hunters; the code uses a simple check-then-set approach (no explicit locking). This is intentional for the assignment, but be aware of race conditions in different contexts.

**Notes & Tips**
- Java version: code uses `ForkJoinPool` and standard concurrency primitives available in Java 8+. Use a Java 8+ JVM.
- If `Makefile` targets do not match your layout (it references `SRC=SoloLevellingParallel`), compile the `.java` files directly as shown above.
- To reproduce results across runs, pass a non-zero `<randomSeed>`.

**Next Actions**
- I can run/compile the project and produce sample output or fix the `Makefile` to match the repo layout if you want.

File location: `README.md` (project root)
# UCT CSC2002S Parallel and Concurrent Programming Assignment 1

Welcome to the repository for Assignment 1 of the University of Cape Town (UCT) CSC2002S: Parallel and Concurrent Programming course.

## Overview

This repository contains my solution to Assignment 1, which focuses on concepts in parallel and concurrent programming. All necessary files and source code are included.

## Contents

- Source code implementing the assignment requirements
- Documentation and instructions for running the code
- Example input and output files (if applicable)

## Getting Started

1. **Clone this repository:**
   ```bash
   git clone https://github.com/traceyomphile/traceyomphile.github.io.git
