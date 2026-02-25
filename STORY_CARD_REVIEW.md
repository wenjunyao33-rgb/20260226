# Story Card vs. Code Gap Analysis

## Executive Summary

**The codebase is the unmodified starter template.** All event handlers
(`CardClicked`, `TileClicked`, `EndTurnClicked`, `Heartbeat`, `OtherClicked`,
`UnitMoving`, `UnitStopped`) contain **no game logic** — only placeholder reads
of JSON fields. `Initalize` still calls `CommandDemo.executeDemo()`.
`GameState` holds only two booleans (`gameInitalised`, `something`).
**None of the 26 Story Cards have been implemented.**

---

## Story Card Status Matrix

| SC | Story Card | Status | Evidence / Gap |
|----|-----------|--------|---------------|
| SC-01 | Game Initialization — Draw 9x5 board, place avatars, set health=20, mana, draw 3 cards | NOT IMPL | `Initalize.java:32` calls `CommandDemo`. No board, no avatar, no deck/hand setup. |
| SC-02 | End Turn — Switch active player, trigger AI turn | NOT IMPL | `EndTurnClicked.java:22` is empty. No turn counter, no AI trigger. |
| SC-03 | Card Draw — Draw card from deck at turn start (max hand 6) | NOT IMPL | No deck or hand data structure in `GameState` or `Player`. |
| SC-04 | Player Mana — Mana increases per turn (Turn 1=2, Turn 2=3, max 9) | NOT IMPL | No turn-based mana logic. |
| SC-05 | Card Click / Highlight — Click card → highlight + show valid tiles | NOT IMPL | `CardClicked.java:26` reads position but does nothing. |
| SC-06 | Play Creature Card — Summon unit, deduct mana, remove card | NOT IMPL | `TileClicked.java:31` has placeholder comment only. |
| SC-07 | Play Spell Card — Target selection + effect resolution | NOT IMPL | No spell handling code. |
| SC-08 | Unit Movement — Show valid tiles (up to 2), move unit | NOT IMPL | No selection state, no range calc, no pathfinding. |
| SC-09 | Unit Attack — Show attackable enemies, attack on click | NOT IMPL | `Unit.java` has **no attack/health fields**. |
| SC-10 | Counter-attack — Defender retaliates | NOT IMPL | No combat system. |
| SC-11 | Unit Death — Health <= 0 → death animation → remove | NOT IMPL | No health tracking on units. |
| SC-12 | Avatar Death / Game Over — Avatar health <= 0 → game ends | NOT IMPL | No win/lose check. |
| SC-13 | Provoke — Adjacent enemies forced to attack Provoke unit | NOT IMPL | No keyword system in game logic. |
| SC-14 | Flying — Move to any unoccupied tile | NOT IMPL | No movement rules. |
| SC-15 | Rush — Attack immediately after summon | NOT IMPL | No hasMoved/hasAttacked tracking. |
| SC-16 | Ranged — Attack any enemy on board | NOT IMPL | No attack range calc. |
| SC-17 | Airdrop — Place on any unoccupied tile | NOT IMPL | No placement validation. |
| SC-18 | Deathwatch — Bad Omen, Shadow Watcher, Bloodmoon Priestess, Shadowdancer | NOT IMPL | No death-event observer system. |
| SC-19 | Opening Gambit — Gloom Chaser, Nightsorrow Assassin, Silverguard Squire | NOT IMPL | No on-summon hooks. |
| SC-20 | Silverguard Knight — Provoke + Zeal (+2 attack on avatar damage) | NOT IMPL | No Zeal mechanic. |
| SC-21 | Horn of the Forsaken — Artifact(3), on-hit summon Wraithling | NOT IMPL | No artifact system. |
| SC-22 | Wraithling Swarm — Summon 3 Wraithlings | NOT IMPL | No spell resolution. |
| SC-23 | Dark Terminus — Destroy enemy, summon Wraithling in its place | NOT IMPL | No targeted spell system. |
| SC-24 | Beamshock — Stun enemy unit | NOT IMPL | No stun/status effects. |
| SC-25 | Sundrop Elixir — Heal unit by 5 (not over max) | NOT IMPL | No maxHealth tracking. |
| SC-26 | Truestrike — Deal 2 damage to enemy unit | NOT IMPL | No targeted damage. |

---

## Critical Missing Infrastructure

| Missing System | Required By | What Needs Building |
|---------------|-------------|-------------------|
| Board Model (9x5 Tile grid) | SC-01, SC-05-09, SC-13-14, SC-17-26 | `Tile[][] board` in GameState; occupancy tracking |
| Player Model (deck, hand, avatar) | SC-01-07, SC-12, SC-18-26 | `List<Card> deck`, `List<Card> hand`, `Unit avatar` per player |
| Unit Combat Stats | SC-09-12, SC-18-26 | `int attack, health, maxHealth` on Unit |
| Turn Manager | SC-02-04 | Turn counter, mana increment, card draw, AI trigger |
| Unit Ownership / State | SC-08-09, SC-13, SC-15-16 | `int owner`, `boolean hasMoved/hasAttacked` |
| Keyword / Ability System | SC-13-20 | Keyword checks + ability triggers |
| Spell Resolution | SC-07, SC-21-26 | Card type dispatch + target validation + effects |
| AI Controller | SC-02 | Basic AI for Player 2 |
| Game Over Check | SC-12 | Avatar health check after every change |

---

## Code Quality Issues Found

1. **`OrderedCardLoader`** — `cardID` never incremented; all cards get id=1
2. **`BasicObjectBuilders.loadUnit()` line 182** — HIT animation block sets `getChannel()` instead of `getHit()` (copy-paste bug)
3. **`Initalize.java`** — Still runs `CommandDemo`; must be replaced with real init
4. **`Tile`** — No `Unit` field for occupancy tracking
5. **`Unit`** — No combat stats (attack, health); only animation/position data
6. **No thread safety** — `GameState` not synchronized; race conditions possible

---

## Recommended Implementation Order

| Phase | Story Cards | Description |
|-------|-------------|-------------|
| Phase 1 | SC-01, SC-02, SC-03, SC-04 | Game init + turn system |
| Phase 2 | SC-05, SC-06, SC-08, SC-09 | Card play + movement + basic combat |
| Phase 3 | SC-10, SC-11, SC-12 | Death + game over |
| Phase 4 | SC-07, SC-22, SC-23, SC-25, SC-26 | Spell cards |
| Phase 5 | SC-13, SC-14, SC-15, SC-16, SC-17 | Keywords |
| Phase 6 | SC-18, SC-19, SC-20 | Triggered abilities |
| Phase 7 | SC-21, SC-24 | Complex mechanics (Artifact, Stun) |
