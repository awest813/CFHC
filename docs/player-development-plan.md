# Player Archetypes & Development Expansion Plan

## Overview

The current player system uses four generic attributes (`ratAttr1–4`) reinterpreted per position, with uniform linear progression and no archetype identity. This plan introduces named archetypes per position that differentiate player identity, biases attribute growth, and unlocks strategic depth in roster building.

---

## 1. Archetype System

### 1.1 Per-Position Archetypes

Each position gets 3–4 archetypes. An archetype defines:
- **Attribute growth multipliers** — which attributes grow faster/slower (e.g., 1.3x for primary, 0.7x for dump)
- **Hard attribute caps** — a lower ceiling for non-primary traits (e.g., Speed max 85 for a Pocket QB)
- **A unique mechanic** that differentiates gameplay feel (see section 1.2)

#### Quarterback (4 archetypes)
| Archetype | attr1 (PassPow) | attr2 (PassAcc) | attr3 (Evasion) | attr4 (Speed) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Pocket Passer  | 1.2x (cap 99) | 1.2x (cap 99) | 0.8x (cap 80) | 0.6x (cap 75) | +10% pass completion in clean pocket |
| Scrambler      | 0.8x (cap 90) | 1.0x (cap 95) | 1.3x (cap 99) | 1.3x (cap 99) | +15% rush yardage on scrambles |
| Field General  | 1.0x (cap 95) | 1.3x (cap 99) | 1.0x (cap 90) | 0.8x (cap 80) | +5 intelligence growth/year, pre-snap adjustment bonus |
| Dual-Threat    | 1.0x (cap 95) | 1.0x (cap 95) | 1.1x (cap 95) | 1.2x (cap 95) | Balanced; option play bonus |

#### Running Back (3 archetypes)
| Archetype | attr1 (Speed) | attr2 (Evasion) | attr3 (Power) | attr4 (Catch) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Speed Back   | 1.3x (cap 99) | 1.1x (cap 95) | 0.7x (cap 80) | 0.9x (cap 85) | +1 ypc on outside runs |
| Power Back   | 0.8x (cap 85) | 0.9x (cap 85) | 1.3x (cap 99) | 0.8x (cap 80) | +15% broken tackle rate |
| Receiving Back | 0.9x (cap 90) | 1.2x (cap 95) | 0.8x (cap 80) | 1.3x (cap 99) | +10% catch rate on checkdowns |

#### Wide Receiver (3 archetypes)
| Archetype | attr1 (Speed) | attr2 (Catch) | attr3 (Evasion) | attr4 (Jump) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Deep Threat  | 1.3x (cap 99) | 0.9x (cap 90) | 0.9x (cap 85) | 1.1x (cap 95) | +15% deep-ball catch rate |
| Route Runner | 0.9x (cap 90) | 1.3x (cap 99) | 1.1x (cap 95) | 0.8x (cap 85) | +10% separation on all routes |
| Slot Receiver | 0.9x (cap 88) | 1.1x (cap 95) | 1.3x (cap 99) | 0.9x (cap 88) | +5 yac per reception |

#### Tight End (3 archetypes)
| Archetype | attr1 (RunBlock) | attr2 (Catch) | attr3 (Evasion) | attr4 (Speed) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Blocking TE   | 1.3x (cap 99) | 0.8x (cap 80) | 0.8x (cap 75) | 0.8x (cap 78) | +20% run-block success rate |
| Receiving TE  | 0.7x (cap 80) | 1.3x (cap 99) | 1.1x (cap 90) | 1.0x (cap 88) | +10% catch rate, seam-route bonus |
| Hybrid TE     | 1.1x (cap 92) | 1.1x (cap 92) | 0.9x (cap 85) | 0.9x (cap 85) | Versatile; no penalty for either role |

