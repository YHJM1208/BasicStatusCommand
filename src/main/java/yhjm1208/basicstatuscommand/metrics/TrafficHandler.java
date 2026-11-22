package yhjm1208.basicstatuscommand.metrics;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

/**
 * 挂在每个玩家连接 pipeline 最前面的字节计数 handler。
 * 放在头部：入站是拆包前的原始字节，出站是编码后的字节。
 */
public final class TrafficHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            TrafficMeter.addIn(((ByteBuf) msg).readableBytes());
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            TrafficMeter.addOut(((ByteBuf) msg).readableBytes());
        }
        ctx.write(msg, promise);
    }
}
