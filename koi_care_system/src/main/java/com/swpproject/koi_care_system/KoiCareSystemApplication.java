package com.swpproject.koi_care_system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class KoiCareSystemApplication {

	@GetMapping("/message")
	public String message(){
		return "Congrats ! you app deployed sucessfully in Azure!";
	}
	public static void main(String[] args) {
		SpringApplication.run(KoiCareSystemApplication.class, args);
	}

}
