package com.harriol.blockwiz.common.config;

import com.harriol.blockwiz.common.i18n.I18n;

import java.util.concurrent.atomic.AtomicReference;

/**
 * 运行时配置持有器（当前快照）。配置热更新后立即生效；
 * 正在执行的任务使用启动时快照（后续 Sprint 引入任务系统时落实）。
 *
 * @author Harriol
 */
public final class ConfigHolder {

    private static final AtomicReference<ConfigData> CURRENT = new AtomicReference<>(new ConfigData());
    private static final AtomicReference<Boolean> VALID = new AtomicReference<>(false);

    private ConfigHolder() {
    }

    public static ConfigData get() {
        return CURRENT.get();
    }

    public static boolean isValid() {
        return VALID.get();
    }

    public static void update(ConfigData config) {
        ConfigData next = config == null ? new ConfigData() : config;
        CURRENT.set(next);
        VALID.set(ConfigValidator.isValid(next));
        I18n.setLanguage(next.getLanguage());
    }
}
