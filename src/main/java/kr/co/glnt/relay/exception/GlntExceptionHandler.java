package kr.co.glnt.relay.exception;

import kr.co.glnt.relay.dto.ResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@Slf4j
@RestControllerAdvice
public class GlntExceptionHandler extends ResponseEntityExceptionHandler {


    @ExceptionHandler(GlntBadRequestException.class)
    public ResponseEntity<ResponseDTO> error400(GlntBadRequestException e) {
        return ResponseEntity
                .badRequest()
                .body(new ResponseDTO(HttpStatus.BAD_REQUEST, e.getMessage()));
    }


    /**
     * 404 에러처리
     */
    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers, HttpStatus status, WebRequest request) {
        ResponseDTO response = new ResponseDTO(HttpStatus.NOT_FOUND, "잘못된 경로입니다.");
        return ResponseEntity
                .status(404)
                .body(response);
    }
}
