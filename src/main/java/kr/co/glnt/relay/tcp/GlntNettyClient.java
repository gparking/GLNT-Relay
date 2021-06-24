package kr.co.glnt.relay.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityStatus;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@DependsOn("gpmsAPI")
@Component
public class GlntNettyClient {
    private NioEventLoopGroup loopGroup;
    private final ObjectMapper objectMapper;
    private final ServerConfig config;
    private static Map<String, Channel> channelMap = new LinkedHashMap<>();
    private final GpmsAPI gpmsAPI;
    private Bootstrap bootstrap;
    private List<FacilityInfo> connectionList;


    public GlntNettyClient(ObjectMapper objectMapper, ServerConfig serverConfig, GpmsAPI gpmsAPI) {
        this.objectMapper = objectMapper;
        this.config = serverConfig;
        this.gpmsAPI = gpmsAPI;
    }

    public void setConnectionList(List<FacilityInfo> list) {
        this.connectionList = list;
        initBootstrap();
    }

    public void setFeatureCount(int featureCount) {
        if (Objects.isNull(loopGroup)) {
            loopGroup = new NioEventLoopGroup(featureCount);
        }
    }

    public void initBootstrap() {
        NioEventLoopGroup nioEventLoopGroup = new NioEventLoopGroup(connectionList.size());
        bootstrap = new Bootstrap();
        bootstrap.group(nioEventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .option(ChannelOption.SO_LINGER, 0)     // 소켓을 close 했을 때 전송되지 않은 데이터를 무시
                .option(ChannelOption.SO_KEEPALIVE, true) // keepalive 패킷 전송 활성화
                .option(ChannelOption.TCP_NODELAY, true); // Nagel 끄기
    }


    public void doConnect(String ip, int port) {
        try {
            String host = String.format("%s:%d", ip, port);
            if (channelMap.containsKey(host)) {
                Channel channel = channelMap.get(host);
                if (channel.isActive()) {
                    return;
                }
            }

            ChannelFuture f = bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel socketChannel) throws Exception {
                    ChannelPipeline pipeline = socketChannel.pipeline();
                    pipeline.addLast("idleStateHandler", new IdleStateHandler(15, 0, 0));
                    pipeline.addLast("glntIdleHandler", new GlntIdleHandler(config));
                    pipeline.addLast(new GlntNettyHandler(objectMapper, config, gpmsAPI));
                }
            }).connect(ip, port).sync();

            f.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    if (!channelFuture.isSuccess()) {
                        channelFuture.channel().close();
                        bootstrap.connect(ip, port).addListener(this);
                    } else {
                        addCloseDetectListener(channelFuture.channel());
                    }
                }
                private void addCloseDetectListener(Channel channel) {
                    channel.closeFuture().addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            reconnect(ip, port);
                        }
                    });
                }
            });

            channelMap.put(host, f.channel());

        } catch (Exception e) {
            log.error(">>>> connection err: {}", e.getMessage());
            reconnect(ip, port);
        }
    }

    @Async
    public void connect() {
        for (FacilityInfo info : connectionList) {
            doConnect(info.getIp(), info.getPort());
        }
    }

    // 채널이 끊겼을때 재 연결
    private void reconnect(String ip, int port) {
        FacilityInfo facilityInfo = config.findFacilityInfoByHost(String.format("%s:%d", ip, port));
        log.info(">>>> {}({}) 재연결 시도", facilityInfo.getFname(), facilityInfo.getDtFacilitiesId());
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
//                connect(ip, port);
                doConnect(ip, port);
            }
        }, 1000);
    }

    // 시설물에 메세지 전송. (차단기, 전광판, 정산기)
    public void sendMessage(String host, String msg, Charset charset) {
        if (!channelMap.containsKey(host)) {
            log.info("<!> channel is not found {}", host);
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

            FacilityStatus facilityStatus = FacilityStatus.deviceHealth(facilityID, healthStatus);

            statusList.add(facilityStatus);
        });
        return statusList;
    }

    // 시설물 아이디와 일치하는 채널의 상태를 리턴
    public List<FacilityStatus> getChannelStatus(String facilityID) {
        String host = config.findByFacilitiesId("연결 확인", facilityID).generateHost();
        String healthStatus = getHealthStatus(channelMap.get(host));

        FacilityStatus facilityStatus = FacilityStatus.deviceHealth(facilityID, healthStatus);

        return Arrays.asList(facilityStatus);
    }

    public List<FacilityStatus> getFullLprStatus() {
        List<FacilityStatus> statusList = new ArrayList<>();
        config.findLprList().stream().forEach(info -> {
            if (Objects.nonNull(info.getFacilitiesId()) && !info.getFacilitiesId().isEmpty()) {
                String healthStatus = sendPing(info.getIp());
                statusList.add(FacilityStatus.deviceHealth(info.getFacilitiesId(), healthStatus));
            }
        });
        return statusList;
    }

    public FacilityStatus getLrpStatus(String facilityID) {
        FacilityInfo info = config.findByFacilitiesId("LPR 확인", facilityID);
        String healthStatus = sendPing(info.getIp());
        return FacilityStatus.deviceHealth(info.getFacilitiesId(), healthStatus);
    }


    // 특정 IP에 Ping 신호 보내기
    private String sendPing(String ip) {
        boolean isActive = false;
        try {
            isActive = InetAddress.getByName(ip).isReachable(1000);
        } catch (IOException e) {
            log.error("<!> ping 보내기 실패: ", e.getMessage());
        }
        return isActive
                ? "NORMAL"
                : "ERROR";
    }


    // 채널 상태값 리턴.
    private String getHealthStatus(Channel channel) {
        return channel.isActive()
                ? "NORMAL"
                : "ERROR";
    }


    // 전체 채널을 담고 있는 맵을 리턴
    public static Map<String, Channel> getChannelMap() {
        return channelMap;
    }

}
