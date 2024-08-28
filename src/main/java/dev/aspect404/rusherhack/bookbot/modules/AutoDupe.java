package dev.aspect404.rusherhack.bookbot.modules;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.Container;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.rusherhack.client.api.events.network.EventPacket;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.*;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * I want to die
 */
public class AutoDupe extends ToggleableModule implements IModule {
    private enum MODES {ChunkDupe}
    private final EnumSetting<MODES> mode = new EnumSetting<>("Mode", "idk lol", MODES.ChunkDupe);

    private final NullSetting selectOptions = new NullSetting("Select Containers", "Select the different containers used in the chunk duplication.");
    private final BooleanSetting selectDupeContainer = new BooleanSetting("Dupe Container", "Select the container to contain the dupe books.", true);
    private final BooleanSetting selectItemContainer = new BooleanSetting("Item Container", "Select the container which contains the items to dupe.", true);
    private final BooleanSetting selectStorageContainer = new BooleanSetting("Storage Container", "Select the container to store the duplicated items in, best to create a hopper system.", true);

    public AutoDupe() {
        super("AutoDupe", "Automatically dupes",ModuleCategory.MISC);
        this.selectOptions.addSubSettings(this.selectDupeContainer, this.selectItemContainer, this.selectStorageContainer);
        registerSettings(this.mode, this.selectOptions);
    }
    @Override
    public String getMetadata() {
        return mode.getDisplayValue();
    }

    private boolean isLookingForContainer = false;
    private BlockPos dupeContainer, itemContainer, storageContainer;
    @Override
    public void onEnable() {
        if (dupeContainer == null) {
            isLookingForContainer = true;
            ChatUtils.print("Select the container to ");
            selectContainer(container -> dupeContainer = container);
        }
    }

    /*
    This trash is used to select the containers.
     */
    private final CompletableFuture<BlockPos> selectedContainerFuture = new CompletableFuture<>();
    @Subscribe
    private void onPacketSend(EventPacket.Send event) {
        if (mc.level == null) return;
        Packet<?> packet = event.getPacket();
        if (!(packet instanceof ServerboundUseItemOnPacket) || !isLookingForContainer) return;
        BlockPos selectedBlock = ((ServerboundUseItemOnPacket) packet).getHitResult().getBlockPos();
        BlockEntity blockEntity = mc.level.getBlockEntity(selectedBlock);
        if (blockEntity instanceof Container) {
            selectedContainerFuture.complete(selectedBlock);
            event.setCancelled(true);
        }
    }
    private void selectContainer(Consumer<BlockPos> callback) {
        selectedContainerFuture.thenAccept(callback);
        isLookingForContainer = false;
    }
}
