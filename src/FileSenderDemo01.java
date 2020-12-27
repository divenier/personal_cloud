import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

/**
 * @author divenier
 * @date 2020/12/27 10:23
 */
public class FileSenderDemo01 {
    public static void main(String[] args) throws IOException {
        Socket socket = new Socket("localhost",6666);
        String filePath = "./resource/kkb.mkv";
        try(FileInputStream fis = new FileInputStream(filePath);
            OutputStream os = socket.getOutputStream()){

            int len = 0;
            byte[] data = new byte[1024];
            while((len = fis.read(data)) > -1){
                os.write(data);
            }
            System.out.println("file write done");
        }
        socket.close();
        System.out.println("the file sender disconnected");
    }
}
