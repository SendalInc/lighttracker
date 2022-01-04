package com.sendal.externalapicommon.security;

import java.security.Principal;

public class IDPrincipal implements Principal {

    public enum PrincipalType {
        BYPASS, // a test-only mode
        HMAC;
    }

    private PrincipalType principalType;

    public void setPrincipalType(PrincipalType principalType) {
        this.principalType = principalType;
    }

    private String Id;

    private String description;

    public String getDescription() {
        return description;
    }

    public IDPrincipal(String Id, PrincipalType principalType) {
        this.principalType = principalType;
        this.Id = Id;
    }

    public IDPrincipal(String Id, PrincipalType principalType, String description) {
        this.principalType = principalType;
        this.Id = Id;
        this.description = description;
    }

   
    public boolean equals(Object another) {
        boolean isEqual = false;
        if(another != null) {
            if(another instanceof IDPrincipal) {
                isEqual = this.Id.equals(((IDPrincipal)another).getId()) &&
                          this.principalType == ((IDPrincipal)another).getPrincipalType();
            }
        }

        return isEqual;
    }

    public String getId() {
        return Id;
    }

    public String getName() {
        return Id;
    }

    public int hashCode() {
        return Id.hashCode();
    }

    public String toString() {
        return Id;
    }

    public PrincipalType getPrincipalType() {
        return principalType;
    }
}
