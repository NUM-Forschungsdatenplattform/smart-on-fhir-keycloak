package de.vita.num.smart.authentication;

public class SmartOnFhirException extends Exception{
    public SmartOnFhirException(String pMsg, Exception pEx){super(pMsg,pEx);}
    public SmartOnFhirException(String pMsg){super(pMsg);}
}
