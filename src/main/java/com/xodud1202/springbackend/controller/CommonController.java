package com.xodud1202.springbackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CommonController {
	@GetMapping("/hello")
	public Map<String, String> hello() {
		Map<String, String> result = new HashMap<>();
		result.put("hello", "world");
		result.put("hello2", "world2");
		return result;
	}
}
