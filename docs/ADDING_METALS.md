# Adding metals to Katzencraft Metals

Source audit: 2026-10-19. Minecraft 1.21.1, NeoForge 21.1.219, Java 21, Gradle 9.2.1, ModDevGradle 2.0.141. This describes the current working source, including untracked Foundry Table files. Phases 1–3 are implemented: catalog/naming, standard forms/resources, and catalog-driven Foundry registration, melting and whole-ingot alloy definitions.

## Current architecture

A metal has a central catalog definition, catalog-driven standard forms, a derived molten-metal definition and optional manual ore/equipment progression. A PNG alone does not register a metal. Foundry support alone does not create obtainable forms, recipes, mining rules or names.

Java paths below are relative to `src/main/java/net/chriskatze/katzencraftmetals/`. Other paths are repository-relative. Assets normally use `katzencraftmetals`; vanilla molten definitions deliberately retain the `minecraft` namespace.

| Concern | Current source and entry point | Scope |
|---|---|---|
| Items | `item/ModItems.java`, catalog-driven standard-item helper | Ingots/nuggets automatic; raw/special items manual |
| Blocks and their items | `block/ModBlocks.java`, catalog-driven standard-block helper | Enabled standard storage cubes automatic; ore/building blocks manual |
| Creative inventory | `KatzencraftMetalsMod.addCreative`, `item/MetalCreativeEntries.java` | Standard forms automatic, legacy positions preserved; other forms manual |
| Molten definitions | `metal/ModMoltenMetals.java`, `createDefinitions`; `FoundryMetalBuilder.fromCatalog` | Every Foundry metal |
| Definition schema | `metal/MoltenMetalDefinition.java` | Reused, not a new class for each metal |
| Item models | `datagen/ModItemModelProvider.java`, `registerModels` | Explicit basic/handheld item entries |
| Block models/states/item models | `datagen/ModBlockStateProvider.java`, `registerStatesAndModels` | Explicit cube or special-model entries |
| Names | `metal/MetalCatalog.java`, `datagen/MetalLanguageEntries.java`, `ModLanguageProvider` | Standard metal/form names generated from catalog; equipment/special names remain explicit |
| Material progression | `datagen/ModMetalRecipeProvider.java`, `build` | Standard crafting conversions |
| Foundry recipes | `datagen/ModFoundryRecipeProvider.java`, `build` | Melting and optional alloy acquisition |
| Recipe orchestration | `datagen/ModRecipeProvider.java`, `buildRecipes` | Already calls grouped providers; special recipes here |
| Drops | `datagen/ModBlockLootTableProvider.java`, `generate`, `getKnownBlocks` | New blocks and their loot rules |
| Mining tags | `datagen/ModBlockTagProvider.java`, `addTags` | Tool categories |
| Custom mining requirements | `config/BlockRequirements.java`, `getRequirement` | Catalog ore/storage requirements; absent → native behavior |
| Equipment | `metal/MetalEquipmentProfiles.java`, existing tier/material/item adapters | Optional catalog profile |
| Equipment recipes | `datagen/ModEquipmentRecipeProvider.java`, `build` | Optional |
| Natural ore generation | `MetalCatalog.java` / `.oreGeneration(...)`; adapters in `worldgen/ModOreGeneration.java` | Optional, explicitly enabled |

`datagen/DataGenerators.java::gatherData` already installs item models, blockstates, English language, recipes, block tags, loot and worldgen providers. `build.gradle` includes `src/generated/resources`. Preserve authored resources under `src/main/resources`; edit generator ownership before editing generated JSON.

### Baseline manual edit locations before Phase 1

A representative **new mod-owned ingot + nugget + storage block, standard conversions, Foundry melting and alloy acquisition, without ores/equipment** touches **12 existing Java files**: ModItems, ModBlocks, KatzencraftMetalsMod, ModMoltenMetals, ModFoundryRecipeProvider, ModMetalRecipeProvider, ModItemModelProvider, ModBlockStateProvider, ModLanguageProvider, ModBlockLootTableProvider, ModBlockTagProvider and BlockRequirements. Four authored PNGs (ingot, nugget, block, molten) bring this example to **16 source/asset locations**, before optional animation metadata, flowing/custom Table textures and generated outputs.

This historical count identifies the pre-Phase-1 repetition. Catalog metadata now replaces numeric/name duplication, and standard names no longer require explicit provider entries. Phase 2 now also removes repetitive standard ingot/nugget/storage-block registration, models, crafting conversions, loot, tool tags and storage mining entries. Specialized forms, exceptional acquisition recipes and progression remain manual; Phase 3 now also automates standard Foundry melting and catalog alloys. This is a scoped checklist, not a universal minimum. Reusing vanilla forms reduces work. Naturally generated ore needs registered ore blocks/assets plus explicit catalog generation entries. Equipment adds tier/material/custom mining handling. Generated JSON counts depend on enabled forms/recipes and must also be included in Git, but are not separate manual definitions.

## Adding a metal today

### 1. Choose identity, forms and balance

Declare a stable lowercase registry path, e.g. `electrum`, and an explicit English name in `metal/MetalCatalog.java` using `metal(id, displayName)` (static import of `MetalDefinition.metal`). Decide acquisition, enabled forms, density, mining tier, outputs and optional equipment. Texture colors cannot determine alloy proportions or tool attributes.

Use `katzencraftmetals:electrum` for a mod-owned metal. The standard helper declares `katzencraftmetals:electrum_ingot`; casting resolves the actual declared INGOT form ID. Do not rename registry IDs to change display names. Existing vanilla metals reuse their vanilla items/blocks rather than creating duplicates.

### 2. Enable standard forms; register specialized content manually

Phase 2 uses `metal/StandardMetalForms` as a cached view of the existing catalog, not another catalog. It selects only explicitly enabled mod-owned `INGOT`, `NUGGET` and `STORAGE_BLOCK` form IDs. Existing vanilla forms are references and are neither re-registered nor added to vanilla recipes/models/tabs by this system. Raw, ore, equipment and specialized building forms remain manual.

`ModItems` and `ModBlocks` keep their existing deferred registration responsibilities. Private helpers reuse one deferred object per ID; existing public fields such as `STEEL_INGOT`, `PLATINUM_NUGGET` and `MYTHRIL_BLOCK` are compatibility aliases calling those helpers at their original declaration positions. Their register methods then enqueue any additional catalog forms before attaching the deferred registers to the mod bus. This preserves existing references and registration order while new standard forms require no alias field.

`metal(id, displayName)` enables the three standard forms and supplies storage defaults. Use named `.storageStrength(...)`, `.storageResistance(...)`, `.storageSound(...)` and `.storageMiningLevel(...)` overrides. The lower-level `MetalDefinition.builder(...)` remains available for nonstandard form sets; a mod-owned `STORAGE_BLOCK` there still requires `.storageBlock(...)` explicitly. Current values are 5.0 hardness / 6.0 resistance, steel COPPER sound and Iron requirement 4, platinum METAL sound and Steel requirement 6, mythril METAL sound and Netherite requirement 7 (with approved saved-Diamond compatibility). Storage registration copies the iron block's baseline properties, applies the declared settings and retains correct-tool drops and normal BlockItem behavior. Missing owned-storage settings or settings without owned storage are rejected. Specialized/non-cube blocks are not supported by this standard form; keep them manual rather than forcing them into the cube path.

`MetalCreativeEntries.add` supplies standard items to INGREDIENTS and storage blocks to BUILDING_BLOCKS. Its small compatibility anchor table preserves the old steel/platinum/mythril positions among raw/building items; new forms append in deterministic catalog order. All other Creative entries remain manual. Faucet/hatch attachment exclusions are unchanged.

