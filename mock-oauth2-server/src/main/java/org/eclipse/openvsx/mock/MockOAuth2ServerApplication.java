package org.eclipse.openvsx.mock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MockOAuth2ServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(MockOAuth2ServerApplication.class, args);
    }
}