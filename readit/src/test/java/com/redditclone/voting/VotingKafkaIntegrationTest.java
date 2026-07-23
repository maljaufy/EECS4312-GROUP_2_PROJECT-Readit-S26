package com.redditclone.voting;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.redditclone.comments.repository.CommentRepository;
import com.redditclone.notification.domain.OutboxEvent;
import com.redditclone.notification.repository.OutboxRepository;
import com.redditclone.notification.repository.ProcessedEventRepository;
import com.redditclone.posts.domain.Post;
import com.redditclone.posts.repository.PostRepository;
import com.redditclone.shared.test.TestcontainersBase;
import com.redditclone.subreddit.domain.Subreddit;
import com.redditclone.subreddit.repository.SubredditRepository;
import com.redditclone.user.domain.User;
import com.redditclone.user.repository.UserRepository;
import com.redditclone.voting.domain.VoteValue;
import com.redditclone.voting.dto.VoteResult;
import com.redditclone.voting.repository.VoteRepository;
import com.redditclone.voting.service.VoteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@DisplayName("Voting context PostgreSQL and Kafka integration tests")
class VotingKafkaIntegrationTest extends TestcontainersBase {

    private static final String HANDLER_NAME = "voting-karma-handler";

    @Autowired private VoteService voteService;
    @Autowired private VoteRepository voteRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private SubredditRepository subredditRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private ProcessedEventRepository processedEventRepository;
    @Autowired private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired private ObjectMapper objectMapper;

    private User author;
    private User voter;
    private Post post;

    @BeforeEach
    void setUp() {
        processedEventRepository.deleteAll();
        outboxRepository.deleteAll();
        voteRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        subredditRepository.deleteAll();
        userRepository.deleteAll();

        author = saveUser("vote-author");
        voter = saveUser("vote-voter");
        Subreddit subreddit = subredditRepository.save(
                new Subreddit("vote-integration", "Integration fixtures", false));
        post = postRepository.save(new Post("Voting integration", "Body", author, subreddit));
    }

    @Test
    @DisplayName("Kafka redelivery applies karma once and records one processed event")
    void duplicateKafkaDeliveryIsIdempotent() throws Exception {
        VoteResult result = voteService.upvotePost(voter.getId(), post.getId());

        assertEquals(1, result.score());
        assertEquals(0, userRepository.findById(author.getId()).orElseThrow().getKarma(),
                "Karma belongs to the idempotent Kafka side effect");

        OutboxEvent outbox = outboxRepository.findAll().stream()
                .filter(event -> "VoteCastEvent".equals(event.getEventType()))
                .findFirst()
                .orElseThrow();
        JsonNode payload = objectMapper.readTree(outbox.getPayload());
        String eventId = payload.get("eventId").asText();

        kafkaTemplate.send("vote.events", outbox.getAggregateId(), outbox.getPayload()).get();
        kafkaTemplate.send("vote.events", outbox.getAggregateId(), outbox.getPayload()).get();

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            assertEquals(1, userRepository.findById(author.getId()).orElseThrow().getKarma());
            assertEquals(1, processedEventRepository.countByEventIdAndHandlerName(eventId, HANDLER_NAME));
        });
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertEquals(1, userRepository.findById(author.getId()).orElseThrow().getKarma());
            assertEquals(1, processedEventRepository.countByEventIdAndHandlerName(eventId, HANDLER_NAME));
        });

        VoteResult repeatedCommand = voteService.upvotePost(voter.getId(), post.getId());
        assertFalse(repeatedCommand.changed());
        assertEquals(1, outboxRepository.findAll().stream()
                .filter(event -> "VoteCastEvent".equals(event.getEventType())).count());
    }

    @Test
    @DisplayName("Clicking the selected post vote direction removes the vote")
    void repeatedUiVoteDirectionTogglesVoteOff() {
        VoteResult selected = voteService.togglePostVote(
                voter.getId(), post.getId(), VoteValue.UPVOTE);

        assertEquals(VoteValue.UPVOTE, selected.currentVote());
        assertEquals(1, selected.score());
        assertEquals(VoteValue.UPVOTE,
                voteService.getPostVote(voter.getId(), post.getId()).orElseThrow());

        VoteResult removed = voteService.togglePostVote(
                voter.getId(), post.getId(), VoteValue.UPVOTE);

        assertNull(removed.currentVote());
        assertEquals(0, removed.score());
        assertTrue(removed.changed());
        assertTrue(voteService.getPostVote(voter.getId(), post.getId()).isEmpty());
        assertEquals(0, voteService.getPostScore(post.getId()));
    }

    private User saveUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com");
        user.setPasswordHash("not-used-in-integration-test");
        return userRepository.save(user);
    }
}