Continue registering raw/ore/equipment/building content in ModItems/ModBlocks and the existing Creative handler. A catalog entry does not automatically create those forms. Vanilla copper still has no nugget, and steel still has no raw/ore forms.

### 3. Supply assets and model-generator entries

The following texture paths are relative to `src/main/resources/assets/katzencraftmetals/`:

| Texture | Requirement | Model/resource handling |
|---|---|---|
| `textures/item/<metal>_ingot.png` | New ingot | Automatically generated `ModItemModelProvider.basicItem` |
| `textures/item/<metal>_nugget.png` | Enabled nugget | Automatic standard model entry |
| `textures/item/raw_<metal>.png` | Enabled raw forms | Explicit model entries |
| `textures/block/<metal>_block.png` | Storage block | Automatically generated `ModBlockStateProvider.blockWithItem` for an enabled standard cube |
| `<metal>_ore.png`, `deepslate_<metal>_ore.png`, `nether_<metal>_ore.png`, `end_<metal>_ore.png` in `textures/block/` | Registered ore variants only | Explicit model/blockstate entries |
| `textures/block/molten_<metal>.png` | Foundry metal | Direct texture selected by runtime definition |
| `textures/block/molten_<metal>_flowing.png` | Optional distinct faucet image | Only used by an explicit `.flowing(...)` exception; ordinary metals reuse the normal molten image |
| `textures/block/foundry_table_ingot_<metal>.png` | Optional custom cooled Table ingot | Discovered automatically on reload; base fallback otherwise |
| Cooled Basin image | Definition must reference a valid texture | Defaults to declared storage-block image; override with `MetalFoundrySupport.cooled(...)` |

The item-model provider writes `src/generated/resources/assets/katzencraftmetals/models/item/<id>.json`. The blockstate provider writes blockstates, block models and block item models. Handheld equipment uses the existing `handheldItem` helper. Do not overwrite manual Blockbench models or assume a texture file creates these generator entries automatically.

#### Molten animation constraints

`client/renderer/MoltenIronAnimation.getFrame` manually selects **20 vertically stacked frames**, 2 ticks per frame, ping-pong sequence 0â€“19â€“1. Match existing molten image dimensions/frame layout. Still images have `.png.mcmeta` metadata for atlas consumers, but metadata alone does not change the manual frame selector. Tank and faucet reuse the same normal PNG through this manual path; retain its mcmeta for atlas consumers. A static 16Ã—16 PNG is not a drop-in replacement for an animated strip.

#### Cooled Table texture constraints

The universal PNG is 16Ã—16 with painted region **X [0,16), Y [4,12)**: 16Ã—8 in actual file orientation. Although often described as an 8Ã—16 painted region, its long axis in the file is horizontal. The working UV mapping rotates it into the 4Ã—8 model-unit mold. New custom cold textures must use the same layout. Side faces sample the painted edge row at V=4.5/16.

`FoundryTableBlockEntityRenderer` deliberately keeps separate mappings: native block-local cropped molten UVs and fitted cold UVs. `FoundryCastingRenderType`/`foundry_casting.vsh/.fsh` blend the molten and selected cold image in one mesh. There is no automatic cooled-ingot tint, brightness/saturation correction or generated per-metal image anymore.

### 4. Declare the molten definition

Enable `.foundry()` on the catalog entry. `ModMoltenMetals.createDefinitions` iterates enabled entries in catalog order; compatibility constants reference those same objects. Density, name key and units come from the catalog. Keep units at 6 for Foundry-enabled entries. No separate registration call is needed.

`.foundry()` / `MetalFoundrySupport.standard()` resolves mod-namespace `textures/block/molten_<path>.png` for both stationary tank surfaces/sides and faucet streams. Supply its matching `.png.mcmeta`; no separate flowing PNG is discovered or required. Explicit `.flowing(...)` overrides remain authoritative for unusual future metals. `.still(...)`, `.flowing(...)` and `.cooled(...)` accept full texture ResourceLocations; `MetalFoundrySupport.texture("name")` builds a mod block PNG path. The cooled Basin image follows the declared STORAGE_BLOCK ID's namespace/path automatically: iron/copper/gold use vanilla iron_block/copper_block/gold_block textures; steel/platinum/mythril use their mod storage-block textures. Ordinary metals need no `.cooled(...)` override. The override API remains available for specialized future cases; it does not affect Table ingot textures.

Basin output resolves the catalog STORAGE_BLOCK form lazily after registration; Table output resolves the catalog INGOT form, including nonconventional IDs and vanilla forms. Both destinations reject a missing/unregistered output. Capacities remain 54 and 6 respectively; no dummy forms are created. Optional hand-painted Table images and their universal fallback remain handled by `FoundryIngotTextures`.

`ModMoltenMetals.getSyncId`/`bySyncId` use definition iteration order for compact network IDs; client/server catalogs must agree. Do not casually rename existing resource IDs or introduce per-metal branches throughout storage/renderers.

### 5. Recipes and quantities

`ModRecipeProvider.buildRecipes` calls the grouped metal/Foundry/equipment providers. `ModFoundryRecipeProvider.buildCatalogStandardMelting` automatically emits recipes for declared INGOT, NUGGET and STORAGE_BLOCK forms of Foundry-enabled entries, including reused vanilla forms. IDs remain `foundry_melting/<actual form path>`. Additional raw/raw-block/ore inputs stay explicit in the existing per-metal helpers; merely declaring ores does not enable melting them.

Default helper durations now come from `.meltingTime(...)` and scale with yield; explicit per-recipe overloads remain available.

`datagen/recipe/FoundryMeltingRecipeSet` writes `data/katzencraftmetals/recipe/foundry_melting/<recipeName>.json`. At current unitsPerOre=6:

| Input helper | Molten units | Default ticks |
|---|---:|---:|
| `raw`, `ingot`, `ore` | 6 | 100 |
| `nugget` | 1 | 17 at the default 100-tick base (proportional ceiling) |
| `rawBlock`, `block` | 54 | 900 |
| `item` | Explicit | Explicit |

The active set imports `datagen/recipe/FoundryMeltingRecipeBuilder`; a similarly named builder also exists directly under `datagen/`. Follow the active imports rather than editing an unused helper.

Actual generated example, `src/generated/resources/data/katzencraftmetals/recipe/foundry_melting/steel_ingot.json`:

```json
{
  "type": "katzencraftmetals:foundry_melting",
  "ingredient": { "item": "katzencraftmetals:steel_ingot" },
  "molten_amount": 6,
  "molten_metal": "katzencraftmetals:steel"
}
```

`recipe/FoundryMeltingRecipe.Serializer` supports an Ingredient, molten ID, positive amount and optional `processing_time` (default 100). Ingredients can support tags, but today's grouped helper emits explicit items. `recipe/ModRecipes` already registers the shared type/serializer: a metal needs recipes, not a new recipe type.

`ModMetalRecipeProvider.blockAndIngot` uses **9 ingots ↔ 1 block**. `ingotAndNugget` uses **6 nuggets ↔ 1 ingot**, with two rows of three, not vanilla's nine. Thus the present system uses 1 unit/nugget, 6/ingot and 54/block. Preserve these ratios until a separate balance decision.

Standard `blockAndIngot` and `ingotAndNugget` calls are now catalog-driven, only when the necessary mod-owned forms exist. Their existing recipe IDs, categories, unlock criteria, advancements and 6/9 balance are preserved. Vanilla recipes are not generated.

