package com.example.spruserrole.controller;

import com.example.spruserrole.exception.ResourceNotFoundException;
import com.example.spruserrole.model.User;
import com.example.spruserrole.payload.*;
import com.example.spruserrole.repository.PollRepository;
import com.example.spruserrole.repository.UserRepository;
import com.example.spruserrole.repository.VoteRepository;
import com.example.spruserrole.security.CurrentUser;
import com.example.spruserrole.security.UserPrincipal;
import com.example.spruserrole.service.PollService;
import com.example.spruserrole.util.AppConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private PollService pollService;

    private final static Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    @GetMapping("/user/me")
    @PreAuthorize("hasRole('USER')")
    public UserSummary getCurrentUser(@CurrentUser UserPrincipal userPrincipal) {
        UserSummary userSummary = new UserSummary(userPrincipal.getId(), userPrincipal.getUsername(), userPrincipal.getName());
        return userSummary;
    }

    @GetMapping("/user/checkUsernameAvailability")
    public UserIdentityAvailability checkUsernameAvailability(@RequestParam(value = "username") String username) {
        Boolean isAvailable = !userRepository.existsByUsername(username);
        return new UserIdentityAvailability(isAvailable);
    }

    @GetMapping("/user/checkEmailAvailability")
    public UserIdentityAvailability checkEmailAvailability(@RequestParam(value = "email") String email) {
        Boolean isAvailable = !userRepository.existsByEmail(email);
        return new UserIdentityAvailability(isAvailable);
    }

    @GetMapping("/user/{username}")
    public UserProfile getUserProfile(@PathVariable(value = "username") String username) {
        User user = userRepository.findByUsername(username).orElseThrow(
                () -> new ResourceNotFoundException("User", "username", username));
        long pollCount = pollRepository.countByCreatedBy(user.getId());
        long voteCount = voteRepository.countByUserId(user.getId());

        UserProfile userProfile = new UserProfile(user.getId(), user.getUsername(), user.getName(), user.getCreatedAt(), pollCount, voteCount);
        return userProfile;
    }

    @GetMapping("/user/{username}/polls")
    public PagedResponse<PollResponse> getPollsCreateBy(@PathVariable(value = "username") String username,
                                                        @CurrentUser UserPrincipal userPrincipal,
                                                        @RequestParam(value = "pgae", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                                                        @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size
    ) {

        return pollService.getPollsCreatedBy(username, userPrincipal, page, size);
    }

    @GetMapping("/user/{username}/votea")
    public PagedResponse<PollResponse> getPollsVoteBy(@PathVariable(value = "username") String username,
                                                      @CurrentUser UserPrincipal userPrincipal,
                                                      @RequestParam(value = "pgae", defaultValue = AppConstants.DEFAULT_PAGE_NUMBER) int page,
                                                      @RequestParam(value = "size", defaultValue = AppConstants.DEFAULT_PAGE_SIZE) int size
    ) {

        return pollService.getPollsVoteBy(username, userPrincipal, page, size);
    }

}
