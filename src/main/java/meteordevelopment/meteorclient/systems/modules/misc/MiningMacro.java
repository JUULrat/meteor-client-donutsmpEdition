package meteordevelopment.meteorclient.systems.modules.misc;

import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.ItemListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import java.util.List;

public class MiningMacro extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    // General
    private final Setting<Integer> checkTimer = sgGeneral.add(new IntSetting.Builder()
        .name("check-timer")
        .description("How often to pause mining and open the shop.")
        .defaultValue(30000)
        .min(0)
        .sliderMax(180000)
        .build());

    // Whitelist
    private final Setting<List<Item>> whitelistItems = sgWhitelist.add(new ItemListSetting.Builder()
        .name("whitelist-items")
        .description("Items that should not be sold.")
        .build());

    private long lastShopCheck = 0; // Timer to track when the shop was last opened
    private long shopOpenedTime = 0; // Timestamp to track when the shop was opened
    private boolean isPaused = false; // Track if the mining is currently paused
    private boolean isWaitingToResume = false; // Track if we're waiting for the delay before resuming mining

    private static final long RESUME_DELAY = 5000; // 5-second delay before resuming mining

    public MiningMacro() {
        super(Categories.Misc, "MiningMacro", "Automates mining and selling.");
    }

    @Override
    public void onActivate() {
        lastShopCheck = System.currentTimeMillis(); // Reset the timer when activated
        isPaused = false;
        isWaitingToResume = false;

        // Start mining copper ore
        sendChatCommand("#mine minecraft:copper_ore");
        System.out.println("Started mining copper ore");
    }

    @Override
    public void onDeactivate() {
        // Clean up any resources here
    }

    @EventHandler
    private void onTickPost(TickEvent.Post event) {
        if (!isActive() || MinecraftClient.getInstance().player == null) return;

        long currentTime = System.currentTimeMillis();

        // Handle the delay for resuming mining
        if (isWaitingToResume && (currentTime - shopOpenedTime >= RESUME_DELAY)) {
            // Resume mining
            sendChatCommand("#resume");
            System.out.println("Resuming mining after delay");
            isPaused = false;
            isWaitingToResume = false;
        }

        // Check if it's time to pause mining and open the shop
        if (!isPaused && (currentTime - lastShopCheck >= checkTimer.get())) {
            lastShopCheck = currentTime;
            isPaused = true;

            // Pause mining
            sendChatCommand("#pause");
            System.out.println("Pausing mining");

            // Open the shop and sell items
            sendChatCommand("/sell"); // Make sure there's no space before the "/sell" command
            System.out.println("Opening shop");

            // Use Minecraft's execute method to handle the task asynchronously
            MinecraftClient.getInstance().execute(() -> {
                // Move items (except for the hotbar) to the shop menu
                for (int i = 9; i < MinecraftClient.getInstance().player.getInventory().size(); i++) { // Skip the hotbar
                    ItemStack itemStack = MinecraftClient.getInstance().player.getInventory().getStack(i);
                    if (!itemStack.isEmpty() && !whitelistItems.get().contains(itemStack.getItem())) {
                        // Move the item to the shop
                        InvUtils.move().slot(i);
                        System.out.println("Moving item to shop in slot: " + i);
                    }
                }
                // Close the shop after selling
                MinecraftClient.getInstance().player.closeHandledScreen();

                // Start the delay countdown for resuming mining
                shopOpenedTime = System.currentTimeMillis();
                isWaitingToResume = true;
            });
        }
    }

    private void sendChatCommand(String command) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player != null && command != null && !command.trim().isEmpty()) {
            player.networkHandler.sendChatMessage(command.trim()); // Ensure no leading/trailing spaces
        }
    }
}
