package org.xivo.cti.message;

public class PhoneConfigUpdate extends CtiMessage implements CtiEvent<UserUpdateListener>{
    private Integer userId;
    private Integer Id;
    private String number;

    public Integer getId() {
        return Id;
    }

    public void setId(Integer id) {
        Id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    @Override
    public void notify(UserUpdateListener listener) {
        listener.onPhoneConfigUpdate(this);

    }

}
