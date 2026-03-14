# AE2Mon — Cobblemon × Applied Energistics 2

**AE2Mon** bridges [Cobblemon](https://cobblemon.com/) and [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2), letting you store, filter, and manage your Pokémon inside a ME network — or carry them in a pocket-sized portable cell.

---

## Features

### Pokemon Cell
A storage cell that slots into any ME network. Stores up to **32 Pokémon** and integrates fully with AE2 automation and drives.

- Supports **Type Filter Cards** (18 types) to restrict which Pokémon can be stored
- Compatible with the Fuzzy Card and Inverter Card upgrades
- Pokémon data is preserved in full NBT fidelity

### Pokemon Terminal (Part)
A cable part that mounts on any AE2 cable. Opens a full-featured GUI to browse every Pokémon across your entire ME network.

- **Search** by name or nature; filter with inline IV expressions (`atk=31`, `spe>=25`, etc.)
- **Shiny filter** and **perfect-IV count filter**
- Live animated Pokémon sprites with detailed stats panel (IVs, EVs, nature, ability, type badges, held item)
- **Deposit** from party; **Withdraw** to party directly from the terminal

### Portable Pokemon Cell
A handheld item that stores up to **6 Pokémon** — no ME network required.

- Right-click to open a simple deposit/withdraw GUI
- Automatically **heals** Pokémon (full HP + clears all status conditions) when stored
- Crafted from a Pokemon Cell + Ender Pearl
- Works completely offline, perfect for exploration

### Type Filter Cards
18 type-specific upgrade cards (Normal, Fire, Water, …). Insert one into a Pokemon Cell to restrict storage to Pokémon of that type.

---

## Requirements

| Dependency | Version |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.x |
| Applied Energistics 2 | 19.x |
| Cobblemon | 1.6+ |

---

## Installation

1. Install [NeoForge 21.1.x](https://neoforged.net/)
2. Download [Applied Energistics 2](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2) and [Cobblemon](https://cobblemon.com/)
3. Drop all three `.jar` files into your `mods/` folder
4. Launch the game

---

## Crafting Recipes

### Pokemon Cell
```
[ AE2 Item Cell Housing ] + [ Cobblemon PC ]
```

### Pokemon Terminal (Part)
```
[ AE2 Item Cell Housing ] + [ AE2 Engineering Processor ]
```

### Portable Pokemon Cell
```
[ Pokemon Cell ] + [ Ender Pearl ]
```

### Type Filter Cards
Crafted with the corresponding type-specific ingredient (shapeless). Check JEI/REI in-game for full details.

---

## Building from Source

```bash
git clone <repo-url>
cd ae2mon
./gradlew build
```

The output JAR will be in `build/libs/`.

To run in a dev environment:

```bash
./gradlew runClient   # launch Minecraft client
./gradlew runServer   # launch dedicated server
```

---

## License

**Creative Commons Attribution-NonCommercial 4.0 International (CC BY-NC 4.0)**

You are free to:
- **Share** — copy and redistribute this mod in any medium or format
- **Adapt** — remix, transform, and build upon this mod

Under the following terms:
- **Attribution** — You must give appropriate credit, link to this project, and indicate if changes were made
- **NonCommercial** — You may not use this mod or any derivative work for commercial purposes (including paid modpacks behind a paywall, monetized servers that charge for gameplay advantages, or selling compiled builds)

> No additional restrictions — you may not apply legal terms or technological measures that legally restrict others from doing anything the license permits.

Full license text: https://creativecommons.org/licenses/by-nc/4.0/

---

## Contributing

Issues and pull requests are welcome. Please describe what you changed and why.

- Keep code style consistent with the existing codebase (Java 21, NeoForge conventions)
- Test deposit/withdraw flows in both ME-network and standalone (portable cell) modes before submitting

---

## Credits

- Built on top of [Applied Energistics 2](https://github.com/AppliedEnergistics/Applied-Energistics-2) and [Cobblemon](https://gitlab.com/cable-mc/cobblemon)
- Pokémon sprite rendering powered by Cobblemon's client rendering API