#### Offensive Line (3 archetypes)
| Archetype | attr1 (RunBlock) | attr2 (PassBlock) | attr3 (Strength) | attr4 (Vision) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Run Blocker    | 1.3x (cap 99) | 0.8x (cap 85) | 1.2x (cap 99) | 0.8x (cap 80) | +15% run-block consistency |
| Pass Protector | 0.8x (cap 85) | 1.3x (cap 99) | 0.9x (cap 90) | 1.1x (cap 95) | -20% sack rate allowed |
| Mauler         | 1.1x (cap 95) | 0.9x (cap 88) | 1.3x (cap 99) | 0.8x (cap 80) | +10% pancake block chance |

#### Defensive Line (3 archetypes)
| Archetype | attr1 (RunStop) | attr2 (Tackle) | attr3 (PassRush) | attr4 (Strength) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Run Stopper    | 1.3x (cap 99) | 1.1x (cap 95) | 0.7x (cap 80) | 1.2x (cap 99) | +15% run-stop rate |
| Pass Rusher    | 0.7x (cap 80) | 0.9x (cap 88) | 1.3x (cap 99) | 1.0x (cap 92) | +20% pressure rate |
| Nose Tackle    | 1.2x (cap 99) | 0.8x (cap 85) | 0.8x (cap 82) | 1.3x (cap 99) | +10% double-team absorption |

#### Linebacker (3 archetypes)
| Archetype | attr1 (Tackle) | attr2 (RunStop) | attr3 (Coverage) | attr4 (Speed) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Run Stopper | 1.3x (cap 99) | 1.2x (cap 99) | 0.7x (cap 78) | 0.9x (cap 85) | +15% run-stop rate |
| Coverage LB | 0.9x (cap 90) | 0.7x (cap 80) | 1.3x (cap 99) | 1.2x (cap 95) | +10% pass deflection rate |
| Blitzer     | 0.9x (cap 90) | 0.9x (cap 88) | 0.9x (cap 85) | 1.3x (cap 99) | +20% blitz pressure rate |

#### Cornerback (3 archetypes)
| Archetype | attr1 (Coverage) | attr2 (Speed) | attr3 (Tackle) | attr4 (Jump) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Shutdown Corner | 1.3x (cap 99) | 1.1x (cap 95) | 0.8x (cap 80) | 1.1x (cap 95) | +10% pass break-up rate |
| Speed CB        | 0.9x (cap 90) | 1.3x (cap 99) | 0.8x (cap 80) | 1.0x (cap 90) | +15% recovery on deep routes |
| Physical CB     | 1.0x (cap 95) | 0.8x (cap 88) | 1.3x (cap 99) | 0.9x (cap 85) | +15% press-coverage success |

#### Safety (3 archetypes)
| Archetype | attr1 (Tackle) | attr2 (Coverage) | attr3 (Speed) | attr4 (RunStop) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Ball Hawk    | 0.9x (cap 90) | 1.3x (cap 99) | 1.1x (cap 95) | 0.8x (cap 82) | +20% interception rate |
| Run Support  | 1.3x (cap 99) | 0.8x (cap 82) | 0.9x (cap 88) | 1.2x (cap 98) | +15% run-stop rate |
| Hybrid S     | 1.1x (cap 95) | 1.1x (cap 95) | 1.0x (cap 92) | 0.9x (cap 88) | Versatile; no penalty |

#### Kicker (2 archetypes)
| Archetype | attr1 (KickPow) | attr2 (KickAcc) | attr3 (Pressure) | attr4 (Form) | Unique Mechanic |
|-----------|:---:|:---:|:---:|:---:|---|
| Power Kicker  | 1.3x (cap 99) | 0.9x (cap 88) | 0.9x (cap 85) | 0.9x (cap 85) | +5 yards effective FG range |
| Accurate Kicker | 0.9x (cap 90) | 1.3x (cap 99) | 1.1x (cap 95) | 1.0x (cap 92) | +10% accuracy from 40+ yards |

### 1.2 Unique Mechanic Implementation

