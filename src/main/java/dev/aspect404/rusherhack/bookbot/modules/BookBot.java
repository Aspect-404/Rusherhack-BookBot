package dev.aspect404.rusherhack.bookbot.modules;

import net.minecraft.world.item.Items;
import org.rusherhack.core.setting.*;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;

import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

public class BookBot extends ToggleableModule implements IModule {
    private enum CHARACTER_SET {KeySpam, Ascii, Unicode} //TODO File input
    private final StringSetting bookName = new StringSetting("Book Name", "Made using BookBot");
    private final NumberSetting<Integer> pages = new NumberSetting<>("Pages", 10, 1, 100).incremental(5);
    private final BooleanSetting pastedPages = new BooleanSetting("Pasted Pages", "All pages will contain the same content as if it was pasted.", false);
    private final EnumSetting<CHARACTER_SET> characterSet = new EnumSetting<>("Character Set", "KeySpam will contain common keyboard keys, Ascii will contain a variety of Ascii characters, and Unicode will contain a variety of Unicode", CHARACTER_SET.Ascii);
    public BookBot() {
        super("BookBot", "Writes books",ModuleCategory.MISC);
        registerSettings(this.bookName, this.pages, this.pastedPages, this.characterSet);
    }
    @Override
    public String getMetadata() {
        return characterSet.getDisplayValue();
    }
    String pastedPage = null;
    @Override
    public void onEnable() {
        if (mc.player == null || mc.getConnection() == null || !mc.player.isHolding(Items.WRITABLE_BOOK)) {
            ChatUtils.print("You must be holding a writeable book in main hand!");
            this.setToggled(false);
            return;
        }
        ArrayList<String> pageList = new ArrayList<>();
        if (this.pastedPages.getValue()) pastedPage = generatePage();
        IntStream.range(0, this.pages.getValue()).forEach(page -> pageList.add(this.pastedPages.getValue() ? pastedPage : generatePage()));
        mc.getConnection().send(new ServerboundEditBookPacket(mc.player.getInventory().selected, pageList, Optional.of(this.bookName.getValue().trim())));
        this.setToggled(false);
    }

    public String generatePage() {
        Random random = new Random();
        StringBuilder pageContent = new StringBuilder();
        while (pageContent.toString().length() < 256) {
            char randomChar = switch (this.characterSet.getValue()) {
                case KeySpam -> (char) (random.nextInt(95) + 32);
                case Ascii -> (char) (random.nextInt(128));
                case Unicode -> (char) (random.nextInt(65536));
            };
            pageContent.append(randomChar);
        }
        return pageContent.toString();
    }
}
