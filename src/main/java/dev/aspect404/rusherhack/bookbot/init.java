package dev.aspect404.rusherhack.bookbot;

import dev.aspect404.rusherhack.bookbot.modules.BookBot;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

public class init extends Plugin {
    @Override
    public void onLoad() {
        RusherHackAPI.getModuleManager().registerFeature(new BookBot());
    }

    @Override
    public void onUnload() {}
}