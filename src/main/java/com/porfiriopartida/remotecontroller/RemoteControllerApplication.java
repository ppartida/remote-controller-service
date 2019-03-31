package com.porfiriopartida.remotecontroller;

import com.porfiriopartida.remotecontroller.swgoh.ClickBot;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

import java.awt.*;

@ComponentScan({"com.porfiriopartida", "com.porfiriopartida.remotecontroller.screen.config"})
@SpringBootApplication
public class RemoteControllerApplication {

	public static void main(String[] args) {
//		ClickBot clickBot = ClickBot.getInstance();
//		clickBot.start();
		SpringApplication.run(RemoteControllerApplication.class, args);
	}
	static {
		System.setProperty("java.awt.headless", "false");
	}
	@Bean(name = "robot")
	public Robot getRobot() throws AWTException {
		return new Robot();
	}
}
