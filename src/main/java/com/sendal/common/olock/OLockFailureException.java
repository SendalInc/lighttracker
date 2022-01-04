package com.sendal.common.olock;

import java.lang.Exception;

public class OLockFailureException extends Exception { 
    public OLockFailureException(String errorMessage) {
        super(errorMessage);
    }
}