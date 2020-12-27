import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author divenier
 * @date 2020/12/27 15:29
 */
public class FileReceiverDemo01 {
    public static void main(String[] args) throws IOException {
        ServerSocket ss = new ServerSocket(6666);
        System.out.println("file receiver is running");
        Socket acceptSocket = ss.accept();

        String recvFilePath = "./resource/recvFile.mkv";
        try(InputStream is = acceptSocket.getInputStream();
            FileOutputStream fos = new FileOutputStream(recvFilePath)){
            int len = 0;
            byte[] recvData = new byte[1024];
            while ((len = is.read(recvData)) > -1){
                fos.write(recvData,0,len);
            }
            System.out.println("file receive done");
        }
        if(acceptSocket != null){
            acceptSocket.close();
        }
        if(ss != null){
            ss.close();
        }
    }
}
