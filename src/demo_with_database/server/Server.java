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
     * isRunning    --本线程是否还在运行
     */
    private Socket clientSocket;
    public Boolean isRunning;

    public Handler(Socket s,Boolean isRunning){
        this.clientSocket = s;
        this.isRunning = isRunning;
    }
    /**
     * 处理请求信息，解析
     * 只解析请求和返回响应，处理文件传输和对象就不要用这个了
     */
    public void parseRequest(){
        try(OutputStream os = this.clientSocket.getOutputStream();
              InputStream is = this.clientSocket.getInputStream();
            ObjectInputStream ois = new ObjectInputStream(is))
        {
            byte[] recvRequestData = new byte[1024];
            int recvRequestLen = is.read(recvRequestData);
            String recvRequestMsg = new String(recvRequestData,0,recvRequestLen);
            if("hello".equals(recvRequestMsg)){
                System.out.println("接收到客户端的hello");
                os.write("hi".getBytes(StandardCharsets.UTF_8));
                Resource resObj = (Resource) ois.readObject();
                System.out.println("添加资源" + addResource(resObj));
            }

            recvRequestLen = is.read(recvRequestData);
            recvRequestMsg = new String(recvRequestData,0,recvRequestLen);
            if("exit".equals(recvRequestMsg)){
                System.out.println("接收到客户端的退出请求");
                this.isRunning = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 做成添加资源
     * @return
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
            parseRequest();
        }
        closeSocket();
    }
}

