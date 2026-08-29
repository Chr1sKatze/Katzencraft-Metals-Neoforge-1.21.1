$ErrorActionPreference = "Stop"

function Read-ProjectFile {
    param([string]$Path)
    return [System.IO.File]::ReadAllText($Path)
}

function Write-ProjectFile {
    param(
        [string]$Path,
        [string]$Content
    )
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $utf8NoBom)
}

function Replace-Once {
    param(
        [string]$Path,
        [string]$Old,
        [string]$New,
        [string]$Label
    )

    $content = Read-ProjectFile $Path

    if ($content.Contains($New)) {
        Write-Host "Already applied: $Label"
        return
    }

    if (-not $content.Contains($Old)) {
        throw "Could not find insertion point for: $Label in $Path"
    }

    $content = $content.Replace($Old, $New)
    Write-ProjectFile $Path $content
    Write-Host "Applied: $Label"
}

$tankBe = "src/main/java/net/chriskatze/katzencraftmetals/block/entity/FoundryTankBlockEntity.java"
$controllerBe = "src/main/java/net/chriskatze/katzencraftmetals/block/entity/FoundryControllerBlockEntity.java"
$tankRenderer = "src/main/java/net/chriskatze/katzencraftmetals/client/renderer/FoundryTankBlockEntityRenderer.java"

if (-not (Test-Path $tankBe)) { throw "Missing file: $tankBe" }
if (-not (Test-Path $controllerBe)) { throw "Missing file: $controllerBe" }
if (-not (Test-Path $tankRenderer)) { throw "Missing file: $tankRenderer" }

# 1. FoundryTankBlockEntity: field
Replace-Once `
    -Path $tankBe `
    -Label "tank intake hatch field" `
    -Old @'
    private long cachedNetworkGameTime =
            Long.MIN_VALUE;
'@ `
    -New @'
    private long cachedNetworkGameTime =
            Long.MIN_VALUE;

    private boolean intakeHatchOpen;
'@

# 2. FoundryTankBlockEntity: methods
Replace-Once `
    -Path $tankBe `
    -Label "tank intake hatch methods" `
    -Old @'
    public boolean hasActiveController() {
        FoundryTankNetwork network =
                getNetwork();

        return network != null
                && network.isActive();
    }

    // =========================
    // MULTI-METAL STORAGE
    // =========================
'@ `
    -New @'
    public boolean hasActiveController() {
        FoundryTankNetwork network =
                getNetwork();

        return network != null
                && network.isActive();
    }

    // =========================
    // INTAKE HATCH
    // =========================

    public boolean isIntakeHatchOpen() {
        return intakeHatchOpen;
    }

    public void setIntakeHatchOpen(
            boolean open
    ) {
        if (intakeHatchOpen == open) {
            return;
        }

        intakeHatchOpen =
                open;

        setChanged();
        syncToClient();
    }

    public boolean isTopTank() {
        return level != null
                && !(level.getBlockEntity(
                worldPosition.above()
        ) instanceof FoundryTankBlockEntity);
    }

    // =========================
    // MULTI-METAL STORAGE
    // =========================
'@

# 3. FoundryTankBlockEntity: save hatch state
Replace-Once `
    -Path $tankBe `
    -Label "save tank intake hatch state" `
    -Old @'
        if (orphanLayoutId != null) {
            tag.putString(
                    "TankOrphanLayoutId",
                    orphanLayoutId.toString()
            );
        }

        /*
         * Preserve an untouched pre-multi-metal save until its owning network
'@ `
    -New @'
        if (orphanLayoutId != null) {
            tag.putString(
                    "TankOrphanLayoutId",
                    orphanLayoutId.toString()
            );
        }

        tag.putBoolean(
                "IntakeHatchOpen",
                intakeHatchOpen
        );

        /*
         * Preserve an untouched pre-multi-metal save until its owning network
'@

# 4. FoundryTankBlockEntity: load hatch state
Replace-Once `
    -Path $tankBe `
    -Label "load tank intake hatch state" `
    -Old @'
        if (tag.contains("TankOrphanLayoutId")) {
            try {
                orphanLayoutId =
                        UUID.fromString(
                                tag.getString(
                                        "TankOrphanLayoutId"
                                )
                        );
            } catch (IllegalArgumentException ignored) {
                orphanLayoutId =
                        null;
            }
        }

        if (
                tag.contains(
                        "MultiMetalLayers",
'@ `
    -New @'
        if (tag.contains("TankOrphanLayoutId")) {
            try {
                orphanLayoutId =
                        UUID.fromString(
                                tag.getString(
                                        "TankOrphanLayoutId"
                                )
                        );
            } catch (IllegalArgumentException ignored) {
                orphanLayoutId =
                        null;
            }
        }

        intakeHatchOpen =
                tag.getBoolean(
                        "IntakeHatchOpen"
                );

        if (
                tag.contains(
                        "MultiMetalLayers",
'@

# 5. FoundryControllerBlockEntity: process hatches once per controller tick
Replace-Once `
    -Path $controllerBe `
    -Label "controller processes intake hatches" `
    -Old @'
        /*
         * Melting or alloying may have inserted a new metal during this tick.
         */
        if (!level.isClientSide()) {
            controller.discoverCurrentTankMetals();
        }
'@ `
    -New @'
        /*
         * Melting, alloying, or an intake hatch may have inserted a new metal or
         * new process item during this tick.
         */
        if (!level.isClientSide()) {
            FoundryTankIntakeHatch.processOpenHatches(controller);
            controller.discoverCurrentTankMetals();
        }
'@

# 6. Tank renderer: render hatch illusion
Replace-Once `
    -Path $tankRenderer `
    -Label "render tank intake hatch illusion" `
    -Old @'
        FoundryTankCasingRenderer.render(
                tank,
                pose,
                bufferSource,
                packedLight,
                packedOverlay
        );

        moltenRenderer.render(
'@ `
    -New @'
        FoundryTankCasingRenderer.render(
                tank,
                pose,
                bufferSource,
                packedLight,
                packedOverlay
        );

        FoundryTankIntakeHatchRenderer.render(
                tank,
                pose,
                bufferSource,
                packedLight,
                packedOverlay
        );

        moltenRenderer.render(
'@

Write-Host ""
Write-Host "Foundry intake hatch patch applied."
Write-Host "Now run: .\gradlew.bat build"
