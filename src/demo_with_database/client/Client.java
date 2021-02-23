package demo_with_database.client;
import demo_with_database.pojo.Resource;
import org.apache.commons.codec.digest.DigestUtils;
//import org.springframework.util.DigestUtils;
import java.io.*;
import java.net.*;
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
    //存储本客户端的用户名
    private static String clientUserName;
    //本客户端的端口号
    private static Integer portNum;

    public Client(Socket clientSocket) throws IOException {
        client = clientSocket;
        is = clientSocket.getInputStream();
        os = clientSocket.getOutputStream();
        br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
        bw = new BufferedWriter(new PrintWriter(os));
        ps = new PrintStream(new BufferedOutputStream(os));
        oos = new ObjectOutputStream(clientSocket.getOutputStream());
        portNum = clientSocket.getPort();
    }

    public static void main(String[] args) throws IOException {
        System.out.println("------客户端开始运行--------");
        int port = 6666;
        Socket socket = new Socket(InetAddress.getByName("localhost"), port);
        //给类变量赋值
        new Client(socket);

        // 用于存储用户的输入命令
        String label = "";
        // 获取一个扫描器，用完需关闭
        scanner = new Scanner(System.in);
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
//            label = scanner.nextLine();
            switch (label) {
                case "regist":
                    String regResult = regist();
                    handleStatus("注册",regResult);
                    break;
                case "login":
                    String loginResult = login();
                    handleStatus("登录",loginResult);
                    //如登录失败，本client就不能记录该用户名
                    if(!loginResult.contains("200")){
                        clientUserName = null;
                    }
                    break;
                case "report":
                    if(!isLogin()){
                        System.out.println("请先登录再进行操作");
                        break;
                    }
                    Resource resource = readResourceMsg();
                    boolean reportSuccess = reportResource(resource);
                    if(reportSuccess){
                        System.out.println("资源声明成功");
                    }
                    break;
                case "list":
                    if(!isLogin()){
                        System.out.println("请先登录再进行操作");
                        break;
                    }
                    String cmdMsg = readListCmd();
                    String[] reourceList = list(cmdMsg);
                    //如果没有查到，就直接输出错误码，没毛病
                    for(String s: reourceList){
                        System.out.println(s);
                    }
                    break;
                case "get":
                    if(!isLogin()){
                        System.out.println("请先登录再进行操作");
                        break;
                    }
                    String cmdForGet = readGetCmd();
                    String retForGet = getResource(cmdForGet);
                    System.out.println(retForGet);
                    break;
                case "exit":
                    boolean exitSuccess = exit();
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
    public static String recvMsg() {
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
//        String[] msgArr = receivedMsg.split("\\s+");
//        return msgArr;
        return receivedMsg;
    }

    public static boolean isLogin(){
        return clientUserName == null ? false : true;
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
    /**
     * 注册（由于port是读取到的，因此仅能注册自己的连接）
     * 上报信息包含：
     * username 用户名
     * password 密码
     * lanip
     * publicip
     * port 是本客户端的socket端口号
     * @return 注册的结果
     */
    public static String regist(){
        String regStatus = null;
        String lanip = IPAddressHelper.getLocalIp();
        String publicip = IPAddressHelper.getPublicIP();
        String portString = portNum.toString();
        //注册指令+输入信息
        String regMsg = "regist " + readUserMsg() + " " + lanip + " " + publicip + " " + portString;
        if(sendMsg(regMsg)){
            String[] retForReg = recvMsg().split("\\s+");
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
    public static String readUserMsg() {
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
    public static String login(){
        String loginStatus = null;
        //login username pwd
        String name_pwd = readUserMsg();

        //先把读取到的用户名赋给类变量clientUserName，如果注册失败再设为null
        String[] arr = name_pwd.split("\\s+");
        clientUserName = arr[0];

        String loginMsg = "login " + name_pwd;
        if(sendMsg(loginMsg)){
            String[] retForLogin = recvMsg().split("\\s+");;
            loginStatus = retForLogin[0];
        }else{
            System.out.println("发送登录指令失败");
        }
        return loginStatus;
    }

    /**
     * list命令时，提醒用户输入并读取用户输入命令
     * @return list + arg(all/otherFileName)
     */
    public static String readListCmd(){
        System.out.println("list all: 查看所有资源");
        System.out.println("list 文件名: 查看所有这个文件的状态");

        Scanner scanner = new Scanner(System.in);
        String cmdMsg = scanner.nextLine();
        return cmdMsg;
    }

    /**
     * 返回查看的结果
     * @param listCmd
     * @return 一个长String，包含所有查询到的结果，查询结果用&分割
     */
    public static String[] list(String listCmd){
        String[] retForList = null;
        if(sendMsg(listCmd)){
            retForList = recvMsg().split("&");
        }
        return retForList;
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
    public static String getResource(String cmdAndCode){
        //发送命令成功
        if(sendMsg(cmdAndCode)){
            String[] retForGet = recvMsg().split("\\s+");
            //如果没有成功状态码
            if(!retForGet[0].contains("200")){
                System.out.println("get请求失败");
                return "FAILED";
            }else{
                //与其他客户端建立连接，传输文件
/*                Socket otherClient = getResFromOtherClinet();
                OtherClient oc = new OtherClient(otherClient);
                String msg = oc.createScan();
                oc.requestRes(msg);
                //将资源保存在本地
                oc.saveRes();

                //同时，再向服务器申明该资源
                String[] data = msg.split("&");
                String name = data[1];
                String code = data[2];
                declToServerAgain(name, code);*/
            }
        }
        return null;
    }
    /**
     * 给服务端发送断开连接指令
     */
    public static boolean exit(){
        //服务端收到你要断开的指令
        boolean getExitOk = false;
        if(sendMsg("exit")){
            String[] retForExit = recvMsg().split("\\s+");;
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

    /**
     * 声明资源时，读取客户端输入
     * resourceName     --输入资源名
     * deviceName       --程序读取用户名（由login保存）
     * path             --用户输入绝对路径（需包含文件名）
     * status           --默认是1（自己添加自己的，自己当然在线了）
     * @return Resource实例
     */
    public static Resource readResourceMsg() {
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
}
