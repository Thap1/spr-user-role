package com.example.spruserrole.service;

import com.example.spruserrole.exception.BadRequestException;
import com.example.spruserrole.exception.ResourceNotFoundException;
import com.example.spruserrole.model.*;
import com.example.spruserrole.payload.PagedResponse;
import com.example.spruserrole.payload.PollRequest;
import com.example.spruserrole.payload.PollResponse;
import com.example.spruserrole.payload.VoteRequest;
import com.example.spruserrole.repository.PollRepository;
import com.example.spruserrole.repository.UserRepository;
import com.example.spruserrole.repository.VoteRepository;
import com.example.spruserrole.security.UserPrincipal;
import com.example.spruserrole.util.AppConstants;
import com.example.spruserrole.util.ModelMapper;
import org.dom4j.rule.Mode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PollService {
    @Autowired
    private PollRepository pollRepository;

    @Autowired
    private VoteRepository voteRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(PollService.class);

//    PagedResponse 1111

    public PagedResponse<PollResponse> getAllPolls(UserPrincipal userPrincipal, int page, int size) {

        validatePageNumberAndSize(page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Poll> polls = pollRepository.findAll(pageable);

        if (polls.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), polls.getNumber(), polls.getNumber(), polls.getSize(), polls.getTotalPages(), polls.isLast());
        }

        //Start 1
        List<Long> pollIds = polls.map(Poll::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(userPrincipal, pollIds);
        Map<Long, User> creatorMap = getPollCreatorMap(polls.getContent());

        List<PollResponse> pollResponses = polls.map(poll -> {
            return ModelMapper.mapPollToPollResponse(poll, choiceVoteCountMap, creatorMap.get(poll.getCreatedBy()), pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).getContent();
        return new PagedResponse<>(pollResponses, polls.getNumber(), polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());

    }

    //    PagedResponse2222
    public PagedResponse<PollResponse> getPollsCreatedBy(String username, UserPrincipal userPrincipal, int page, int size) {

        validatePageNumberAndSize(page, size);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "CreateAt");
        Page<Poll> polls = pollRepository.findByCreatedBy(user.getId(), pageable);

        if (polls.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), polls.getNumber(), polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
        }
        List<Long> pollIds = polls.map(Poll::getId).getContent();
        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(userPrincipal, pollIds);

        List<PollResponse> pollResponses = polls.map(poll -> {
            return ModelMapper.mapPollToPollResponse(poll, choiceVoteCountMap, user, pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).getContent();

        return new PagedResponse<>(pollResponses, polls.getNumber(), polls.getSize(), polls.getTotalElements(), polls.getTotalPages(), polls.isLast());
    }

    //    PagedResponse333
    public PagedResponse<PollResponse> getPollsVoteBy(String username, UserPrincipal userPrincipal, int page, int size) {
        validatePageNumberAndSize(page, size);
        User user = userRepository.findByUsername(username).orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
        Pageable pageable = PageRequest.of(page, size, Sort.Direction.DESC, "createdAt");
        Page<Long> userVotePollIds = voteRepository.findVotePollIdsByUserId(user.getId(), pageable);
        if (userVotePollIds.getNumberOfElements() == 0) {
            return new PagedResponse<>(Collections.emptyList(), userVotePollIds.getNumber(), userVotePollIds.getSize(), userVotePollIds.getTotalElements(), userVotePollIds.getTotalPages(), userVotePollIds.isLast());
        }
        List<Long> pollIds = userVotePollIds.getContent();
        Sort sort = Sort.by(Sort.Direction.DESC, "cratedAt");
        List<Poll> polls = pollRepository.findByIdIn(pollIds, sort);

        Map<Long, Long> choiceVoteCountMap = getChoiceVoteCountMap(pollIds);
        Map<Long, Long> pollUserVoteMap = getPollUserVoteMap(userPrincipal, pollIds);
        Map<Long, User> creatorMap = getPollCreatorMap(polls);

        List<PollResponse> pollResponses = polls.stream().map(poll -> {
            return ModelMapper.mapPollToPollResponse(poll, choiceVoteCountMap,
                    creatorMap.get(poll.getCreatedBy()),
                    pollUserVoteMap == null ? null : pollUserVoteMap.getOrDefault(poll.getId(), null));
        }).collect(Collectors.toList());
        return new PagedResponse<>(pollResponses, userVotePollIds.getNumber(), userVotePollIds.getSize(), userVotePollIds.getTotalElements(), userVotePollIds.getTotalPages(), userVotePollIds.isLast());
    }

    // createPoll

    public Poll createPoll(PollRequest pollRequest) {

        Poll poll = new Poll();
        poll.setQuestion(pollRequest.getQuestion());
        pollRequest.getChoiceRequests().forEach(choiceRequest -> {
            poll.addChoice(new Choice(choiceRequest.getText()));
        });
        Instant now = Instant.now();
        Instant expirationDateTime = now.plus(Duration.ofDays(pollRequest.getPollLength().getDays())).plus(Duration.ofHours(pollRequest.getPollLength().getHours()));

        return pollRepository.save(poll);
    }

    public PollResponse getPollById(Long pollId, UserPrincipal userPrincipal) {
        Poll poll = pollRepository.findById(pollId).orElseThrow(() ->
                new ResourceNotFoundException("Poll", "id", pollId));
        List<ChoiceVoteCount> voteCounts = voteRepository.countByPollIdGroupByChoiceId(pollId);
        Map<Long, Long> choiceVotesMap = voteCounts.stream().collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));
        User creator = userRepository.findById(poll.getCreatedBy()).orElseThrow(() -> new ResourceNotFoundException("User", "Id", poll.getCreatedBy()));

        Vote userVote = null;
        if (userPrincipal !=null) {
            userVote = voteRepository.findByUserIdAndPollId(userPrincipal.getId(), pollId);
        }
        return ModelMapper.mapPollToPollResponse(poll, choiceVotesMap, creator, userVote !=null ? userVote.getId(): null);

    }
//    PollResponse 44
    public PollResponse castVoteAndGetUpdatedPoll(Long pollId, VoteRequest voteRequest, UserPrincipal userPrincipal){
        Poll poll = pollRepository.findById(pollId).orElseThrow(() -> new ResourceNotFoundException("Poll", "id", pollId));

        if (poll.getExpirationDateTime().isBefore(Instant.now())){
            throw new BadRequestException("Sorry! this Poll has already expired");
        }
        User user = userRepository.getOne(userPrincipal.getId());
        Choice selectedChoice = poll.getChoices().stream().filter(choice-> choice.getId().equals(voteRequest.getChoiceId())).findFirst()
                .orElseThrow(()-> new ResourceNotFoundException("Choice" "Id", voteRequest.getChoiceId()));
        Vote vote = new Vote();
        vote.setPoll(poll);
        vote.setUser(user);
        vote.setChoice(selectedChoice);

        try{
            vote =  voteRepository.save(vote)
        }catch (DataIntegrityViolationException ex){
            LOGGER.info("User {} has already vote");
        }

    }

    private Map<Long, User> getPollCreatorMap(List<Poll> content) {
        List<Long> creatorIds = content.stream().map(Poll::getCreatedBy).distinct().collect(Collectors.toList());
        List<User> creators = userRepository.findByIdIn(creatorIds);
        Map<Long, User> creatorMap = creators.stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return creatorMap;
    }

    private Map<Long, Long> getPollUserVoteMap(UserPrincipal userPrincipal, List<Long> pollIds) {
        Map<Long, Long> pollUserVoteMap = null;
        if (userPrincipal != null) {
            List<Vote> userVote = voteRepository.findByUserIdAndPollIdIn(userPrincipal.getId(), pollIds);
            pollUserVoteMap = userVote.stream().collect(Collectors.toMap(vote -> vote.getPoll().getId(), vote -> vote.getChoice().getId()));
        }
        return pollUserVoteMap;
    }


    private Map<Long, Long> getChoiceVoteCountMap(List<Long> pollIds) {
        List<ChoiceVoteCount> voteCounts = voteRepository.countByPollIdInGroupByChoiceId(pollIds);
        Map<Long, Long> choiceVoteMap = voteCounts.stream().collect(Collectors.toMap(ChoiceVoteCount::getChoiceId, ChoiceVoteCount::getVoteCount));
        return choiceVoteMap;
    }

    private void validatePageNumberAndSize(int page, int size) {

        if (page < 0) {
            throw new BadRequestException("Page number cannot be less than zero");
        }

        if (size > AppConstants.MAX_PAGE) {
            throw new BadRequestException("Page size must not be greater than" + AppConstants.MAX_PAGE);
        }

    }
}
