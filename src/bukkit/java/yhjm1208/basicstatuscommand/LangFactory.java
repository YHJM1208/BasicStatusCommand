package yhjm1208.basicstatuscommand;

/** bukkit 线（1.8.8 – 1.18.1）：语言文本用 & 传统色码。 */
public final class LangFactory {

    private LangFactory() {
    }

    public static Lang create(BasicStatusCommand plugin) {
        return new LegacyLang(plugin);
    }
}
