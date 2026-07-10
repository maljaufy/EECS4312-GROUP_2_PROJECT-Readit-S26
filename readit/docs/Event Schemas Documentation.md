# **Event Schemas Documentation**



## **# Event Schemas**



**This document defines the JSON schemas for all domain events published by the Readit application.**



### **## 1. UserRegisteredEvent**



**Triggered when a new user registers.**



**\*\*Topic:\*\* `user.events`**



**\*\*Schema:\*\***

**```json**

**{**

&#x20; **"eventId": "UUID",**

&#x20; **"occurredAt": "ISO-8601 datetime",**

&#x20; **"userId": 123,**

&#x20; **"username": "string",**

&#x20; **"email": "string"**

**}**





### **## 2. PostCreatedEvent**



**Triggered when a new post is created.**



**Topic: post.events**



**\*\*Schema:\*\***

**```json**



**{**

&#x20; **"eventId": "UUID",**

&#x20; **"occurredAt": "ISO-8601 datetime",**

&#x20; **"postId": 456,**

&#x20; **"title": "string",**

&#x20; **"authorUsername": "string",**

&#x20; **"subredditName": "string"**

**}**



### **## 3. VoteCastEvent**



**Triggered when a user votes on a post or comment.**



**Topic: vote.events**



**\*\*Schema:\*\***

**```json**



**{**

&#x20; **"eventId": "UUID",**

&#x20; **"occurredAt": "ISO-8601 datetime",**

&#x20; **"targetType": "POST|COMMENT",**

&#x20; **"targetId": 456,**

&#x20; **"userId": 123,**

&#x20; **"currentVote": "UPVOTE|DOWNVOTE|null",**

&#x20; **"delta": 1,  // score change: +1, -1, +2, or -2**

&#x20; **"newScore": 42**

**}**





### **## 4. CommentAddedEvent**



**Triggered when a comment is added to a post.**



**Topic: comment.events**



**\*\*Schema:\*\***

**```json**



**{**

&#x20; **"eventId": "UUID",**

&#x20; **"occurredAt": "ISO-8601 datetime",**

&#x20; **"commentId": 789,**

&#x20; **"postId": 456,**

&#x20; **"authorUsername": "string",**

&#x20; **"text": "string",**

&#x20; **"parentCommentId": null  // or comment ID if reply**

**}**





### **## 5. NotificationEvent**



**Used to send notifications (e.g., email, push).**



**Topic: notification.events**



**\*\*Schema:\*\***

**```json**



**{**

&#x20; **"eventId": "UUID",**

&#x20; **"occurredAt": "ISO-8601 datetime",**

&#x20; **"recipientUserId": 123,**

&#x20; **"type": "REPLY|MENTION|VOTE|MODERATION",**

&#x20; **"message": "string",**

&#x20; **"link": "string"**

**}**



