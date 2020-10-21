package pl.andrzejkarwoski.Zadanie_Rekrutacyjne_Vertx.verticles;


import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.mongo.MongoClient;
import org.mindrot.jbcrypt.BCrypt;

import pl.andrzejkarwoski.Zadanie_Rekrutacyjne_Vertx.models.Item;
import pl.andrzejkarwoski.Zadanie_Rekrutacyjne_Vertx.models.User;

public class MongoVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(MongoVerticle.class);

  private MongoClient mongo;
  private JsonObject config;

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    setupConfig("myDatabase");
    mongo = MongoClient.createShared(vertx,config,"MyMongoPool");
    prepareDatabase();

    vertx.eventBus().consumer("mongo.queue", this::onMessage);

  }



  private void onMessage(Message<JsonObject> message) {
    if (!message.headers().contains("action")) {
      LOGGER.error("No action header specified for message with headers {} and body {}",
        message.headers(), message.body().encodePrettily());
      message.fail(401, "No action header specified");
      return;
    }
    String action = message.headers().get("action");
    switch (action) {
      case "register-new-user":
        registerNewUser(message);
        break;
      case "login-user":
        checkUserCredentials(message);
        break;
      case "get-items":
        getListOfItems(message);
        break;
      case "add-item":
        addItem(message);
        break;
      default:
        message.fail(400, "Bad action: " + action);
    }
  }

  private void getListOfItems(Message<JsonObject> message) {
    JsonObject body = message.body();
    if(body.getString("login") == null) message.fail(400,"Bad parameters");
    mongo.findOne("users", new JsonObject().put("login", body.getString("login")), null, res -> {
      mongo.find("items",new JsonObject().put("owner",res.result().getString("id")), rs -> {
        message.reply(rs.result().toString());
      });
    });
  }

  private void checkUserCredentials(Message<JsonObject> message) {
    JsonObject body = message.body();
    if(body.getString("login") == null) message.fail(400,"Bad parameters");
    else if(body.getString("password") == null) message.fail(400,"Bad parameters");
    mongo.findOne("users", new JsonObject().put("login", body.getString("login")), null, res -> {
      if(res.result() != null){
        if(BCrypt.checkpw(body.getString("password"), res.result().getString("password"))){
          message.reply("ok");
        }
        else message.fail(400,"Bad credentials");
      }
      else message.fail(400,"Bad credentials");
      });
    }

  private void registerNewUser(Message<JsonObject> message) {
    JsonObject body = message.body();
    if(body.getString("login") == null) message.fail(400,"Bad parameters");
    else if(body.getString("password") == null) message.fail(400,"Bad parameters");
    mongo.findOne("users", new JsonObject().put("login", body.getString("login")), null, res -> {
      if(res.result() != null){
        LOGGER.info(String.format("There is already %s in database",body.getString("login")));
        message.fail(400,"There is already user with given login");
      }
      else{
          User user = new User(body.getString("login"),body.getString("password"));
          mongo.insert("users",new JsonObject().put("id",user.getId()).put("login",user.getLogin()).put("password",user.getPassword()), rs -> {
            if (rs.succeeded()) {
              LOGGER.info(String.format("User %s added to database", user.getId()));
              message.reply("ok");
            } else {
              message.fail(500,"Error while adding user to database");
            }
          });
      }
      return;
    });

  }

  private void addItem(Message<JsonObject> message){
    JsonObject body = message.body();
    if(body.getString("title") == null) message.fail(400,"Bad request");
    mongo.findOne("users", new JsonObject().put("login", body.getString("login")), null, res -> {
      Item item = new Item(body.getString("title"));
      mongo.insert("items", new JsonObject()
        .put("id",item.getId())
        .put("owner", res.result().getString("id"))
        .put("title",item.getTitle()), result -> {
        if (result.succeeded()) {
          LOGGER.info(String.format("Item %s added to database for user %s",  item.getTitle(), res.result().getValue("id")));
          message.reply("ok");
        } else {
          message.fail(400,"Bad request");
        }
      });
    });
  }

    private void prepareDatabase(){
      mongo.getCollections(rs -> {
        if(rs.result().contains("users") || rs.result().contains("items")) return;
        else{
          mongo.createCollection("users", res -> {
            if (res.succeeded()) {
              // Created ok!
            } else {
              res.cause().printStackTrace();
            }
          });
          mongo.createCollection("items", res -> {
            if (res.succeeded()) {
              // Created ok!
            } else {
              res.cause().printStackTrace();
            }
          });
        }
      });

      return;
    }



    private void setupConfig(String dbName){
      config = new JsonObject();
      config.put("address", "");
      config.put("connection_string", "mongodb://127.0.0.1:27017");
      config.put("db_name", dbName);
    }
}
