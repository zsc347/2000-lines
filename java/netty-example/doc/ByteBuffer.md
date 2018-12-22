# ByteBuffer
Java NIO ByteBuffer用于实现Java NIO Channel之间
的通信。通常的使用方式是数据从一个channel中读取出来，然后写入另一个channel中。

使用ByteBuffer来读取数据通常需要有以下4个步骤

````$xslt
1. 将数据写入buffer
2. 调用buffer.flip()
3. 将数据从buffer中读取出来
4. 调用buffer.clear() 或 buffer.compact()
````

`buffer.flip()`的作用是将buffer从写模式切换到读模式，
在读模式下buffer允许读出所有写入的数据。
当数据全部读出后，可以通过调用`buffer.clear()`
或`buffer.compact()`来清空buffer, `buffer.clear()`的作用是
清空整个buffer， `buffer.compact()`的作用是清空所有已经读出的数据，
并将所有未读出的数据移动到buffer的开始处，再这之后写入的数据会写到
未读出的数据之后。