package yhjm1208.basicstatuscommand;

/** paper 线（1.18.2+）：语言文本用 MiniMessage。 */
public final class LangFactory {

    private LangFactory() {
    }

    public static Lang create(BasicStatusCommand plugin) {
        return new MiniLang(plugin);
    }
}
