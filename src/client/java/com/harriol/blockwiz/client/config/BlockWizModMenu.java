package com.harriol.blockwiz.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/**
 * Mod Menu 入口：提供 BlockWiz 配置页。
 *
 * @author Harriol
 */
public class BlockWizModMenu implements ModMenuApi {

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return BlockWizConfigScreen::create;
    }
}
