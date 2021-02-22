package demo_with_database.server;

import demo_with_database.pojo.Resource;
import demo_with_database.pojo.User;
import demo_with_database.utils.Constants;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

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
    /*    private DataInputStream dis;
        private DataOutputStream dos;*/
    private BufferedReader br;
    private BufferedWriter bw;
    private PrintStream ps;
    private ObjectInputStream ois;
    private ObjectOutputStream oos;
    public Boolean isRunning;

    //存储客户端socket的登录用户名
    private String clientName;

    public Handler(Socket s,Boolean isRunning){
        this.clientSocket = s;
        this.isRunning = isRunning;
        try {
/*            this.dis = new DataInputStream(s.getInputStream());
            this.dos = new DataOutputStream(s.getOutputStream());*/
            this.br = new BufferedReader(new InputStreamReader(s.getInputStream(),"UTF-8"));
            this.bw = new BufferedWriter(new PrintWriter(s.getOutputStream()));
            this.ps = new PrintStream(new BufferedOutputStream(s.getOutputStream()));
            this.ois = new ObjectInputStream(s.getInputStream());
            this.oos = new ObjectOutputStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收客户端的信息，DataInputStream对于处理数据更方便
     * 把服务端的函数也分开弄
     * 1. 2/19晚，暂时不涉及报文，直接用最基础的命令
     *      1. 需要整一个发送函数
     *      2. 客户端对服务端返回的信息也要用readUTF()读取
     * 2.2/21，server首次对client的返回信息，头部会多三个乱码，太奇怪了；
     */
    private void recvRequest(){
        //收到的整个信息
        String reqMesg = null;
        //解析到的命令（第一个指令，regist,login等等）
        String cmd = null;
        //用于按照正则表达式分割信息
        String[] arr;
        String[] args = new String[4];
        /**
         * regist       -- cmd + username + pwd + lanip + publicip
         * login        -- cmd + username + pwd
         * exit         -- cmd + nothing
         * report       -- cmd + nothing 资源通过对象直接传入
         * list         -- cmd + all/resourceName
         * get          -- cmd + code(自己选择获取哪一个)
         */
        try {
            reqMesg = br.readLine();
            System.out.println("接收到的信息是: " + reqMesg);
            //按照空格分割命令
            arr = reqMesg.split("\\s+");
            cmd = arr[0];
            //如果这个命令附带的有参数
            if(arr.length > 1){
                for(int i = 0;i < arr.length - 1;i++){
                    args[i] = arr[i+1];
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        switch (cmd){
            case "regist":
                System.out.println("接收到客户端的注册请求");
                if(regist(args[0],args[1],args[2],args[3])){
                    System.out.println("注册成功");
                    send(Constants.SUCCESS_CODE + " " + "REGIST_OK");
                }else{
                    System.out.println("注册失败");
                    send(Constants.ERROR_CODE + " " + "REGIST_ERROR");
                }
                break;
            case "login":
                System.out.println("接收到客户端的登录请求");
                if(login(args[0],args[1])){
                    System.out.println("应答登录成功");
                    //把登录成功的用户名再返回去
                    send(Constants.SUCCESS_CODE + " " + args[0] + " " + "LOGIN_OK");
                }else{
                    System.out.println("登录失败");
                    send(Constants.ERROR_CODE + " " + "LOGIN_ERROR");
                }
                break;
            case "report":
                Resource resObj = recvResource();
                System.out.println("添加资源" + DaoHelper.addResource(resObj));
                break;
            case "list":
                String s = list(args[0]);
                if(send(s)){
                    System.out.println("返回资源列表成功");
                }else{
                    System.out.println("返回资源列表失败");
                }
                break;
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
            bw.write(sendMsg);
            //相当于手动添加了换行符，让readline可以读取
            bw.newLine();
            bw.flush();
/*            ps.println(sendMsg);
            ps.flush();*/
            System.out.println("运行了一次send()函数，发送的消息是：" + sendMsg);
            System.out.println("发送的string长度是" + sendMsg.length());
            sendSuccess = true;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return sendSuccess;
    }

    /**
     * 判断当前是否已登录，如未登录则权限受限，不允许report和list
     * @return 当前是否已登录
     */
    public boolean isLogin(){
        return clientName == null ? false : true;
    }

    /**
     * 执行注册，要修改数据库
     * @return 返回数据库受影响的行数
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
     * 接收资源对象
     * @return 资源实例
     */
    public Resource recvResource(){
        Resource resObj = null;
        try {
            resObj = (Resource) ois.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return resObj;
    }

    /**
     * 查询结果
     * @param arg -all查询所有东西 -文件名 查询该文件
     * @return 查询结果，不同条用&分隔开，可以用于直接返回给客户端
     *
     */
    public String list(String arg){
        Resource[] resourceList = DaoHelper.getResourceList(arg);
        StringBuilder sb = new StringBuilder();
        for (Resource r:resourceList) {
            sb.append(r.getResourceName() + " " + r.getDeviceName() + " " + r.getPath() + " " + r.getStatus() + " " + r.getCode() + " " + r.getNote());
            //分割不同的资源
            sb.append("&");
        }
        //删掉最后一个&
        sb.deleteCharAt(sb.length()-1);
        return sb.toString();
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
        if(send(Constants.SUCCESS_CODE + " " + "EXIT_OK")){
            if(DaoHelper.changeUserStatus(this.clientName,0) == 1){
                int resourceStatusChanged = DaoHelper.changeResourceStatus(this.clientName,0);
                //返回exitOK成功后才能把状态结束
                this.isRunning = false;
                retMsgSuccess = true;
                clientName = null;
            }else{
                System.out.println("下线用户时出现意外——请检查数据库");
            }
        }else{
            System.out.println("返回exit消息失败");
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
//            parseRequest();
            recvRequest();
        }
        closeSocket();
    }
}

