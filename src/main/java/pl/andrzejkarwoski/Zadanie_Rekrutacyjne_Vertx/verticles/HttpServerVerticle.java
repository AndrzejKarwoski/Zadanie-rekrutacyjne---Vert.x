package pl.andrzejkarwoski.Zadanie_Rekrutacyjne_Vertx.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.PubSecKeyOptions;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.auth.jwt.JWTAuthOptions;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.JWTAuthHandler;



public class HttpServerVerticle extends AbstractVerticle {

  private static final Logger LOGGER = LoggerFactory.getLogger(HttpServerVerticle.class);

  JWTAuth jwt;
  private String mongodbQueue = "mongo.queue";

  @Override
  public void start(Promise<Void> promise) throws Exception {
    HttpServer server = vertx.createHttpServer();
    preapareJWT();
    Router router = Router.router(vertx);
    router.post("/register").handler(this::register);
    router.post("/login").handler(this::login);
    router.route("/items").handler(JWTAuthHandler.create(jwt));
    router.post("/items").handler(this::addItem);
    router.get("/items").handler(this::getItems);




    server
      .requestHandler(router)
      .listen(3000, ar -> {
        if (ar.succeeded()) {
          LOGGER.info("HTTP server running on port " + 3000);
          promise.complete();
        } else {
          LOGGER.error("Could not start a HTTP server", ar.cause());
          promise.fail(ar.cause());
        }
      });
  }

  private void login(RoutingContext routingContext) {
    routingContext.request().bodyHandler(bodyHandler -> {
      JsonObject json = bodyHandler.toJsonObject();
      DeliveryOptions options = new DeliveryOptions();
      options.addHeader("action","login-user");
      vertx.eventBus().request(mongodbQueue, json, options, reply -> {
        if (reply.succeeded()) {
          routingContext.response().setStatusCode(200).end(new JsonObject().put("token",generateToken(json.getString("login"))).toString());
        } else {
          if(reply.cause().getMessage().equals("Bad credentials")){
            routingContext.response().setStatusCode(400).end("Bad credentials");
          }
          else routingContext.fail(reply.cause());
        }
      });
    });
  }

  private void register(RoutingContext routingContext) {
    routingContext.request().bodyHandler(bodyHandler -> {
      JsonObject json = bodyHandler.toJsonObject();
      DeliveryOptions options = new DeliveryOptions();
      options.addHeader("action","register-new-user");
      vertx.eventBus().request(mongodbQueue, json, options, reply -> {
        if (reply.succeeded()) {
          routingContext.response().setStatusCode(204).end();
        } else {
          if(reply.cause().getMessage().equals("Bad parameter")){
            routingContext.response().setStatusCode(400).end("Bad parameter");
          }
          else if(reply.cause().getMessage().equals("There is already user with given login")) {
            routingContext.response().setStatusCode(400).end("There is already user with given login");
          }
          else {
            routingContext.response().setStatusCode(500).end();
          }
        }
      });
    });
  }


  private void addItem(RoutingContext routingContext) {
    routingContext.request().bodyHandler(bodyHandler -> {
      JsonObject json = bodyHandler.toJsonObject();
      json.put("login", routingContext.user().principal().getString("login"));
      DeliveryOptions options = new DeliveryOptions();
      options.addHeader("action","add-item");
      vertx.eventBus().request(mongodbQueue, json, options, reply -> {
        if (reply.succeeded()) {
          routingContext.response().setStatusCode(204).end();
        } else {
          if(reply.cause().getMessage().equals("Bad request")){
            routingContext.response().setStatusCode(400).end("Bad request");
          }
          else {
            routingContext.response().setStatusCode(500).end();
          }
        }
      });
    });
  }



  private void getItems(RoutingContext routingContext) {
    routingContext.request().bodyHandler(bodyHandler -> {
      JsonObject json = new JsonObject();
      json.put("login", routingContext.user().principal().getString("login"));
      DeliveryOptions options = new DeliveryOptions();
      options.addHeader("action","get-items");
      vertx.eventBus().request(mongodbQueue, json, options, reply -> {
        if (reply.succeeded()) {
          routingContext.response().setStatusCode(200).end(reply.result().body().toString());
        } else {
          if(reply.cause().getMessage().equals("Bad request")){
            routingContext.response().setStatusCode(400).end("Bad request");
          }
          else {
            routingContext.response().setStatusCode(500).end();
          }
        }
      });
    });
  }