Supported metal acquisition uses existing raw/ore Foundry melting and catalog-defined molten alloying, alongside standard ingot/nugget/block conversions. No universal raw-to-ingot furnace recipe is generated. Any unrelated special processing must remain explicitly defined; adding a metal does not implicitly add progression routes.

### 6. Optional alloys

Declare `.alloy(input("iron", 1), ...)` on the output catalog entry. Quantities are positive integer whole ingots (1 → 6 units, 2 → 12). Short `iron`, `copper` and `gold` inputs resolve to Minecraft; other short IDs such as `steel` resolve to `katzencraftmetals`. Explicit `namespace:path` IDs remain exact. Matching never uses display names; unknown or Foundry-disabled inputs are rejected by completed-catalog validation, which allows forward references. `.alloy(140, inputs...)` overrides the default 100 ticks. The API has no output amount: the provider always writes exactly 6 output units to `foundry_alloy/<output path>.json` through the existing serializer/builder. Validation rejects duplicate/unknown/Foundry-disabled ingredients, zero/negative quantities, more than three inputs, empty recipes and integer multiplication overflow; fractional Java arguments do not compile. Deliberate material loss is allowed; there are no returns or byproducts.

`recipe/FoundryAlloyRecipe` permits **1â€“3 molten ingredients**, an output metal/amount and processing time. Inputs are stored molten units, not item Ingredients. Coal cannot simply be supplied as another molten ingredient. Alloy recipes are explicitly started in the Controller; `matches()` does not auto-apply them.

| Current output | Inputs | Output units | Ticks |
|---|---|---:|---:|
| Steel | Iron 6 | 6 | 100 |
| Platinum | Iron 6 + gold 6 | 6 | 100 |
| Mythril | Steel 6 + platinum 6 | 6 | 140 |

Steel's intended primary production is the catalog Foundry alloy: 6 molten iron → 6 molten steel, in 100 ticks. Normal steel crafting, recycling, equipment and building blocks remain available.

### 7. Loot, tags, mining and natural ore

Standard storage-block self-drops, known-block coverage and pickaxe tags are now generated by iterating StandardMetalForms. Add ore/special block drops and tags manually in the existing providers. This mod additionally enforces custom rules through `config/BlockRequirements`, `ToolLevels`, `MiningLevels` and `event/ToolRequirementEvents`. Mineable tags alone do not integrate custom progression: unknown blocks default to WOOD requirements, unknown tools return level 0.

There is no `ModItemTagProvider` installed in `DataGenerators`. Common material item tags such as `c:ingots/electrum` are an optional interoperability addition, not already automatic. A tag provider and tag-aware recipe inputs would need deliberate implementation; current explicit-item recipes do not automatically recognize arbitrary equivalent mod ingots.

For ore generation, configure `.oreGeneration(...)` in the catalog as described below; do not add per-metal bootstrap code. Each enabled ore block still needs registration/model/loot/mining coverage first. Test newly generated chunks; changing settings does not populate existing terrain.

### 8. Phase 1 catalog and automatic names (implemented)

`metal/MetalDefinition.java` is an immutable record with a concise builder: stable ResourceLocation, required English name, optional locale names, density, units, explicit enabled form IDs/types, and per-locale/per-form name overrides. Collections are copied and exposed read-only; duplicate IDs/forms/name keys, blank names, invalid locales, nonpositive values and absent-form overrides are rejected. No item/block objects, client classes or resource scanning are needed to load the catalog before deferred registration.

`MetalCatalog.values()` preserves **iron, copper, steel, mythril, gold, platinum** order and original namespaces/numeric properties. Core form IDs are explicit, including no copper nugget and no raw steel. Equipment/building variants remain in existing owners rather than speculative catalog structures. Alloys now live in the catalog; exceptional acquisition and equipment remain in their existing manual owners; ore generation is now catalog-driven.

Example addition (enabled standard forms now register automatically):

```java
metal("electrum", "Sun Forged Electrum")
        .density(12500)
        .localizedName("de_de", "Sonnenelektrum")
        .formName("de_de", "katzencraftmetals:electrum_ingot", "Sonnenelektrumbarren")
        .build();
```

The numbers/name are illustrative. Add the definition to the ordered catalog and supply authored textures. Enabled standard forms/resources register automatically; add `.foundry()` for derived molten definitions and standard melting. Ore/progression exceptions remain manual.

`datagen/MetalLanguageEntries.entries` now generates metal keys plus standard ingot/nugget/storage/raw/ore names for enabled **mod-owned** form IDs. It does not override vanilla item names, invent absent forms, or generate equipment/building-set names. `ModLanguageProvider` retains all unrelated/custom/equipment entries. Surviving English translations are preserved; STEEL_HOE's existing "Steel Sword" remains untouched as unrelated equipment wording.

`MetalLanguageEntries.locales()` includes English and each locale explicitly declared by a name or form override. `DataGenerators.gatherData` creates a narrowly scoped language provider for each. All current definitions provide English only, so no additional locale files were invented. Optional locale names fall back to English. English and German form templates are supported; additional grammar can be added in `MetalDefinition.Form`, with per-form overrides for irregular wording. An unsupported grammar falls back to a complete English form name rather than joining a localized name with English word order. Per-form locale overrides take priority. Generate ordinary lang JSON; resource packs retain normal override behavior.

`metal/MetalNames.name` returns translation-aware Components using `Component.translatableWithFallback`, with the explicit English catalog name as fallback. Unknown IDs are shown as diagnostic full IDs, never converted into guessed names. The Controller metal-list, alloy names/ingredients, selected output/Faucet Lock and Basin interaction now use this shared path. Existing narrow list fitting calls `.getString()` only when measuring/truncating localized text; direct tooltips retain Components and append quantity text. IDs, saved data and packets are unchanged. The tank schematic itself has no separately introduced name system; its adjacent metal list shares this naming path. Generic English UI sentences are outside this phase.

### 9. Foundry paths already generic

After valid definitions, recipes and outputs exist, these paths require no ordinary per-metal switch:

- `block/entity/FoundryMeltingRecipes` and melting-job helpers resolve output IDs through `ModMoltenMetals`.
- `FoundryControllerTankStorage`, `FoundryTankNetwork`, `FoundryControllerMetalDiscovery` handle quantities, tanks and discovery by ID. `FoundryControllerMenuData` derives sync data from definitions. No per-metal tank BlockEntity is needed.
- `menu/FoundryControllerMenu` and screen lists use definitions/discovery/availability. `FoundryControllerMetalsRenderer.getDisplayIcon` prefers the matching ingot, falling back to the Basin cast result.
- `client/renderer/FoundryControllerTankLiquidRenderer`, `FoundryTankSurfaceRenderer` and `FoundryTankSurfaceRenderType` consume the registered textures/density through existing surface/shader/depth paths. Do not change geometry/gap fixes to add a metal.
- `FoundryMetalEffectPalette.prepare/apply` analyzes molten still textures on reload, calibrated with iron/steel references; `baseTint` prioritizes curated overrides. All six current metals are curated. New metals use automatic analysis without a Java case; manual override is optional art direction. This affects molten rim/hotspots, not cooled ingots.
- Existing side-glow policy uses 100% strength, a longer iron/steel fade and shorter normal-metal fade. A dark texture does not automatically classify another metal as steel.
- `FoundryFaucetBlockEntity`, `FoundryOutputSelection` and Faucet Lock select registered IDs; an active pour retains its selected metal. Side/down attachments need no new classes/models. Faucet batches are **6 units**, with the existing **4-block** maximum casting-target search.
- `FoundryBasinBlockEntity` casts its configured result at **54 units**, cooling over **100 ticks by default**, configurable per metal. `FoundryTableBlockEntity` casts a matching ingot at **6 units** and rejects missing ingot items. These capacities are fixed, not scaled by unitsPerOre.
- `FoundryIngotTextures.prepare/resolve/apply` checks `katzencraftmetals:textures/block/foundry_table_ingot_<metal path>.png` once per resource reload. It uses registry path, not localized display name; missing optional files quietly use `foundry_table_ingot_base.png`. Packs can add/remove/override images. Different namespaces sharing one path would currently share this asset naming convention.
- Output inventory, manual pickup and hopper extraction are generic. Check unfinished/finished states and output identities; no per-metal hopper registration is needed.

