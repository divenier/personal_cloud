package demo_with_database.server;

import demo_with_database.pojo.Resource;
import demo_with_database.utils.Constants;
import demo_with_database.utils.JdbcUtils;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static java.nio.charset.StandardCharsets.UTF_8;

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
                if(regist(args[0],args[1],args[2],args[3]) == 1){
                    System.out.println("注册成功");
                    send(Constants.SUCCESS_CODE + " " + "registOK");
                }else{
                    System.out.println("注册失败");
                    send(Constants.ERROR_CODE + " " + "registERROR");
                }
                break;
            case "login":
                System.out.println("接收到客户端的登录请求");
                if(login()){
                    System.out.println("应答登录成功");
                }
                break;
            case "report":
                Resource resObj = recvResource();
                System.out.println("添加资源" + addResource(resObj));
                break;
            case "exit":
                clientExit();
                System.out.println("接收到客户端的退出请求");
                break;
            default:
                System.out.println("客户端命令错误，请让它重新输入");
                break;
        }
    }

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
     * 执行注册，要修改数据库
     * @return 返回数据库受影响的行数  吧
     */
    public int regist(String username,String pwd,String lanip,String publicip){
        //受影响的行数，插入成功应该是1
        int updateRows = 0;
        Connection connection = null;
        try {
            connection = JdbcUtils.getConnection();
            //开启JDBC事务管理
            connection.setAutoCommit(false);
            //开始插入数据库
            PreparedStatement pstm = null;
            if(null != connection){
                String sql = "insert into user(username, password, status, lanip, publicip) VALUES (?,?,?,?,?)";
                Object[] params = {username,pwd,1,lanip,publicip};
                updateRows = JdbcUtils.execute(connection, pstm, sql, params);
                //因为catch中还要回滚，所以connection暂时不关闭，在finally中关闭
                JdbcUtils.closeResource(null, pstm, null);
            }
            connection.commit();
        } catch (Exception e) {
            //插入失败要回滚
            e.printStackTrace();
            try {
                System.out.println("rollback==================");
                connection.rollback();
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        }finally{
            JdbcUtils.closeResource(connection, null, null);
        }
        return updateRows;
    }

    /**
     * 登录——待完善
     * 数据库验证用户名+密码
     *
     * @return
     */
    public boolean login(){
        boolean loginSuccess = false;
        if(send("hi")){
            loginSuccess = true;
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
     * 做成添加资源
     * @return 添加资源是否成功
     */
    public boolean addResource(Resource resource){
        boolean addSucceess = false;
        Connection connection = null;
        try {
            connection = JdbcUtils.getConnection();
            //开启JDBC事务管理
            connection.setAutoCommit(false);

            //开始插入数据库
            PreparedStatement pstm = null;
            int updateRows = 0;
            if(null != connection){
                String sql = "insert into resource (resourcename, devicename, status, code, note) " +
                        "values(?,?,?,?,?)";
                Object[] params = {resource.getResourceName(),resource.getDeviceName(),resource.getStatus(),resource.getCode(),resource.getNote()};
                updateRows = JdbcUtils.execute(connection, pstm, sql, params);
                JdbcUtils.closeResource(null, pstm, null);
            }

            connection.commit();
            if(updateRows > 0){
                addSucceess = true;
                System.out.println("add success!");
            }else{
                System.out.println("add failed!");
            }

        } catch (Exception e) {
            //插入失败要回滚
            e.printStackTrace();
            try {
                System.out.println("rollback==================");
                connection.rollback();
            } catch (SQLException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }finally{
            JdbcUtils.closeResource(connection, null, null);
        }
        return addSucceess;
    }

    /**
     * 处理客户端退出的一系列问题
     * @return
     */
    public boolean clientExit(){
        boolean retMsgSuccess = send("exitOK");
        if(retMsgSuccess){
            //返回exitOK成功后才能把状态结束
            this.isRunning = false;
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

