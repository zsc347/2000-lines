package io.vertx.guides.wiki;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.guides.wiki.database.HttpServerVerticle;
import io.vertx.guides.wiki.database.WikiDatabaseVerticle;

public class MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx.vertx().deployVerticle(new MainVerticle());
  }

  @Override
  public void start(Future<Void> startFuture) {
    Future<String> dbVerticle = Future.future();
    vertx.deployVerticle(new WikiDatabaseVerticle(), dbVerticle.completer());

    dbVerticle.compose(id -> {
      Future<String> httpVerticleDeploy = Future.future();
      vertx.deployVerticle(HttpServerVerticle.class, new DeploymentOptions().setInstances(2),
        httpVerticleDeploy.completer());
      return httpVerticleDeploy;
    }).setHandler(ar -> {
      if (ar.succeeded()) {
        startFuture.complete();
      } else {
        startFuture.fail(ar.cause());
      }
    });
  }
}