Each unique mechanic should be stored as a **tag** on the player (`archetypeTag` string) and checked during simulation. Tags include:

- `pocket_passer_bonus` — checked in play outcome: if QB has clean pocket, +10% completion probability
- `scrambler_bonus` — checked when QB scrambles: +15% rush yardage
- `speed_back_bonus` — outside runs: +1 ypc
- `power_back_broken_tackle` — +15% broken tackle rate
- `deep_threat_bonus` — deep pass (20+ air yards): +15% catch rate
- `run_blocker_bonus` — +15% run-block success
- `pass_protector_bonus` — -20% sack rate allowed
- `pass_rusher_bonus` — +20% pressure rate
- `ball_hawk_bonus` — +20% interception rate
- `shutdown_corner_bonus` — +10% pass break-up rate
- etc.

These tags are referenced in the game simulation logic (e.g., `Game.java`, play outcome methods).

---

## 2. Progression Overhaul

### 2.1 Archetype-Biased Growth

In `Player.genericAdvanceSeason()` and `Player.midSeasonProgression()`, after computing base progression deltas, apply **archetype multipliers** to each attribute:

```java
// Applied after base deltas, before clamping:
double[] archMult = getArchetypeMultipliers(); // e.g. {1.2, 1.2, 0.8, 0.6}
ratAttr1 += (int) ((baseDeltaAttr1) * archMult[0] - baseDeltaAttr1); // boost/suppress
```

Or more directly, multiply the random delta before integer casting.

**Attribute caps enforcement**: After each progression step, enforce archetype-specific hard caps (e.g., `Math.min(ratAttr1, archetypeCap1)`).

### 2.2 Breakthrough System (Replace current)

Current: `Math.random() * 100 < progression` for a uniform 4-attr bonus.

New **breakthrough system**:
- **Breakthrough** (rating > expected): ~5% chance per season. Player gains +8–15 to key archetype attributes in a single off-season. Triggered by: high production stats + high potential + good coaching.
- **Bust** (rating < expected): ~3% chance per season. Player gains only +0–3 across all attributes or actually declines by –2–5 in non-key attributes. Triggered by: low playing time, low character, poor coaching.
- **Late Bloomer**: Players with `year >= 3` and `ratPot >= 80` get a hidden "bloom" flag. If they've underperformed relative to potential, they get a one-time +8–12 boost.

### 2.3 Scheme Fit

Add a **scheme archetype preference** to each team's offensive/defensive coordinator:
- OC has a preferred QB archetype + preferred RB/WR archetype
- DC has a preferred DL archetype + preferred LB/CB/S archetype

Players whose archetype matches the coordinator's scheme get:
- +15% progression speed
- +2 OVR boost in scheme-fit rating display

Players who don't fit may request transfers or have reduced morale (future enhancement).

### 2.4 Mentor System

Veteran players (`year >= 3`, `ratOvr >= 85`) with high character (`character > 70`) can act as **mentors**:
- Each mentor assigned to 1–2 younger players at the same position
- Mentored players get +10% progression speed for that season
- Mentors have a small chance (10%) to increase their own character attribute
- Displayed as "Mentor: X" on player profile

Maximum 2 mentors per position group.

### 2.5 Training Camp (Pre-Season)

Add a **training camp phase** before each season:
- User chooses 3 players to "focus train" each off-season
- Focus-trained players get a guaranteed +3–8 to their primary archetype attribute(s)
- All other players get +0–2 from baseline camp
- Camp results displayed in a pre-season summary screen

For CPU teams: auto-select highest-potential underclassmen.

### 2.6 Regression

Older players (`year >= 4`) should face mild regression in physical attributes (Speed, Evasion, Jump, Strength, Pass Rush):
- Year 4: –1–3 to selected physical attributes (10% chance per attribute)
- Year 5: –2–5 to selected physical attributes (25% chance)
- Mental attributes (Intelligence, Pass Accuracy, Coverage) are unaffected or continue to grow slowly

