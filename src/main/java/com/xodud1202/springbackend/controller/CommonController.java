package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.UserBase;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CommonController {
	@GetMapping("/hello")
	public Map<String, String> hello() {
		Map<String, String> result = new HashMap<>();
		result.put("hello", "world");
		result.put("hello2", "world2");
		System.out.println(result);
		return result;
	}

	@PostMapping("/backoffice/login")
	public Map<String, String> login(@RequestBody UserBase param) {
		Map<String, String> result = new HashMap<>();
		System.out.println("check parameter");
		System.out.println(param.getId());
		result.put("id", param.getId());
		result.put("password", param.getPassword());
		return result;
	}
}
