package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.entity.DropItemsEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class MiningMacro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    // General
    private final Setting<Integer> dropTimer = sgGeneral.add(new IntSetting.Builder()
        .name("drop-timer")
        .description("How long to wait between opening the inventory to clean.")
        .defaultValue(30000)
        .min(0)
        .sliderMax(180000)
        .build());

    // Whitelist
    private final Setting<List<Item>> whitelistItems = sgWhitelist.add(new ItemListSetting.Builder()
        .name("whitelist-items")
        .description("Items that should not be dropped.")
        .build());

    private long lastInventoryOpen = 0; // Timer to track when the inventory was last opened

    public MiningMacro() {
        super(Categories.Misc, "MiningMacro", "Helps manage your inventory during mining.");
    }

    @Override
    public void onActivate() {
        lastInventoryOpen = System.currentTimeMillis(); // Reset the timer when activated
    }

    @Override
    public void onDeactivate() {
        // Clean up any resources here
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (mc.player.playerScreenHandler == null || !isActive()) return;

        long currentTime = System.currentTimeMillis();

        // Automatically open the inventory based on the dropTimer setting
        if (currentTime - lastInventoryOpen >= dropTimer.get()) {
            // Simulate pressing the inventory key (default is "E")
            MinecraftClient.getInstance().options.inventoryKey.setPressed(true);
            lastInventoryOpen = currentTime; // Reset the timer

            // Schedule a task to drop non-whitelisted items after the inventory is open
            mc.execute(() -> {
                // Loop through inventory slots, excluding hotbar (slots 0-8)
                for (int i = 9; i < mc.player.getInventory().size(); i++) {
                    ItemStack itemStack = mc.player.getInventory().getStack(i);

                    if (itemStack.isEmpty() || whitelistItems.get().contains(itemStack.getItem())) continue;

                    // Drop the item if it's not whitelisted
                    InvUtils.drop().slot(i);
                    System.out.println("Dropping NOW");
                }

                // Close the inventory after cleaning
                mc.player.closeHandledScreen();
            });

            // Release the inventory key
            MinecraftClient.getInstance().options.inventoryKey.setPressed(false);
        }
    }

    @EventHandler
    private void onDropItems(DropItemsEvent event) {
        // Prevent dropping in creative mode if desired
    }
}
