# Research Task

Carefully study blert nylocas recordings (eg. https://blert.io/raids/tob/653f90c7-176b-4fd4-a0eb-1cbd818481ce/nylocas) for regular tob (theatre of blood) in solo or duo scales. The intention is to research how nylocas interact with each other around the 4 pillars in the corners of the room. It is essential that our simulation feels authentic and this is the part that feels the least realistic. In each recording you look at try and predict where nylos will path and how they will group up on a pillar based on our simulation implementation and compare it to what actually happens.

Record all findings, observations, corrections, etc. in a `docs/nylo-pathing` directory.

## Observations

The first step is documenting observations, ways that the blert recordings follow our simulation and ways that they don't. Gather a wide range of recordings and highlight differences across all stages of the room, and both sizes of nylocas. Focus the investigation on regular, non-fighting nylocas.

## Corrections

Review the observations and suggest fixes to the simulation that correct the pathing/stacking results. It is important to review observations as a whole and not individually, it is likely to require small fundamental engine changes rather than lots of special cases for each scenario.

Don't implement anything concrete but suggest the changes using pseudo-code, images and examples.

Keep recording more observations and corrections until you can clearly explain what needs to be fixed to bring our simulation in line with the recordings.

## Leads

Here are a couple of observations that I have noticed while testing our simulation:

- Real nylos are not so eager to wrap around to another side of the pillar, they tend to pace back and fourth near the side they were originally targetting. They can move to the other side but this feels rare.

- When real nylos pace, they alternate between moving back and forward in straight lines and back and forward diagonally.

- The wave 25 and 27 big nylos that path to the south east pillar nearly always end up on the north side of it, and quite often the 27 big is poking outside of the room boundary.

- Generally our simulation nylos pace too much, real nylos tend to better group up on pillars. However, they rarely are stacked more than 2 on top of the same tile.
