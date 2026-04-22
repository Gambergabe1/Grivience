# NamelessMC Wiki Model (Grivience Draft Set)

Last updated: 2026-03-13

## Overview

This page defines the draft model for publishing Grivience docs to a NamelessMC wiki.
All pages in `wiki-drafts/` are written in markdown and can be copied directly into NamelessMC wiki pages.

Verification status:

- Verified against current `plugin.yml`, `config.yml`, and runtime data file usage in source on 2026-03-13.

## NamelessMC Structure

Use these top-level categories in NamelessMC:

- Getting Started
- Core Progression
- Combat and Equipment
- Farming and Gathering
- Economy and Trading
- Travel and World Systems
- Quests, Events, and Social
- Admin and Configuration
- Developer and Data Files

## Draft Pages Ready

Core progression pages:

- `SKYBLOCK_LEVELING_AND_SKILLS.md`
- `ENCHANTING_SYSTEM.md`
- `FARMING_AND_FARM_HUB.md`
- `DUNGEONS_AND_PARTIES.md`

Admin/developer reference pages:

- `COMMANDS_AND_PERMISSIONS.md`
- `CONFIGURATION_REFERENCE.md`
- `DATA_FILES_AND_PERSISTENCE.md`

## Suggested NamelessMC Page Titles

- Skyblock Leveling and Skills
- Enchanting System
- Farming and Farm Hub
- Dungeons and Parties
- Commands and Permissions
- Configuration Reference
- Data Files and Persistence

## Publication Order

1. Commands and Permissions
2. Configuration Reference
3. Skyblock Leveling and Skills
4. Enchanting System
5. Farming and Farm Hub
6. Dungeons and Parties
7. Data Files and Persistence

## Draft Standard Used

System pages follow this section model:

```md
# <System Name>

## Overview
## Player Flow
## Commands
## Permissions
## Configuration Keys
## Data Files
## Admin Setup
## Balancing Notes
## Troubleshooting
## Related Pages
```

## NamelessMC Publishing Notes

- Keep one wiki page per draft file.
- Preserve command and permission nodes exactly as written.
- Keep config paths exact (`section.subkey`) for copy/paste safety.
- Add screenshots after publish (GUI + practical command examples).
