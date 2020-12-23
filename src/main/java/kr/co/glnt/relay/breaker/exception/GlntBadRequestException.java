package kr.co.glnt.relay.breaker.exception;

public class GlntBadRequestException extends RuntimeException {
    public GlntBadRequestException(String msg) {
        super(msg);
    }

}
