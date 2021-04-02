package kr.co.glnt.relay.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;

@Slf4j
public class GlntIdleHandler extends ChannelDuplexHandler {

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if (((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
                String host = ctx.channel().remoteAddress().toString();
                String ip = host.substring(1, host.indexOf(":"));

                boolean isActive = InetAddress.getByName(ip).isReachable(3000);
                if (!isActive) {
                    ctx.close();
                }
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}
