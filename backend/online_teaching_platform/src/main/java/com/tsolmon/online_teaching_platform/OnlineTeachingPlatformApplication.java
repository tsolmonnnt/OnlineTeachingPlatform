package com.tsolmon.online_teaching_platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OnlineTeachingPlatformApplication {

	public static void main(String[] args) {
		DotEnvBootstrap.load();
		SpringApplication.run(OnlineTeachingPlatformApplication.class, args);
	}

}
