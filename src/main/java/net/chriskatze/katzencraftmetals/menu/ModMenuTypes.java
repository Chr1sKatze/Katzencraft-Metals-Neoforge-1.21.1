package net.chriskatze.katzencraftmetals.menu;

import net.chriskatze.katzencraftmetals.KatzencraftMetalsMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModMenuTypes {

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, KatzencraftMetalsMod.MODID);

    public static final Supplier<MenuType<CrusherMenu>> CRUSHER_MENU =
            MENUS.register("crusher_menu",
                    () -> IMenuTypeExtension.create(CrusherMenu::new));

    public static final Supplier<MenuType<FuelChamberMenu>> FUEL_CHAMBER_MENU =
            MENUS.register(
                    "fuel_chamber_menu",
                    () -> IMenuTypeExtension.create(FuelChamberMenu::new)
            );

    public static final Supplier<MenuType<FoundryControllerMenu>>
            FOUNDRY_CONTROLLER_MENU =
            MENUS.register(
                    "foundry_controller_menu",
                    () -> IMenuTypeExtension.create(
                            FoundryControllerMenu::new
                    )
            );

    public static final Supplier<MenuType<KatzencraftAnvilMenu>> KATZENCRAFT_ANVIL_MENU =
            MENUS.register("katzencraft_anvil_menu",
                    () -> IMenuTypeExtension.create((containerId, inventory, extraData) ->
                            new KatzencraftAnvilMenu(containerId, inventory)));
}