The molten definitions are not registrations of Minecraft world Fluid blocks or obtainable fluid items. Do not invent Fluid registries as a requirement for the current visual/storage system.

### 10. Optional equipment and building sets

Steel and mythril have tools/armor; platinum does not. For equipment extend `ModToolTiers`/`FixedToolTier`, `ModArmorMaterials`, actual `ModItems` registrations/attributes/durability, equipment recipes, item models, names, Creative entries and textures. Standard armor layers are under `assets/katzencraftmetals/textures/models/armor/<material>_layer_1.png` and `_layer_2.png`; steel horse armor has separate item/entity texture handling.

Also recognize a new pickaxe in `config/ToolLevels.getLevel`; inspect `CatoToolTierConfig`, `MiningLevels` and `ToolRequirementEvents` before adding a custom level. A Tier's repair ingredient alone does not integrate this enforcement.

Steel-style doors, trapdoors, stairs/slabs, chains, bars, buttons, levers and ladders need their own registration/property/model/item/recipe/loot/name/tag/mining coverage. These are optional, not required for every metal.

## Complete representative traces

### Iron: ordinary, mostly vanilla

`ModMoltenMetals.IRON` uses `minecraft:iron`, density 7874, 6 units, molten_iron, vanilla iron_block texture and vanilla iron-block Basin output. Vanilla provides ingot/nugget/raw/ore/block. Catalog forms generate ingot/nugget/block melting; `buildIronMeltingRecipes` retains raw/raw-block/normal/deepslate ore exceptions. The still texture is its flowing fallback even though a molten_iron_flowing PNG exists: selection follows the definition, not filenames. Curated molten glow is FFD080. Table output is vanilla iron ingot; custom Table texture naming uses `iron`.

### Steel: alloy, intermediate and equipment

ModItems registers ingot/nugget; ModBlocks storage/cut/building forms; Creative/model/language/loot providers explicitly enumerate them. Definition: mod namespace, density 7850, still+flowing images, steel_block cooled/result. Melting accepts ingot/nugget/block. Foundry alloy is 6 iron → 6 steel. Conversion uses six nuggets. Tools use the 512-use steel tier/ingot repair; armor has its own material and horse armor. ToolLevels recognizes the steel pickaxe. Curated molten glow is F28E58; there is no automatic cooled-item tinting.

### Platinum: ore and alloy

Items: nugget/raw/ingot. Blocks: storage plus four ore variants. Definition: density 21450, still+flowing, platinum block result. Alloy: 6 iron+6 gold → 6 platinum. Existing ore/raw Foundry melting remains available. Ore drops include Fortune/Silk Touch handling and separate 5% amethyst chance; custom requirements use STEEL for ore/storage. Catalog entries install each dimension variant. No platinum equipment set exists to copy by default.

### Mythril: optional high-tier behavior

Mythril shares platinum's raw/ore forms and worldgen architecture but alloys 6 steel+6 platinum → 6 at 140 ticks, density 9500, plus tools/armor. Ore loot has four binomial raw-bonus rolls at 18% and separate 5% sapphire chance. Block mining requirements use DIAMOND; its pickaxe has a distinct MYTHRIL level. These are explicit gameplay choices, not automatic name/texture consequences.

## Generation, validation and common failures

After an implementation change, from the project root:

```powershell
.\gradlew.bat runData
.\gradlew.bat build
```

Do not run `clean` unnecessarily. `runData` writes many providers' outputs: inspect its diff, preserve manual resources and particularly `src/generated/resources/assets/katzencraftmetals/models/block/foundry_controller_on.json`. Do not blindly overwrite authored models. Build JAR: `build/libs/katzencraftmetals-<mod_version>.jar`. No dependency/version changes are required to add a metal.

Expected generated roots:

- `src/generated/resources/assets/katzencraftmetals/models/{item,block}/`, `blockstates/`, `lang/`.
- `src/generated/resources/data/katzencraftmetals/recipe/`, `advancement/recipes/`, `loot_table/blocks/`.
- Optional `data/katzencraftmetals/worldgen/{configured_feature,placed_feature}/` and `neoforge/biome_modifier/`.
- `src/generated/resources/data/minecraft/tags/block/` for vanilla tool categories.

Generated JSON, Java and PNG/mcmeta assets must all be in the intended commit. A local build can use files absent from Git. Run `git status --short`, compare required files with `git ls-files`/the index, and check `git check-ignore -v <required paths>`. Stage only when explicitly authorized, never indiscriminate `git add .`; never automatically commit/push. Build logs and temporary check harnesses are not implementation resources.

### Minecraft checklist

- [ ] All IDs register, obtainable forms have names and Creative entries, no duplicate vanilla forms.
- [ ] Item/block/armor textures and models load, transparency and animation work.
- [ ] New-chunk ore distribution, mining tiers, Fortune/Silk Touch and bonus drops are correct.
- [ ] Enabled crafting and melting routes retain intended unit quantities; alloys may deliberately consume more than their fixed output.
- [ ] Controller discovery, alloy quantities, availability and cancellation work.
- [ ] Tank density order, quantities, selected output, save/load and multiplayer sync are correct.
- [ ] Controller metals/tank tooltips/alloys, Faucet Lock and Basin show intended names; verify the shared translation-aware naming path.
- [ ] Side/down faucet pouring, rectangular rotation and both finishing animations work.
- [ ] Basin casts the intended block at 54 units; Table the matching ingot at 6; unsupported ingot IDs reject Table insertion.
- [ ] Custom Table images work, absent images fall back; reload adds/removes overrides without stale results.
- [ ] Molten animation/native Table crop and fitted cold UVs remain correct.
- [ ] Integrated rim/side glow/hotspots remain coherent; no tank/frame depth/gap regression.
- [ ] Successful manual pickup yields one output; empty/unfinished pickup fails; hopper extraction works with intended insertion restrictions.
- [ ] Optional equipment attributes, repairs and custom mining levels work.
- [ ] Survival/Creative, dedicated server and matching multiplayer clients work.

A build with `test NO-SOURCE` is not a gameplay or visual test.

Common mistakes: recipe without molten definition; definition without matching ingot; wrong namespace; missing generated model/name/recipe in Git; assuming an unused PNG selects itself; unitsPerOre incompatible with fixed casting capacities; missing custom tool-level lookup; missing loot getKnownBlocks entry; assuming explicit-item recipes accept common tags; attempting runtime item registration from resource-pack files; storing English display names instead of IDs.

## Proposed Future Workflow — Adding a Metal with Minimal Manual Work

**Phases 1–3 are implemented as described above. The fluent Java declaration below is the current recommended workflow; optional acquisition/progression automation remains future work.** Prefer a small internal declarative catalog feeding the current registration/datagen architecture. No public modding API or new rendering system is needed.

