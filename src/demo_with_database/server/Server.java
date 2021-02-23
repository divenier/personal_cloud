package demo_with_database.server;

import demo_with_database.pojo.Resource;
import demo_with_database.pojo.User;
import demo_with_database.utils.Constants;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author divenier
 * @date 2021/2/16 13:57
 * 服务端的主程序
 */
public class Server {

    public static void main(String[] args) throws IOException {
        System.out.println("---------服务端运行--------");
//        init();
/*        try {
            new Server().start();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        new Server().start();
    }

    /**
     * 服务器端口
     * @throws IOException
     */
    private void start() throws IOException {
        ServerSocket server = new ServerSocket(Constants.SERVER_PORT);
        //用来记录连接的个数
        int num = 1;
//        ExecutorService executorService = Executors.newCachedThreadPool();
        while (true) {
            Socket clientSocket= server.accept();
            System.out.println(num + "个客户端建立了连接");
            //局域网IP
            System.out.println("局域网IP " + clientSocket.getLocalAddress());
//			System.out.println(client.getLocalSocketAddress());//局域网IP加端口
//			System.out.println(client.getRemoteSocketAddress());//公网IP+端口
            //公网IP
            System.out.println("公网IP " + clientSocket.getInetAddress());
            num++;
            Handler handler = new Handler(clientSocket,true);
            handler.start();
//            Detection detection = new Detection(clientSocket,true);
//            detection.start();
        }
    }


    /**
     * 初始化
     * 好像 也不需要连接数据库，需要对数据库操作可以直接调用JdbcUtil
     */
    public static void init(){

    }
}

class Handler extends Thread{
    /**
     * clientSocket       --要处理的socket连接
     * dataInputStream    --用来接收socket的输入流，方便解析
     * dataOutputStream   --输出流
     * reqMesg            --接收到的信息，准备直接用换行符解析
     * isRunning          --本线程是否还在运行
     */
    private Socket clientSocket;
    public InputStream is;
    public OutputStream os;
//    private BufferedReader br;
//    private BufferedWriter bw;
//    private PrintStream ps;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    public Boolean isRunning;

    //存储客户端socket的登录用户名
    private String clientName;

