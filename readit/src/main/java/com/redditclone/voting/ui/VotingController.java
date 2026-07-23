package com.redditclone.voting.ui;

import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.dto.VoteCommand;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.service.VoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VotingController {

    private final VoteService voteService;

    @PostMapping
    public VoteResult vote(@Valid @RequestBody VoteCommand command) {
        return command.targetType() == VoteTargetType.POST
                ? voteService.voteOnPost(command.voterId(), command.targetId(), command.value())
                : voteService.voteOnComment(command.voterId(), command.targetId(), command.value());
    }

    @DeleteMapping
    public VoteResult removeVote(@RequestParam Long voterId,
                                 @RequestParam VoteTargetType targetType,
                                 @RequestParam Long targetId) {
        return targetType == VoteTargetType.POST
                ? voteService.removePostVote(voterId, targetId)
                : voteService.removeCommentVote(voterId, targetId);
    }
}
