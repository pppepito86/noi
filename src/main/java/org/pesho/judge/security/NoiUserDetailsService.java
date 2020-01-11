package org.pesho.judge.security;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

@Controller
public class NoiUserDetailsService implements UserDetailsService {

	public static final String ROLE_PREFIX = "ROLE_";

	@Autowired
	private JdbcTemplate template;
	
	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		if (username.equalsIgnoreCase("admin")) {
			return new User("admin", "admin", 
					Arrays.asList(new SimpleGrantedAuthority(ROLE_PREFIX + "ADMIN")));
		}
		if (username.equalsIgnoreCase("results")) {
			return new User("results", "noi1_results", 
					Arrays.asList(new SimpleGrantedAuthority(ROLE_PREFIX + "RESULT")));
		}
		
		Optional<Map<String, Object>> result = template.queryForList(
				"select name, password, role from users where name=?", 
				username).stream().findFirst();
		
		return result.map(user -> new User(
				user.get("name").toString(), 
				user.get("password").toString(), 
				Arrays.asList(new SimpleGrantedAuthority(ROLE_PREFIX + user.get("role").toString())))).orElse(null);
	}
	
}