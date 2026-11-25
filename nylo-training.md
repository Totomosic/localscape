# Nylocas agent training

## Glossary

- Stall: when a wave has to retry spawning because there are too many nylocas in the room (not because of a natural delay between waves). The naming convention is to name the stall based on the last wave that spawned. For example, a wave 12 spawn would mean that wave 12 has already spawned and it had to retry wave 13 spawning.

- Pre-cap: any wave 1-19

- Post-cap: any wave 20-31

- Cleanup: The time after wave 31 spawns until the last nylocas dies

- Transition: The period from waves 18-21, where players focus on killing big nylocas

- Crab: alias for nylocas

- Lost tick: tick spent not attacking when you are able to attack

- Dim: a birth-time cutoff anchored to a wave's actual spawn, optionally plus a tick offset. A **29 dim** ignores nylos born on or before the instant wave 29 spawns; later splits remain fresh even while wave 29 is current. **28+16 dim** uses the instant 16 ticks after wave 28 spawned, identical to 29 dim when there are exactly three wave-28 stalls. Applying both uses the later cutoff. See the [domain glossary](GLOSSARY.md).

## Sources

The main source to use is blert recordings, for example, https://blert.io/raids/tob/97bfe92d-7206-4444-92c6-3c315e8f9b61/overview. Only use recordings from duo regular tob raids. Only use recordings with a nylocas boss spawn time of 3:16 or less. Only use recordings from vetted duos. A vetted duo has more than 5 recordings together as a pair with an average of a 3:16 boss time or faster. Before starting, write a list of vetted duos.

## Goal

The end goal is to have 2 automated bots that can do the nylocas encounter. A player will send a command to the server to add a bot to their raid team (one of 2 roles; range or mage), then a player (controlled by the server) will do the nylocas encounter with the other player (duo scale).

## Training goals

Here are some interim goals to work towards that let the agents know they are on the right track.

- Work towards minimal pre-cap stalls. The agents should be able to reasonably consistently have 1 or fewer pre-cap stalls with wave 12 being the most common stall. Occasionally, you could have 2, 3 or 4 pre-cap stalls if damage is bad.

- Have a good transition, aim to kill all wave 18 bigs, all 19 bigs and the south and west wave 20 bigs. All this while not stall wave 21.

- Work towards 0 stalls from wave 21 to 28. This should be very consistent if played well.

- Aim for 2 or 3 wave 28 stalls and then ignore all nylos that spawn on wave 29 or below as they will naturally expire.

- Aim for an average nylocas boss spawn of 3:09. 3:07 - 3:12 should be reasonably common with 3:02 being the fastest, 3:04 being uncommon and anything more than 3:14 being rare.

- Compare agent strategies against blert recordings and ensure they have similarities. This should ensure that the agents will still perform well when working with an actual human player since humans have certain expectations about strategies used.

## Training

Orchestrate 2 sub-agents, one will make changes to the range nylocas agent and the other to the mage nylocas agent. After changes are made, run the agents through a series of nylocas simulations and review the results. Continue this iteratively until the training goals are reached.

Swap between researching strategies using blert recordings, implementing those strategies and reviewing the impact it has on performance metrics.

This is a very complex task and will require many training iterations to get right.

Continually report training progress throughout, highlighting average boss spawn time as a key metric.

## Implementation

Implement each agent (range and mage bots) on the osrs server. They will control a player that follows the normal rules that players do but does not need to use a client or UI to interact with the game. These agents will also have access to internal details about the nylocas encounter they are operating in to help influence their decision making.

Read mage-strategy.md for notes on how to implement the mage strategy.

Read range-strategy.md for notes on how to implement the range strategy.

## Combined strategy

- Try to minimise lost ticks

- Avoid doubling up with your partner (it is not good use of time for both players to kill the same nylocas)
