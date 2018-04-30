package nl.jpoint.vertx.mod.deploy.util;

import io.vertx.rxjava.core.Vertx;
import nl.jpoint.vertx.mod.deploy.request.ModuleRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;
import rx.Subscriber;

import java.io.*;

import static rx.Observable.just;

public class ObservableCommand<R extends ModuleRequest> {

    private static final Logger LOG = LoggerFactory.getLogger(ObservableCommand.class);
    private static final Long POLLING_INTERVAL_IN_MS = 500L;
    private final Integer expectedResultCode;
    private final Vertx rxVertx;
    private final R request;
    private Process process;

    public ObservableCommand(R request, Integer expectedResultCode, Vertx vertx) {
        this.request = request;
        this.expectedResultCode = expectedResultCode;
        this.rxVertx = vertx;
    }

    public Observable<Integer> execute(ProcessBuilder builder) {
        return observableCommand(builder)
                .flatMap(x -> waitForExit())
                .flatMap(x -> {
                    if (process.exitValue() != expectedResultCode) {
                        throw new IllegalStateException("Error executing process");
                    }
                    return just(x);
                });
    }

    private Observable<Integer> waitForExit() {
        return rxVertx.timerStream(POLLING_INTERVAL_IN_MS).toObservable()
                .flatMap(x -> {
                    if (process.isAlive()) {
                        return waitForExit();
                    } else {
                        if (process.exitValue() != expectedResultCode) {
                            throw new IllegalStateException("Error while executing process");
                        }
                        return just(process.exitValue());
                    }
                });
    }

    private Observable<String> observableCommand(ProcessBuilder builder) {
        return Observable.create(subscriber -> {
            process = null;
            try {
                builder.directory(new File(System.getProperty("java.io.tmpdir")));
                process = builder.start();
            } catch (IOException e) {
                subscriber.onError(e);
            }

            if (process != null) {
                if (process.exitValue() == 0) {
                    printStream(process.getInputStream(), subscriber, false);
                } else {
                    throw new IllegalStateException(printStream(process.getErrorStream(), subscriber, true));
                }
            } else {
                subscriber.onError(new IllegalStateException("Error executing command"));
            }
            subscriber.onNext("Done");
            subscriber.onCompleted();
        });
    }

    private String printStream(InputStream stream, Subscriber subscriber, boolean error) {
        if (stream == null) {
            return null;
        }
        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            while ((line = reader.readLine()) != null) {
                if (error) {
                    LOG.error("[{} - {}]: Command output -> '{}'", LogConstants.CONSOLE_COMMAND, request.getId(), line);
                } else {
                    LOG.info("[{} - {}]: Command output -> '{}'", LogConstants.CONSOLE_COMMAND, request.getId(), line);
                }
            }
            return line;
        } catch (Exception e) {
            subscriber.onError(e);
            return e.getMessage();
        }
    }
}
