package com.example.spruserrole.controller;

import com.example.spruserrole.exception.AppException;
import com.example.spruserrole.model.Role;
import com.example.spruserrole.model.RoleName;
import com.example.spruserrole.model.User;
import com.example.spruserrole.payload.ApiResponse;
import com.example.spruserrole.payload.JwtAuthenticationResponse;
import com.example.spruserrole.payload.LoginRequest;
import com.example.spruserrole.payload.SignUpRequest;
import com.example.spruserrole.repository.RoleRepository;
import com.example.spruserrole.repository.UserRepository;
import com.example.spruserrole.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Autowired
    PasswordEncoder passwordEncoder;

    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest){
        System.out.println("------------Vao Login / Controller");

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsernameOrEmail(),
                        loginRequest.getPassword()
                )
        );
        System.out.println("------------Authenticate se tra ra : Principal --- Credentials --- Authenticated --- Details --- Granted Authoritie(role)");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        System.out.println("------------Set Authentication vao getContext trong SecurityContextHolder");

        String jwt = jwtTokenProvider.genarateToken(authentication);
        System.out.println("-----------tao Jwt voi authentication :" + jwt);

        return ResponseEntity.ok(new JwtAuthenticationResponse(jwt));
    }

    @PostMapping("/signup")
    public ResponseEntity registerUser(@Valid @RequestBody SignUpRequest signUpRequest){
        if (userRepository.existsByUsername(signUpRequest.getUsername())){
            return new ResponseEntity(new ApiResponse(false, "Usernam is alrealy taken!"), HttpStatus.BAD_REQUEST);
        }
        if (userRepository.existsByEmail(signUpRequest.getEmail())){
            return new ResponseEntity(new ApiResponse(false, "Email Address alreadly in use!"), HttpStatus.BAD_REQUEST);
        }
        User user = new User();
        user.setName(signUpRequest.getName());
        user.setUsername(signUpRequest.getUsername());
        user.setEmail(signUpRequest.getEmail());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
        Role role = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow(() -> new AppException("User not set."));

        user.setRoles(Collections.singleton(role));
        User result = userRepository.save(user);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/users/{username}").buildAndExpand(result.getUsername()).toUri();

        return ResponseEntity.created(location).body(new ApiResponse(true, "User refistered successfully"));

    }
}
