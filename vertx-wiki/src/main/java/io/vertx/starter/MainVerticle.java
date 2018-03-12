package io.vertx.starter;

import com.github.rjeschke.txtmark.Processor;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.templ.FreeMarkerTemplateEngine;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private static final String EMPTY_PAGE_MARKDOWN =
    "# A new page\n" +
      "\n" +
      "Feel-free to write in Markdown!\n";
  private static Vertx vertx;
  private JDBCClient dbClient;

  private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
  private final FreeMarkerTemplateEngine templateEngine = FreeMarkerTemplateEngine.create();

  public static void main(String[] args) {
    vertx = Vertx.vertx();
    vertx.deployVerticle(MainVerticle.class.getName());
  }

  @Override
  public void start(Future<Void> startFuture) {
    Future<Void> steps = prepareDatabase().compose(v -> startHttpServer());
    steps.setHandler(startFuture.completer());
  }

  private Future<Void> prepareDatabase() {
    Future<Void> future = Future.future();

    dbClient = JDBCClient.createShared(vertx, new JsonObject()
      .put("url", "jdbc:hsqldb:file:db/wiki")
      .put("driver_class", "org.hsqldb.jdbcDriver")
      .put("max_pool_size", 30));

    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        LOGGER.error("Could not open a database connection", ar.cause());
        future.fail(ar.cause());
      } else {
        SQLConnection connection = ar.result();
        connection.execute(DBStatement.SQL_CREATE_PAGES_TABLE, create -> {
          connection.close();
          if (create.failed()) {
            LOGGER.error("Database preparation error", create.cause());
            future.fail(create.cause());
          } else {
            future.complete();
          }
        });
      }
    });

    return future;
  }

  private Future<Void> startHttpServer() {
    Future<Void> future = Future.future();
    HttpServer server = vertx.createHttpServer();
    Router router = Router.router(vertx);
    router.get("/").handler(this::indexHandler);
    router.get("/wiki/:page").handler(this::pageRenderHandler);

    router.post().handler(BodyHandler.create());
    router.post("/save").handler(this::pageUpdateHandler);
    router.post("/create").handler(this::pageCreateHandler);
    router.post("/delete").handler(this::pageDeleteHandler);

    router.post().handler(BodyHandler.create());

    server
      .requestHandler(router::accept)
      .listen(8080, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP Server running on 8080");
          future.complete();
        } else {
          LOGGER.info("Could not start a HTTP Server");
          future.fail(ar.cause());
        }
      });
    return future;
  }

  private void pageUpdateHandler(RoutingContext ctx) {
    String id = ctx.request().getParam("id");
    String title = ctx.request().getParam("title");
    String mdContent = ctx.request().getParam("markdown");
    boolean newPage = "yes".equals(ctx.request().getParam("newPage"));

    dbClient.getConnection(car -> {
      if (car.succeeded()) {
        SQLConnection conn = car.result();
        String sql = newPage ? DBStatement.SQL_CREATE_PAGE : DBStatement.SQL_SAVE_PAGE;
        JsonArray params = new JsonArray();
        if (newPage) {
          params.add(title).add(mdContent);
        } else {
          params.add(mdContent).add(id);
        }
        conn.updateWithParams(sql, params, res -> {
          conn.close();
          if (res.succeeded()) {
            ctx.response().setStatusCode(303);
            ctx.response().putHeader("Location", "/wiki/" + title);
            ctx.response().end();
          } else {
            ctx.fail(res.cause());
          }
        });
      } else {
        ctx.fail(car.cause());
      }
    });
  }

  private void indexHandler(RoutingContext context) {
    dbClient.getConnection(car -> {
      if (car.succeeded()) {
        SQLConnection connection = car.result();
        connection.query(DBStatement.SQL_ALL_PAGES, res -> {
          connection.close();

          if (res.succeeded()) {
            List<String> pages = res.result()
              .getResults()
              .stream()
              .map(json -> json.getString(0))
              .sorted()
              .collect(Collectors.toList());

            context.put("title", "Wiki home");
            context.put("pages", pages);
            templateEngine.render(context, "templates", "/index.ftl", ar -> {
              if (ar.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().end(ar.result());
              } else {
                context.fail(ar.cause());
              }
            });

          } else {
            context.fail(res.cause());
          }
        });
      } else {
        context.fail(car.cause());
      }
    });
  }


  private void pageRenderHandler(RoutingContext ctx) {
    String page = ctx.request().getParam("page");
    dbClient.getConnection(car -> {
      if (car.succeeded()) {
        SQLConnection conn = car.result();
        conn.queryWithParams(DBStatement.SQL_GET_PAGE, new JsonArray().add(page), fetch -> {
          conn.close();
          if (fetch.succeeded()) {
            JsonArray row = fetch.result().getResults()
              .stream()
              .findFirst().orElseGet(() -> new JsonArray().add(-1).add(EMPTY_PAGE_MARKDOWN));
            Integer id = row.getInteger(0);
            String rawContent = row.getString(1);
            ctx.put("title", page);
            ctx.put("id", id);
            ctx.put("newPage", fetch.result().getResults().size() == 0 ? "yes" : "no");
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
            ctx.fail(car.cause());
          }
        });
      } else {
        ctx.fail(car.cause());
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

  private void pageDeleteHandler(RoutingContext ctx) {
    String id = ctx.request().getParam("id");
    dbClient.getConnection(car -> {
      if (car.succeeded()) {
        SQLConnection conn = car.result();
        conn.updateWithParams(DBStatement.SQL_DELETE_PAGE, new JsonArray().add(id), res -> {
          if (res.succeeded()) {
            conn.close();
            ctx.response().setStatusCode(303);
            ctx.response().putHeader("Location", "/");
            ctx.response().end();
          } else {
            ctx.fail(car.cause());
          }
        });
      } else {
        ctx.fail(car.cause());
      }
    });
  }
}
