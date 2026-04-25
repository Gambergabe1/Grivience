# Drill Forge System Guide

This document explains the **Thermal Drill Forge** in plain language.

The short version:

- The **Drill Bay** is where you refuel your drill and install drill parts.
- the **Project Queue** is where you start timed forge jobs to create better drill supplies and parts.
- the **Overdrive Chamber** is where you spend **Forge Heat** to temporarily power up your drill.

If you only want the basic flow, read the next section and stop there.

## Simple Player Flow

1. Hold your drill in your main hand.
2. Open the forge with `/drillforge` or by sneak-right-clicking an anvil while holding a drill.
3. In the **Drill Bay**, refuel the drill or install an engine / fuel tank.
4. Open **Projects** and start a timed forge job.
5. Come back when the timer is done and claim the finished item.
6. Claimed projects give **Forge Heat**.
7. Use that heat in **Overdrive** to make your drill better for a limited time.

That is the whole loop.

## What Each Screen Does

### 1. Drill Bay

The Drill Bay is the fast-use screen.

Use it for:

- adding fuel to the drill
- instantly refilling the whole fuel tank with coins
- installing engine upgrades
- installing fuel tank upgrades

You must be holding a drill to use the Drill Bay.

### 2. Project Queue

The Project Queue is the long-term progression part of the forge.

You start timed forge Remove the Native Dragon TracjubgMakejobs here.
Each job costs:

- some materials
- some coins
- some real waiting time

When the timer finishes, you claim the output from the queue.

You can queue up to **3 projects** at once.

### 3. Overdrive Chamber

Overdrive is the “power spike” part of the system.

It costs:

- **Forge Heat**
- coins

While Overdrive is active:

- your drill uses less fuel per block
- your drill ability cooldown is shorter
- your drill ability lasts longer
- your drill ability is stronger

This is meant for grinding sessions, not permanent uptime.

## What Forge Heat Is

**Forge Heat** is the resource that makes the forge feel alive instead of static.

You get Forge Heat by **claiming finished forge projects**.

Forge Heat does two things:

1. It speeds up future forge jobs.
2. It lets you activate **Overdrive**.

That means the more you use the forge, the better it feels.
Players who keep coming back get faster projects and better drill burst windows.

## Why The System Matters

The Drill Forge is not just a cosmetic menu.

It directly affects drills in gameplay:

- drills can be refueled and maintained here
- drill engines and tanks are installed here
- forge projects create useful mining items and advanced drill parts
- Overdrive changes real drill behavior while mining

## Forge Project Types

The forge currently supports long-form projects for:

- `Volta`
- `Oil Barrel`
- `Prospector's Compass`
- `Mining XP Scroll`
- `Stability Anchor`
- `Mithril Engine`
- `Titanium Engine`
- `Gemstone Engine`
- `Divan Engine`
- `Medium Fuel Tank`
- `Large Fuel Tank`
- `Temporary Mining Speed Boost`

Some of these are quick utility projects.
Some are major progression projects.

## Commands

### Player Commands

- `/drillforge`
  - Opens the main Drill Bay.
- `/drillforge projects`
  - Opens the forge queue and project list.
- `/drillforge overdrive`
  - Opens the Overdrive Chamber.
- `/drillforge status`
  - Shows Forge Heat, queue status, and Overdrive status in chat.
- `/drillforge parts`
  - Lists the main drill parts and fuel items.
- `/drillforge help`
  - Shows command help.

Aliases:

- `/df`
- `/drillmechanic`

## Common Use Cases

### I just want to refuel my drill

1. Hold the drill.
2. Run `/drillforge`.
3. Click a fuel option in the Drill Bay.

### I want to make stronger drill parts

1. Run `/drillforge projects`.
2. Choose the engine or tank project you want.
3. Make sure you have the materials and coins.
4. Start the project.
5. Come back later and claim it.
6. Install the finished part in the Drill Bay.

### I want faster mining right now

1. Build up Forge Heat by claiming finished projects.
2. Run `/drillforge overdrive`.
3. Ignite Overdrive.
4. Go mining while the timer is active.

## Example Session

Here is what a normal session looks like:

1. You open `/drillforge`.
2. You top off your drill fuel.
3. You queue a `Volta` project and a `Mithril Engine` project.
4. Later, you claim them.
5. Your Forge Heat goes up.
6. You install the new engine.
7. You ignite Overdrive.
8. You go back to mining with lower fuel burn and a better drill surge.

That loop is the intended experience.

## Data Storage

Runtime forge data is stored in:

- `plugins/Grivience/drill-forge-data.yml`

This includes:

- profile Forge Heat
- total project claims
- active forge queue entries
- Overdrive timers

## Troubleshooting

### “The Drill Bay says I need to hold a drill”

You must hold a custom drill in your **main hand**.

### “I can’t start a project”

Check all three of these:

- you have the required items
- you have enough coins
- your queue is not already full

### “I can’t claim a project”

The timer has probably not finished yet.
Use `/drillforge status` or reopen `/drillforge projects`.

### “Overdrive won’t start”

You probably do not have enough:

- Forge Heat
- coins

### “Why is the forge getting faster?”

That is intended.
Higher **Forge Heat** reduces project times.

## Admin / Documentation Notes

Primary source files for this system:

- `src/main/java/io/papermc/Grivience/mines/DrillForgeManager.java`
- `src/main/java/io/papermc/Grivience/mines/DrillMechanicGui.java`
- `src/main/java/io/papermc/Grivience/mines/DrillForgeCommand.java`
- `src/main/java/io/papermc/Grivience/mines/MiningItemListener.java`
- `src/main/resources/plugin.yml`

If written docs ever disagree with code, trust those files first.
