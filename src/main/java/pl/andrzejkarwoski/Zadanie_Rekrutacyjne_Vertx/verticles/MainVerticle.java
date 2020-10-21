package pl.andrzejkarwoski.Zadanie_Rekrutacyjne_Vertx.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;


public class MainVerticle extends AbstractVerticle {



  @Override
  public void start(Promise<Void> promise) throws Exception {
    Promise<String> dbVerticleDeployment = Promise.promise();
    vertx.deployVerticle(new HttpServerVerticle(), dbVerticleDeployment);


    dbVerticleDeployment.future().compose(id -> {

      Promise<String> mongoVerticleDeployment = Promise.promise();
      vertx.deployVerticle(
        "pl.andrzejkarwoski.Zadanie_Rekrutacyjne_Vertx.verticles.MongoVerticle",
      new DeploymentOptions(),
        mongoVerticleDeployment);

      return mongoVerticleDeployment.future();

    }).setHandler(ar -> {
      if (ar.succeeded()) {
        promise.complete();
      } else {
        promise.fail(ar.cause());
      }
    });

  }




}
