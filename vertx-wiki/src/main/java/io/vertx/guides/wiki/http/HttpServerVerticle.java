package io.vertx.guides.wiki.http;

import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.codec.BodyCodec;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;
import io.vertx.guides.wiki.database.WikiDatabaseService;
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

  private WikiDatabaseService dbService;

  private WebClient webClient;

  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

  @Override
  public void start(Future<Void> startFuture) {

    wikiDbQueue = config().getString(CONFIG_WIKIDB_QUEUE, "wikidb.queue");

    dbService = WikiDatabaseService.createProxy(vertx, wikiDbQueue);

    webClient = WebClient
      .create(vertx, new WebClientOptions().setSsl(true).setUserAgent("vertx-x3"));

    HttpServer server = vertx.createHttpServer();

    Router router = Router.router(vertx);
    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderingHandler);
    router.get("/backup").handler(this::backupHandler);

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

  private void backupHandler(RoutingContext ctx) {
    dbService.fetchAllPagesData(reply -> {
      if (reply.failed()) {
        ctx.fail(reply.cause());
      } else {
        JsonObject filesObject = new JsonObject();
        JsonObject gistPayload = new JsonObject()
          .put("files", filesObject)
          .put("description", "A wiki backup")
          .put("public", true);

        reply.result()
          .forEach(page -> {
            JsonObject fileObject = new JsonObject();
            filesObject.put(page.getString("NAME"), fileObject);
            fileObject.put("content", page.getString("CONTENT"));
          });

        webClient.post(443, "api.github.com", "/gists")
          .putHeader("Accept", "application/vnd.github.v3+json")
          .putHeader("Content-Type", "application/json")
          .as(BodyCodec.jsonObject())
          .sendJsonObject(gistPayload, ar -> {
            if (ar.failed()) {
              ctx.fail(ar.cause());
            } else {
              HttpResponse<JsonObject> response = ar.result();
              if (response.statusCode() == 201) {
                ctx.put("backup_gist_url", response.body().getString("html_url"));
                indexHandler(ctx);
              } else {
                StringBuilder message = new StringBuilder()
                  .append("Could not backup the wiki: ")
                  .append(response.statusMessage());
                JsonObject body = response.body();
                if (body != null) {
                  message.append(System.getProperty("line.separator"))
                    .append(body.encodePrettily());
                }
                LOGGER.error(message.toString());
                ctx.fail(502);
              }
            }
          });
      }
    });
  }

  private void pageDeletionHandler(RoutingContext ctx) {
    String id = ctx.request().getParam("id");
    dbService.deletePage(Integer.valueOf(id), reply -> {
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

    Handler<AsyncResult<Void>> handler = reply -> {
      if (reply.succeeded()) {
        ctx.response().setStatusCode(303);
        ctx.response().putHeader("Location", "/wiki/" + title);
        ctx.response().end();
      } else {
        ctx.fail(reply.cause());
      }
    };

    String markdown = ctx.request().getParam("markdown");
    if ("yes".equals(ctx.request().getParam("newPage"))) {
      dbService.createPage(title, markdown, handler);
    } else {
      dbService.savePage(Integer.valueOf(ctx.request().getParam("id")), markdown, handler);
    }
  }

  private void pageRenderingHandler(RoutingContext ctx) {
    String requestedPage = ctx.request().getParam("page");
    dbService.fetchPage(requestedPage, reply -> {

      if (reply.succeeded()) {
        JsonObject body = reply.result();
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
    dbService.fetchAllPages(reply -> {
      if (reply.succeeded()) {
        ctx.put("title", "Wiki home");
        ctx.put("pages", reply.result());
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
