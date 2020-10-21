package pl.andrzejkarwoski.Zadanie_Rekrutacyjne_Vertx.models;

import java.util.UUID;

public class Item {

  private String title;
  private String id;

  public Item(String title) {
    this.title = title;
    this.id = UUID.randomUUID().toString();
  }

  public Item(String title, String id) {
    this.title = title;
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getId() {
    return id;
  }

}
