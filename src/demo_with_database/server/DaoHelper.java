package demo_with_database.server;

import demo_with_database.pojo.Resource;
import demo_with_database.pojo.User;
import demo_with_database.utils.JdbcUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * @author divenier
 * @date 2021/2/22 18:41
 * 用来放修改数据库的方法
 */
public class DaoHelper {

    /**
     * 用于插入新的用户
     * @param user 用户实例
     * @return 受影响的行数 ——1
     */
    public static int addUser(User user){
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
                //初始注册未登录，状态为不可用
                Object[] params = {user.getUserName(),user.getUserPassword(),0,user.getLanIp(),user.getPublicIp()};
                updateRows = JdbcUtils.execute(connection, pstm, sql, params);
                connection.commit();
                //因为catch中还要回滚，所以connection暂时不关闭，在finally中关闭
                JdbcUtils.closeResource(null, pstm, null);
            }
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
     * 通过用户名，查询到数据库中的用户
     * @param name 给定的用户名
     * @return User实例，查询道的这个用户
     */
    public static User getUserByName(String name){
        Connection connection = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        User user = null;

        try {
            connection = JdbcUtils.getConnection();
            connection.setAutoCommit(false);
            if(null != connection){
                String sql = "select * from user where username=?";
                Object[] params = {name};
                rs = JdbcUtils.execute(connection, pstm, rs, sql, params);
                if(rs.next()){
                    user = new User();
                    user.setUserName(rs.getString("username"));
                    user.setUserPassword(rs.getString("password"));
                    user.setStatus(rs.getInt("status"));
                    user.setLanIp(rs.getString("lanip"));
                    user.setPublicIp(rs.getString("publicip"));
                }
                connection.commit();
                JdbcUtils.closeResource(null, pstm, null);
            }
        } catch (Exception e) {
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

        //如果没有查到就返回null
        return user;
    }

    /**
     * 把用户的状态修改为可用/不可用（数据库内部）
     * @param username 要修改的用户的名字
     * @return 受影响的行数
     */
    public static int changeUserStatus(String username,int status){
        int affactRows = 0;
        Connection connection = null;
        try {
            connection = JdbcUtils.getConnection();
            connection.setAutoCommit(false);
            PreparedStatement pstm = null;
            if(null != connection){
                String sql = "update user set status = ? where username = ?;";
                Object[] params = {status,username};
                affactRows = JdbcUtils.execute(connection, pstm, sql, params);
                connection.commit();
                JdbcUtils.closeResource(null, pstm, null);
            }
        } catch (Exception e) {
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
        return affactRows;
    }

    /**
     * 修改username持有的资源的状态
     * @param username 用户名/设备名
     * @param status    要修改的目的状态
     * @return 受影响的行数（资源数量）
     */
    public static int changeResourceStatus(String username,int status){
        //受影响的行数，几都有可能
        int affactRows = 0;
        Connection connection = null;
        try {
            connection = JdbcUtils.getConnection();
            connection.setAutoCommit(false);
            PreparedStatement pstm = null;
            if(null != connection){
                String sql = "update resource set status = ? where devicename = ?;";
                Object[] params = {status,username};
                affactRows = JdbcUtils.execute(connection, pstm, sql, params);
                connection.commit();
                JdbcUtils.closeResource(null, pstm, null);
            }
        } catch (Exception e) {
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
        return affactRows;
    }

    /**
     *
     * @param arg all/文件名
     * @return 查询到的resource数组
     */
    public static Resource[] getResourceList(String arg){
        Connection connection = null;
        PreparedStatement pstm = null;
        ResultSet rs = null;
        ArrayList<Resource> arrayList = new ArrayList<>();

        try {
            connection = JdbcUtils.getConnection();
            connection.setAutoCommit(false);
            if(null != connection){
                String sql = null;
                //查询的是所有资源
                if("all".equalsIgnoreCase(arg)){
                    sql = "select * from resource";
                    Object[] params = {};
                    rs = JdbcUtils.execute(connection, pstm, rs, sql, params);
                }else{
                    //查询某个资源
                    sql = "select * from resource where resourceName = ?";
                    Object[] params = {arg};
                    rs = JdbcUtils.execute(connection, pstm, rs, sql, params);
                }
                if(rs.next()){
                    Resource r = new Resource(rs.getString("resourceName"),rs.getString("deviceName"),rs.getString("path"),rs.getInt("status"),rs.getString("code"),rs.getString("note"));
                    arrayList.add(r);
                }
                connection.commit();
                JdbcUtils.closeResource(null, pstm, null);
            }
        } catch (Exception e) {
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

        Resource[] resourceArr = new Resource[arrayList.size()];
        for (int i = 0; i < arrayList.size(); i++) {
            resourceArr[i] = arrayList.get(i);
        }
        return resourceArr;
    }
    /**
     * 做成添加资源
     * @return 添加资源是否成功
     */
    public static boolean addResource(Resource resource){
        boolean addSucceess = false;
        Connection connection = null;
        try {
            connection = JdbcUtils.getConnection();
            connection.setAutoCommit(false);
            PreparedStatement pstm = null;
            int updateRows = 0;
            if(null != connection){
                String sql = "insert into resource (resourcename, devicename, path, status, code, note) " +
                        "values(?,?,?,?,?,?)";
                Object[] params = {resource.getResourceName(),resource.getDeviceName(),resource.getPath(),resource.getStatus(),resource.getCode(),resource.getNote()};
                updateRows = JdbcUtils.execute(connection, pstm, sql, params);
                connection.commit();
                JdbcUtils.closeResource(null, pstm, null);
            }
            if(updateRows > 0){
                addSucceess = true;
                System.out.println("add success!");
            }else{
                System.out.println("add failed!");
            }
        } catch (Exception e) {
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
        return addSucceess;
    }
}
