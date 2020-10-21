package pl.andrzejkarwoski.Zadanie_Rekrutacyjne_Vertx.models;

import org.mindrot.jbcrypt.BCrypt;

import java.util.UUID;

public class User {

  private String id;
  private String login;
  private String password;

  public User(String login, String password) {
    this.id = UUID.randomUUID().toString();
    this.login = login;
    this.password = BCrypt.hashpw(password, BCrypt.gensalt(10));;
  }

  public String getId() {
    return id;
  }

  public String getLogin() {
    return login;
  }

  public String getPassword() {
    return password;
  }
}
