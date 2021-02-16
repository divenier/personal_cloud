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

**结果：**
解决了错误，保留了资源类；
