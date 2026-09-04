package net.chriskatze.katzencraftmetals.client.renderer;

import net.chriskatze.katzencraftmetals.block.entity.FoundryControllerBlockEntity;
import net.chriskatze.katzencraftmetals.block.entity.FoundryTankNetwork;
import net.chriskatze.katzencraftmetals.metal.ModMoltenMetals;
import net.chriskatze.katzencraftmetals.metal.MoltenMetalDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.WeakHashMap;

import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.DRAIN_ANIMATION_TICKS;
import static net.chriskatze.katzencraftmetals.client.renderer.FoundryTankRenderConstants.RISE_ANIMATION_TICKS;

/**
 * Client-only smoothing for one Controller-owned molten column.
 *
 * Targets come directly from the Controller's synchronized storage. No Tank
 * lookup, structure scan, or storage aggregation occurs here.
 */
final class FoundryControllerLiquidSmoother {

    private final Map<FoundryControllerBlockEntity, State> states =
            new WeakHashMap<>();

    Map<ResourceLocation, Float> getDisplayedAmounts(
            FoundryControllerBlockEntity controller,
            FoundryTankNetwork network,
            float partialTick
    ) {
        Level level = controller.getLevel();

        if (level == null || network == null) {
            return Map.of();
        }

        Map<ResourceLocation, Integer> targetAmounts =
                network.getMoltenContents();

        double renderTime = level.getGameTime() + partialTick;

        State state = states.computeIfAbsent(
                controller,
                ignored -> new State(
                        createTargetBoundaries(targetAmounts),
                        renderTime
                )
        );

        Map<ResourceLocation, Float> displayedBoundaries =
                state.updateAndGet(
                        createTargetBoundaries(targetAmounts),
                        renderTime
                );

        Map<ResourceLocation, Float> displayedAmounts =
                new LinkedHashMap<>();

        float previousBoundary = 0.0f;
        float capacity = network.getCapacity();

        for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
            float boundary = Mth.clamp(
                    displayedBoundaries.getOrDefault(
                            definition.id(),
                            previousBoundary
                    ),
                    previousBoundary,
                    capacity
            );

            float amount = Math.max(0.0f, boundary - previousBoundary);

            if (amount > 0.0001f) {
                displayedAmounts.put(definition.id(), amount);
            }

            previousBoundary = boundary;
        }

        return displayedAmounts;
    }

    private static Map<ResourceLocation, Float> createTargetBoundaries(
            Map<ResourceLocation, Integer> amounts
    ) {
        Map<ResourceLocation, Float> result = new LinkedHashMap<>();
        float cumulative = 0.0f;

        for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
            cumulative += Math.max(
                    0,
                    amounts.getOrDefault(definition.id(), 0)
            );
            result.put(definition.id(), cumulative);
        }

        return result;
    }

    private static final class State {

        private final Map<ResourceLocation, Animation> animations =
                new LinkedHashMap<>();

        private State(
                Map<ResourceLocation, Float> initialTargets,
                double time
        ) {
            for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
                float target = initialTargets.getOrDefault(definition.id(), 0.0f);
                animations.put(
                        definition.id(),
                        new Animation(target, target, time, 0.0f)
                );
            }
        }

        private Map<ResourceLocation, Float> updateAndGet(
                Map<ResourceLocation, Float> targets,
                double time
        ) {
            Map<ResourceLocation, Float> result = new LinkedHashMap<>();
            float previous = 0.0f;

            for (MoltenMetalDefinition definition : ModMoltenMetals.heaviestFirst()) {
                ResourceLocation metal = definition.id();
                Animation animation = animations.get(metal);
                float current = animation.sample(time);
                float target = targets.getOrDefault(metal, 0.0f);

                if (Math.abs(target - animation.target()) > 0.0001f) {
                    float duration = target >= current
                            ? RISE_ANIMATION_TICKS
                            : DRAIN_ANIMATION_TICKS;

                    animation = new Animation(
                            current,
                            target,
                            time,
                            Math.max(1.0f, duration)
                    );
                    animations.put(metal, animation);
                    current = animation.sample(time);
                }

                current = Math.max(previous, current);
                result.put(metal, current);
                previous = current;
            }

            return result;
        }
    }

    private record Animation(
            float start,
            float target,
            double startTime,
            float duration
    ) {
        float sample(
                double time
        ) {
            if (duration <= 0.0f) {
                return target;
            }

            float progress = Mth.clamp(
                    (float) ((time - startTime) / duration),
                    0.0f,
                    1.0f
            );

            return Mth.lerp(progress, start, target);
        }
    }
}
