package com.xodud1202.springbackend.service;

import com.xodud1202.springbackend.domain.UserBase;
import com.xodud1202.springbackend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

import java.util.Optional;

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

	/**
	 * Retrieves a user by their login ID.
	 * @param loginId the login ID of the user to be retrieved
	 * @return an {@code Optional} containing the {@code UserBase} if found, or an empty {@code Optional} if no user exists with the provided login ID
	 */
	public Optional<UserBase> loadUserByLoginId(String loginId) {
		return userRepository.findByLoginId(loginId);
	}
}