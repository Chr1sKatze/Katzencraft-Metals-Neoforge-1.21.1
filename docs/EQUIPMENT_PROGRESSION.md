# Equipment profiles and mining progression

Minecraft 1.21.1 / NeoForge 21.1.219. This implementation centralizes equipment without changing Foundry rendering, metal order, density, recipes, cooling or recycling.

## Configuration ownership

- `metal/MetalEquipmentProfiles.java`: immutable custom and vanilla-reference profiles. Named total combat values, durability, speed, enchantability, repair ingredient, native incorrect-tool tag, armor defense/toughness/knockback/sound/layers.
- `metal/MetalCatalog.java`: optional `.equipment(profile)` link plus `.oreMiningLevel(1..8)` and `.storageMiningLevel(1..8)`. These requirements are independent of tool capability and density/Foundry timing. The old `.miningLevel(...)` spelling remains a storage-only compatibility alias.
- `ModItems`, `ModToolTiers`, `ModArmorMaterials`: registration adapters reading the linked catalog profile. Per-tool tier objects honor individual durability.
- Vanilla wood, stone and netherite profiles live alongside the metals without inventing catalog metals or changing molten synchronization IDs. Platinum has no equipment profile.
- No equipment values are read from the legacy COMMON `katzencraftmetals-tooltiers.toml`. Its registration and keys remain, explicitly deprecated/inert, so existing files are not deleted or silently activated.

## Approved progression

These are mod capabilities, not native Minecraft tier numbers.

| Material | Capability |
|---|---:|
| Wood | 2 |
| Stone | 3 |
| Copper | 4 |
| Gold | 4 |
| Iron | 4 |
| Steel | 6 |
| Netherite | 7 |
| Mythril | 8 |

All requirement levels 1 through 8 exist, including resource-only level 5 and currently unused level 8 blocks. Diamond has no active profile/capability. Existing Diamond pickaxes retain access to the explicitly generated `katzencraftmetals:legacy_diamond_access` block tag for existing Steel, Platinum and Mythril content, at requirements no greater than 7. No new Diamond access to future level-8 resources is granted. This exception identifies the Diamond item type, not its creation date: newly acquired Diamond pickaxes also retain that block-scoped access until the remaining acquisition paths are separately removed. No save-tag or item-age migration is invented.

Steel storage and specialized Iron-gated building blocks require 4. Steel buttons require 3, trapdoors 2. Platinum ores (all four variants) and storage require 6. Mythril ores (all four variants) and storage require the explicitly approved level 7. Netherite and Mythril tools satisfy that gate; Steel does not. The legacy Diamond exception preserves saved-tool access.

Iron/Copper ore and storage metadata reference native Stone access (3). Gold ore/storage reference native Iron access (4), with Nether gold ore explicitly referencing Wood access (2). These explicit vanilla references now participate in our requirement tags and denial-only harvest checks.

## Native harvesting and compatibility

Supported pickaxes receive drop-only exceptions for explicitly configured vanilla resources at or below their profile capability. These precede only the canonical native tier rejection; foreign component denials retain priority. Mining speeds, durability and unrelated native rules remain unchanged. Gold and Copper can therefore harvest Gold Ore without granting them Obsidian access. Catalog metal requirements stay authoritative; Coal and the basic Stone family use the starting Wood requirement in BlockRequirements.

`ToolRequirementEvents` is denial-only and scoped to explicitly configured/tagged blocks. It never changes `false` to `true`, preserving native rejection and other mods' denials. Wrong tool types, empty hands and unrecognized tools cannot acquire custom resource drops through numeric fallback. Insufficient tools get the existing 1% speed multiplier only on custom requirement blocks; the normal no-harvest break penalty also applies. Blocks can still be destroyed without drops.

Data packs can put compatible items into `katzencraftmetals:mining_capability_1` through `_8` (highest match wins). The item must also be an actual pickaxe via native `PICKAXE_DIG` ability or `minecraft:pickaxes`, and native tool rules must allow the target. Opt-in never bypasses a native denial. This supports other mods without item-ID switches. For known tools, raising a capability tag alone cannot undo their profile-derived native denial rules; change the authoritative profile for a genuine rebalance.

Blocks can opt into `katzencraftmetals:needs_mining_level_1` through `_8`; highest tag wins. Catalog-owned block requirements remain the fallback if their tag is absent. Unrelated blocks with no explicit requirement retain native harvesting and speeds.

The former hand-minable stone fallback has been removed. Empty hands and wrong tool types cannot receive manually injected stone drops. Normal loot runs only through Minecraft's existing successful-harvest path; no new drop-spawning handler is added.

