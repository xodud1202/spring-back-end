package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

	private final UserRepository userRepository;

	/**
	 * Loads the user by their username.
	 * @param username the username of the user to be loaded
	 * @return the user details of the specified username
	 * @throws UsernameNotFoundException if a user with the provided username could not be found
	 */
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepository.findByLoginId(username)
				.orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
	}
}