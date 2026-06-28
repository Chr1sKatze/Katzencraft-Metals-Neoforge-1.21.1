package net.chriskatze.katzencraftmetals.datagen;

import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Gives an existing DataProvider a unique externally visible name.
 *
 * RecipeProvider#getName() is final in Minecraft 1.21.1, so a second
 * RecipeProvider cannot rename itself through inheritance. This wrapper
 * supplies the unique name while delegating the actual generation work.
 */
public final class NamedDataProvider
        implements DataProvider {

    private final String name;
    private final DataProvider delegate;

    public NamedDataProvider(
            String name,
            DataProvider delegate
    ) {
        if (
                name == null
                        || name.isBlank()
        ) {
            throw new IllegalArgumentException(
                    "Data provider name cannot be blank."
            );
        }

        this.name =
                name;

        this.delegate =
                Objects.requireNonNull(
                        delegate,
                        "delegate"
                );
    }

    @Override
    public CompletableFuture<?> run(
            CachedOutput output
    ) {
        return delegate.run(output);
    }

    @Override
    public String getName() {
        return name;
    }
}
