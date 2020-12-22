package kr.co.glnt.relay.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Slf4j
public class RestTemplateLoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        request.getHeaders().set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        URI uri = request.getURI();
//        traceRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
//        traceResponse(response, uri);
        return response;
    }

    private void traceRequest(HttpRequest request, byte[] body) {
        StringBuilder requestLog = new StringBuilder();
        requestLog.append(">>> [REQUEST] ")
                .append("uri: ").append(request.getURI())
                .append(", body: ").append(new String(body, StandardCharsets.UTF_8));
        log.info(requestLog.toString());
    }

    private void traceResponse(ClientHttpResponse response, URI uri) throws IOException {
        StringBuilder responseLog = new StringBuilder();
        responseLog.append(">>> [RESPONSE] ")
                .append("uri: ").append(uri)
                .append(", body: ").append(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
        log.info(responseLog.toString());
    }
}
