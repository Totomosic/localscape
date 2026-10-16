# Scythe of Vitur AoE ordering

Researched 2026-10-17.

Jagex's [Dev Blog: Theatre of Blood](https://secure.runescape.com/m=news/dev-blog-theatre-of-blood?oldschool=1), dated 20 March 2018, is the primary source for the following mechanics:

- A swing covers a 1×3 arc in front of the player and can hit up to three enemies once.
- A swing can hit a large creature multiple times, with at most three hits and three NPCs overall.
- The first, second and third hits deal 100%, 50% and 25% damage respectively.
- Each attack has an independent roll against its recipient's defence, regardless of the primary target.

These statements appear in the **Scythe of Vitur** section and poll question 14. This is the original proposal, so it establishes the intended baseline rather than proving every detail of the current server implementation.

## Unresolved ordering

The official description does **not** map the second and third hits to the player's left/right, identify an NPC-index priority, or explicitly promise that the selected NPC receives the first hit. Searches did not locate a primary source establishing those rules. Consequently, a comment should identify lateral damage ordering as unverified; any deterministic choice in this simulator is an implementation assumption, not a sourced OSRS rule.
