# 开发流程
1. 忽略文件传输
   * 先实现socket通信
   * 实现server端操作数据库（简单操作）
   * 把client的信息注册到数据库
   * 完善注册功能
2. 


# 问题记录
## 1、对象传输问题
**问题描述：**
希望把pojo对象通过socket传输，
但发现outputStream传输对象后就无法正常传输其他字节
```
java.net.SocketException: Socket is closed
```

**解决过程：**
在StackOverflow找到指导性观点：是因为socket被关闭了
于是继续搜索，发现包装在`OutputStream`外层的`ObjectOutputStream`
的关闭导致了基础IO流`OutputStream`的关闭，最终socket被意外关闭

传输结束时不应该关闭IO流，而是通过`shutdownOutput`或在报文中实现设置`end`等关键词
对传输是否结束进行判断
**结果：**
解决了错误，保留了资源类；

2、
受原有python项目影响，希望尽量模块化，把解析请求和其他操作分开；
但socket连接是持续的，IO流一旦建立就不能频繁关闭重启，导致第一次bug出现；

## 2、DataInputStream的readUTF()问题
在read完毕时，会报一个异常，EOFException


查阅`readUTF()`源码，发现调用了`readFully()`函数，`readFully()`会
直接读取若干长度，
> The Javadoc for DataInput.readFully(byte[] b) states:
> 
> Reads some bytes from an input stream and stores them into the buffer array b. 
The number of bytes read is equal to the length of b.

查看`writeUTF()`的源码可以发现，它首先把str的长度写入，再传str，与`readUTF()`是对应的

StackOverflow回答：在read之前，先告诉它传输内容的长度
> You need to send the file size ahead of each file.


2. 解决过程：
改用BufferedReader读取输入，读到换行符的时候就知道输入结束；

## 3. 莫名其妙的BufferedWriter的使用问题
第一次使用BufferedWriter传输时，字符串的最前面会被加上一段乱码，大概三个字符
后面第二次、第n次再使用都是正常的；