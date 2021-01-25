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
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityStatus;
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
    private static boolean RESTART;
    private final SimpMessagingTemplate webSocket;
    private final ObjectMapper objectMapper;
    private final ServerConfig config;
    private Timer healthCheckTimer = new Timer();

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

    // 채널이 끊겼을때 재 연결
    private void reconnect(String ip, int port) {
        if (!RESTART) {
            log.info(">>> {}:{} channel reconnect...!!!", ip, port);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    connect(ip, port);
                }
            }, 1000);
        }
    }

    // 시설물에 메세지 전송. (차단기, 전광판, 정산기)
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


    // 모든 채널의 상태를 리턴.
    public List<FacilityStatus> getChannelsStatus() {
        List<FacilityStatus> statusList = new ArrayList<>();
        channelMap.forEach((host, channel) -> {
            String facilityID = config.findFacilityInfoByHost(host).getFacilitiesId();
            String healthStatus = getHealthStatus(channel);
            statusList.add(new FacilityStatus(facilityID, healthStatus));
        });
        return statusList;
    }

    // 시설물 아이디와 일치하는 채널의 상태를 리턴
    public List<FacilityStatus> getChannelStatus(String facilityID) {
        String host = config.findByFacilitiesId(facilityID).generateHost();
        String healthStatus = getHealthStatus(channelMap.get(host));
        return Arrays.asList(new FacilityStatus(facilityID, healthStatus));
    }

    // 채널 상태값 리턴.
    private String getHealthStatus(Channel channel) {
        return channel.isActive()
                ? "NORMAL"
                : "ANNORMAL";
    }

    // 전체 채널을 담고 있는 맵을 리턴
    public static Map<String, Channel> getChannelMap() {
        return channelMap;
    }

    // 서버 리스타트 시 진행중인지 아닌지 상태 추후
    public static void setRESTART(boolean restart) {
        RESTART = restart;
    }


}
