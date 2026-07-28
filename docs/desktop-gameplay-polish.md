# Desktop gameplay polish (Jul 2026)

## Fixed
- Week-result dialog used wrong schedule index (off-by-one vs engine `currentWeek - 1`)
- New careers quit without save prompt (`dirty` stayed false until first mutation)
- Season rollover replaced `DesktopUiBridge`, leaving Home dashboard on a stale bridge
- Mid-season recruiting session could freeze through NLI — cleared when recruiting gate opens
- Dashboard “Recent Outcome” used last schedule slot (often unplayed); now last played non-BYE
- Launcher audio disposed when handing off to league window
- Coordinator hire dialog has an explicit Close button

## Deferred
- Contract retirement follow-through (Android parity: reincarnate / job offers)
- Double coordinator hire pass after accepting a new job
- Stacking informational modals on Play Week (awards/midseason/result)