### One definition, explicit names and optional features

Phase 1 now provides an immutable Java catalog available before deferred registration and shared with datagen. Extend it incrementally; do not replace it with a competing catalog. A development-time manifest/generator is a possible later alternative, not a second competing source of truth. Illustrative data (not an existing loadable schema):

```json
{
  "id": "katzencraftmetals:electrum",
  "displayName": "Electrum",
  "localizedNames": { "de_de": "Elektrum" },
  "forms": ["ingot", "nugget", "storage_block"],
  "density": 12500,
  "unitsPerIngot": 6,
  "acquisition": {
    "alloy": {
      "inputs": { "minecraft:gold": 3, "minecraft:iron": 3 },
      "outputUnits": 6,
      "ticks": 100
    }
  }
}
```

Example density/alloy values are illustrative, not a proposed balance change. Require ID, explicit default display name and essential properties/acquisition; use conventional texture names and processing defaults. Reuse vanilla form references instead of duplicating them. Optional ore/tools/armor/building sets, byproducts and exceptional recipes should be opt-in sections. An alloy-only metal should not require raw ore or equipment.

### Metal names and generated localization

Keep `id: electrum` and `displayName: Electrum` separate. IDs remain stable in registries, recipes, networking, saves and texture naming. Display names may contain spaces, punctuation or capitalization unrelated to the ID.

Generate standard language entries for **enabled forms only**:

| Key | Generated English value |
|---|---|
| `metal.katzencraftmetals.electrum` | Electrum |
| `item.katzencraftmetals.electrum_ingot` | Electrum Ingot |
| `item.katzencraftmetals.electrum_nugget` | Electrum Nugget |
| `block.katzencraftmetals.electrum_block` | Electrum Block |
| Optional molten-context label | Molten Electrum |
| Optional raw/ore/tool forms | Raw Electrum / Electrum Ore / Electrum Pickaxe |

There are no per-metal registered world Fluids/molten inventory items today; do not create fictitious forms just to name them. Keep the bare metal key for existing Controller/tank lists and use a translated `Molten %s` context template if needed.

Use per-language form templates plus one optional per-metal language name, rather than hand-maintaining every standard-form translation. German `Elektrum` can produce `Elektrumbarren`, `Elektrumnugget` and `Elektrumblock` using suitable templates. Grammar/compounds/gender/word order are not universally English concatenation: allow per-locale/per-form overrides for exceptions. Most metals then need one name per supported language, with human overrides only where necessary.

Define precedence: explicit form/locale override > localized name with locale template > generated English fallback. Missing localized names fall back to declared English rather than invented machine translations. Generate normal Minecraft lang JSON; resource packs can override it. Reject duplicate keys and avoid overwriting existing custom/vanilla translations. The English display name must be explicit, not guessed from filename capitalization.

Phase 1 has replaced ID capitalization with `MetalNames.name`, using a definition-aware translatable Component and explicit English fallback. Continue using that provider in future consumers. Update Controller metal list, tank contents/tooltips, alloy labels/ingredients, selected output and Faucet Lock together; keep Basin on that same source. Preserve Components until measurement/truncation needs localized strings. `Component.literal(id-derived text)` is not localization. Language changes must not alter persisted IDs or require inventory regeneration. Test a deliberately nonmatching ID/display label as well as Electrum.

### What should become automatic

1. Register enabled item/block forms through existing responsibilities, preserving deterministic IDs and initialization ordering.
2. Populate Creative entries, excluding attachment-only blocks.
3. Generate standard item models, cube blockstates/models/item models, loot and tags.
4. Generate language JSON from explicit names, templates and optional exceptions.
5. Build Foundry definitions, standard melting inputs and output references from the same catalog.
6. Generate standard conversions/processing according to explicit unit policy; preserve the present six-nugget balance unless separately changed.
7. Resolve conventional molten still/flowing/cooled paths with explicit overrides and validation.
8. Retain automatic molten effect palettes with curated overrides; retain manual Table textures and their existing fallback.
9. Add optional ore/equipment/building templates without forcing their fields on ordinary metals; retain special recipes/byproducts as overrides.

A filename cannot determine density, alloy proportions, mining tiers, enabled forms or translations. Resource packs load/reload too late to safely drive item registration. If desired, use development-time texture discovery only to scaffold or validate catalog entries, not to mutate registries at runtime. Texture-only addition is realistic only for an intentionally constrained template with defaults and an independently declared name.

### Intended developer workflow and phases

Future workflow: declare ID/name/essential properties once; supply conventional textures; run generation/validation; build and play; review Git completeness. No Java switch should be required for a new manual Table texture or automatic molten effect palette.

Recommended phases:

1. **Names and validation (Phase 1 implemented):** central schema, explicit display names, generated language forms, GUI translation consumers, language-switch/exception tests; snapshot existing IDs/recipes/palettes.
2. **Standard forms/resources (Phase 2 implemented):** migrate one ordinary metal, then existing content, preserving exact registry IDs, Creative order and authored art. Extend current providers rather than adding competing output systems.
3. **Foundry/recipes (Phase 3 implemented):** enabled definitions and standard melting derive from the catalog; whole-ingot alloy declarations generate fixed six-unit outputs. Exceptional raw/ore melting routes remain explicit.
4. **Optional progression:** ore, equipment and building-set templates with custom mining integration and byproduct overrides.
5. **Optional scaffolding:** only add texture discovery/code generation if the catalog still leaves meaningful repetition; require deterministic output and fresh-checkout builds.

Defaults reduce boilerplate but can conceal balance errors. Validate missing density/output/acquisition rather than silently producing copper-colored liquid or empty cast results. Localization generation removes repetitive entries, not human language decisions. Do not overwrite manual models/textures during migration.

## Recommended fluent declaration

Normally edit only the declarations in `MetalCatalog.ORDERED`. **Append new entries at the end; never reorder existing entries**, because Foundry synchronization IDs depend on catalog order. Import:

```java
import static net.chriskatze.katzencraftmetals.metal.MetalDefinition.metal;
import static net.chriskatze.katzencraftmetals.metal.MetalDefinition.StorageSound.*;
import static net.chriskatze.katzencraftmetals.metal.MetalAlloy.input;
```

Add this hypothetical entry (Electrum is example content only):

```java
metal("electrum", "Electrum")
        .density(12500)
        .storageMiningLevel(4)
        .storageSound(METAL)
        .foundry()
        .alloy(input("iron", 1), input("gold", 1))
        .build()
```

`metal` is an entry point on the existing `MetalDefinition.Builder`, not a separate builder or registry. It uses the mod namespace and enables `<id>_ingot`, `<id>_nugget` and `<id>_block`. Existing specialized vanilla helpers reference Minecraft's objects instead of duplicating them. The ore helper only declares the existing extra form IDs; raw/ore objects are still manually registered.