All five Wooden crafting recipes now fall through to their original Minecraft recipes; the five dormant nested disabling overrides and Creative hide entries were removed. The nested overrides pack had no pack metadata or registration, so those files alone did not establish runtime recipe suppression. Other nested overrides are preserved without activating them.

## Equipment balance

Total damage includes the player's base 1 attack damage. Attacks/second includes base attack speed 4. Registration converts to `damage - 1 - tierAttackBonus` and `attacksPerSecond - 4`, preserving the current internal Steel/Mythril tier bonuses. Profiles expose real vanilla statistics from registered items/tiers rather than making copied numbers authoritative.

| Material | Tool durability | Speed | Enchantability | Damage sword/pickaxe/axe/shovel/hoe | Attacks/sec sword/pickaxe/axe/shovel/hoe |
|---|---:|---:|---:|---|---|
| Copper | 190 | 5 | 12 | 5 / 3 / 7 / 3.5 / 1 | 1.6 / 1.2 / 0.9 / 1 / 2 |
| Steel | 512 | 7 | 18 | 7 / 5 / 5.5 / 5.5 / 2 | 1.6 / 1.2 / 1 / 1 / 3 |
| Mythril | 1536 | 10 | 25 | 10 / 7 / 7.5 / 7.5 / 4 | 1.6 / 1.2 / 1 / 1 / 3 |

Shared `.toolDurability(n)` applies to all five tools; `.toolDurability(ToolKind.AXE,n)` or `.pickaxeDurability(n)` overrides one. Damage and attack speeds are configured independently for each tool.

Player armor order is helmet/chestplate/leggings/boots:

| Material | Defense | Durability | Toughness | Knockback | Enchantability |
|---|---|---|---:|---:|---:|
| Copper | 2 / 5 / 4 / 1 | 132 / 192 / 180 / 156 | 0 | 0 | 12 |
| Steel | 2 / 6 / 5 / 2 | 264 / 384 / 360 / 312 | 0 | 0 | 18 |
| Mythril | 3 / 8 / 6 / 3 | 440 / 640 / 600 / 520 | 2 | 0 | 25 |

Steel and Mythril non-helmet durability was explicitly approved to change from uniform helmet durability. Their damage, attack speed, mining speed, enchantability, defenses, toughness, knockback, sounds, repair ingredients and authored layers are preserved. Existing saved item damage is not rescaled or rewritten. Steel horse armor retains its existing BODY defense 5 and nondamageable item behavior.

Armor supports a shared actual durability plus independent `.helmetDurability`, `.chestplateDurability`, `.leggingsDurability`, `.bootsDurability` overrides, and independent defenses. Positive durability, finite/ranged statistics, nonnegative defenses and 1..8 capability/requirements are validated. Incomplete custom profiles fail early.

## Explicit vanilla overrides

```java
public static final Profile IRON = vanillaEquipment("iron")
        .miningLevel(4)
        // Optional example only; current live Iron profile has NO stat overrides:
        .pickaxeDurability(300)
        .build();
```

Only explicitly present values are applied. Current Wood/Stone/Gold/Iron/Netherite profiles assign capability but do not rebalance native equipment statistics. Defaults are patched once during the supported NeoForge `ModifyDefaultComponentsEvent`, on both physical sides; no ticks, reflection, item replacement or per-inventory rewrites are used.

Supported vanilla component overrides: shared/per-tool and shared/per-slot MAX_DAMAGE, total tool damage/attack speed, existing TOOL-rule mining speeds, player armor defense/toughness/knockback. Attribute updates replace only the matching native modifier ID and preserve unrelated entries. Existing enchantment/name/damage/custom component patches remain; explicit saved stack overrides can intentionally win over new defaults.

Limitations in 1.21.1: enchantability, repair ingredients, material equip sound and armor-layer identity are methods/material properties, not equivalent mutable item components. Explicit vanilla overrides of those fields are rejected with a descriptive error. Custom equipment fully supports them. Equip sounds use lazy suppliers (`.equipSound(() -> SoundEvents.ARMOR_EQUIP_IRON)`) so catalog metadata stays safe before registry bootstrap. No mixin/global behavior hack is introduced. Profile snapshots must be checked against actual item component/attribute behavior, not just `Tier`/`ArmorMaterial` getters, which retain native identities.

## Copper integration and temporary artwork

All nine Copper equipment items are registered under `katzencraftmetals:copper_<part>` and crafted using ordinary shapes and vanilla copper ingots. Tool repairs and armor repair ingredients use the same profile. Copper equipment is not automatically enrolled in Foundry recycling/melting; existing recipe definitions remain unchanged. The mod's replacement anvil also has Copper-only repair entries, at 25% max durability per ingot using the existing menu's rounding; previous repair entries are untouched. Copper has normal tool/armor and capability tags plus names and Creative entries.

