# Threading model

CFHC's shared engine (`simulation`, `positions`, `staff`, `comparator`, `recruiting`)
assumes **single-threaded mutation** of game state.

## Contract

- Treat `League` and `Team` (and objects they own — players, staff, schedules,
  recruiting session data) as **not thread-safe**.
- Only one thread may call mutating APIs at a time:
  `SeasonController.advanceWeek()`, `League.playWeek()`, `setTeamRanks()`,
  save/load that rebuilds league state, recruiting commit, transfers, etc.
- UI shells (Android, Swing desktop) may use background threads for **I/O or
  progress dialogs**, but must hop back to a single game/UI thread before
  touching league state.

Annotated types use `simulation.NotThreadSafe`.

## Current shells

| Shell | Pattern today |
|:---|:---|
| Android | Main-thread UI; season advances from activity callbacks |
| Desktop Swing | EDT owns league mutation. `DesktopBulkSimulator` runs progress on a worker and hops each `advanceWeek()` onto the EDT via `invokeAndWait`. |

## Safe future direction

If background simulation is needed:

1. Keep a **single game thread** that owns the `League`.
2. Send commands on a queue (`advanceWeek`, `save`, `recruit`, …).
3. Publish immutable snapshots or EDT/UI-thread runnables for display.

Do **not** share a live `League` across threads with ad-hoc locking — collections
and public mutable fields are not guarded.

## What is OK off-thread

- Reading classpath / filesystem resources into local buffers
- Parsing custom CSV/universe files into intermediate structures **before** applying
  them on the game thread
- Pure functions on detached copies / records (e.g. formatting strings)

When in doubt, mutate on the game thread.