  private void preapareJWT(){
    jwt = JWTAuth.create(vertx, new JWTAuthOptions()
      .addPubSecKey(new PubSecKeyOptions()
        .setAlgorithm("RS256")
        .setPublicKey(
          "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA2v1dQXBi2cgEBXnFp/sJ\n" +
            "XJzIBq3oRPknmZI2hKhDYQvCGlTnbU6c4Sl0jDYVuzEq0/nkG17PcIJeTbbk2WW5\n" +
            "7O+s6KQqZwTg3FJ8bj/XViCofj84JImcT+y6l1iLLXzhiGcwnxmt0CqUNTwbdXFl\n" +
            "93hPEZvpkza4nD5OhIpDJP05WoWtDbbag6h3aF5G+VY/kZJ2Rj73V0kvXGk5PqVm\n" +
            "ldhqj25XnDYKJSw1trV/D4bv027oF3NHFEwiV62n88YajlZgm8yg8Ltg/uT87K9m\n" +
            "q9t6o3lhKH1dLknoCCuzampgw7HA6H8LPHHu00dmDStPR2h8sdA9A0K1vjyLKnw3\n" +
            "sQIDAQAB")
        .setSecretKey(
          "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDa/V1BcGLZyAQF\n" +
            "ecWn+wlcnMgGrehE+SeZkjaEqENhC8IaVOdtTpzhKXSMNhW7MSrT+eQbXs9wgl5N\n" +
            "tuTZZbns76zopCpnBODcUnxuP9dWIKh+PzgkiZxP7LqXWIstfOGIZzCfGa3QKpQ1\n" +
            "PBt1cWX3eE8Rm+mTNricPk6EikMk/Tlaha0NttqDqHdoXkb5Vj+RknZGPvdXSS9c\n" +
            "aTk+pWaV2GqPblecNgolLDW2tX8Phu/TbugXc0cUTCJXrafzxhqOVmCbzKDwu2D+\n" +
            "5Pzsr2ar23qjeWEofV0uSegIK7NqamDDscDofws8ce7TR2YNK09HaHyx0D0DQrW+\n" +
            "PIsqfDexAgMBAAECggEALWFlyuLMSU8urifO8wGNrhh7Rw8Q3AI515Q6IgFwfRLf\n" +
            "tW80yi0JS4u4sDmhBTqqImVdhOJ/4FPT0Qz+naJ8BgKg9VDXzKNxKZcaXAp0yE9a\n" +
            "O1dEfiXaM6HeBPD/XnzLi+W+aKn8n+/C6Mk883h9ZewTjpWdsLkdRpuOz96LDYXe\n" +
            "CrIyBuplKzuZOtAmJFbq9MLNmPHqYsJdWyzl9AkKFKxKuUg94elaFZmq57XltUVz\n" +
            "8rL6/hlFzEwq8vz8AjcuHitqDf6YWnIIJgu/VI7xkrTXuBzxcFKtbTrD9NZ599sB\n" +
            "1MCKXgoJiEEJjjs0X3XWAE6vj9Tb9ye+jANPfYrGsQKBgQDzPbHAvZUZiYxjwTMT\n" +
            "j2hKZIbOyRvrEPM1Nl4jWD7eaCyDEENl1zXr4QFan1H6yYZwZbfD8CHJkF9xt7bq\n" +
            "r2YBPA0INgChuLdVpwjf2JXxyRIE24w/a0vA3e+xWd5bgyuHxmMbEXIo1G1j9WBt\n" +
            "zSw4FIQYTdl5ubaJk8tJUvBybQKBgQDmegRfWgUjXId5fbX0nZ1edWCCdB3zxqug\n" +
            "9bBIiDBkmGR0sLSyNHSNzURjSxj+y1yYFNIfBmHyu1zXjADJTvJUcmWKXGW62X7N\n" +
            "YdoqOt5SdCsJrYYlA47iaxPwE6ZRgcbzFQ0RH16gH+zVOXXlmE3bAivRFoB72JX2\n" +
            "Ej3rZjcv1QKBgH+1j6M7ppHe5wflAxUtATu15hWh/3d+0cLJhcmW5oNNPLmfi1No\n" +
            "GBZ9b4GODrPWpTBUJ5THemXi2EnThEVZy/uv9MOv3ssKOa/N4Fnu1GM7B6vnPY56\n" +
            "ni5oGBYMsjNn/i3uWlB263JGfhyyU2uzApl7JBPCpAFGIXHAWEZqNnZNAoGALmdE\n" +
            "uBn8zNjVYe2gk+akB5+kVb+hRDKs3ZR58LCS5b4VA7WMPBD5oo9AhKlbF/nD487V\n" +
            "W2/CiseUcsV6Zw2hFsWNkiT2Sn9920YnUbdWic1f2Ov6BTvJKecbNYwPQXvanZiQ\n" +
            "b/Eb8StcWLXf+eEHU+AFCGz6Y3UBBHE5zEphNB0CgYEA596g1RrvHerJjmFiOVT/\n" +
            "T4vDEGvdqnYErcv1WnCLbaQ++HUU9wAG3ZwTmmUmkGEQM+i6yry5FKP51KYd1Ilz\n" +
            "Rg+MIQW2TVS614zonWC3gcggqMCXYyCw++RoO4rWZAQJwLpjOehM1B4mKdKfmqYN\n" +
            "ZUiVDg+d3/tYk3ZbO9XVZvk=")
      ));
  }

  private String generateToken(String login){
    return jwt.generateToken(new JsonObject().put("login",login), new JWTOptions().setAlgorithm("RS256"));
  }
}
