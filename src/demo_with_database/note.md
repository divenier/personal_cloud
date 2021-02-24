# 开发流程
1. 忽略文件传输
   * 先实现socket通信
   * 实现server端操作数据库（简单操作）
   * 把client的信息注册到数据库
   * 完善注册功能
   * 完善登录、退出功能
      * 未登录状态写其他命令会报错
2. List功能
   * 基本完成
   
3. 添加文件传输（重要）
   * 如何在只有Server Client双向通信的前提下
   让服务端收到A的资源再发给B？
   * 如何做到监听服务端？
      * 用一个新的类，new一个线程，一直运行；

4. 心跳检测

## 如何实现文件传输？？

1. 客户端一直监听服务端命令，可以做到发文件给服务端；
2. 服务端收到文件后，通过notifyAll()唤醒等待的handler线程
3. 等待的线程把文件传输给自己的客户端

客户端的`main`只负责收集第一道命令；

失败的想法：
1. 本质上，客户端不应该与客户端建立socket连接，
2. server的`handle`应该保存两个socket，一个处理客户端，
另一个为空；
   * 当接收到get文件请求时，从请求端A得到资源方B的
     `username/devicename`，给另一个socket赋值
     
     


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

该乱码用GBK编码会变成 锟斤拷 ，搜索得知是一个Unicode和utf-8转化的常见问题（占位符）
**未解决**
目前在用`contains()`函数判断，而不是用`equals()`


后面使用会出现首次使用时，前两个字母被吞掉的情况，
不是常见的`(line = br.readLine()) != null)`用法导致的错误，
是第一次掉用readLine()结果就不对

**决定取消所有IO包装流，仅保留InputStream，OutputStream和ObjectOutputStream**

最后，客户端仍会在**服务端未发送数据**时，接收到三个字符的乱码
## 4. MD5加密
1. Spring有个包，apache也有个包

第一次导入工具，未使用maven管理项目，最终下载apache的jar包，直接导入的类

## 5. socket关闭，IO流仍在读取的问题
判断`(socket != null)`再读取也没用
经测试，发现是客户端没有关闭socket，显式关闭socket即可

后来，改用OutputStream的时候，再次出现该问题；判断`isRunning`也没用

## 6. 最后的问题
1. 长度问题，文件长度莫名其妙变成了4096
2. 偶尔出现Socket closed异常
# 协议设计
真正的协议，使用时应该严格遵守协议规定，不管做什么动作，先看协议内容；
之前的设计，思路有很多不到位的地方，客户端的功能比较零散，不够体系化；

客户端请求报文：
```
req&        --请求
cmd&        --命令/回复的代码
args&       --命令参数
data        --其他附带信息，
```

客户端响应报文：
```
resp&
status&     --给服务端端的响应码(标识成功失败等)
cmd&        --服务端刚才请求的命令（如果是get，服务端就要准备接收文件了）
data
```

服务端响应报文：
```
resp&
status&     --给客户端的响应码(标识成功失败等)
cmd&        --客户刚才请求的命令
data
```

服务端请求报文：
```
req&
cmd&
args&
data
```

# 项目局限
1. demo性质，数组开的小