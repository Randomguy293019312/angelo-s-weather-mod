# Angelo's Weather Mod

> **Stormwatch** — field meteorology, radar, and severe weather for Minecraft.

Angelo's Weather Mod adds meteorological instruments and weather phenomena to Minecraft. Monitor wind, pressure, and precipitation, track storm cells on the radar, and prepare for supercells and tornadoes.

> **Project status:** In development. The `.jar` filename may change between releases; download the artifact attached to the appropriate release.

## Overview

- Doppler radar and a weather console that displays nearby targets.
- Wind, atmospheric pressure, and precipitation readings.
- Supercells with severity levels and tornadoes rated on the EF scale.
- Placeable instruments and a portable tablet.
- Storm siren, shelter beacon, and survival equipment.
- Admin commands for checking and testing weather events.

## Dashboard and radar display

The Stormwatch dashboard features a circular radar with range rings, reference lines, and an animated sweep. The player is shown at the center; nearby cells and tornadoes appear as targets.

```text
┌──────────────────── STORMWATCH // RADAR ────────────────────┐
│                                                              │
│                                                              │
│      .-----------------.                                     │
│     /   rings & targets \                   info here        │
│    |    cells      ●     |                                   │
│    |        + player     |                                   │
│     \___________________/                                    │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```


## Mod content

### Instruments and items

- **Radar** — console for tracking nearby cells and tornadoes.
- **Weather Station** — local-conditions dashboard.
- **Weather phone** — portable access to the dashboard.
- **Storm Siren** — warning instrument.
- **energy bar** — themed shelter and warning equipment.
- **Survival kit** — preparation item for severe conditions.
- **Hot Air Balloon** — item associated with the mod's hot air balloon.

### Weather phenomena and entities

- **Supercells** with severity levels from 1 to 5.
- **Tornadoes** rated from EF0 to EF5.
- **Hot air balloon**.
- Tornadoes move across terrain, have formation stages, emit particles, and affect nearby entities, destroy blocks.

## Installation

### Requirements

- Minecraft **Java Edition 1.21.1**.
- **NeoForge 21.1.x or later**.
- **Java 21**.

### Install a released version

1. Install Fabric Loader for Minecraft 1.20.1.
2. Download and install the Fabric API version matching Minecraft 1.20.1.
3. Download the mod `.jar` from this repository's **Releases** section.
4. Place the mod `.jar` and Fabric API `.jar` in the `mods` folder for your Fabric profile.
5. Launch Minecraft using that profile.


## Usage

Interact with a placed instrument or use the tablet to open the weather dashboard. Press **Esc** to close it. Instruments show a similar interface with information relevant to their function.

### Commands

The following commands use the `/stormwatch` root and require permission level 2 (server operator):

| Command | Description |
| --- | --- |
| `/meteo info` | Displays a short report on weather and nearby phenomena. |
| `/meteo rain` | Starts heavy rain for 10 minutes. |
| `/meteo rain <severity>` | Starts heavy rain for the specified duration (1–60 minutes). | 
| `/meteo supercell <severity>` | Creates a supercell with severity 1–5. |
| `/meteo tornado <ef>` | Creates a tornado rated EF0–EF5. |

Creation commands place phenomena near the player who ran the command. These are admin and testing tools, not available to players without the required permission.

## Build from source

The project uses Gradle, Fabric Loom, and Java 17. From the repository root, run:

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

Build outputs are written to `build/libs/`. The project also provides a `copyModJar` task, which copies the remapped artifact to `build/deliverables/` as `stormwatch-<version>.jar`.

## Technologies

- Minecraft 1.20.1
- Fabric Loader and Fabric API
- Java 17
- Gradle and Fabric Loom

## Screenshots

Add screenshots of the radar, instruments, and weather phenomena here. Upload images to the repository and reference them like this:

```md
![Stormwatch radar dashboard](docs/images/radar.png)
```

## Credits

- **Repository:** Angelo's Weather Mod
- **Mod name:** Stormwatch
- **Author:** Angelo

## License

This project is **All Rights Reserved**. No permission is granted to copy, modify, distribute, or republish this work without prior written permission from the copyright holder. See [`LICENSE`](LICENSE) for the full terms. The mod metadata uses `LicenseRef-Proprietary` to identify this custom, non-standard license.
