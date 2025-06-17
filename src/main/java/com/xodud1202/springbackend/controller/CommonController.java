package com.xodud1202.springbackend.controller;

import com.xodud1202.springbackend.domain.UserBase;
import com.xodud1202.springbackend.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
public class CommonController {
	@GetMapping("/hello")
	public Map<String, String> hello() {
		Map<String, String> result = new HashMap<>();
		result.put("hello!!", "world!!");
		result.put("hello!!2", "world!!2");
		System.out.println(result);
		return result;
	}

	/*@PostMapping("/backoffice/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody UserBase param) {
            if ("xodud1202".equals(param.getUsername()) && "qwer".equals(param.getPassword())) {
                    String token = JwtUtil.generateToken(param.getUsername());
                    Map<String, String> result = new HashMap<>();
                    result.put("token", token);
                    return ResponseEntity.ok(result);
            }
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }*/
}