This makes roster management more strategic: decide when to replace aging stars.

### 2.7 Expanded Practice Focus

Current `PracticeFocus` enum (BALANCED, FOOTBALL_IQ, FUNDAMENTALS, ATHLETICISM, PHYSICAL) is a good foundation. Expand it:

- **Add position-group targeting**: PracticeFocus includes a sub-focus on a specific position group (e.g., "Fundamentals — QB" or "Speed — WR/CB")
- **Add risk/reward**: "Intense" focus gives +20% growth but +10% injury chance for that week
- **Weekly practice outcomes**: Each week of the season, the practice focus produces small rating changes (not just mid-season and end-of-season). Currently, mid-season only runs once. Add weekly fluctuations.

---

## 3. User Interface Changes

### 3.1 Player Profile
- Display archetype name prominently (e.g., "Pocket Passer" in bold)
- Show archetype tag icon or text
- Show scheme-fit indicator: "Fits [Coach]'s [System]" or "Misfit — requests transfer risk"
- Show mentor assignments
- Show attribute caps: "Speed cap: 75 (Pocket Passer)"

### 3.2 Depth Chart
- Show archetype next to each player name
- Allow filtering/sorting by archetype

### 3.3 Recruiting
- Show which archetypes each recruit is projected to develop into (based on initial attribute distribution)
- Show scheme fit for your team's coordinators
- Recruits have a preferred archetype that they develop best in

### 3.4 Training & Development Screen (New)
- Weekly practice focus selector with position-group sub-focus
- Training camp focus assignment (3 slots)
- Mentor assignment interface
- Player development history (rating changes per season)

---

## 4. Save/Load & Persistence

### 4.1 New Fields
Add to `PlayerRecord`:
- `archetypeTag: String` — e.g. `"POCKET_PASSER"`, `"SPEED_BACK"`
- `schemeFitTeamId: int` — optional; -1 if none
- `mentorIds: int[2]` — up to 2 mentee player IDs
- `mentorId: int` — which veteran mentors this player
- `trainingCampFocusUsed: boolean` — reset each off-season
- `archetypeCaps: int[4]` — hard caps per attribute

### 4.2 Schema Migration
- `PlayerRecord` proto/record gains new fields with defaults
- Backward compatible: missing archetype = null = balanced growth (no multiplier)

---

## 5. Implementation Phases

### Phase 1: Archetype Data Layer
1. Add `archetypeTag` field to `Player` and `PlayerRecord`
2. Define archetype multiplier arrays and cap arrays per position in a new class `Archetypes.java`
3. Add `getArchetypeMultipliers()`, `getArchetypeCaps()`, `getArchetypeTag()` to `Player`
4. Assign archetypes to generated players in constructors based on initial attribute distribution (e.g., high speed + low power = Speed Back archetype for RB)
5. Test: verify archetype assignment, save/load roundtrip

**Files to modify:**
- `src/main/java/positions/Player.java` — add archetype fields, getter methods, clamp with caps
- `src/main/java/positions/Player*.java` — assign archetype in constructor
- `src/main/java/simulation/PlayerRecord.java` — add archetype fields to record
- `src/main/java/simulation/Archetypes.java` — **new file** with multiplier/cap tables

### Phase 2: Archetype-Biased Progression
1. Modify `genericAdvanceSeason()` and `midSeasonProgression()` to apply archetype multipliers
2. Modify `createGenericAttributes()` and `createImportedSkills()` to apply archetype caps at creation
3. Test: verify attribute growth respects archetype biases, verify caps enforced

**Files to modify:**
- `src/main/java/positions/Player.java` — progression methods

### Phase 3: Scheme Fit
1. Add archetype preferences to OC/DC staff classes
2. Add scheme fit calculation and display
3. Modify progression to apply scheme fit bonus
4. Test: verify scheme fit boosts progression

