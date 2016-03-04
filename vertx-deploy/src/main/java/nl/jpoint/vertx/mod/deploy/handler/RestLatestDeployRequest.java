package nl.jpoint.vertx.mod.deploy.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.ext.web.RoutingContext;
import nl.jpoint.vertx.mod.deploy.request.DeployRequest;
import nl.jpoint.vertx.mod.deploy.service.AwsService;

import java.util.Optional;

public class RestLatestDeployRequest implements Handler<RoutingContext> {

    private final Optional<AwsService> awsService;

    public RestLatestDeployRequest(AwsService awsService) {
        this.awsService = Optional.ofNullable(awsService);
    }

    @Override
    public void handle(RoutingContext event) {
        if (awsService.isPresent()) {
            ObjectWriter writer = new ObjectMapper().writerFor(DeployRequest.class);
            Buffer buffer = Buffer.buffer();
            try {
                buffer.appendBytes(writer.writeValueAsBytes(awsService.get().getLatestDeployRequest()));
                event.response().end(buffer);
            } catch (JsonProcessingException e) {
                event.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code());
                event.response().end();
            }
        } else {
            event.response().setStatusCode(HttpResponseStatus.NO_CONTENT.code());
            event.response().end();
        }
    }
}
