package demo_with_database.client;

import demo_with_database.pojo.Resource;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * @author divenier
 * @date 2021/2/16 14:20
 * 客户端
 */
public class Client {
    private static Socket client;
//    private static DataInputStream is; // 数据输出流
//    private static DataOutputStream os; // 数据输入流
//    private static Scanner scanner;

    public static void main(String[] args) throws IOException {
        System.out.println("客户端开始运行");
        Socket socket = new Socket(InetAddress.getByName("localhost"),6666);
        try(OutputStream os = socket.getOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(os);
            InputStream is = socket.getInputStream())
        {
            //打招呼，第一次
            os.write("hello".getBytes(StandardCharsets.UTF_8));

            //对服务端返回的打招呼信息处理
            byte[] responseData = new byte[1024];
            int responseLen = is.read(responseData);
            String responseMsg = new String(responseData,0,responseLen);
            System.out.println("接收到第一次server返回的消息" + responseMsg);
            if(responseMsg.equals("hi")){
                System.out.println("服务端和我说hi");
                Resource resource = new Resource("a.txt","divenier",1,"9762349ygha9sfafda","some notes");
                oos.writeObject(resource);
                oos.flush();
                os.write("exit".getBytes(StandardCharsets.UTF_8));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        if(socket != null){
            socket.close();
        }
        System.out.println("客户端停止运行");
    }
}
