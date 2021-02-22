package demo_with_database.pojo;

/**
 * @author divenier
 * @date 2021/2/16 14:28
 * 用户类
 */
public class User {
    /**
     * userName     --用户名称 作为账户名，兼有id功能
     * userPassword --密码
     * status       --标识用户的状态，1为可用（在线），0不可用（不在线）
     * lanIp        --局域网IP（优先使用）
     * publicIp     --公网IP（socket双方不在一个局域网再用）
     */
    private String userName;
    private String userPassword;
    private Integer status;
    private String lanIp;
    private String publicIp;

    public User(String userName, String userPassword, Integer status, String lanIp, String publicIp) {
        this.userName = userName;
        this.userPassword = userPassword;
        this.status = status;
        this.lanIp = lanIp;
        this.publicIp = publicIp;
    }
    public User(){}

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserPassword() {
        return userPassword;
    }

    public void setUserPassword(String userPassword) {
        this.userPassword = userPassword;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getLanIp() {
        return lanIp;
    }

    public void setLanIp(String lanIp) {
        this.lanIp = lanIp;
    }

    public String getPublicIp() {
        return publicIp;
    }

    public void setPublicIp(String publicIp) {
        this.publicIp = publicIp;
    }
}