**Files to modify:**
- `src/main/java/staff/OC.java`, `DC.java` — add preferred archetype fields
- `src/main/java/positions/Player.java` — scheme fit progression bonus
- UI files for displaying scheme fit

### Phase 4: Breakthrough/Bust/Regression
1. Replace current breakthrough logic in `genericAdvanceSeason()`
2. Add bust and late-bloomer logic
3. Add regression for year >= 4 players
4. Test: verify breakthrough/bust rates, verify regression applies

**Files to modify:**
- `src/main/java/positions/Player.java` — `genericAdvanceSeason()`

### Phase 5: Mentor System
1. Add mentor assignment logic to `Team.java`
2. Add mentor progression bonus in `Player.genericAdvanceSeason()`
3. Display mentor info in player profile
4. Test: verify mentor bonus, verify max constraints

**Files to modify:**
- `src/main/java/positions/Player.java` — mentor bonus
- `src/main/java/simulation/Team.java` — mentor assignment logic
- UI files

### Phase 6: Training Camp
1. Add pre-season `trainingCamp()` method to `Team.java`
2. Apply focus-train bonuses to selected players
3. Display training camp summary
4. Test: verify camp bonuses applied, verify reset each season

**Files to modify:**
- `src/main/java/simulation/Team.java` — training camp method
- `src/main/java/positions/Player.java` — camp bonus application
- UI files for camp screen

### Phase 7: Expanded Practice Focus
1. Add position-group sub-focus to `PracticeFocus.java`
2. Add weekly practice outcome application (small weekly changes)
3. Add risk/reward mechanic for intense focus
4. Test: verify sub-focus, verify weekly changes

**Files to modify:**
- `src/main/java/simulation/PracticeFocus.java` — add sub-focus, risk level
- `src/main/java/positions/Player.java` — weekly practice application
- `src/main/java/simulation/Team.java` — weekly practice orchestration

---

## 6. Testing Strategy

### Unit Tests
- Each archetype assigned correctly based on attribute distribution
- Attribute multipliers produce expected growth biases over multiple seasons
- Hard caps enforced after progression (no attribute exceeds cap)
- Save/load roundtrip preserves archetype tag
- Scheme fit calculation matches expected values
- Mentor assignment respects constraints

### Integration Tests
- Full 10-season sim with archetypes produces varied player development outcomes
- Breakthrough/bust rates are within expected statistical ranges
- Regression reduces physical attributes for older players
- Training camp produces expected rating changes
- Practice focus sub-focus correctly targets position groups

### Test Files
- `src/test/java/positions/ArchetypeTest.java` — archetype assignment & progression
- Extend existing `ProgressionTest.java` — archetype-biased growth
- `src/test/java/simulation/SchemeFitTest.java`
- `src/test/java/simulation/MentorTest.java`
- `src/test/java/simulation/TrainingCampTest.java`
- `src/test/java/simulation/RegressionTest.java`

---

## 7. Design Notes & Tradeoffs

### Why archetypes over more attributes?
- Adding more attributes (e.g., separate throw power, short accuracy, deep accuracy) would require massive simulation rewrites
- Archetypes reuse existing `ratAttr1–4` with multipliers — minimal footprint, maximum strategic impact
- Players still feel different: a Speed Back plays differently than a Power Back even though both are RB

### Why hard caps?
- Without caps, archetype differentiation blurs over time as all attributes converge toward 99
- Caps preserve identity: a Pocket QB will never become a running threat, even after 4 years of development

### Why not full attribute refactor?
- `ratAttr1–4` maps cleanly to named attributes per position; the system works
- Full refactor (e.g., separating into 10+ named ratings) would be a 6+ month project
- This plan delivers 80% of the strategic value at 20% of the effort

### Potential balance issues
- Some archetypes may be strictly better than others; monitor sim results and adjust multipliers
- Caps may be too restrictive; consider making them soft caps (diminishing returns rather than hard stop)
- Mentor stacking could over-centralize development; limit to 2 mentors per position group
