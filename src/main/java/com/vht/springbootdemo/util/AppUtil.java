package com.vht.springbootdemo.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;

@Component
@RequiredArgsConstructor
public class AppUtil {
    private final Environment env;

    public String getApplicationUrl() {
        try {
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            final String defaultPort = "8080";
            String port = env.getProperty("server.port", defaultPort );
            return "http://" + hostAddress + ":" + port;

        }catch (UnknownHostException ex) {
            throw new RuntimeException("Failed to get application URL", ex);
        }
    }

}
