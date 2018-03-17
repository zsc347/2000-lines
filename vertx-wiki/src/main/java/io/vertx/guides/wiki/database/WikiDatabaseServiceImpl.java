package io.vertx.guides.wiki.database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class WikiDatabaseServiceImpl implements WikiDatabaseService {

  private static final Logger LOGGER = LoggerFactory.getLogger(WikiDatabaseServiceImpl.class);
  private final HashMap<SqlQuery, String> sqlQueries;
  private final JDBCClient dbClient;

  public WikiDatabaseServiceImpl(JDBCClient dbClient, HashMap<SqlQuery, String> sqlQueries,
    Handler<AsyncResult<WikiDatabaseService>> readyHandler) {
    this.dbClient = dbClient;
    this.sqlQueries = sqlQueries;

    dbClient.getConnection(ar -> {
      if (ar.failed()) {
        LOGGER.error("Could not open a db connection", ar.cause());
        readyHandler.handle(Future.failedFuture(ar.cause()));
      } else {
        SQLConnection conn = ar.result();
        conn.execute(sqlQueries.get(SqlQuery.CREATE_PAGES_TABLE), create -> {
          if (create.failed()) {
            LOGGER.error("Create tables failed", create.cause());
            readyHandler.handle(Future.failedFuture(create.cause()));
          } else {
            readyHandler.handle(Future.succeededFuture(this));
          }
        });
      }
    });
  }

  @Override
  public WikiDatabaseService fetchAllPagesData(
    Handler<AsyncResult<List<JsonObject>>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.ALL_PAGES_DATA), queryResult -> {
      if (queryResult.succeeded()) {
        resultHandler.handle(Future.succeededFuture(queryResult.result().getRows()));
      } else {
        LOGGER.error("Database query error", queryResult.cause());
        resultHandler.handle(Future.failedFuture(queryResult.cause()));
      }
    });
    return this;
  }

  @Override
  public WikiDatabaseService fetchAllPages(Handler<AsyncResult<JsonArray>> resultHandler) {
    dbClient.query(sqlQueries.get(SqlQuery.ALL_PAGES), res -> {
      if (res.succeeded()) {
        List<String> pages = res.result()
          .getResults()
          .stream()
          .map(json -> json.getString(0))
          .sorted()
          .collect(Collectors.toList());
        resultHandler.handle(Future.succeededFuture(new JsonArray(pages)));
      } else {
        LOGGER.error("DB query error", res.cause());
        resultHandler.handle(Future.failedFuture(res.cause()));
      }
    });
    return this;
  }

  @Override
  public WikiDatabaseService fetchPage(String name,
    Handler<AsyncResult<JsonObject>> resultHandler) {
    JsonArray params = new JsonArray().add(name);
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.GET_PAGE), params, fetch -> {
      if (fetch.succeeded()) {
        JsonObject response = new JsonObject();
        ResultSet resultSet = fetch.result();
        if (resultSet.getNumRows() == 0) {
          response.put("found", false);
        } else {
          response.put("found", true);
          JsonArray row = resultSet.getResults().get(0);
          response.put("id", row.getInteger(0));
          response.put("rawContent", row.getString(1));
        }
        resultHandler.handle(Future.succeededFuture(response));
      } else {
        LOGGER.error("DB query error", fetch.cause());
        resultHandler.handle(Future.failedFuture(fetch.cause()));
      }
    });
    return this;
  }

  @Override
  public WikiDatabaseService createPage(String title, String markdown,
    Handler<AsyncResult<Void>> resultHandler) {
    JsonArray data = new JsonArray()
      .add(title)
      .add(markdown);

    dbClient.updateWithParams(sqlQueries.get(SqlQuery.CREATE_PAGE), data, create -> {
      if (create.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("DB query error", create.cause());
        resultHandler.handle(Future.failedFuture(create.cause()));
      }
    });
    return this;
  }

  @Override
  public WikiDatabaseService savePage(int id, String markdown,
    Handler<AsyncResult<Void>> resultHandler) {
    JsonArray data = new JsonArray()
      .add(markdown)
      .add(id);

    dbClient.updateWithParams(sqlQueries.get(SqlQuery.SAVE_PAGE), data, update -> {
      if (update.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("DB query error", update.cause());
        resultHandler.handle(Future.failedFuture(update.cause()));
      }
    });
    return this;
  }

  @Override
  public WikiDatabaseService deletePage(int id, Handler<AsyncResult<Void>> resultHandler) {
    JsonArray data = new JsonArray()
      .add(id);
    dbClient.queryWithParams(sqlQueries.get(SqlQuery.DELETE_PAGE), data, delete -> {
      if (delete.succeeded()) {
        resultHandler.handle(Future.succeededFuture());
      } else {
        LOGGER.error("DB query error", delete.cause());
        resultHandler.handle(Future.failedFuture(delete.cause()));
      }
    });
    return this;
  }
}
