package com.example.spruserrole.repository;

import com.example.spruserrole.model.ChoiceVoteCount;
import com.example.spruserrole.model.Vote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VoteRepository extends JpaRepository<Long, Vote> {

    @Query("SELECT NEW com.example.spruserrole.model.ChoiceVoteCount(v.choice.id,count(v.id)) FROM Vote v WHERE v.poll.id in :pollIds GROUP By v.choice.id")
    List<ChoiceVoteCount> countByPollIdInGroupByChoiceId(@Param("pollIds") List<Long> pollIds);

    @Query("SELECT NEW com.example.spruserrole.model.ChoiceVoteCount(v.choice.id, count(v.id)) FROM Vote v WHERE v.poll.id =:pollId GROUP By v.choice.id")
    List<ChoiceVoteCount> countbyPollIdGroupByChoiceId(@Param("pollId") Long pollId);

    @Query("SELECT v FROM Vote v WHERE v.user.id =:userId and v.poll.id in :pollIds")
    List<Vote> findByUseridAndPollIdIn(@Param("userId") Long userId);

    @Query("SELECT COUNT(v.id) FROM Vote v where v.user.id = :userId and v.poll.id in:pollIds")
    Vote findByUserIdAndPollId(@Param("userId") Long userId);

    @Query("SELECT COUNT(v.id) from Vote v where v.user.id =:pollId")
    long countByUserId(@Param("userId") Long userId);

    @Query("SELECT v.poll.id FROM Vote v WHERE v.user.id= :userId")
    Page<Long> findVotePollIdaByUserId(@Param("userId") Long userId, Pageable pageable);

}