| Setting | Default / requirement |
|---|---|
| ID / display name | Explicit; ID is an unqualified mod path, name is independent English text |
| `.density(int)` | Required positive value; no implicit density. Controls heavier-below-lighter molten layers |
| Molten units | Fixed at 6 per ingot for the standard entry point |
| `.storageMiningLevel(int)` | 4 (Iron) for new standard storage blocks; independent of tools/ores |
| `.oreMiningLevel(int)` | Optional 1..8 requirement for declared mod ore variants |
| `.equipment(profile)` | Optional centralized tool/armor profile; capability is declared there once |
| `.meltingTime(int)` | 100 ticks per 6 molten units, scaled with yield |
| `.ingotCoolingTime(int)` | 100 ticks, Foundry Table only |
| `.blockCoolingTime(int)` | 100 ticks, Foundry Basin only |
| `.meltingTimeOverride("namespace:item", ticks)` | Optional absolute input duration for genuinely exceptional recipes; normal nuggets scale proportionally |
| `.storageSound(...)` | METAL; steel explicitly retains COPPER |
| `.storageStrength(float)` | 5.0 hardness |
| `.storageResistance(float)` | 6.0 blast resistance |
| `.foundry()` | Disabled unless called; enables conventional molten textures and standard melting |
| `.foundry(support)` | Explicit still/flowing/cooled texture exceptions through existing `MetalFoundrySupport` |
| `.alloy(inputs...)` | Absent unless declared; default 100 ticks and fixed 6 output units |
| `.alloy(ticks, inputs...)` | Explicit positive processing time, same fixed output |
| `.localizedName(locale, name)` | Optional localized metal name; English fallback |
| `.formName(locale, formId, name)` | Optional per-form override, taking precedence over generated names |
| `.form(type, "namespace:id")` | Additional declared form; nonstandard registration/processing remains manual |
| `.build()` | Completes/validates immutable metadata; cross-metal validation runs on the completed catalog |

Storage overrides update the same stored properties independently. They do not affect vanilla referenced blocks. Invalid quantities/properties, missing density and duplicate form/alloy declarations are rejected. For deliberately different form sets use the existing lower-level builder; no standard forms or specialized content are silently invented there.

Density/properties and this illustrative acquisition are explicit design choices. This consumes 6 iron + 6 gold and produces exactly 6 electrum units in 100 ticks. Add optional `.localizedName("de_de", "Elektrum")` or per-form locale overrides; standard names derive from the explicit display name, never ID capitalization.

Supply these authored files under `src/main/resources/assets/katzencraftmetals/`:

- Required `textures/item/electrum_ingot.png`, `textures/item/electrum_nugget.png`, `textures/block/electrum_block.png`.
- Required `textures/block/molten_electrum.png`: existing 20-frame vertical animated layout, 2 ticks/frame ping-pong; preserve matching `.png.mcmeta` for atlas consumers. Follow the existing frame dimensions, not a static square PNG.
- No separate flowing image is required: the faucet reuses `molten_electrum.png`. A distinct image is an optional explicit `.foundry(MetalFoundrySupport.standard().flowing(...))` exception, with the same manually sampled frame layout.
- Optional `textures/block/foundry_table_ingot_electrum.png`, same painted layout/UV convention as the universal 16×16 base. Absent → `foundry_table_ingot_base.png`; no automatic cooled tinting.

Run `.\gradlew.bat runData` then `.\gradlew.bat build`. Review generated resources and Git completeness. Standard registrations, Creative entries, models, crafting conversions, translations, loot/mining tags, molten registration, standard melting and alloy JSON are automatic. Generic tank storage/layering, faucets, Table (6→1 ingot), Basin (54→1 block), casting textures, molten animation and effect palettes consume the resulting definitions without another metal-specific switch. Curated effect-color overrides remain optional and current approved colors are preserved.

Raw item registration, ore/world generation, equipment/tool tiers, custom progression, special processing, nonstandard building blocks and nonstandard recipes remain manual in their current owners. Additional raw/ore melting remains explicit; no automatic progression bypass is introduced. Do not regenerate or overwrite authored models/textures.

Migration: platinum first, then remaining metals. All six molten IDs, sync order, densities, textures and casting outputs remain unchanged. Standard and exceptional melting JSON are unchanged. Only platinum/mythril alloy inputs changed from 3+3 to 6+6; outputs/times/IDs remain unchanged. Steel remains 6→6.

## Validation and Git completeness

The initial source audit was documentation-only. Phases 1 and 2 subsequently added catalog/naming and standard-form registration/generation. Build, datagen, metadata/localization checks and registered-form/Foundry regression checks are required when changing it. Validate generated English against the previous entries, verify catalog/molten ordering and numeric equality, confirm actual item/block IDs exist, and inspect Minecraft for text layout, language switching and multiplayer behavior. No claim of Minecraft visual verification follows from headless tests.

Do not omit the existing untracked Foundry Table implementation files or the new catalog sources from the intended commit. Run `git status --short`, inspect every new/modified file and check ignore rules before declaring GitHub readiness. Preserve staged/unrelated work; do not stage, commit or push without explicit permission.

## Removed processing content and existing worlds

The Crusher, its crushed-material chain and Steel Charge have been permanently removed.
New metals need no Crusher integration. There is no CRUSHED catalog form, powder-ore
registration, replacement intermediate, compatibility alias or automatic substitute recipe.
Independent vanilla gunpowder loot and the Foundry's blaze-powder fuel are preserved.

Removed registry entries (all IDs use the `katzencraftmetals` namespace):

| Registry | Removed paths |
|---|---|
| Block | `crusher` |
| Item | `crusher`, `crushed_coal`, `crushed_iron_ore`, `crushed_platinum_ore`, `crushed_mythril_ore`, `steel_charge` |
| Block entity type | `crusher` |
| Menu | `crusher_menu` |
| Recipe type and serializer | `crusher` in each registry |

Removed recipe IDs: `crusher/coal`, `crusher/raw_iron`, `crusher/raw_platinum`,
`crusher/raw_mythril`, `crushed_coal`, `coal_from_crushed_coal`, `steel_charge`,
`steel_nugget_from_blasting_steel_charge`, `platinum_nugget_from_blasting_crushed_platinum_ore`
and `mythril_nugget_from_blasting_crushed_mythril_ore`. Their applicable unlock advancements,
models, textures and machine loot are removed. The steel Foundry alloy is unchanged:
1 iron ingot equivalent (6 units) -> 1 steel ingot equivalent (6 units), 100 ticks.

Back up an existing world before upgrading and test a copy. Old saves can still contain
these IDs in chunks, player/container inventories, recipe unlocks and saved machine data.
Without registered replacements they cannot resolve to usable content. Depending on the
loading path, missing-registry warnings or loading/recipe diagnostics may appear; placed
machines and their saved inventories/progress can be discarded, and removed item stacks
can become empty or be lost. Do not rely on retaining recoverable data after saving again.
No migration, compensation or substitution is provided. Old-world loading and saving must
be verified in Minecraft; headless datagen/build checks do not establish migration safety.

## Processing times and explicit item recycling

The fluent builder accepts independent positive integer tick settings:

```java
metal("electrum", "Electrum")
        .density(12500)
        .meltingTime(120)
        .ingotCoolingTime(80)
        .blockCoolingTime(160)
        .foundry()
        .build()
```

Defaults for all three settings are 100. `.meltingTime` is the base time for 6 molten
units; default helper durations use `max(1, ceil(baseTime * moltenUnits / 6))`, with
wide intermediates and explicit overflow rejection. All existing ingot/raw/ore inputs
remain 100 ticks, blocks/raw blocks 900. All five existing iron/gold/steel/mythril/platinum
nugget inputs now use 17 ticks with the default 100-tick base, just like future metals.
The former 12-tick compatibility overrides have been removed; `.meltingTimeOverride(...)`
and explicit helper duration overloads remain available for exceptional recipes. Single
nuggets still melt to one unit; tank storage, capacities, casting and alloy quantities
are unchanged. Existing per-recipe helper overloads can also specify an absolute duration.
The Controller still applies its existing 175% processing speed (ceil(base ticks / 1.75));
these configured/JSON times are base durations, not elapsed wall-clock ticks.

