package nl.jpoint.vertx.mod.deploy.util;

import io.vertx.core.http.HttpClientOptions;
import io.vertx.rxjava.core.Vertx;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import nl.jpoint.vertx.mod.deploy.DeployConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Observer;

import java.net.URI;
import java.util.UUID;
import java.util.function.Consumer;

public class RxHttpUtil {

    private final Logger LOG = LoggerFactory.getLogger(RxHttpUtil.class);

    private final Vertx rxVertx;
    private final DeployConfig config;
    private final HttpClient httpClient;

    public RxHttpUtil(Vertx rxVertx, DeployConfig config) {
        this.rxVertx = rxVertx;
        this.config = config;
        HttpClientOptions options = new HttpClientOptions().setSsl(config.getNexusUrl().getScheme().equals("https")).setVerifyHost(true);
        httpClient = rxVertx.createHttpClient(options);

    }

    public Observable<HttpClientResponse> get(UUID id, URI location, String filename) {
        return executeGet(httpClient.getAbs(location.toString()), HttpClientRequest::end, filename)
                .retry(3)
                .doOnError(t -> LOG.error("[{}]: Error downloading file {} from location {}, {}", id.toString(), filename, location.toString(), t.getMessage()));
    }

    private Observable<HttpClientResponse> executeGet(HttpClientRequest request,
                                                      Consumer<HttpClientRequest> requestFiller, String filename) {
        return Observable.create(onsubscribe -> {
            request.handler(response -> responseHandler(response, onsubscribe, filename))
                    .exceptionHandler(onsubscribe::onError);
            if (config.isHttpAuthentication()) {
                setAuthorizationHeader(request);
            }
            requestFiller.accept(request);

        });
    }

    private void responseHandler(HttpClientResponse response,
                                 Observer<? super HttpClientResponse> subscriber, String filename) {
        response.exceptionHandler(subscriber::onError);
        response.bodyHandler(bodyBuffer -> {
            //TODO Test observable write.
            //rxVertx.fileSystem().writeFileObservable(filename, bodyBuffer).map(x -> response).subscribe();
            rxVertx.fileSystem().writeFileBlocking(filename, bodyBuffer);
            subscriber.onNext(response);
            subscriber.onCompleted();
        });
    }

    private void setAuthorizationHeader(HttpClientRequest httpClientRequest) {
        String usernameAndPassword = config.getHttpAuthUser() + ":" + config.getHttpAuthPassword();
        String authorizationHeaderName = "Authorization";
        String authorizationHeaderValue = "Basic " + java.util.Base64.getEncoder().encodeToString(usernameAndPassword.getBytes());
        httpClientRequest.putHeader(authorizationHeaderName, authorizationHeaderValue);
    }
}
