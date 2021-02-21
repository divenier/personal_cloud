package demo.helpdebug;

import demo_with_database.pojo.Resource;

import java.io.*;
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
    /**
     * bufferedWriter第一次使用总是有乱码
     */
    private static Socket client;
    private static InputStream is;
    private static OutputStream os;
    private static PrintStream ps;
    private static BufferedWriter bw;
    private static BufferedReader br;
    private static ObjectOutputStream oos;
    private static Scanner scanner;

    public Client1(Socket clientSocket) throws IOException {
        client = clientSocket;
        is = clientSocket.getInputStream();
        os = clientSocket.getOutputStream();
        br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
        bw = new BufferedWriter(new PrintWriter(os));
        ps = new PrintStream(new BufferedOutputStream(os));
        oos = new ObjectOutputStream(clientSocket.getOutputStream());
    }

    public static void main(String[] args) throws IOException {
        System.out.println("------客户端开始运行--------");
        Socket socket = new Socket(InetAddress.getByName("localhost"), 6666);
        //给类变量赋值
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
//            label = scanner.nextLine();
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
        scanner.close();
        socket.close();
    }

    /**
     * 客户端的发送信息的方法，几乎与server的send函数一样
     * @param sendMsg
     * @return 发送是否成功
     */
    public static boolean sendMsg(String sendMsg){
        boolean sendSuccess = false;
        if(sendMsg == null || "".equals(sendMsg)){
            System.out.println("发送的消息为空");
            return true;
        }else {
            try {
/*                dos.writeUTF(sendMsg);
                dos.flush();*/
                bw.write(sendMsg);
                //换行，否则server的 readline就会一直阻塞
                bw.newLine();
                bw.flush();
/*                ps.println(sendMsg);
                ps.flush();*/
                System.out.println("运行了一次send()函数，发送的消息是：" + sendMsg);
                sendSuccess = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sendSuccess;
    }


    /**
     * 把读取/接收回复的实现放到一个函数中
     * @return 接收到的String
     */
    public static String recvMsg() throws IOException {
        String receivedMsg = br.readLine();
        if(receivedMsg == null || "".equals(receivedMsg)){
            System.out.println("recvMsg收到了个寂寞");
            return null;
        }
        //\\s+表示匹配任意>1个空格
        System.out.println(receivedMsg);
        String[] msgArr = receivedMsg.split("\\s+");
//        return new String(msgArr[0].getBytes(StandardCharsets.UTF_8),"UTF-8");
        return msgArr[0];
    }
    /**
     * 和服务端打招呼
     */
    public static boolean hello(){
        //是否得到了服务端的hi回应
        boolean getHi = false;

        //打招呼，第一次
        try {
            if(sendMsg("hello")){
                //对服务端返回的打招呼信息处理
                String retForHello = recvMsg();
                if("hi".equals(retForHello)){
                    System.out.println("服务端和我说hi");
                    getHi = true;
                }
            }else {
                System.out.println("发送hello失败");
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
            if(sendMsg("exit")){
                if("exitOK".equals(recvMsg())){
                    getExitOk = true;
                }
            }else{
                System.out.println("发送exit指令失败");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return getExitOk;
    }
    /**
     * 向服务端汇报自己拥有的资源
     * 资源对象是pojo的Resource类实例
     * @param r 资源实例
     * @return 写入对象是否成功
     */
    public static boolean reportResource(Resource r){
        boolean reportSuccess = false;
        try {
            if(sendMsg("report")){
                oos.writeObject(r);
                oos.flush();
                reportSuccess = true;
            }else{
                System.out.println("发送report指令失败");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return reportSuccess;
    }
}
