package com.example.spruserrole.util;

import com.example.spruserrole.model.Choice;
import com.example.spruserrole.model.Poll;
import com.example.spruserrole.model.User;
import com.example.spruserrole.payload.ChoiceResponse;
import com.example.spruserrole.payload.PollResponse;
import com.example.spruserrole.payload.UserSummary;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ModelMapper {
    public static PollResponse mapPollToPollResponse(Poll poll, Map<Long, Long> choiceVotesMap, User user, Long userVote) {
        PollResponse pollResponse = new PollResponse();
        pollResponse.setId(poll.getId());
        pollResponse.setQuestion(poll.getQuestion());
        pollResponse.setCreationDateTime(poll.getCreatedAt());
        pollResponse.setExpirationDateTime(poll.getExpirationDateTime());
        Instant now = Instant.now();
        pollResponse.setIsExpired(poll.getExpirationDateTime().isBefore(now));

        List<ChoiceResponse> choiceResponses = poll.getChoices().stream().map(choice -> {

            ChoiceResponse choiceResponse = new ChoiceResponse();

            choiceResponse.setId(choice.getId());
            choiceResponse.setText(choice.getText());
            if (choiceVotesMap.containsKey(choice.getId())) {
                choiceResponse.setVoteCount(choiceVotesMap.get(choice.getId()));
            } else {
                choiceResponse.setVoteCount(0);
            }

            return choiceResponse;

        }).collect(Collectors.toList());

        pollResponse.setChoiceResponses(choiceResponses);
        UserSummary userSummary = new UserSummary();

        userSummary.setId(user.getId());
        userSummary.setUsername(user.getUsername());
        userSummary.setName(user.getName());

        pollResponse.setCreatedBy(userSummary);

        if (userVote != null) {
            pollResponse.setSelectedChoice(userVote);
        }
        long totalVotes = pollResponse.getChoiceResponses().stream().mapToLong(ChoiceResponse::getVoteCount).sum();
        pollResponse.setTotalVotes(totalVotes);
        return pollResponse;
    }
}
