package com.redditclone.comments;

import com.redditclone.comments.domain.Comment;
import com.redditclone.comments.domain.CommentSortOption;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.comments.service.CommentService;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.shared.test.TestcontainersBase;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.repository.SubredditRepository;
import com.redditclone.user.domain.User;
import com.redditclone.user.repository.UserRepository;
import com.redditclone.voting.domain.Vote;
import com.redditclone.voting.domain.VoteTargetType;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.repository.VoteRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@DisplayName("Comment context PostgreSQL integration tests")
class CommentContextIntegrationTest extends TestcontainersBase {

    @Autowired private CommentService commentService;
    @Autowired private CommentRepository commentRepository;
    @Autowired private VoteRepository voteRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubredditRepository subredditRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private MeterRegistry meterRegistry;

    private Post post;
    private User author;
    private User voterOne;
    private User voterTwo;

    @BeforeEach
    void setUp() {
        author = saveUser("comment-author");
        voterOne = saveUser("comment-voter-one");
        voterTwo = saveUser("comment-voter-two");
        Subreddit subreddit = subredditRepository.save(
                new Subreddit("comment-integration", "Integration fixtures", false));
        post = postRepository.save(new Post("Comment integration", "Body", author, subreddit));
    }

    @Test
    @DisplayName("Ranks best and controversial comments using persisted votes")
    void ranksComments() {
        Comment highScore = saveComment("Popular but one-sided", 10);
        Comment balanced = saveComment("Balanced discussion", 0);
        Comment oneSided = saveComment("Two upvotes", 2);

        voteRepository.saveAll(List.of(
                new Vote(voterOne.getId(), VoteTargetType.COMMENT, balanced.getId(), VoteValue.UPVOTE),
                new Vote(voterTwo.getId(), VoteTargetType.COMMENT, balanced.getId(), VoteValue.DOWNVOTE),
                new Vote(voterOne.getId(), VoteTargetType.COMMENT, oneSided.getId(), VoteValue.UPVOTE),
                new Vote(voterTwo.getId(), VoteTargetType.COMMENT, oneSided.getId(), VoteValue.UPVOTE)
        ));
        voteRepository.flush();

        List<Comment> best = commentService.findTopLevelByPost(post, CommentSortOption.BEST);
        List<Comment> controversial = commentService.findTopLevelByPost(post, CommentSortOption.CONTROVERSIAL);

        assertEquals(highScore.getId(), best.getFirst().getId());
        assertEquals(balanced.getId(), controversial.getFirst().getId());
    }

    @Test
    @DisplayName("Uses PostgreSQL full-text search and records search latency")
    void searchesCommentsWithPostgresFullText() {
        Comment expected = saveComment("Reliable event delivery with PostgreSQL search", 0);
        saveComment("A completely unrelated sentence", 0);
        commentRepository.flush();

        List<Comment> results = commentService.searchComments(post.getId(), "reliable event");

        assertEquals(List.of(expected.getId()), results.stream().map(Comment::getId).toList());
        Integer indexCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM pg_indexes
                WHERE tablename = 'comments' AND indexname = 'idx_comments_body_search'
                """, Integer.class);
        assertEquals(1, indexCount);
        assertTrue(meterRegistry.get("readit.comments.latency")
                .tag("operation", "search").tag("outcome", "success").timer().count() >= 1);
    }

    private Comment saveComment(String body, int score) {
        Comment comment = new Comment(post, author, body);
        comment.setVoteScore(score);
        return commentRepository.save(comment);
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("not-used-in-integration-test");
        return userRepository.save(user);
    }
}
