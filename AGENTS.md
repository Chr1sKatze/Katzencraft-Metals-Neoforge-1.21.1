# Development guidance

These instructions apply throughout this repository. Preserve existing work and follow any additional instructions in nested AGENTS.md files when working in their directories.

## Project baseline

- This is Katzencraft Metals, mod ID `katzencraftmetals`.
- Preserve Minecraft `1.21.1` and NeoForge `21.1.219` unless explicitly instructed otherwise.
- Preserve the existing Gradle configuration, wrapper, mappings, repositories, dependencies, and Java 21 toolchain unless explicitly instructed otherwise. The current wrapper uses Gradle 9.2.1 and the ModDevGradle plugin uses 2.0.141.
- Version properties live in `gradle.properties`; build configuration lives in `build.gradle`; mod metadata is expanded from `src/main/templates/META-INF/neoforge.mods.toml`.
- The main mod class is `net.chriskatze.katzencraftmetals.KatzencraftMetalsMod` in `src/main/java/net/chriskatze/katzencraftmetals/KatzencraftMetalsMod.java`.

## Architecture and style

- Follow the existing `net.chriskatze.katzencraftmetals` packages, architecture, naming, and nearby Java formatting conventions.
- Keep registrations in the existing `Mod*` classes, such as `ModBlocks`, `ModItems`, `ModBlockEntities`, `ModMenuTypes`, and `ModRecipes`.
- Foundry block entities and their focused processing, storage, persistence, network, and structure helpers live in `block/entity`. Extend the appropriate existing helper rather than unnecessarily rewriting the controller or duplicating responsibilities.
- Follow existing client organization in `client` and `screen`, menu organization in `menu`, and networking organization in `network`. Keep client-only code out of common server execution paths.
- Prefer small, maintainable changes scoped to the requested behavior. Avoid unrelated refactors and broad formatting changes.
- Assets and data are in `src/main/resources` and `src/generated/resources`; data generators are in the `datagen` package. The build includes generated resources. Inspect generator ownership before editing generated output and preserve existing manual work.

## Autonomy and preservation

- You may independently inspect files, edit code, build, run tests, and fix compilation errors inside the repository within the user's requested scope.
- Inspect the current working tree before editing. Preserve existing tracked changes, untracked files, and generated resources; do not overwrite unrelated work.
- In particular, treat `src/generated/resources/assets/katzencraftmetals/models/block/foundry_controller_on.json` as existing work that must be preserved regardless of its tracked or staged status.
- Do not delete untracked files, reset existing work, change dependencies, delete worlds, force-push, or publish releases without explicit approval.
- Request approval when additional filesystem, network, or execution permissions are required. Do not work around sandbox restrictions.

## Validation and reporting

- Run `.\gradlew.bat build` from the project root after implementation changes, including Java, shaders and resources, unless the user explicitly requests otherwise. Investigate failures and fix compilation errors within the authorized scope; respect requests limiting edits to investigation only.
- Do not run `clean` unnecessarily. For documentation-only tasks, report that the build was not run and why.
- Use appropriate focused tests when relevant. A build with `test NO-SOURCE` does not establish runtime or gameplay correctness.
- Build JARs are written to `build/libs`; report the actual output path and Gradle's build duration when reporting a successful build.
- Never claim a rendering fix is visually verified unless it was actually inspected in-game. Distinguish compilation checks, resource inspection, and in-game visual verification, and state when in-game verification remains outstanding.
- At the end of every task, summarize changed files, build/test commands and results (or why they were not run), and unresolved issues or verification gaps.

## Git completeness after every coding task

- Run `git status --short` after implementation and validation. Inspect all newly created, modified and untracked files, including changes produced by build or data-generation tasks.
- Identify every new Java class, shader, JSON resource, model and other file required by the implementation, including required generated resources. Compare local implementation files with Git's tracked files and staging index; verify required files will be included in a fresh checkout after the intended changes are committed.
- Check ignore rules for required files, using `git check-ignore -v` where appropriate. Explicitly warn the user about any required implementation files that remain untracked or ignored, listing their paths and the relevant ignore rule when known. A successful local build alone does not establish Git completeness.
- Never declare changes ready for GitHub, commit or push without verifying Git completeness. Required files must be tracked or staged, and any required unstaged modifications must be identified as pending staging before the commit is complete.
- In the final report for every task, always include the build result (or why it was not run), new files created (or none), required files still untracked, ignored or unstaged (or none), and whether the changes are ready to commit and push. Explain any remaining blockers.

## Git and asset safety

- Never automatically commit or push without the user's explicit permission.
- Never automatically stage files unless the user explicitly asks. If staging is authorized, stage only the requested, reviewed files; never use `git add .` indiscriminately.
- Preserve existing staged changes without altering or unstaging them unless explicitly authorized. Preserve unrelated, untracked and manually edited files.
- Never delete or overwrite the user's Blockbench models or textures without permission.
- Do not modify `.gitignore` without explaining why the change is necessary.
