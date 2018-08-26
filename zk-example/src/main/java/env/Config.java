package env;

public class Config {

  public static final String HOST_PORT = System
      .getProperty("HOST_PORT", "localhost:2181");
  public static final int SESSION_TIMEOUT = 3000;
}