Table/Basin cooling durations are independent and are captured on the first accepted
pour for a cast, then retained through subsequent pours and cooling. Cooling starts
only once the target is full, as before. Optional `CoolingDuration` NBT travels through
the existing save/update packets; rendering divides progress by that captured duration.
Existing saves without it resolve the metal's configured duration (100 for current metals).
No shader, mesh or liquid animation was changed.

### Add recycling in one place

`metal/MetalRecyclingRecipes.java` contains the explicit declaration list. Only the two
examples below are currently enabled; other tools/armor are not automatically recycled.
Append a declaration ending in `.build()` to that list, using the existing builder:

```java
recycle("minecraft:iron_pickaxe")
        .into("iron")
        .ore(3)
        .durabilityScaling(true)
        .minimumRecovery(1)
        .build()

recycle("minecraft:iron_chestplate")
        .into("iron")
        .ore(8)
        .durabilityScaling(true)
        .minimumRecovery(1)
        .build()
```

Static-import `MetalRecyclingRecipes.recycle` outside its class. Output metal shorthand
uses the same namespace rules as alloy inputs. `.ore(n)` requires positive whole ore
quantities: one ore is always six molten units. Scaling defaults to false and minimum
recovery defaults to zero; set a positive minimum explicitly when desired. Disabled
scaling always returns the maximum even if the item is damaged. `.meltingTime(ticks)`
on this recycling builder optionally supplies an absolute recipe duration. Otherwise time
uses the metal's base scaled by MAXIMUM ore recovery, independent of current damage:
pickaxe 300 ticks, chestplate 800 ticks with current iron settings. A metal-level input
exception is respected unless the recycling definition supplies its own time override.

With scaling enabled, read the actual stack's damage/max-damage components and clamp
remaining durability to [0,1]. Recovery is
`round(minOre + (maxOre - minOre) * remainingFraction)`, using positive-half-up
`Math.round`, clamped to [min,max]. Full durability returns max, zero remaining returns
min, and 50% of an 8-to-1 range returns 5 ore; a 3-to-1 range returns 2. Yield is always
whole ore and converted to units only once. A zero-yield stack (minimum zero at exhaustion)
cannot begin melting; it does not fall back to a competing ordinary recipe.

Definitions emit the existing `foundry_melting` recipe type at
`foundry_melting/recycling/<input namespace>/<input path>`. Its optional `recycling`
metadata contains `minimum_ore` and `durability_scaling`; old normal recipe JSON stays
unchanged. There is no new machine or competing processing path. Explicit recycling
wins over ordinary matching melting recipes. Duplicate recycling inputs are rejected
during generation. Missing input items, unknown/unsupported output metals, non-damageable
scaled inputs, invalid ranges/times and molten/time overflow fail validation. No fractional
ore configuration is accepted. Item resolution/durability validation happens during datagen,
after registry creation; run datagen rather than relying on compilation alone.

The Controller and tooltip share `FoundryMeltingRecipes` lookup and recipe `resolve(stack)`.
An active job persists a one-item component snapshot in optional `ActiveInput` NBT; changing
the item, damage, enchantments or other components resets progress, even if recovery stays
within the same rounding step. Count changes alone do not change the committed one-item
result. Completion revalidates the snapshot and actual yield against tank capacity before
inserting metal and consuming exactly one item. Insufficient capacity consumes nothing;
capacity comparisons use subtraction to avoid overflow. Reload resumes a matching snapshot.
Legacy active jobs without a snapshot safely restart their progress once without consuming
an input; existing inventory/storage NBT keys and units are retained. Matching mod versions
are required on client/server for the extended recipe synchronization codec.

### Recovery tooltips and testing

In a loaded world, synchronized valid recipes add a gray localized tooltip such as
`Foundry Recovery: 8 Iron Ore` or `Foundry Recovery: 5 Iron Ore`. Damage changes update
the value through the same resolution used by processing; names use `MetalNames`.
Unsupported items and yields below one ore (including single nuggets) show no recovery
line. Non-whole-ore custom normal yields also omit it rather than claiming an inaccurate
whole amount. There is no parallel tooltip quantity table or recipe cache to become stale
after recipe reload. The translation key is `tooltip.katzencraftmetals.foundry_recovery`;
English and German templates follow existing generated locale selection.

Run datagen and build after changing definitions. Check every durability step/endpoints,
scaling disabled, time overrides/overflow, overlapping normal/recycling inputs, replacement
of equal-yield inputs, interrupted/saved jobs, insufficient space, independently configured
Table/Basin cooling, hopper/manual pickup and client progress/tooltips. In-game multiplayer
and interrupted live Controller operation still need Minecraft testing; headless checks do
not establish those behaviors visually. Tank surface/side-glow improvements remain outside
this processing task.


## Centralized equipment and mining

See [EQUIPMENT_PROGRESSION.md](EQUIPMENT_PROGRESSION.md) for the live profile API, approved values,
vanilla reference/override limitations, 1..8 requirement tags, compatibility opt-in, Copper assets,
Diamond acquisition audit and pending Netherite recipe decision. MetalCatalog links profiles and
owns ore/storage requirements; density and Foundry processing stay independent. Equipment remains
explicitly registered through existing ModItems/ModArmorMaterials adapters, not automatically
invented for every metal. Platinum remains resource-only. Copper equipment is now implemented with
explicitly approved temporary vanilla Iron artwork. The legacy tooltiers TOML remains inert.


## Netherite progression (seventh catalog entry)

Netherite appends `minecraft:netherite` after the original six definitions (sync ID 6).
It references vanilla `ancient_debris` (localized Netherite Ore), `netherite_scrap`
(localized Raw Netherite), `netherite_ingot` and `netherite_block`; no duplicate
registrations or world generation are introduced. Provisional fictional density is
20,000 kg/m³, between Gold and Platinum; this is a layering balance assumption,
not a real-world material claim. Ore and storage gates are 6; the existing Netherite
equipment profile remains capability 7 with all native statistics intact.

Normal ore loot follows the existing weighted convention: one base raw item plus
Binomial(2, 0.5), yielding 1/2/3 with probabilities 25%/50%/25%. Fortune boosts the
base portion using the existing ore bonus, and Silk Touch returns the intact ore.
Raw Netherite and intact ore each melt to 6 units in 100 base ticks; ingots do too,
blocks melt to 54 units in 900 base ticks. Existing Controller speed scaling still
applies. Table casts 6 units to a vanilla ingot and Basin casts 54 to a vanilla block,
with default 100-tick cooling. Vanilla storage-block texture resolves automatically.

A required high-priority `datapacks/netherite_progression` pack disables furnace /
blasting refining, scrap-plus-gold crafting, all nine smithing upgrades and template
duplication. Direct equipment recipes reuse the ordinary equipment-set generator
and vanilla output IDs. Existing Bastion replacements omit templates and refined
Netherite ingots; effective loaded loot is regression-audited. Other templates,
armor trims, lodestones, block compression/decompression and saved inventories remain.

Manual artwork still required: `textures/block/molten_netherite.png` and its matching
`.png.mcmeta`, under assets/katzencraftmetals. Follow the existing animated molten image
layout. No placeholder art is generated. Optional `foundry_table_ingot_netherite.png`
uses the existing rectangular Table layout; by explicit user choice the universal
base fallback remains until that manual texture is supplied. Do not substitute the
vanilla item sprite or alter Table UVs. No separate flowing image is required.


## Catalog-driven ore generation

