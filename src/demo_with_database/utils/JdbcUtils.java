package demo_with_database.utils;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

/**
 * 加载驱动的工具类
 * 用来解耦
 */
public class JdbcUtils {
    private static String driver = null;
    private static String url = null;
    private static String username = null;
    private static String password = null;

    static{//静态代码块,在类加载的时候执行
        init();
    }

    /**
     * 初始化连接参数,从配置文件里获得
     */
    public static void init(){
        InputStream in = JdbcUtils.class.getResourceAsStream("db.properties");
        //这里getResourceAsStream时，properties需要和本类在同一文件夹
        Properties properties = new Properties();
        try {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("加载jdbc配置异常");
        }
        driver = properties.getProperty("driver");
        url = properties.getProperty("url");
        username = properties.getProperty("username");
        password = properties.getProperty("password");
    }

    //获取链接
    public static Connection getConnection() {
        Connection connection = null;
        try{
            Class.forName(driver);
            //建立连接
            connection = DriverManager.getConnection(url,username,password);
            if(!connection.isClosed()) {
                System.out.println("数据库连接成功");
            }
        }catch (SQLException | ClassNotFoundException e){
            e.printStackTrace();
            System.out.println("数据库连接失败");
        }
        return connection;
    }


    /**
     * 查询公共类
     * @param connection
     * @param preparedStatement
     * @param resultSet
     * @param sql
     * @param params
     * @return 查询结果集
     * @throws SQLException
     */
    public static ResultSet execute(Connection connection,PreparedStatement preparedStatement,ResultSet resultSet,String sql,Object[] params) throws SQLException {
        preparedStatement = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            //注意resultSet和数组索引区别
            preparedStatement.setObject(i+1,params[i]);
        }
        resultSet = preparedStatement.executeQuery();
        return resultSet;
    }

    /**
     * 增删改公共方法，增删改不需要返回结果集
     * @param connection
     * @param preparedStatement
     * @param sql
     * @param params
     * @return 受影响的增删改行数
     * @throws SQLException
     */
    public static int execute(Connection connection,PreparedStatement preparedStatement,String sql,Object[] params) throws SQLException {
        preparedStatement = connection.prepareStatement(sql);
        for (int i = 0; i < params.length; i++) {
            //注意resultSet和数组索引区别
            preparedStatement.setObject(i+1,params[i]);
        }
        int updateRows = preparedStatement.executeUpdate();
        return updateRows;
    }

    /**
     * 释放资源
     * @param connection
     * @param preparedStatement
     * @param resultSet
     * @return 是否回收成功 boolean
     */
    public static boolean closeResource(Connection connection,PreparedStatement preparedStatement,ResultSet resultSet){
        //是否回收成功的标志位
        boolean closeSuccess = true;
        if(resultSet != null){
            try{
                resultSet.close();
                //交给GC回收
                resultSet = null;
            }catch (SQLException e){
                e.printStackTrace();
                closeSuccess = false;
            }
        }
        if(preparedStatement != null){
            try{
                preparedStatement.close();
                preparedStatement = null;
            }catch (SQLException e){
                e.printStackTrace();
                closeSuccess = false;
            }
        }
        if(connection != null){
            try{
                connection.close();
                connection = null;
            }catch (SQLException e){
                e.printStackTrace();
                closeSuccess = false;
            }
        }
        return closeSuccess;
    }

}
