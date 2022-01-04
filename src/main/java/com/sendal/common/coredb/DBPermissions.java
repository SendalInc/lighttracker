package com.sendal.common.coredb;

import java.io.Serializable;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.sendal.common.coredb.DBPermissionsConfiguration;

import com.sendal.common.coredb.DBPermissionsConfiguration;
import com.sendal.common.coredb.DBPermissionsCybermodel;
import com.sendal.common.coredb.DBPermissionsStates;
import com.sendal.common.coredb.DBPermissionsControl;
import com.sendal.common.coredb.DBPermissionsUsers;

// CLOVER:OFF
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DBPermissions implements Serializable {

    public static final String PERMISSION_READ = "read";
    public static final String PERMISSION_READWRITE = "readwrite";

    private DBPermissionsConfiguration configuration;

    public DBPermissionsConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(DBPermissionsConfiguration configuration) {
        this.configuration = configuration;
    }


    private DBPermissionsCybermodel cybermodel;

    public DBPermissionsCybermodel getCybermodel() {
        return cybermodel;
    }

    public void setCybermodel(DBPermissionsCybermodel cybermodel) {
        this.cybermodel = cybermodel;
    }


    private DBPermissionsStates states;

    public DBPermissionsStates getStates() {
        return states;
    }

    public void setStates(DBPermissionsStates states) {
        this.states = states;
    }


    private DBPermissionsControl control;

    public DBPermissionsControl getControl() {
        return control;
    }

    public void setControl(DBPermissionsControl control) {
        this.control = control;
    }

    private DBPermissionsUsers users;
    
    public DBPermissionsUsers getUsers() {
        return users;
    }

    public void setUsers(DBPermissionsUsers users) {
        this.users = users;
    }
}
// CLOVER:ON