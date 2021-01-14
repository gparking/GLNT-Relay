package kr.co.glnt.relay.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import kr.co.glnt.relay.config.ServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@Component
public class GlntNettyClient {
    private NioEventLoopGroup loopGroup;
    private static Map<String, Channel> channelMap = new LinkedHashMap<>();

    private final SimpMessagingTemplate webSocket;
    private final ObjectMapper objectMapper;
    private final ServerConfig config;

    public GlntNettyClient(SimpMessagingTemplate webSocket, ObjectMapper objectMapper, ServerConfig serverConfig) {
        this.webSocket = webSocket;
        this.objectMapper = objectMapper;
        this.config = serverConfig;
    }

    public void setFeatureCount(int featureCount) {
        if (Objects.isNull(loopGroup)) {
            loopGroup = new NioEventLoopGroup(featureCount);
        }
    }

    public void connect(final String host, final int port) {
        Bootstrap bootstrap = new Bootstrap();
        try {
            bootstrap.group(loopGroup).channel(NioSocketChannel.class)
                    .remoteAddress(host, port)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new GlntNettyHandler(webSocket, objectMapper, config));
                        }
                    });
            ChannelFuture channelFuture = bootstrap.connect().sync();
            channelFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (!future.isSuccess()) {
                        future.channel().close();
                        bootstrap.connect(host, port).addListener(this);
                    } else {
                        addCloseDetectListener(future.channel());
                    }
                }

                private void addCloseDetectListener(Channel channel) {
                    channel.closeFuture().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            reconnect(host, port);
                        }
                    });
                }
            });

            channelMap.put(String.format("%s:%d", host, port), channelFuture.channel());
        } catch (Exception e) {
            reconnect(host, port);
        }
    }

    private void reconnect(String ip, int port) {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                connect(ip, port);
            }
        }, 1000);
    }

    public void sendMessage(String host, String msg, Charset charset) {
        log.info("send msg : {}", msg);
        if (!channelMap.containsKey(host)) {
            log.info("없는 시설물 아이디: {}", host);
            return;
        }
        ByteBuf byteBuf = Unpooled.copiedBuffer(msg, charset);
        Channel channel = channelMap.get(host);
        channel.writeAndFlush(byteBuf);
    }


    public static Map<String, Channel> getChannelMap() {
        return channelMap;
    }


}
