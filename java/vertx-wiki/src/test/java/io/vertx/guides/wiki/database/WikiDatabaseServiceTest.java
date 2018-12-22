package io.vertx.guides.wiki.database;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class WikiDatabaseServiceTest {

  private Vertx vertx;
  private WikiDatabaseService dbService;

  @Before
  public void setUp(TestContext ctx) {
    vertx = Vertx.vertx();

    JsonObject conf = new JsonObject()
      .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_URL, "jdbc:hsqldb:mem:testdb;shutdown=true")
      .put(WikiDatabaseVerticle.CONFIG_WIKIDB_JDBC_MAX_POOL_SIZE, 4);

    vertx.deployVerticle(new WikiDatabaseVerticle(), new DeploymentOptions()
      .setConfig(conf), ctx.asyncAssertSuccess(id -> dbService = WikiDatabaseService
      .createProxy(vertx, WikiDatabaseVerticle.CONFIG_WIKIDB_QUEUE)));
  }

  @Test
  public void crudOperations(TestContext ctx) {
    Async async = ctx.async();
    dbService.createPage("Test", "Test content", ctx.asyncAssertSuccess(
      v1 -> dbService.fetchPage("Test", ctx.asyncAssertSuccess(json1 -> {
        ctx.assertTrue(json1.getBoolean("found"));
        ctx.assertTrue(json1.containsKey("id"));
        ctx.assertTrue("Test content".equals(json1.getString("rawContent")));

        dbService.savePage(json1.getInteger("id"), "modified content",
          ctx.asyncAssertSuccess(
            v2 -> dbService.fetchAllPages(ctx.asyncAssertSuccess(array1 -> {
              ctx.assertEquals(1, array1.size());

              dbService.fetchPage("Test", ctx.asyncAssertSuccess(json2 -> {
                ctx.assertEquals("modified content", json2.getString("rawContent"));

                dbService.deletePage(json2.getInteger("id"),
                  v3 -> dbService.fetchAllPages(ctx.asyncAssertSuccess(array2 -> {
                    ctx.assertTrue(array2.isEmpty());
                    async.complete();
                  })));
              }));
            }))));
      }))
    ));
    async.awaitSuccess(5000);
  }


  @Test
  public void startHttpServer(TestContext ctx) {
    Async async = ctx.async();

    vertx.createHttpServer().requestHandler(req -> {
      req.response().putHeader("Content-Type", "text/plain").end("Ok");
    }).listen(8080, ctx.asyncAssertSuccess(server -> {
      WebClient webClient = WebClient.create(vertx);
      webClient.get(8080, "localhost", "/").send(ar -> {
        if (ar.succeeded()) {
          HttpResponse<Buffer> response = ar.result();
          ctx.assertTrue(response.headers().contains("Content-Type"));
          ctx.assertEquals("text/plain", response.getHeader("Content-Type"));
          ctx.assertEquals("Ok", response.body().toString());
          webClient.close();
          async.complete();
        } else {
          async.resolve(Future.failedFuture(ar.cause()));
        }
      });
    }));

  }

  @After
  public void tearDown(TestContext tc) {
    vertx.close(tc.asyncAssertSuccess());
  }
}