`MetalCatalog.java` is the configuration point. `.form(...)` only declares a registered
block: **it never enables generation**. Keep catalog order and all existing form IDs
stable. Ore block registration, textures, loot, Fortune/Silk Touch and mining requirements
remain in their existing owners. Generation is independent of Foundry support and density.

```java
.oreGeneration(DEEPSLATE_ORE, c -> c
    .dimension("minecraft:overworld")
    .biomes("#minecraft:is_overworld")
    .replace("#minecraft:deepslate_ore_replaceables")
    .veinSize(8).attemptsPerChunk(3)
    .height(-64, 0).distribution(BOTTOM_HEAVY))
```

Static imports: `MetalDefinition.Form.*`, `MetalOreGeneration.Distribution.*`, and
`MetalOreGeneration.Mode.*`. Repeat entries freely for the same form, biome or dimension;
avoid accidentally overlapping settings that would intentionally add multiple passes.
Each entry produces a separate configured/placed feature under
`catalog/<metal namespace>/<metal path>/<entry index>`. Generated IDs follow entry order;
regenerate resources after changing the catalog. This does not change metal synchronization IDs.

- `.dimension("namespace:dimension")` and `.replace(...)` are required. Modded dimensions,
  blocks and tags use the same API; the dimension is checked during placement, not inferred
  from a biome tag. Register/provide those resources before enabling an entry.
- `.biomes("minecraft:badlands", "#example:hot_biomes")` is an **OR** of biome IDs and tags.
  Omitting it means all biomes within the configured dimension. Biomes are checked both
  when features are installed and at the sampled position.
- `.replace("minecraft:netherrack", "#example:host_rocks")` is an **OR** of host-block IDs
  and tags. Every matching host becomes this entry's declared ore form. Use separate stone
  and deepslate entries to avoid the wrong ore variant in each host. Nothing infers a host
  from the dimension or ore name.
- Defaults: vein size 8 (valid 1..64), attempts 3 (1..256), inclusive Y -64..64,
  `UNIFORM`, `ADDITIVE`, enabled. Attempts are candidate deposits, not guaranteed ore counts;
  native ore features can produce fewer blocks than the configured maximum.
- `UNIFORM`: equal probability; `TRIANGLE`: native triangular middle peak;
  `BOTTOM_HEAVY` / `TOP_HEAVY`: minimum / maximum of two independent inclusive uniform
  samples, giving smoothly decreasing / increasing weights. Only the native chunk random
  source is used. Count, square spread, biome filtering and ore shapes remain native.
- Heights are sampled only from the intersection of the entry and dimension/generator
  build limits. Ranges are spawn-origin heights, as with native ore placement; a vein's
  shape can extend beyond its origin. Completely disjoint ranges fail server validation.
- `.enabled(false)` emits no feature or replacement and does not suppress vanilla generation.
  Invalid structural settings are rejected even on disabled entries; unresolved external
  selectors only need to exist when an entry is enabled.
- Unknown blocks/biomes, missing/empty selector tags and unknown dimensions fail clearly
  during generation or server startup. Datagen cannot resolve another mod's dimension
  build limits; startup validates against the actual loaded world/datapacks.

### Vanilla replacement versus addition

Vanilla Iron, Copper, Gold and Netherite have **no configured overrides by default**.
Their existing generation, including Ancient Debris, remains native. Merely calling
`.oreGenerationMode(REPLACE)` with no enabled entries does nothing.

```java
// On an existing vanilla metal definition, with its forms already declared:
.oreGenerationMode(REPLACE) // default for this metal's entries; otherwise ADDITIVE
.oreGeneration(ORE, c -> c.dimension("minecraft:overworld")
    .biomes("minecraft:badlands")
    .replace("#minecraft:stone_ore_replaceables")
    .veinSize(9).attemptsPerChunk(4).height(0, 96).distribution(TRIANGLE))
// .mode(ADDITIVE) or .mode(REPLACE) can override the metal default per entry.
```

`ADDITIVE` retains native deposits and adds only the configured features. `REPLACE`
substitutes dimension/biome-guarded copies of **only the relevant vanilla placements**
at their original step/order. Those copies retain all native modifiers outside the
replacement scopes and produce nothing inside them; the configured deposits take over.
There is one guarded copy per original placement, even with multiple replacement entries.
No unrelated stone, gems, structures, quartz or other metals are removed.

Replacement families:

| Metal/form | Native placements affected |
|---|---|
| Iron ORE or DEEPSLATE_ORE | `ore_iron_upper`, `ore_iron_middle`, `ore_iron_small` |
| Copper ORE or DEEPSLATE_ORE | `ore_copper`, `ore_copper_large` |
| Gold ORE or DEEPSLATE_ORE | `ore_gold`, `ore_gold_lower`, `ore_gold_extra` (Badlands) |
| Gold NETHER_ORE | `ore_gold_nether`, `ore_gold_deltas` |
| Netherite ORE (Ancient Debris) | `ore_ancient_debris_large`, `ore_debris_small` |

Native stone/deepslate ores share placements, so replacement operates on the complete
Overworld family, not half of a mixed native vein. Configure **both forms** if both are
wanted in the replacement scope. Nether Gold is a separate family. A Badlands-only
replacement preserves normal Gold elsewhere and all Nether Gold. Iron/Copper large
noise veins bypass placed features: a narrow generator hook suppresses their ore/raw
blocks only inside the same active REPLACE scopes, returning the normal terrain host;
other metals, vein filler rocks, biomes and dimensions are retained. With no such
overrides it leaves all native output unchanged. This is not a retrogen/migration system.

### Initial migrated balance

These are deliberately editable initial values, not promises of old worldgen parity:

| Metal | Form / dimension | Origin Y | Distribution | Vein max | Attempts/chunk |
|---|---|---|---|---:|---:|
| Mythril | stone / Overworld | 0..64 | TRIANGLE | 8 | 3 |
| Mythril | deepslate / Overworld | -64..0 | BOTTOM_HEAVY | 8 | 3 |
| Mythril | Nether | 8..112 | UNIFORM | 8 | 3 |
| Mythril | End | 0..80 | TRIANGLE | 8 | 3 |
| Platinum | stone / Overworld | 0..64 | TRIANGLE | 10 | 5 |
| Platinum | deepslate / Overworld | -64..0 | BOTTOM_HEAVY | 10 | 5 |
| Platinum | Nether | 8..112 | UNIFORM | 10 | 5 |
| Platinum | End | 0..80 | TRIANGLE | 10 | 5 |

All use dimension biome tags and explicit stone/deepslate/netherrack/end-stone targets.
The unrelated extra dungeon placements and Diamond/Emerald suppression are preserved.
Old custom ore feature/modifier JSON is obsolete and removed by datagen; inspect Git's
resource deletions after regenerating. Existing chunks are unchanged.

### Raw Netherite artwork and validation

`assets/minecraft/models/item/netherite_scrap.json` points to the authored
`assets/katzencraftmetals/textures/item/raw_netherite.png`. No copied/recolored PNG or
new item ID is used: `minecraft:netherite_scrap` remains Raw Netherite.

Run `./gradlew.bat runData` and `./gradlew.bat build` without clean. The opt-in
`./gradlew.bat --no-configuration-cache -I verification/equipment.init.gradle runData`
checks seeded distributions, bounds, validation, native targets/codecs, biome scopes,
Gold overrides, noise-vein scope and the existing equipment/Foundry regressions.
Inspect newly generated chunks in Minecraft for balance, dimension variants and
biome transitions; compilation and headless checks do not establish visual worldgen
or item-art appearance. Include all new Java/JSON/resources in Git after review.
