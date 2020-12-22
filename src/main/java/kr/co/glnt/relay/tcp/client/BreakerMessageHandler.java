package kr.co.glnt.relay.tcp.client;

import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class BreakerMessageHandler extends SimpleChannelInboundHandler<String> {

    // 연결 성공
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info("연결 성공 : {}", channel.remoteAddress());
    }

    // 연결 종료
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        log.info("연결 종료 : {}", channel.remoteAddress());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        log.info("receive msg : {}", msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.info("TCP ERR => remoteAddress : {}", ctx.channel().remoteAddress());
    }
}
