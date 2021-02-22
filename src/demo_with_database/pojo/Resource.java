package demo_with_database.pojo;

import java.io.Serializable;

/**
 * @author divenier
 * @date 2021/2/16 14:36
 * 资源类 （每个资源都是唯一的，同样的资源可能有多个设备持有，算多个资源）
 * 只是做记录，不存储资源
 * 应该是可序列化的
 */
public class Resource implements Serializable {
    /**
     * resourceName --资源文件名
     * deviceName   --存储该资源的设备名(username)
     * path         --文件的绝对路径
     * status       --该资源是否可用   1-可用 0-不可用
     * code         --资源唯一编码（对  文件存储路径 + 文件名 + 设备名  使用MD5加密）
     * note         --用户report时的一些备注
     */
    private String resourceName;
    private String deviceName;
    private String path;
    private Integer status;
    private String code;
    private String note;

    public Resource(String resourceName, String deviceName, String path, Integer status, String code, String note) {
        this.resourceName = resourceName;
        this.deviceName = deviceName;
        this.path = path;
        this.status = status;
        this.code = code;
        this.note = note;
    }

    public String getResourceName() {
        return resourceName;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "Resource{" +
                "resourceName='" + resourceName + '\'' +
                ", deviceName='" + deviceName + '\'' +
                ", status=" + status +
                ", code='" + code + '\'' +
                ", note='" + note + '\'' +
                '}';
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
