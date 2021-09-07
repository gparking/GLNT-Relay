package kr.co.glnt.relay.tcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;
import kr.co.glnt.relay.config.ServerConfig;
import kr.co.glnt.relay.dto.FacilityInfo;
import kr.co.glnt.relay.dto.FacilityPayloadWrapper;
import kr.co.glnt.relay.dto.FacilityStatus;
import kr.co.glnt.relay.service.BreakerService;
import kr.co.glnt.relay.service.DisplayService;
import kr.co.glnt.relay.service.PaymentService;
import kr.co.glnt.relay.web.GpmsAPI;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.*;

@Slf4j
@ChannelHandler.Sharable
public class GlntNettyHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private final ServerConfig config;
    private final ObjectMapper objectMapper;
    private final GpmsAPI gpmsAPI;
    private final DisplayService displayService;
    private final BreakerService breakerService;
    private final  PaymentService paymentService;

    public GlntNettyHandler(ObjectMapper objectMapper, ServerConfig config, GpmsAPI gpmsAPI, DisplayService displayService, BreakerService breakerService, PaymentService paymentService) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.gpmsAPI = gpmsAPI;
        this.displayService = displayService;
        this.breakerService = breakerService;
        this.paymentService = paymentService;
    }

    // TCP 연결 성공
    // GPMS 에 연결 메세지 전송.
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        FacilityInfo facilityInfo = config.findFacilityInfoByHost(ctx.channel().remoteAddress().toString().substring(1));
        log.info(">>>> {}({}) 연결 성공", facilityInfo.getFname(), facilityInfo.getDtFacilitiesId());
        gpmsAPI.sendFacilityHealth(FacilityPayloadWrapper.healthCheckPayload(
                Arrays.asList(FacilityStatus.reconnect(facilityInfo.getDtFacilitiesId()))
        ));

        // 전광판이 연결 성공시 리셋 메세지 전송.
        if (facilityInfo.getCategory().equals("DISPLAY"))
            displayService.displayActive(ctx,facilityInfo);

        if (facilityInfo.getCategory().equals("BREAKER")) {
            breakerService.breakerActive(ctx);
        }

        // 재연결시 barStatus 가 lock 일 경우 다시 lock 상태로 변경.
//        if (facilityInfo.getBarStatus().equals("GATE UNLOCK OK")) {
//            char stx = 0x02, etx = 0x03;
//            String msg = String.format("%s%s%s", stx, "GATE UPLOCK", etx);
//            ByteBuf byteBuf = Unpooled.copiedBuffer(msg, Charset.forName("ASCII"));
//            ctx.channel().writeAndFlush(byteBuf);
//        }
    }


    // 연결 종료
    // GPMS 에 연결 종료 메세지 전송.
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        FacilityInfo facilityInfo = config.findFacilityInfoByHost(ctx.channel().remoteAddress().toString().substring(1));

        gpmsAPI.sendFacilityHealth(FacilityPayloadWrapper.healthCheckPayload(

                Arrays.asList(FacilityStatus.deviceDisconnect(facilityInfo.getDtFacilitiesId()))
        ));
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        String host = ctx.channel().remoteAddress().toString().substring(1);

        FacilityInfo facilityInfo = config.findFacilityInfoByHost(host);

        String message = byteBufToString(msg);

        switch (facilityInfo.getCategory()){
            case "BREAKER":
                breakerService.breakerProcessing(ctx.channel(),facilityInfo,message);
                break;
            case "PAYSTATION":
                paymentService.paymentProcessing(facilityInfo,message);
                break;
            default:

        }
        /**
        // 전광판 헬스체크 메세지
        if (message.startsWith("![0080")) {
            return;
        }*/

        // myService.startProcessing(ctx.channel(),facilityInfo,message);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("<!> TCP ERR remoteAddress : {}, message: {}", ctx.channel().remoteAddress(), cause.getMessage());
    }

    // Netty ByteBuf convert to string
    private String byteBufToString(ByteBuf byteBuf) {
        ByteBuf readbyte = byteBuf;

        int length = readbyte.readableBytes();

        byte[] read = new byte[length];

        for(int i=0; i<length; i++) {

            read[i] = readbyte.getByte(i);

        }

        return new String(read, CharsetUtil.UTF_8);

    }



}
