package kr.co.glnt.relay.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Slf4j
public class NgisLoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        HttpHeaders headers = request.getHeaders();
        headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        URI uri = request.getURI();
        traceRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        traceResponse(response, uri);

        return execution.execute(request, body);
    }

    private void traceRequest(HttpRequest request, byte[] body) {
        StringBuilder requestLog = new StringBuilder();
        requestLog.append(">>>> [REQUEST] ")
                .append("uri: ").append(request.getURI())
                .append(", body: ").append(new String(body, StandardCharsets.UTF_8));
        log.info(requestLog.toString());
    }

    private void traceResponse(ClientHttpResponse response, URI uri) throws IOException {
        StringBuilder responseLog = new StringBuilder();

        if (Objects.nonNull(response)) {
            responseLog.append(">>>> [RESPONSE] ")
                    .append("uri: ").append(uri)
                    .append(", body: ").append(StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8));
        }
        log.info(responseLog.toString());
    }
}