The user explicitly approved TEMPORARY vanilla Iron item sprites and Iron armor layers. No texture was generated, copied or modified. Copper currently looks like Iron; it is not finished Copper artwork. Future authored files:

- `textures/item/copper_sword.png`, `copper_pickaxe.png`, `copper_axe.png`, `copper_shovel.png`, `copper_hoe.png`.
- `textures/item/copper_helmet.png`, `copper_chestplate.png`, `copper_leggings.png`, `copper_boots.png`.
- `textures/models/armor/copper_layer_1.png`, `copper_layer_2.png`.

After authoring, change only Copper's model-generator texture references and `.armorLayer("katzencraftmetals:copper")`. Preserve original files and UV conventions.

## Diamond acquisition audit and Netherite migration

Diamond equipment is excluded from active progression but is NOT yet made unobtainable. Only the nine tool/armor crafting recipes are now disabled, by explicitly authorized overrides using `neoforge:conditions` / `neoforge:false` in the required `datapacks/disabled_diamond_equipment` pack. Existing items, trades, rewards and inventories are preserved.

Remaining acquisition paths in the inspected base-game and mod resources:

- Commands remain; the nine vanilla Diamond crafting recipes are disabled and the nine items are hidden from Creative tabs/search. Underlying vanilla registrations and existing items are retained.
- Vanilla Armorer, Toolsmith and Weaponsmith trades still include Diamond equipment.
- Mob armor/weapon equipment and drops remain possible; trial equipment tables contain Diamond gear.
- Unoverridden static gear loot remains in trial-chamber `intersection`, `intersection_barrel`, `reward_rare`, `reward_ominous_rare`, and equipment tables `trial_chamber` / `trial_chamber_melee`.
- Twelve other vanilla chest tables that contain Diamond gear are already overridden by this mod (ancient city, bastions, desert/jungle temples, end city, nether bridge, dungeon, stronghold corridor, village weaponsmith, woodland mansion); their current mod replacements omit that equipment.
- Diamond gem loot/archaeology and saved/other-mod items remain. The Diamond gem is not a metal and is not removed.

Netherite now uses direct crafting from Netherite Ingots in standard tool/armor patterns. All nine former smithing upgrades, upgrade-template duplication, and old refining recipes are disabled by the required Netherite progression pack. The obsolete template is hidden in Creative; other templates and smithing/trims are unchanged. See ADDING_METALS.md for the seventh catalog entry and Foundry-only refining path.

The nine recipe overrides must be in the dedicated required high-priority data pack, registered through `AddPackFindersEvent` on the mod bus. Ordinary mod resources lost to NeoForge's later-loaded common-tag replacements at the exact same Minecraft recipe IDs. Do not move these files back to root `data/` or into the unrelated dormant override pack. The scoped pack has valid 1.21.1 data format 48 and is automatically selected for existing/new pack repositories.

`CatoDisableItemsEvents` runs `BuildCreativeModeTabContentsEvent` on the mod bus, removes target item identities (including component variants) from both parent and search entries, and shares its predicate with the existing client scroll/search mixin. Diamonds, blocks, Wooden tools and Netherite remain visible. World loot, trades and existing-item repair are outside this crafting/Creative change.

## Reproducible validation

Run without clean:

```powershell
.\gradlew.bat runData
.\gradlew.bat --no-configuration-cache -I verification/equipment.init.gradle runData
.\gradlew.bat build
```

The opt-in init script compiles `verification/equipment/EquipmentChecks.java` into a separate verification source set, registers it only in the data run, and writes isolated generated output to `build/equipment-datagen`. Verification classes/fixtures are not shipped in the normal JAR. The pre-migration fixture records 79 actual registered item statistics; armor durability differences are approved explicitly. Tests use real registered items, native tool rules, genuine startup component modification, isolated tag bindings, test players and production event handlers. They cover all eight capabilities/requirements, wrong/unknown tools, Diamond compatibility, native denial preservation, component patches, independent durability, total attacks, all nine actual Copper crafting recipes and all nine repair previews through the actual custom anvil menu.

These checks are NOT an interactive client/dedicated-server playtest. Actual block breaking/drops, world reload of damaged/enchanted items, multiplayer tooltips, Copper crafting/anvil UI, stone loot and other-mod event integration, temporary armor art and tag synchronization still need Minecraft verification. No rendering verification is claimed.
