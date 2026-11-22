package yhjm1208.basicstatuscommand;

import org.bukkit.command.CommandSender;

/**
 * 语言文件的统一入口。
 * 两个产物各一份实现：paper 走 MiniMessage（{@code src/paper}），
 * bukkit 走 {@code &} 传统色码（{@code src/bukkit}），与 BasicTpCmd 的两条线保持一致。
 */
public interface Lang {

    /** 读配置、释放语言文件、补缺失键。 */
    void load();

    String language();

    /**
     * 取一条已填占位符、但尚未渲染的原始文本（保留 &amp; / MiniMessage 标记），
     * 用于作为其它文案的内嵌片段；调用方要用 {@link #rawValue(String)} 包一层再当占位符。
     */
    String str(String key, Object... placeholders);

    /** 带插件前缀发送一条消息。 */
    void send(CommandSender sender, String key, Object... placeholders);

    /** 不带前缀发送一行（表格、标题用）。 */
    void line(CommandSender sender, String key, Object... placeholders);

    /** 语义色：good / warn / bad，按平台翻译成 &amp;a / &lt;green&gt; 等。 */
    String color(String tone);

    /** 已渲染片段标记：填进其它模板时不再转义。 */
    static Raw rawValue(String text) {
        return new Raw(text);
    }

    final class Raw {
        public final String text;

        private Raw(String text) {
            this.text = text;
        }
    }
}
