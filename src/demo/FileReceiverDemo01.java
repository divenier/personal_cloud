package demo;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author divenier
 * @date 2020/12/27 15:29
 */
public class FileReceiverDemo01 {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(6666);
        System.out.println("file receiver is running");
        /**
         * 新增线程处理新来的连接
         * 每个文件发送前先用简短的报文上报文件名
         */
        while(true){
            Socket acceptSocket = ss.accept();
            System.out.println("accept a connection from"+ acceptSocket.getRemoteSocketAddress());
            Handler handler = new Handler(acceptSocket);
            handler.start();
        }
        //serverSocket没办法关闭吗？
    }
}

class Handler extends Thread{
    Socket socket;

    public Handler(Socket s){
        this.socket = s;
    }

    @Override
    public void run() {
        //接收到的文件存储位置
        try(InputStream is = this.socket.getInputStream();
            OutputStream os = this.socket.getOutputStream()
        ){
            byte[] recvRequestData = new byte[1024];
            int recvRequestLen = is.read(recvRequestData);
            String recvRequestMsg = new String(recvRequestData,0,recvRequestLen);
            String[] fileNameArr = recvRequestMsg.split("\\.");
            String fileType = fileNameArr[1];
            //如果文件后缀名是blabla就拒绝接收
            if("blabla".equals(fileType)){
                os.write("refuse".getBytes(StandardCharsets.UTF_8));
            }
            else{
                //告诉发送方准备接收了
                os.write("ready".getBytes(StandardCharsets.UTF_8));
                String recvFileName = "./resource/receiveFiles/" + fileNameArr[0] + "_recv." + fileType;
                try(FileOutputStream fos = new FileOutputStream(recvFileName)){
                    int len = 0;
                    byte[] recvData = new byte[1024];
                    while ((len = is.read(recvData)) > -1){
                        fos.write(recvData,0,len);
                    }
                    System.out.println("file receive done");
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //关闭连接
        if(this.socket != null){
            try {
                this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
