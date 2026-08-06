package com.monsterhouse.admin.entity;

public enum AdminRole {
    SUPER_ADMIN,
    MANAGER;
    public String authority(){
        return "ROLE_" + name();
    }
}
