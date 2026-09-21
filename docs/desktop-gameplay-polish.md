# Desktop gameplay polish (Jul 2026)

## Fixed
- Week-result dialog used wrong schedule index (off-by-one vs engine `currentWeek - 1`)
- New careers quit without save prompt (`dirty` stayed false until first mutation)
- Season rollover replaced `DesktopUiBridge`, leaving Home dashboard on a stale bridge
- Mid-season recruiting session could freeze through NLI — cleared when recruiting gate opens
- Dashboard “Recent Outcome” used last schedule slot (often unplayed); now last played non-BYE
- Launcher audio disposed when handing off to league window
- Coordinator hire dialog has an explicit Close button

## Fixed (September 2026 polish pass)
- Contract retirement follow-through: RETIRE now shows the career retrospective,
  offers a save, and returns to the Career Hub (reincarnate/job-offers parity
  intentionally deferred — exit path covers the flag that nothing read).
- Double coordinator hire pass: the post-job-offer pass only opens when the new
  staff actually needs a hire (otherwise the CPU carousel runs directly), the
  OC/DC passes are labeled "PASS n OF 2", and CLOSE ("KEEP STAFF & PROCEED")
  renews the current coordinators and runs the carousel instead of silently
  skipping both.
- Stacking informational modals: bulk-run text digests (season summary,
  midseason report, realignment) merge into one "Season Digest" dialog, replayed
  BEFORE the new-season save prompt.

## Fixed (follow-up)
- Coach reincarnation ("Use Same Team" Android parity): after retiring, the
  user can start a fresh coaching career at the same program — prestige
  knockdown (x0.925), staff/facilities reset, old coach to the free-agent
  pool, new named user coach. Full refresh re-renders the shell in place;
  scroll positions of every screen are preserved across weekly refreshes.

## Deferred
- "Pick New Team" reincarnation branch (needs the fired-coach job pipeline
  reused from a mid-offseason state).