    public Handler(Socket s,Boolean isRunning){
        this.clientSocket = s;
        this.isRunning = isRunning;
        try {
            this.is = s.getInputStream();
            this.os = s.getOutputStream();
//            this.br = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));
//            this.bw = new BufferedWriter(new PrintWriter(s.getOutputStream()));
//            this.ps = new PrintStream(new BufferedOutputStream(s.getOutputStream()));
            this.ois = new ObjectInputStream(is);
            this.oos = new ObjectOutputStream(os);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 随时接收客户端的消息
     * 解析客户的请求，并对之做出响应
     */
    private void handleRequest(){
        //收到的整个信息
        String reqMesg = null;
        //是请求还是响应
        String msgType = null;
        //解析到的命令（第一个指令，regist,login等等）
        String cmd = null;
        //参数-最多包含，resourceName,deviceName,path,code,note 5个参数
        String[] args = new String[5];
        String[] datas= new String[2];

        //用于按照正则表达式分割信息
        String[] arr;
        try {
//            reqMesg = br.readLine();
            byte[] recvBytes = new byte[4096];
            int len = is.read(recvBytes);
            reqMesg = new String(recvBytes,0,len);
            System.out.println("接收到的信息是: " + reqMesg);

            //按行分割消息
            arr = reqMesg.split("&");
            msgType = arr[0];
            //是请求
            if("req".equals(msgType)){
                cmd = arr[1];
                String[] splitArgs = arr[2].split("\\s+");
                for (int i = 0; i < splitArgs.length; i++) {
                    args[i] = splitArgs[i];
                }
                String[] splitDatas = arr[3].split("\\s+");
                for (int i = 0; i < splitDatas.length; i++) {
                    datas[i] = splitDatas[i];
                }
            }else{
                //是客户端的响应

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         * regist       -- req & cmd & username + pwd & lanip + publicip
         * login        -- req & cmd & username + pwd & nothing
         * exit         -- req & cmd & nothing
         * report       -- req & cmd & nothing 资源通过对象直接传入
         * list         -- req & cmd & all/resourceName
         * get          -- req & cmd & code(自己选择获取哪一个)
         */
        switch (cmd){
            case "regist":
                System.out.println("接收到客户端的注册请求");
                if(regist(args[0],args[1],datas[0],datas[1])){
                    System.out.println("注册成功");
                    send("resp&" + Constants.SUCCESS_CODE + "&" + cmd + "&" + "REGIST_OK");
                }else{
                    System.out.println("注册失败");
                    send("resp&" + Constants.ERROR_CODE + "&" + cmd + "&" +"REGIST_ERROR");
                }
                break;
            case "login":
                System.out.println("接收到客户端的登录请求");
                if(login(args[0],args[1])){
                    System.out.println("应答登录成功");
                    send("resp&" + Constants.SUCCESS_CODE + "&" + cmd + "&" + "LOGIN_OK");
                }else{
                    System.out.println("登录失败");
                    send("resp&" + Constants.ERROR_CODE + "&" + cmd + "&" + "LOGIN_ERROR");
                }
                break;
            case "report":
                Resource resObj = new Resource(args[0],args[1],args[2],1,args[3],args[4]);
                if(DaoHelper.addResource(resObj)){
                    System.out.println("添加资源成功");
                    send("resp&" + Constants.SUCCESS_CODE + "&" + cmd + "&" + "REPORT_OK");
                }
                break;
            case "list":
                String s = list(args[0]);
                //查询失败
                if("500".equals(s)){
                    send("resp&" + Constants.ERROR_CODE + "&" + cmd + "&" + "LIST_ERROR");
                }else{
                    //查询成功
                    send("resp&" + Constants.SUCCESS_CODE + "&" + cmd + "&" + s);
                }
                break;
            case "get":
                if(getResourceStatus(args[0])){
                    System.out.println("请求资源可用");
                }else{
                    System.out.println("请求资源不可用");
                }
                break;
            /*
            exit的消息必须在关闭之前发送，否则就发不出去了！！
             */
            case "exit":
                if(clientExit()){
                    System.out.println("退出成功");
                }
                break;
            default:
                System.out.println("客户端命令错误，请让它重新输入");
                break;
        }
    }

    /**
     * 给客户端发送消息
     * @param sendMsg
     * @return
     */
    public boolean send(String sendMsg){
        boolean sendSuccess = false;
        if(sendMsg == null || "".equals(sendMsg)){
            System.out.println("发送的消息为空");
            return false;
        }
        try {
/*            bw.write(sendMsg);
            //相当于手动添加了换行符，让readline可以读取
            bw.newLine();
            bw.flush();*/
            os.write(sendMsg.getBytes(StandardCharsets.UTF_8));
            System.out.println("运行了一次send()函数，发送的消息是：" + sendMsg);
            System.out.println("发送的string长度是" + sendMsg.length());
            sendSuccess = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sendSuccess;
    }

    /**
     * 执行注册，要修改数据库
     * @return 注册是否成功
     */
    public boolean regist(String username,String pwd,String lanip,String publicip){
        boolean addSuccess = false;
        User regUser = new User(username,pwd,0,lanip,publicip);
        int resultForAddUser = DaoHelper.addUser(regUser);
        if(resultForAddUser == 1){
            addSuccess = true;
        }
        return addSuccess;
    }

    /**
     * 登录——待完善 修改用户状态 ——已完善
     * 数据库验证用户名+密码
     * @return 登录是否成功
     */
    public boolean login(String username,String pwd){
        boolean loginSuccess = false;

        User user = DaoHelper.getUserByName(username);
        if(user == null){
            throw new RuntimeException("用户不存在");
        }else if(pwd.equals(user.getUserPassword())){
            //输入的密码和查询到的用户的密码相同
            loginSuccess = true;
            //登录了要把在线状态设为1
            DaoHelper.changeUserStatus(user.getUserName(),1);
            DaoHelper.changeResourceStatus(user.getUserName(),1);
            //handler保存该socket连接的用户名
            this.clientName = username;
        }
        return loginSuccess;
    }

    /**
     * 查询结果
     * @param arg -all查询所有东西 -文件名 查询该文件
     * @return 查询结果，不同条用&分隔开，可以用于直接返回给客户端
     *
     */
    public String list(String arg){
        Resource[] resourceList = DaoHelper.getResourceList(arg);
        //如果没有查到，
        if(resourceList.length == 0){
            return Constants.ERROR_CODE;
        }
        StringBuilder sb = new StringBuilder();
        for (Resource r:resourceList) {
            sb.append(r.getResourceName() + " " + r.getDeviceName() + " " + r.getPath() + " " + r.getStatus() + " " + r.getCode() + " " + r.getNote());
            //分割不同的资源
            sb.append("#");
        }
        //删掉最后一个&
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
    }

    /**
     * 判断用户请求的资源是否可用
     * @return
     */
    public boolean getResourceStatus(String code){
        /**
         * 1. 从数据库中查询文件绝对路径和对应状态
         * 2. if(status != 1) 不可用，重新输入命令
         */
        boolean available = false;
        Resource resourceFound = DaoHelper.getResourceByCode(code);
        if(resourceFound.getStatus() != 1){
            send(Constants.ERROR_CODE + " RESOURCE_OUTLINE");
            System.out.println("客户端请求的这个资源不在线，没法用");
        }else{
            User resourceHolder = DaoHelper.getUserByName(resourceFound.getDeviceName());
            //成功码 + path + publicIP + lanIP + port
            send(Constants.SUCCESS_CODE + " " + resourceFound.getPath() + " " + resourceHolder.getPublicIp() + " " + resourceHolder.getLanIp());
            available = true;
        }
        return available;
    }

    /**
     * 处理客户端退出的一系列问题
     * 1. 给客户端返回报文
     * 2. 把用户下线
     * 3. 把所有该用户持有的资源下线
     * 4. 终止处理线程
     * @return
     */
    public boolean clientExit(){
        boolean retMsgSuccess = false;
        if(send("resp&" + Constants.SUCCESS_CODE + "&" + "exit&" + "EXIT_OK")){
            if(DaoHelper.changeUserStatus(this.clientName,0) == 1){
                int resourceStatusChanged = DaoHelper.changeResourceStatus(this.clientName,0);
                //返回exitOK成功后才能把状态结束
                retMsgSuccess = true;
                clientName = null;
                this.isRunning = false;
            }else{
                System.out.println("下线用户时出现意外——请检查数据库");
            }
        }else{
            System.out.println("给客户端发送EXITOK失败");
        }

        return retMsgSuccess;
    }
    /**
     * 关闭socket连接
     */
    public void closeSocket(){
        if(this.clientSocket != null){
            try {
                this.clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void run() {
        while (isRunning){
            handleRequest();
        }
        closeSocket();
    }
}

//问题的关键就是，让客户端可以随时接收到服务端的消息，而不是想接收再接收
class Detection extends Thread{
    /**
     * br       --用于接收信息
     * ois      --用于接收对象
     */
    private BufferedReader br;
    private ObjectInputStream ois;
    public boolean needDetection;

    public Detection(Socket s,boolean b){
        try {
            this.br = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));
            this.ois = new ObjectInputStream(s.getInputStream());
            this.needDetection = b;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        while(needDetection){
            needDetection = false;
        }
    }
}
