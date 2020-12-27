package demo;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * @author divenier
 * @date 2020/12/27 10:23
 */
public class FileSenderDemo01 {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket(InetAddress.getByName("localhost"),6666);
        String filePath = "./resource/kkb.mkv";

        try(FileInputStream fis = new FileInputStream(filePath);
            OutputStream os = socket.getOutputStream();
            InputStream is = socket.getInputStream())
        {
            String[] nameArr = filePath.split("/");
            String srcFileName = nameArr[nameArr.length - 1];

            //先把文件名传输到接收端，确认接收端做好了接收准备工作再开始发送文件
            os.write(srcFileName.getBytes(StandardCharsets.UTF_8));

            //对接收到的response信息处理
            byte[] responseData = new byte[1024];
            int responseLen = is.read(responseData);
            String responseMsg = new String(responseData,0,responseLen);
            if(responseMsg.equals("refuse")){
                System.out.println("receiver refuse to accept this file, file transmission failed");
                return;
            }
            else if(responseMsg.equals("ready")){
                int len = 0;
                byte[] data = new byte[1024];
                while((len = fis.read(data)) > -1){
                    os.write(data,0,len);
                }
                System.out.println("file write done");
            }
            else{
                throw new Exception("Unknown response message from receiver");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(socket != null){
            socket.close();
        }
        System.out.println("the file sender disconnected");
    }
}
