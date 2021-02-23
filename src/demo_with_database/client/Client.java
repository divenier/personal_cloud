package demo_with_database.client;
import demo_with_database.pojo.Resource;
import org.apache.commons.codec.digest.DigestUtils;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
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
//                case "get":
//                    if(!isLogin()){
//                        System.out.println("请先登录再进行操作");
//                        break;
//                    }
//                    String cmdForGet = readGetCmd();
//                    String retForGet = getResource(cmdForGet);
//                    System.out.println(retForGet);
//                    break;
                case "exit":
                    boolean exitSuccess = clientHanlder.exit();
                    if(exitSuccess){
                        System.out.println("服务端接收到退出请求");
                        //让自己这边结束运行
                        if (socket != null) {
                            socket.close();
                        }
                        loop = false;
                    }
                    break;
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
     * 提示用户输入要获取的文件的参数，并读取参数
     * @return get + code
     */
    public static String readGetCmd(){
        System.out.println("请输入资源code（唯一编码）");
        Scanner scanner = new Scanner(System.in);
        String resourceCode = scanner.nextLine();

        return "get " + resourceCode;
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

/**
 * 用于客户端的输入输出，以及处理各种信息
 *
 */
class CHanlder extends Thread{
    /**
     * waitForExit是防止在退出时，服务器已关闭连接，但客户端仍试图读取socket的消息
     */
    public Boolean isRunning;
//    public Boolean waitForExit;
    public Socket client;
    public InputStream is;
    public OutputStream os;
//    public PrintStream ps;
//    public BufferedWriter bw;
//    public BufferedReader br;
    public ObjectOutputStream oos;
    public Scanner scanner;
    //存储本客户端的用户名
    public String clientUserName;

    public CHanlder(Socket clientSocket,boolean b) throws IOException {
        this.client = clientSocket;
        this.is = clientSocket.getInputStream();
        this.os = clientSocket.getOutputStream();
//        this.br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
//        this.bw = new BufferedWriter(new PrintWriter(os));
//        this.ps = new PrintStream(new BufferedOutputStream(os));
        this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
        //登录之后修改为用户名
        this.clientUserName = "NOT_INIT";
        this.isRunning = b;
//        this.waitForExit = false;
    }

    public void closeSocket(){
        if(this.client != null){
            try {
                this.client.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 客户端的发送请求的方法
     * @param msg 要发送的信息
     * @return 发送是否成功
     */
    public boolean sendReq(String msg){
        boolean sendSuccess = false;
        if(msg == null || "".equals(msg)){
            System.out.println("发送的消息为空");
            return true;
        }else {
            try {
                os.write(msg.getBytes(StandardCharsets.UTF_8));
                System.out.println("运行了一次send()函数，发送的消息是：" + msg);
                sendSuccess = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sendSuccess;
    }

    /**
     * 读取服务端发来的消息
     * 并对之进行处理
     */
    public String recvMsg() {
        //收到的整个信息
        String recvMesg = null;
        //是请求还是响应
        String msgType = null;
        //解析到的命令（要么是自己发出的命令，要么是服务端的命令）
        String cmd = null;
        //响应时才有的响应码，如果接收到的不是响应，就初始化为NOT_INIT
        String status = "NOT_INIT";
        //参数，请求时才会有
        String[] args = new String[2];
        //最多不超过10个资源
        String[] datas= new String[10];
        int dataLength = 0;
        //用于按照正则表达式分割信息
        String[] arr;
        try {
            byte[] recvBytes = new byte[4096];
            //如果socket已经关闭，防止socket关闭但客户端状态还未更新，因此每次执行前先判断一下
            if(client != null){
                int len = is.read(recvBytes);
                recvMesg = new String(recvBytes,0,len);
            }
//            System.out.println("很抱歉服务端关闭了");
//            isRunning = false;
//            return null;
            System.out.println("接收到的信息是: " + recvMesg);

            //按行分割消息
            arr = recvMesg.split("&");
            if(arr.length > 0){
                msgType = arr[0];
            }
            //是响应
            if(msgType.contains("resp")){
                status = arr[1];
                cmd = arr[2];
                //不同的资源之间用#分割
                String[] splitDatas = arr[3].split("#");
                dataLength = splitDatas.length;
                for (int i = 0; i < dataLength; i++) {
                    datas[i] = splitDatas[i];
                }
            }else{
                //是服务端的请求，包含get file和心跳检测
                //TODO

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //如果接收出错了，就直接不处理了
        if(recvMesg == null||msgType == null){
            return null;
        }
        //对响应进行处理
        if(msgType.contains("resp")){
            handleStatus(cmd,status);
            switch (cmd){
                case "login":
                    //如果登录失败，就把本线程的用户名设为未初始化，标识未登录
                    if(!("200".equals(status))) {
                        clientUserName = "NOT_INIT";
                    }
                    break;
                case "exit":
                    if("200".equals(status)){
                        this.isRunning = false;
                        clientUserName = "NOT_INIT";
                        closeSocket();
                    }
                    break;
                case "list":
                    //得到服务端成功回复，分行输出资源信息
                    if("200".equals(status)){
                        for (int i = 0; i < dataLength; i++) {
                            System.out.println(datas[i]);
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        else{
            //处理服务器的要求
            //TODO

        }
        return null;
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
    public String regist(){
        String regStatus = null;
        String lanip = IPAddressHelper.getLocalIp();
        String publicip = IPAddressHelper.getPublicIP();
        //req+注册指令+输入信息+IP
        String regMsg = "req&" + "regist&" + readUserMsg() + "&" + lanip + " " + publicip;
        if(sendReq(regMsg)){
            System.out.println("发送注册指令成功");
            regStatus = "我也不知道写啥了";
        }else{
            System.out.println("发送注册指令失败");
        }
        return regStatus;
    }
    /**
     * 用一个新的scanner读取注册时输入的信息
     * @return 用户名+密码（用于注册）
     */
    public String readUserMsg() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入用户名: ");
        String user = scanner.next();
        System.out.println("请输入密码: ");
        String passwd = scanner.next();

        String msg = user + " " + passwd;
        return msg;
    }

    /**
     * 登录，
     */
    public String login(){
        String loginStatus = null;
        //username pwd
        String name_pwd = readUserMsg();

        //先把读取到的用户名赋给类变量clientUserName，如果注册失败再设为null
        String[] arr = name_pwd.split("\\s+");
        clientUserName = arr[0];

        String loginMsg = "req&" + "login&" + name_pwd + "&" + "LOGIN_DATA";
        if(sendReq(loginMsg)){
            loginStatus = "已发送登录命令";
        }else{
            System.out.println("发送登录指令失败");
        }

        return loginStatus;
    }

    /**
     * 通过本线程的clientUserName有无被赋值判断当前是否在登录状态
     * @return 当前是否在登录状态
     */
    public boolean isLogin(){
        return "NOT_INIT".equals(clientUserName) ? false : true;
    }

    /**
     * 声明资源时，读取客户端输入
     * resourceName     --输入资源名
     * deviceName       --程序读取用户名（由login保存）
     * path             --用户输入绝对路径（需包含文件名）
     * status           --默认是1（自己添加自己的，自己当然在线了）
     * @return Resource实例
     */
    public Resource readResourceMsg() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入资源名: ");
        String resourceName = scanner.next();
        String deviceName = clientUserName;

        System.out.println("请输入资源存储绝对路径: ");
        String path = scanner.next();
        //转化为code的成分，绝对路径（包含文件名）+设备名
        String codeBeforeMd5 = path + " " + deviceName;
        String code = DigestUtils.md5Hex(codeBeforeMd5.getBytes());
        System.out.println("请输入备注: ");
        String note = scanner.next();

        Resource resource = new Resource(resourceName,deviceName,path,1,code,note);
        return resource;
    }

    /**
     * 向服务端汇报自己拥有的资源
     * 资源对象是pojo的Resource类实例
     * @return 写入对象是否成功
     */
    public boolean reportResource(){
        boolean reportSuccess = false;
        Resource r = readResourceMsg();
        String resMsg = r.getResourceName() + " " + r.getDeviceName() + " " + r.getPath() + " " + r.getCode() + " " + r.getNote();
        String sendMsg = "req&" + "report&" + resMsg + "&" + "REPORT_DATA";
        if(sendReq(sendMsg)){
            reportSuccess = true;
        }else{
            System.out.println("发送report指令失败");
        }
        return reportSuccess;
    }


    /**
     * 返回查看的结果
     * @return 一个长String，包含所有查询到的结果，查询结果用&分割
     */
    public String[] list(){
        String arg = readListCmd();
        String listMsg = "req&" + "list&" + arg + "&" + "LIST_DATA";
//        String[] retForList = null;
        if(sendReq(listMsg)){
            System.out.println("发送list命令成功");
        }
        return null;
    }

    /**
     * list命令时，提醒用户输入并读取用户输入命令
     * @return list + arg(all/otherFileName)
     */
    public String readListCmd(){
        System.out.println("all: 查看所有资源");
        System.out.println("文件名: 查看所有这个文件的状态");

        Scanner scanner = new Scanner(System.in);
        String arg = scanner.nextLine();
        return arg;
    }

    /**
     * 给服务端发送断开连接指令
     * 发送就行了，接收处理交给recvMsg
     * TODO 修改协议
     */
    public boolean exit(){
        //服务端收到你要断开的指令
        boolean getExitOk = false;
        if(sendReq("req&" + "exit&" + "NO_ARGS&" + "EXIT_DATA")){
            getExitOk = true;
        }else{
            System.out.println("发送exit指令失败");
        }
//        waitForExit = true;
        return getExitOk;
    }
    /**
     * 对不同指令收到的回复状态码进行处理和回显
     * @param cmd 客户端发出的指令 注册，登录等
     * @param status 服务端回复的状态码（经过recvMsg及对应的各个功能函数和main函数处理后的string）
     */
    public static void handleStatus(String cmd,String status){
        if(status.contains("200")){
            System.out.println(cmd + "成功");
        }else if(status.contains("500")){
            System.out.println(cmd + "出现错误");
        }else{
            System.out.println(cmd + "出现未知状况");
        }
    }

    @Override
    public void run() {
        while(isRunning){
            recvMsg();
        }
    }
}
