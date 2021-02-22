package demo_with_database.client;

import demo.helpdebug.Client1;
import demo_with_database.pojo.Resource;

import java.io.*;
import java.net.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @author divenier
 * @date 2021/2/16 14:20
 * 客户端
 */
public class Client {
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

    public Client(Socket clientSocket) throws IOException {
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
        new Client(socket);

        // 用于存储用户的输入命令
        String label = "";
        // 获取一个扫描器，用完需关闭
        scanner = new Scanner(System.in);
        // 定义一个布尔变量，用于退出程序
        boolean loop = true;
        while (loop) {
            System.out.println("注册 -》声明资源-》下线");
            System.out.println("regist: 注册");
            System.out.println("login: 登录到服务器");
            System.out.println("report: 声明资源");
            System.out.println("exit: 申请下线");

            label = scanner.next();
//            label = scanner.nextLine();
            switch (label) {
                case "regist":
                    String regResult = regist();
                    if(regResult.contains("200")){
                        System.out.println("注册成功");
                    }else if(regResult.contains("500")){
                        System.out.println("注册出现错误");
                    }else{
                        System.out.println("注册出现未知状况");
                    }
                    break;
                case "login":
                    boolean loginSuccess = login();
                    if(loginSuccess){
                        System.out.println("登录成功");
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
    public static String[] recvMsg() {
        String receivedMsg = null;
        try {
            receivedMsg = br.readLine();
        } catch (IOException e) {
            System.out.println("接收服务端的信息出现异常");
            e.printStackTrace();
        }
        if(receivedMsg == null || "".equals(receivedMsg)){
            System.out.println("recvMsg收到了个寂寞");
            return null;
        }
        //\\s+表示匹配任意>1个空格
        System.out.println(receivedMsg);
        String[] msgArr = receivedMsg.split("\\s+");
        return msgArr;
    }

    /**
     * 注册
     * 上报信息包含：
     * username 用户名
     * password 密码
     * lanip
     * publicip
     * @return 注册的结果
     */
    public static String regist() throws UnknownHostException {
        String regStatus = null;
        String lanip = getLocalIp();
        String publicip = getPublicIP();
        //注册指令+输入信息
        String regMsg = "regist " + readRegMsg() + " " + lanip + " " + publicip;
        if(sendMsg(regMsg)){
            String[] retForReg = recvMsg();
            regStatus = retForReg[0];
        }else{
            System.out.println("发送注册指令失败");
        }
        return regStatus;
    }

    /**
     * 用一个新的scanner读取注册时输入的信息
     * @return 用户名+密码（用于注册）
     */
    public static String readRegMsg() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入您的用户名: ");
        String user = scanner.next();
        System.out.println("请设置密码: ");
        String passwd = scanner.next();

        String msg = user + " " + passwd;
        // 用完扫描器需要关闭
        //scanner.close();
        return msg;
    }

    /**
     * 获取本机内网ip
     * @return 内网ip
     */
    private static String getLocalIp(){
        String ip = null;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ip;
    }

    /**
     * 获取本机外网ip
     * @return
     */
    private static String getPublicIP(){
        String ip=null;
        try {
            URL url = new URL("https://www.taobao.com/help/getip.php");
            URLConnection URLconnection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) URLconnection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
//                System.out.println("成功");
                InputStream in = httpConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(in);
                BufferedReader bufr = new BufferedReader(isr);
                String str;
                while ((str = bufr.readLine()) != null) {
                    ip=str;
//                    System.out.println(str);
                }
                bufr.close();
            } else {
                System.err.println("失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(ip!=null){

            char[] chs=ip.toCharArray();
            ip="";
            for(int i=0;i<chs.length;i++){
                if((chs[i]>=48&&chs[i]<=57)||chs[i]==46){
                    ip+=chs[i];
                }
            }
        }
        return ip;
    }
    /**
     * 和服务端打招呼
     * 登录
     */
    public static boolean login(){
        //是否得到了服务端的hi回应
        boolean getHi = false;

        //打招呼，第一次
        if(sendMsg("hello")){
            //对服务端返回的打招呼信息处理
            String retForHello[] = recvMsg();
            //如果含有注册成功码
            if(retForHello[0].contains("200")){
                System.out.println("登录成功");
                getHi = true;
            }
        }else {
            System.out.println("发送hello失败");
        }
        return getHi;
    }

    /**
     * 给服务端发送断开连接指令
     */
    public static boolean exit(){
        //服务端收到你要断开的指令
        boolean getExitOk = false;
        if(sendMsg("exit")){
            String[] retForExit = recvMsg();
            if(retForExit[0].contains("200")){
                getExitOk = true;
            }
        }else{
            System.out.println("发送exit指令失败");
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
