package kr.co.glnt.relay.tcp;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@Slf4j
@ChannelHandler.Sharable
public class GlntNettyHandler extends SimpleChannelInboundHandler<String> {

    private final SimpMessagingTemplate webSocket;

    public GlntNettyHandler(SimpMessagingTemplate webSocket) {
        this.webSocket = webSocket;
    }

    // 연결 성공
    // webSocket 에 연결된 client 에게 연결 성공 되었다고 전송
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        webSocket.convertAndSend("/subscribe/connect", "connect");
    }

    // 연결 종료
    // webSocket 에 연결된 client 에게 연결 종료 되었다고 전송
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String remoteAddr = channel.remoteAddress().toString();
        webSocket.convertAndSend("/subscribe/disconnect", "disconnect");
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
