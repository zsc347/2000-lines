package classloader;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Objects;

public class DynamicLoader extends ClassLoader {

  private static final String DYNAMIC_FOLDER = System
      .getProperty("dynamic", "target/classes");

  @Override
  public Class<?> loadClass(String name) throws ClassNotFoundException {
    if (name.endsWith("Impl")) {
      byte[] classData = getClassData(name);
      if (classData != null) {
        return defineClass(name, classData, 0, classData.length);
      } else {
        return null;
      }
    }
    return DynamicLoader.class.getClassLoader().loadClass(name);
  }


  private byte[] getClassData(String name) {
    Objects.requireNonNull(name);
    String filePath = DYNAMIC_FOLDER + '/' + name.replace('.', '/') + ".class";
    try {
      InputStream ins = new FileInputStream(filePath);
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];

      int readNum;
      while ((readNum = ins.read(buffer)) != -1) {
        out.write(buffer, 0, readNum);
      }
      return out.toByteArray();
    } catch (Exception e) {
      return null;
    }
  }
}
