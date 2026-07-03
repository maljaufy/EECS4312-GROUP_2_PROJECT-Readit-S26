package com.redditclone.comments.service;

import com.redditclone.comments.model.Comment;
import com.redditclone.comments.repository.CommentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentService {
    private final CommentRepository commentRepository;

    public Comment createComment(UUID postId, UUID userId, String content) {
        Comment comment = Comment.createComment(postId, userId, content);
        return commentRepository.save(comment);
    }
    public Comment replyComment(UUID postId, UUID userId, String content, UUID replyId)
    {
        Comment comment = Comment.reply(postId, userId, content,replyId);
        return commentRepository.save(comment);
    }
    public Comment editComment(UUID commentId, String content)
    {
        Comment comment = commentRepository.findByCommentId(commentId);
        comment.setContent(content);
        return commentRepository.save(comment);
    }

    public void deleteComment(UUID commentId)
    {
        commentRepository.deleteById(commentId);
    }
    public List<Comment> rankCommentsOnPost_Contr(UUID postId)
    {
        return commentRepository.findMostContreversial(postId);

    }
    public List<Comment> rankCommentsOnPost_voteCount(UUID postId, boolean descending)
    {
        if(descending) {
            return commentRepository.findMostPopularComment(postId);
        }
        else {
            return commentRepository.findLeastPopularComment(postId);
        }
    }
    public List<Comment> rankCommentsOnPost_recency(UUID postId, boolean descending)
    {
        if(descending) {
            return commentRepository.findMostRecentComment(postId);
        }
        else {
            return commentRepository.findOldestComment(postId);
        }
        }
}