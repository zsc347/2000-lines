ClassLoader比较重要的五个方法
- defineClass()：完成从Java字节代码的字节数组到java.lang.Class的转换。一般使用原生代码实现。

- findLoadedClass()：这个方法用来根据名称查找已经加载过的Java类。
一个类加载器不会重复加载同一名称的类。

- findClass()
推荐的覆盖方法，用来根据名称查找并加载java类。
这个方法用来根据名称查找并加载Java类。

- loadClass()：
这个方法用来根据名称加载Java类。

- resolveClass()：
链接一个Java类。链接是指Java类的二进制代码合并到JVM的运行状态之中的过程。包括验证，准备和解析等步骤。

当实现一个classloader时，如果采用建议的双亲委托机制，则只需重写findClass方法，默认的loadClass
会先用父加载器来加载类型，当父加载器无法加载时，才会使用子类加载器。这个策略称为双亲委派模型。
这样做的目的是为了确保类在java虚拟机中的唯一性。JVM中两个类相等，指的是由同一个类加载加载，
并且FQCN(Full Qualified Class Name)相同。

当需要跳出这种模型时，如在程序运行时替换class实现(热替换)，则需要覆盖loadClass的实现。