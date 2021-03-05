package kr.co.glnt.relay.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

import java.io.IOException;
import java.net.InetAddress;

public class GlntIdleHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            String host = ctx.channel().remoteAddress().toString();
            String ip = host.substring(1, host.indexOf(":"));

            boolean isActive = InetAddress.getByName(ip).isReachable(1500);
            if (!isActive) {
                ctx.close();
            }
        }
    }
}
