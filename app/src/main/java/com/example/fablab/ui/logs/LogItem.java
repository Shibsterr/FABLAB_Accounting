package com.example.fablab.ui.logs;

public class LogItem {
    private String dateTime;
    private String userName;
    private String email;
    private String itemName;
    private String desc;
    private String quantity;
    private boolean isAddition; // true for addition, false for subtraction

    public LogItem(String dateTime, String userName, String email, String itemName, String quantity, boolean isAddition) {
        this.dateTime = dateTime;
        this.userName = userName;
        this.email = email;
        this.itemName = itemName;
        this.quantity = quantity;
        this.isAddition = isAddition;
    }
    public LogItem(String dateTime, String fullname, String email, String code, String desc){
        this.dateTime = dateTime;
        this.userName = fullname;
        this.email = email;
        this.itemName = code;
        this.desc = desc;
    }

    public String getDesc(){return desc;}
    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getQuantity() {
        return quantity;
    }

    public void setQuantity(String quantity) {
        this.quantity = quantity;
    }

    public boolean isAddition() {
        return isAddition;
    }

    public void setAddition(boolean addition) {
        isAddition = addition;
    }
}
