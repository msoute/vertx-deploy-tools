package nl.jpoint.vertx.mod.deploy.command;

import rx.Observable;

public interface Command<T> {
    Observable<T> executeAsync(T request);
}
