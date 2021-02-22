package demo_with_database.client;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.*;

/**
 * @author divenier
 * @date 2021/2/22 20:49
 */
public class IPAddressHelper {
    /**
     * 获取本机内网ip
     * @return 内网ip的String形式
     */
    public static String getLocalIp(){
        String ip = null;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ip;
    }

    /**
     * 获取本机外网ip
     * @return 外网ip的String形式
     */
    public static String getPublicIP(){
        String ip=null;
        try {
            URL url = new URL("https://www.taobao.com/help/getip.php");
            URLConnection URLconnection = url.openConnection();
            HttpURLConnection httpConnection = (HttpURLConnection) URLconnection;
            int responseCode = httpConnection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream in = httpConnection.getInputStream();
                InputStreamReader isr = new InputStreamReader(in);
                BufferedReader bufr = new BufferedReader(isr);
                String str;
                while ((str = bufr.readLine()) != null) {
                    ip=str;
                }
                bufr.close();
            } else {
                System.err.println("失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if(ip!=null){
            char[] chs=ip.toCharArray();
            ip="";
            for(int i=0;i<chs.length;i++){
                if((chs[i]>=48&&chs[i]<=57)||chs[i]==46){
                    ip+=chs[i];
                }
            }
        }
        return ip;
    }
}
