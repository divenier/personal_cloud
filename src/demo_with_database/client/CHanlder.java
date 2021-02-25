package demo_with_database.client;

/**
 * @author divenier
 * @date 2021/2/24 15:39
 */

import demo_with_database.pojo.Resource;
import demo_with_database.utils.Constants;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * 用于客户端的输入输出，以及处理各种信息
 *
 */
class CHanlder extends Thread{
    /**
     * clientUserName，存储本客户端的用户名，用于标记是否在登录状态
     */
    public Boolean isRunning;
    public Socket client;
    public InputStream is;
    public OutputStream os;
    //    public FileInputStream fis;
//    public FileOutputStream fos;
//    public PrintStream ps;
//    public BufferedWriter bw;
//    public BufferedReader br;
//    public ObjectOutputStream oos;
    public Scanner scanner;
    public String clientUserName;
    //存放自己拥有的资源，code+path
//    public HashMap<String,String> code2path;

    public CHanlder(Socket clientSocket,boolean b) throws IOException {
        this.client = clientSocket;
        this.is = clientSocket.getInputStream();
        this.os = clientSocket.getOutputStream();
//        this.fis = new FileInputStream(clientSocket.getInputStream());
//        this.br = new BufferedReader(new InputStreamReader(is,"UTF-8"));
//        this.bw = new BufferedWriter(new PrintWriter(os));
//        this.ps = new PrintStream(new BufferedOutputStream(os));
//        this.oos = new ObjectOutputStream(clientSocket.getOutputStream());
        //登录之后修改为用户名
        this.clientUserName = "NOT_INIT";
        this.isRunning = b;
//        this.code2path = new HashMap<String,String>();
//        this.waitForExit = false;
    }

    public void closeSocket(){
        if(this.client != null){
            try {
                this.is.close();
                this.os.close();
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
            if(client != null && isRunning){
                int len = is.read(recvBytes);
                recvMesg = new String(recvBytes,0,len);
            }
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
                String[] splitDatas = null;
                if("list".equals(cmd)){
                    //不同的资源之间用#分割
                    splitDatas = arr[3].split("#");
                }else{
                    //对于其他命令的回复，用空格分割
                    splitDatas = arr[3].split("\\s+");
                }
                dataLength = splitDatas.length;
                for (int i = 0; i < dataLength; i++) {
                    datas[i] = splitDatas[i];
                }
            }else if("req".equals(msgType)){
                //是服务端的请求，包含get file和心跳检测
                //TODO
                cmd = arr[1];
                String[] splitArgs = arr[2].split("\\s+");
                //第三行args 是文件path;
                for (int i = 0; i < splitArgs.length; i++) {
                    args[i] = splitArgs[i];
                }
                //第四行data是文件名 + 文件code
                String[] splitDatas = arr[3].split("\\s+");
                dataLength = splitDatas.length;
                for (int i = 0; i < dataLength; i++) {
                    datas[i] = splitDatas[i];
                }
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
                        closeSocket();
                        this.isRunning = false;
                        clientUserName = "NOT_INIT";
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
                case "get":
                    //准备接收文件
                    File fileGot = recvFile(datas[0],Integer.parseInt(datas[1]));
                    System.out.println(fileGot.getName());
                    break;
                default:
                    break;
            }
        }
        else{
            //处理服务器的要求
            //TODO
            switch (cmd){
                //服务端想要文件
                case "get":
                    //发送响应报文，准备给服务端发送文件了，文件名+文件长度
                    /*
                    resp&
                    status&
                    cmd&
                    filename fileLength username
                     */
                    File file = new File(args[0]);
                    //resp&  200&  get& filename fileLength fileCode
                    sendReq("resp&" + Constants.SUCCESS_CODE + "&" + cmd + "&" + datas[0] + " " + file.length() + " " + datas[1]);
                    sendFile(args[0]);
                    break;
                default:
                    break;
            }

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
            regStatus = "SEND_REGIST_OK";
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
        //在本机记录下自己汇报的资源--不对，每次运行都归零了！！
//        code2path.put(r.getCode(),r.getPath());
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
     * 提示用户输入要获取的文件的参数，并读取参数
     * @return code
     */
    public String readGetCmd(){
        System.out.println("请输入资源code（唯一编码）");
        Scanner scanner = new Scanner(System.in);
        String resourceCode = scanner.nextLine();

        return resourceCode;
    }

    /**
     * 向服务端发送get文件的请求
     * @return
     */
    public String getResource(){
        String sendGet = null;
        String resourceCode = readGetCmd();
        if(sendReq("req&" + "get&" + resourceCode + "&" + "GET_DATA")){
            sendGet = "SEND_GET_OK";
            //服务端阻塞，等待服务端有了文件后，就向我返回文件
        }else{
            System.out.println("发送get请求失败");
        }
        return sendGet;
    }

    /**
     * 从服务端接收文件
     * @param fileLength
     * @return
     */
    public File recvFile(String fileName,Integer fileLength){
        //从其他流接收文件，
        //用字节数组存储接收到的数据，
        byte[] recvBytes = new byte[4096];
        Integer recvLength = 0;
        //作为接收方暂存的位置
        String path = "C:\\Users\\Charl\\Desktop\\testFolder\\";
        File file = new File(path + fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            while(recvLength < fileLength){
                //记录已经接收的字节数
                int len = is.read(recvBytes);
                recvLength += len;
                fos.write(recvBytes,0,len);
            }
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        String savePath = fileName + fileLength;
        return file;
    }

    /**
     * 向服务端发送文件
     * @param filePath
     * @return
     */
    public String sendFile(String filePath){
        String sendOK = null;
        File file = new File(filePath);
        //TODO
        //发送文件
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] fileData = new byte[4096];
            int len = 0;
            while((len = fis.read(fileData)) > 0){
                os.write(fileData,0,len);
            }
            os.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
//        sendReq("resp&" + Constants.SUCCESS_CODE + "&" + file.length() + "&" + "假装sendfile");
        return sendOK;
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
            try {
                client.shutdownOutput();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            System.out.println("发送exit指令失败");
        }
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
            if(client != null){
                recvMsg();
            }
        }
    }
}