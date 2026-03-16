package io.serge.flashbackextras.compat;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import io.serge.flashbackextras.screen.FlashbackExtrasConfigScreen;

public final class ModMenuCompat implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return FlashbackExtrasConfigScreen::new;
    }
}
