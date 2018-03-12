package io.vertx.starter;

import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;
import java.util.Date;

public class HttpServerVerticle extends AbstractVerticle {

  private static final String EMPTY_PAGE_MARKDOWN =
    "# A new page\n" +
      "\n" +
      "Feel-free to write in Markdown!\n";

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  public static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";
  public static final String CONFIG_WIKIDB_QUEUE = "wikidb.queue";

  private String wikiDbQueue = "wikidb.queue";

  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

  @Override
  public void start(Future<Void> startFuture) throws Exception {

    wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");

    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);

    router.post().handler(BodyHandler.create());
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/create").handler(this::pageCreateHandler);
    router.post("/delete").handler(this::pageDeletionHandler);

    int portNumber = config().getInteger(CONFIG_HTTP_SERVER_PORT, 8080);
    server
      .requestHandler(router::accept)
      .listen(portNumber, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port " + portNumber);
          startFuture.complete();
        } else {
          LOGGER.error("Could not start a HTTP server", ar.cause());
          startFuture.fail(ar.cause());
        }
      });
  }

  private void pageDeletionHandler(RoutingContext ctx) {
    String id = ctx.request().getParam("id");
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "delete-page");
    JsonObject request = new JsonObject().put("id", id);

    vertx.eventBus().send(wikiDbQueue, request, options, reply -> {
      if (reply.succeeded()) {
        ctx.response().setStatusCode(303);
        ctx.response().putHeader("Location", "/");
        ctx.response().end();
      } else {
        ctx.fail(reply.cause());
      }
    });
  }

  private void pageCreateHandler(RoutingContext ctx) {
    String pageName = ctx.request().getParam("name");
    String location = "/wiki/" + pageName;
    if (pageName == null || pageName.isEmpty()) {
      location = "/";
    }
    ctx.response().setStatusCode(303);
    ctx.response().putHeader("Location", location);
    ctx.response().end();
  }

  private void pageUpdateHandler(RoutingContext ctx) {
    String title = ctx.request().getParam("title");
    JsonObject request = new JsonObject()
      .put("id", ctx.request().getParam("id"))
      .put("title", title)
      .put("markdown", ctx.request().getParam("markdown"));
    DeliveryOptions options = new DeliveryOptions();
    if ("yes".equals(ctx.request().getParam("newPage"))) {
      options.addHeader("action", "create-page");
    } else {
      options.addHeader("action", "save-page");
    }
    vertx.eventBus().send(wikiDbQueue, request, options, reply -> {
      if (reply.succeeded()) {
        ctx.response().setStatusCode(303);
        ctx.response().putHeader("Location", "wiki/" + title);
        ctx.response().end();
      } else {
        ctx.fail(reply.cause());
      }
    });
  }

  private void pageRenderingHandler(RoutingContext ctx) {
    String requestedPage = ctx.request().getParam("page");
    JsonObject request = new JsonObject().put("page", requestedPage);

    DeliveryOptions options = new DeliveryOptions().addHeader("action", "get-page");
    vertx.eventBus().send(wikiDbQueue, request, options, reply -> {

      if (reply.succeeded()) {
        JsonObject body = (JsonObject) reply.result().body();

        boolean found = body.getBoolean("found");
        String rawContent = body.getString("rawContent", EMPTY_PAGE_MARKDOWN);
        ctx.put("title", requestedPage);
        ctx.put("id", body.getInteger("id", -1));
        ctx.put("newPage", found ? "no" : "yes");
        ctx.put("rawContent", rawContent);
        ctx.put("content", Processor.process(rawContent));
        ctx.put("timestamp", new Date().toString());

        templateEngine.render(ctx, "templates", "/page.ftl", ar -> {
          if (ar.succeeded()) {
            ctx.response().putHeader("Content-Type", "text/html");
            ctx.response().end(ar.result());
          } else {
            ctx.fail(ar.cause());
          }
        });

      } else {
        ctx.fail(reply.cause());
      }
    });
  }

  private void indexHandler(RoutingContext ctx) {
    DeliveryOptions options = new DeliveryOptions().addHeader("action", "all-pages");
    vertx.eventBus().send(wikiDbQueue, new JsonObject(), options, reply -> {
      if (reply.succeeded()) {
        JsonObject body = (JsonObject) reply.result().body();
        ctx.put("title", "Wiki home");
        ctx.put("pages", body.getJsonArray("pages").getList());
        templateEngine.render(ctx, "templates", "/index.ftl", ar -> {
          if (ar.succeeded()) {
            ctx.response().putHeader("Content-Type", "text/html");
            ctx.response().end(ar.result());
          } else {
            ctx.fail(ar.cause());
          }
        });
      } else {
        ctx.fail(reply.cause());
      }
    });
  }
}
