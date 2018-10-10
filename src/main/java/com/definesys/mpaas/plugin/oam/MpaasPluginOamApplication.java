package com.definesys.mpaas.plugin.oam;

import com.definesys.mpaas.common.adapter.IMpaasSSOAuthentication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MpaasPluginOamApplication {
	@Autowired(required = false)
	private IMpaasSSOAuthentication auth;

	public static void main(String[] args) {
		SpringApplication.run(MpaasPluginOamApplication.class, args);
	}
}
