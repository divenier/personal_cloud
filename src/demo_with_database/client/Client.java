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

//            System.out.println("客户端停止运行");
//            reportResource(resource);
        }
        scanner.close();
        socket.close();
    }

    /**
     * 进行文件传输
     * 1. 发送get指令给server，server验证请求资源的合法性
     * 2. 根据deviceName 查询到资源客户端A，并与客户端建立连接（用固定的端口号？？）
     * 3. 两个客户端进行传输
     * @return 传输结果+存储位置
     */
//    public static String getResource(String cmdAndCode){
//        //发送命令成功
//        if(sendReq(cmdAndCode)){
//            String[] retForGet = recvMsg().split("\\s+");
//            //如果没有成功状态码
//            if(!retForGet[0].contains("200")){
//                System.out.println("get请求失败");
//                return "FAILED";
//            }else{
//                //与其他客户端建立连接，传输文件
//                //资源在客户端A上的绝对路径
//                String resourcePath = retForGet[1];
//                //客户端A的publicIP
//                String RCPubIP = retForGet[2];
//                //lanip
//                String RCLanIP = retForGet[3];
//                //端口号
//                Integer RCPort = Integer.parseInt(retForGet[4]);
//
////                Socket resClientSocket = getResHolderSocket(RCPort);
//
//            }
//        }
//        return null;
//    }

    /**
     * 与拥有资源的客户端建立连接
     * 目前只做 本地连接
     * @return
     */
    public static Socket getResHolderSocket(Integer resHolderPort){
        Socket socket = null;
        try {
            socket = new Socket("localhost", resHolderPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return socket;
    }

}
