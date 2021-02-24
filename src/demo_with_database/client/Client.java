package demo_with_database.client;
import java.io.*;
import java.net.*;
import java.util.Scanner;

/**
 * @author divenier
 * @date 2021/2/16 14:20
 * 客户端
 */
public class Client {
    public static void main(String[] args) throws IOException {
        System.out.println("------客户端开始运行--------");
        int port = 6666;
        Socket socket = new Socket(InetAddress.getByName("localhost"), port);
        //让CHandler处理一切事务
        CHanlder clientHanlder = new CHanlder(socket,true);
        clientHanlder.start();

        // 用于存储用户的输入命令
        String label = "";
        // 获取一个扫描器，用完需关闭
        Scanner scanner = new Scanner(System.in);
        // 定义一个布尔变量，用于退出程序
        boolean loop = true;
        while (loop) {
            System.out.println("regist: 注册");
            System.out.println("login: 登录到服务器");
            System.out.println("report: 声明资源");
            System.out.println("list: 查看资源列表");
            System.out.println("get: 获取资源");
            System.out.println("exit: 申请下线");

            label = scanner.next();
            switch (label) {
                case "regist":
                    String regResult = clientHanlder.regist();
                    break;
                case "login":
                    String loginResult = clientHanlder.login();
                    break;
                case "report":
                    if(!clientHanlder.isLogin()){
                        System.out.println("请先登录再进行操作");
                        break;
                    }
                    clientHanlder.reportResource();
                    break;
                case "list":
                    if(!clientHanlder.isLogin()){
                        System.out.println("请先登录再进行操作");
                        break;
                    }
                    clientHanlder.list();
                    break;
                case "get":
                    if(!clientHanlder.isLogin()){
                        System.out.println("请先登录再进行操作");
                        break;
                    }
                    String retForGet = clientHanlder.getResource();
//                    String cmdForGet = readGetCmd();
//                    System.out.println(retForGet);
                    break;
                case "exit":
                    boolean exitSuccess = clientHanlder.exit();
                    if(exitSuccess){
                        System.out.println("服务端接收到退出请求");
                        //让自己这边结束运行
                        loop = false;
                    }
                    break;
                default:
                    System.out.println("输入有误，请重新输入命令！");
                    break;
            }
        }
        scanner.close();
        socket.close();
    }
}
