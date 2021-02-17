package demo.helpdebug;

import demo_with_database.pojo.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @author divenier
 * @date 2021/2/16 14:20
 * 客户端
 */
public class Client1 {
    private static Socket client;
    private static InputStream is;
    private static OutputStream os;
    private static ObjectOutputStream oos;
    private static Scanner scanner;

    public Client1(Socket clientSocket) throws IOException {
        this.client = clientSocket;
        this.is = clientSocket.getInputStream();
        this.os = clientSocket.getOutputStream();
        this.oos = new ObjectOutputStream(this.os);
    }

    public static void main(String[] args) throws IOException {
        System.out.println("客户端开始运行");
        Socket socket = new Socket(InetAddress.getByName("localhost"), 6666);
        new Client1(socket);

        // 用于存储用户的输入命令
        String label = "";
        // 获取一个扫描器，用完需关闭
        scanner = new Scanner(System.in);
        // 定义一个布尔变量，用于退出程序
        boolean loop = true;
        while (loop) {
            System.out.println("打招呼-》声明资源-》下线");
            System.out.println("hello: 和服务器打招呼");
            System.out.println("report: 声明资源");
            System.out.println("exit: 申请下线");

            label = scanner.next();
            switch (label) {
                case "hello":
                    boolean helloSuccess = hello();
                    if(helloSuccess){
                        System.out.println("打招呼成功");
                    }
                    break;
                case "report":
                    Resource resource = new Resource("a.txt", "divenier", 1, "9762349ygha9sfafda", "some notes");
                    boolean reportSuccess = reportResource(resource);
                    if(reportSuccess){
                        System.out.println("资源声明成功");
                    }
                    break;
                case "exit":
                    boolean exitSuccess = exit();
                    if(exitSuccess){
                        System.out.println("服务端接收到退出请求");
                        if (socket != null) {
                            socket.close();
                        }
                        loop = false;
                    }
                default:
                    break;
            }
//            System.out.println("客户端停止运行");
//            reportResource(resource);
        }
    }

    /**
     * 和服务端打招呼
     */
    public static boolean hello(){
        //是否得到了服务端的hi回应
        boolean getHi = false;

        //打招呼，第一次
        try {
            os.write("hello".getBytes(StandardCharsets.UTF_8));

            //对服务端返回的打招呼信息处理
            byte[] responseData = new byte[1024];
            int responseLen = is.read(responseData);
            String responseMsg = new String(responseData,0,responseLen);
            System.out.println("接收到第一次server返回的消息" + responseMsg);
            if(responseMsg.equals("hi")){
                System.out.println("服务端和我说hi");
                getHi = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return getHi;
    }

    /**
     * 给服务端发送断开连接指令
     */
    public static boolean exit(){
        //服务端收到你要断开的指令
        boolean getExitOk = false;
        try {
            os.write("exit".getBytes(StandardCharsets.UTF_8));

            byte[] responseData = new byte[1024];
            int responseLen = is.read(responseData);
            String responseMsg = new String(responseData,0,responseLen);
            System.out.println("接收到server 针对exit的消息：" + responseMsg);
            if(responseMsg.equals("exitOK")){
                getExitOk = true;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getExitOk;
    }
    /**
     * 向服务端汇报自己拥有的资源
     * 资源对象是pojo的Resource类实例
     * @param r
     * @return
     */
    public static boolean reportResource(Resource r){
        boolean reportSuccess = false;
        try {
            oos.writeObject(r);
            oos.flush();
            reportSuccess = true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reportSuccess;
    }
}
