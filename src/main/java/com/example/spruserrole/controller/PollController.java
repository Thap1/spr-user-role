package com.example.spruserrole.controller;

import com.example.spruserrole.repository.PollRepository;
import com.example.spruserrole.repository.UserRepository;
import com.example.spruserrole.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/polls")
public class PollController {
    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    private PollSer
}
