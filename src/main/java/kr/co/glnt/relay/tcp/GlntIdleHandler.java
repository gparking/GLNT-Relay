package kr.co.glnt.relay.tcp;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.DisplayMessage;
import kr.co.glnt.relay.dto.FacilityInfo;
import lombok.extern.slf4j.Slf4j;

import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.List;


@Slf4j
public class GlntIdleHandler extends ChannelDuplexHandler {

    private final ServerConfig config;

    public GlntIdleHandler(ServerConfig config) {
        this.config = config;
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            String host = ctx.channel().remoteAddress().toString();
            FacilityInfo info = config.findFacilityInfoByHost(host.substring(1));

            if (((IdleStateEvent) evt).state() == IdleState.READER_IDLE) {
                // 전광판은 헬스체크 메세지가 5초에 한번 들어옴.
                if (info.getCategory().equals("DISPLAY")) {
                    ctx.close();
                    return;
                }


                String ip = host.substring(1, host.indexOf(":"));

                try {
                    boolean isActive = InetAddress.getByName(ip).isReachable(3000);
                    if (!isActive) {
                        ctx.close();
                    }
                } catch (Exception e) {
                    ctx.close();
                }
            }
//            else if (((IdleStateEvent) evt).state() == IdleState.WRITER_IDLE) {
//
//            }
//                String writeHost = host.substring(1, host.length());
//
//                FacilityInfo info = config.findFacilityInfoByHost(writeHost);
//
//                String category = info.getCategory();
//
//                if (category.equals("BREAKER")) {
//                    String command = config.getBreakerCommand().get("health");
//                    char stx = 0x02, etx = 0x03;
//                    String msg = String.format("%s%s%s", stx, command, etx);
//
//                    ByteBuf byteBuf = Unpooled.copiedBuffer(msg, Charset.forName("ASCII"));
//                    ctx.channel().writeAndFlush(byteBuf);
//
//                } else if (category.equals("DISPLAY")) {
//                    List<DisplayMessage.DisplayMessageInfo> messages;
//                    if (info.getFname().contains("입구")) {
//                        messages = config.getResetMessage().getIn();
//                    } else {
//                        messages = config.getResetMessage().getOut();
//                    }
//
//                    config.generateMessageList(messages).forEach(msg -> {
//                        ByteBuf bytebuf = Unpooled.copiedBuffer(msg, Charset.forName("euc-kr"));
//                        ctx.channel().writeAndFlush(bytebuf);
//                    });
//                }
//            }
        }
    }
}
