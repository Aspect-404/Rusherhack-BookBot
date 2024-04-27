package dev.aspect404.rusherhack.bookbot.modules;

import net.minecraft.world.item.Items;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.core.setting.*;
import org.rusherhack.client.api.utils.ChatUtils;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ModuleCategory;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import net.minecraft.network.protocol.game.ServerboundEditBookPacket;

import java.io.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.stream.IntStream;

public class BookBot extends ToggleableModule implements IModule {
    private enum MODES {Random, File}
    private enum CHARACTER_SET {KeySpam, Ascii, Unicode}
    private final EnumSetting<MODES> mode = new EnumSetting<>("Mode", "Random is useful for making high-data books for chunk duping and ban books, file will check .rusherhack/books directory for file.txt to write in a collection of books.", MODES.Random);
    private final StringSetting bookName = new StringSetting("Book Name", "Made using BookBot");
    private final NumberSetting<Integer> pages = new NumberSetting<>("Max Pages", "Maximum amount of pages to write per book, some servers have this set to an arbitrary limit.", 10, 1, 100).incremental(5);

    private final StringSetting fileName = new StringSetting("File Name", "example.txt");
    private final BooleanSetting listFiles = new BooleanSetting("List Files", "Maximum amount of pages to write per book, some servers have this set to an arbitrary limit.", true);

    private final BooleanSetting pastedPages = new BooleanSetting("Pasted Pages", "All pages will contain the same content as if it was pasted.", false);
    private final EnumSetting<CHARACTER_SET> characterSet = new EnumSetting<>("Character Set", "KeySpam will contain common keyboard keys, Ascii will contain a variety of Ascii characters, and Unicode will contain a variety of Unicode", CHARACTER_SET.Ascii);

    private final String bookPath = RusherHackAPI.getConfigPath().toString().replace("config", "books/");
    public BookBot() {
        super("BookBot", "Writes books",ModuleCategory.MISC);
        this.fileName.setHidden(!this.mode.getValue().equals(MODES.File));
        this.listFiles.setHidden(!this.mode.getValue().equals(MODES.File));
        this.pastedPages.setHidden(this.mode.getValue().equals(MODES.File));
        this.characterSet.setHidden(this.mode.getValue().equals(MODES.File));
        this.mode.onChange((value) -> {
            this.fileName.setHidden(!value.equals(MODES.File));
            this.listFiles.setHidden(!value.equals(MODES.File));
            this.pastedPages.setHidden(value.equals(MODES.File));
            this.characterSet.setHidden(value.equals(MODES.File));
        });
        this.listFiles.onChange((value) -> {
            File bookDir = new File(bookPath);
            if (value) return;
            this.listFiles.setValue(true);
            if (bookDir.mkdirs()) {
                ChatUtils.print("Created directory .rusherhack/books/!");
                return;
            }
            if (!bookDir.isDirectory() || !bookDir.canRead()) {
                ChatUtils.print("Error reading from .rusherhack/books/ directory, perhaps filesystem permission issues?");
                return;
            }
            if (bookDir.listFiles() == null) {
                ChatUtils.print("No files found in .rusherhack/books/ directory!");
                return;
            }
            for (File file : Objects.requireNonNull(bookDir.listFiles())) {
                ChatUtils.print(file.getName());
            }
        });
        registerSettings(this.mode, this.bookName, this.pages, this.fileName, this.listFiles, this.pastedPages, this.characterSet);
    }
    @Override
    public String getMetadata() {
        return mode.getDisplayValue();
    }
    String pastedPage = null;
    @Override
    public void onEnable() {
        if (mc.player == null || mc.getConnection() == null || !mc.player.isHolding(Items.WRITABLE_BOOK)) {
            ChatUtils.print("You must be holding a writeable book in main hand!");
            this.setToggled(false);
            return;
        }
        ArrayList<String> bookPageList = new ArrayList<>();
        if (mode.getValue().equals(MODES.Random)) {
            if (this.pastedPages.getValue()) pastedPage = generateRandomPage();
            IntStream.range(0, this.pages.getValue()).forEach(page -> bookPageList.add(this.pastedPages.getValue() ? pastedPage : generateRandomPage()));
        } else {
            try {
                bookPageList.addAll(generatePagesFromFile());
            } catch (IOException ignored) {
                ChatUtils.print("Could not find a file with name: " + this.fileName.getValue());
                this.setToggled(false);
                return;
            }
        }
        mc.getConnection().send(new ServerboundEditBookPacket(mc.player.getInventory().selected, bookPageList, Optional.of(this.bookName.getValue().trim())));
        this.setToggled(false);
    }

    public ArrayList<String> generatePagesFromFile() throws IOException {
        ArrayList<String> pageList = new ArrayList<>();
        try (BufferedReader fileReader = new BufferedReader(new FileReader(bookPath + this.fileName.getValue()))) {
            char[] buffer = new char[256];
            int charsRead;
            while (pageList.size() < pages.getValue() && (charsRead = fileReader.read(buffer)) != -1) {
                pageList.add(String.valueOf(buffer, 0, charsRead));
            }
        }
        return pageList;
    }

    /**
     * Generates a random page for the book, relying on this.characterSet.getValue().
     * @return A 255 character long random string.
     */
    public String generateRandomPage() {
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
