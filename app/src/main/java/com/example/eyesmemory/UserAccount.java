package com.example.eyesmemory;

public class UserAccount {
    private String userId;
    private String pwd;
    private String userName;
    private int points;

    // 기본 생성자
    public UserAccount() {
    }

    // 매개변수가 있는 생성자 추가
    public UserAccount(String userId, String pwd, String userName) {
        this.userId = userId;
        this.pwd = pwd;
        this.userName = userName;
    }

    // Getter 메서드
    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getPwd() {
        return pwd;
    }

    // Setter 메서드
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }
}
