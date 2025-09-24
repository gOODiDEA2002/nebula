package io.nebula.discovery.core;

public class ServiceDiscoveryException extends Exception {

    private static final long serialVersionUID = 1L;

    public ServiceDiscoveryException(String message) {
        super(message);
    }

    public ServiceDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public ServiceDiscoveryException(Throwable cause) {
        super(cause);
    }
}
