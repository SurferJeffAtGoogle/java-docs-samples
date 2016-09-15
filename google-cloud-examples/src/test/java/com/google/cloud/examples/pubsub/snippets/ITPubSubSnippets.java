/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.cloud.examples.pubsub.snippets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.cloud.Identity;
import com.google.cloud.Page;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.pubsub.PubSub;
import com.google.cloud.pubsub.PubSubOptions;
import com.google.cloud.pubsub.ReceivedMessage;
import com.google.cloud.pubsub.Subscription;
import com.google.cloud.pubsub.SubscriptionId;
import com.google.cloud.pubsub.SubscriptionInfo;
import com.google.cloud.pubsub.Topic;
import com.google.cloud.pubsub.TopicInfo;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class ITPubSubSnippets {

  private static final String NAME_SUFFIX = UUID.randomUUID().toString();

  private static PubSub pubsub;
  private static PubSubSnippets pubsubSnippets;

  @Rule
  public Timeout globalTimeout = Timeout.seconds(300);

  private static String formatForTest(String resourceName) {
    return resourceName + "-" + NAME_SUFFIX;
  }

  @BeforeClass
  public static void beforeClass() {
    pubsub = PubSubOptions.defaultInstance().service();
    pubsubSnippets = new PubSubSnippets(pubsub);
  }

  @AfterClass
  public static void afterClass() throws Exception {
    if (pubsub != null) {
      pubsub.close();
    }
  }

  @Test
  public void testTopicAndSubscription() throws ExecutionException, InterruptedException {
    String topicName1 = formatForTest("topic-name1");
    String topicName2 = formatForTest("topic-name2");
    Topic topic1 = pubsubSnippets.createTopic(topicName1);
    Topic topic2 = pubsubSnippets.createTopicAsync(topicName2);
    assertNotNull(topic1);
    assertNotNull(topic2);
    topic1 = pubsubSnippets.getTopic(topicName1);
    topic2 = pubsubSnippets.getTopicAsync(topicName2);
    assertNotNull(topic1);
    assertNotNull(topic2);
    Set<Topic> topics = Sets.newHashSet(pubsubSnippets.listTopics().iterateAll());
    while (!topics.contains(topic1) || !topics.contains(topic2)) {
      Thread.sleep(500);
      topics = Sets.newHashSet(pubsubSnippets.listTopics().iterateAll());
    }
    topics = Sets.newHashSet(pubsubSnippets.listTopicsAsync().iterateAll());
    while (!topics.contains(topic1) || !topics.contains(topic2)) {
      Thread.sleep(500);
      topics = Sets.newHashSet(pubsubSnippets.listTopicsAsync().iterateAll());
    }
    String subscriptionName1 = formatForTest("subscription-name1");
    String subscriptionName2 = formatForTest("subscription-name2");
    Subscription subscription1 =
        pubsubSnippets.createSubscription(topicName1, subscriptionName1);
    Subscription subscription2 =
        pubsubSnippets.createSubscriptionAsync(topicName2, subscriptionName2);
    assertNotNull(subscription1);
    assertNotNull(subscription2);
    Page<SubscriptionId> page = pubsubSnippets.listSubscriptionsForTopic(topicName1);
    while (Iterators.size(page.iterateAll()) < 1) {
      page = pubsubSnippets.listSubscriptionsForTopic(topicName1);
    }
    assertEquals(subscriptionName1, page.iterateAll().next().subscription());
    page = pubsubSnippets.listSubscriptionsForTopicAsync(topicName2);
    while (Iterators.size(page.iterateAll()) < 1) {
      page = pubsubSnippets.listSubscriptionsForTopicAsync(topicName2);
    }
    assertEquals(subscriptionName2, page.iterateAll().next().subscription());
    String endpoint = "https://" + pubsub.options().projectId() + ".appspot.com/push";
    pubsubSnippets.replacePushConfig(subscriptionName1, endpoint);
    pubsubSnippets.replacePushConfigAsync(subscriptionName2, endpoint);
    subscription1 = pubsubSnippets.getSubscription(subscriptionName1);
    subscription2 = pubsubSnippets.getSubscriptionAsync(subscriptionName2);
    assertEquals(endpoint, subscription1.pushConfig().endpoint());
    assertEquals(endpoint, subscription2.pushConfig().endpoint());
    pubsubSnippets.replacePushConfigToPull(subscriptionName1);
    pubsubSnippets.replacePushConfigToPullAsync(subscriptionName2);
    subscription1 = pubsubSnippets.getSubscription(subscriptionName1);
    subscription2 = pubsubSnippets.getSubscriptionAsync(subscriptionName2);
    assertNull(subscription1.pushConfig());
    assertNull(subscription2.pushConfig());
    assertTrue(pubsubSnippets.deleteTopic(topicName1));
    assertTrue(pubsubSnippets.deleteTopicAsync(topicName2));
    assertTrue(pubsubSnippets.deleteSubscription(subscriptionName1));
    assertTrue(pubsubSnippets.deleteSubscriptionAsync(subscriptionName2));
  }

  @Test
  public void testPublishAndPullMessage() throws Exception {
    String topicName = formatForTest("topic-name");
    String subscriptionName = formatForTest("subscription-name");
    pubsub.create(TopicInfo.of(topicName));
    pubsub.create(SubscriptionInfo.of(topicName, subscriptionName));
    assertNotNull(pubsubSnippets.publishOneMessage(topicName));
    pubsubSnippets.pull(subscriptionName);
    assertNotNull(pubsubSnippets.publishOneMessage(topicName));
    assertEquals(2, pubsubSnippets.publishMessages(topicName).size());
    assertEquals(2, pubsubSnippets.publishMessageList(topicName).size());
    Set<ReceivedMessage> messages = new HashSet<>();
    while (messages.size() < 5) {
      Iterators.addAll(messages, pubsub.pull(subscriptionName, 100));
    }
    Iterator<ReceivedMessage> messageIterator = messages.iterator();
    pubsubSnippets.modifyAckDeadlineOneMessage(subscriptionName, messageIterator.next().ackId());
    pubsubSnippets.modifyAckDeadlineMoreMessages(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    pubsubSnippets.modifyAckDeadlineMessageList(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    messageIterator = messages.iterator();
    pubsubSnippets.nackOneMessage(subscriptionName, messageIterator.next().ackId());
    pubsubSnippets.nackMoreMessages(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    pubsubSnippets.nackMessageList(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    messages.clear();
    while (messages.size() < 5) {
      Iterators.addAll(messages, pubsub.pull(subscriptionName, 100));
    }
    messageIterator = messages.iterator();
    pubsubSnippets.ackOneMessage(subscriptionName, messageIterator.next().ackId());
    pubsubSnippets.ackMoreMessages(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    pubsubSnippets.ackMessageList(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    assertTrue(pubsubSnippets.deleteTopic(topicName));
    assertTrue(pubsubSnippets.deleteSubscription(subscriptionName));
  }

  @Test
  public void testPublishAndPullMessageAsync() throws Exception {
    String topicName = formatForTest("topic-name-async");
    String subscriptionName = formatForTest("subscription-name-async");
    pubsub.create(TopicInfo.of(topicName));
    pubsub.create(SubscriptionInfo.of(topicName, subscriptionName));
    pubsubSnippets.pullWithMessageConsumer(subscriptionName);
    assertNotNull(pubsubSnippets.publishOneMessageAsync(topicName));
    pubsubSnippets.pullAsync(subscriptionName);
    assertNotNull(pubsubSnippets.publishOneMessageAsync(topicName));
    assertEquals(2, pubsubSnippets.publishMessagesAsync(topicName).size());
    assertEquals(2, pubsubSnippets.publishMessageListAsync(topicName).size());
    Set<ReceivedMessage> messages = new HashSet<>();
    while (messages.size() < 5) {
      Iterators.addAll(messages, pubsub.pull(subscriptionName, 100));
    }
    Iterator<ReceivedMessage> messageIterator = messages.iterator();
    pubsubSnippets.modifyAckDeadlineOneMessageAsync(subscriptionName,
        messageIterator.next().ackId());
    pubsubSnippets.modifyAckDeadlineMoreMessagesAsync(subscriptionName,
        messageIterator.next().ackId(), messageIterator.next().ackId());
    pubsubSnippets.modifyAckDeadlineMessageListAsync(subscriptionName,
        messageIterator.next().ackId(), messageIterator.next().ackId());
    messageIterator = messages.iterator();
    pubsubSnippets.nackOneMessageAsync(subscriptionName, messageIterator.next().ackId());
    pubsubSnippets.nackMoreMessagesAsync(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    pubsubSnippets.nackMessageListAsync(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    messages.clear();
    while (messages.size() < 5) {
      Iterators.addAll(messages, pubsub.pull(subscriptionName, 100));
    }
    messageIterator = messages.iterator();
    pubsubSnippets.ackOneMessageAsync(subscriptionName, messageIterator.next().ackId());
    pubsubSnippets.ackMoreMessagesAsync(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    pubsubSnippets.ackMessageListAsync(subscriptionName, messageIterator.next().ackId(),
        messageIterator.next().ackId());
    assertTrue(pubsubSnippets.deleteTopicAsync(topicName));
    assertTrue(pubsubSnippets.deleteSubscriptionAsync(subscriptionName));
  }

  @Test
  public void testTopicSubscriptionPolicy() {
    String topicName = formatForTest("test-topic-policy");
    Topic topic = pubsubSnippets.createTopic(topicName);
    Policy policy = pubsubSnippets.getTopicPolicy(topicName);
    assertNotNull(policy);
    policy = pubsubSnippets.replaceTopicPolicy(topicName);
    assertTrue(policy.bindings().containsKey(Role.viewer()));
    assertTrue(policy.bindings().get(Role.viewer()).contains(Identity.allAuthenticatedUsers()));
    List<Boolean> permissions = pubsubSnippets.testTopicPermissions(topicName);
    assertTrue(permissions.get(0));
    String subscriptionName = formatForTest("test-subscription-policy");
    Subscription subscription = pubsubSnippets.createSubscription(topicName, subscriptionName);
    policy = pubsubSnippets.getSubscriptionPolicy(subscriptionName);
    assertNotNull(policy);
    policy = pubsubSnippets.replaceSubscriptionPolicy(subscriptionName);
    assertTrue(policy.bindings().containsKey(Role.viewer()));
    assertTrue(policy.bindings().get(Role.viewer()).contains(Identity.allAuthenticatedUsers()));
    permissions = pubsubSnippets.testSubscriptionPermissions(subscriptionName);
    assertTrue(permissions.get(0));
    topic.delete();
    subscription.delete();
  }

  @Test
  public void testTopicPolicyAsync() throws ExecutionException, InterruptedException {
    String topicName = formatForTest("test-topic-policy-async");
    Topic topic = pubsubSnippets.createTopic(topicName);
    Policy policy = pubsubSnippets.getTopicPolicyAsync(topicName);
    assertNotNull(policy);
    policy = pubsubSnippets.replaceTopicPolicyAsync(topicName);
    assertTrue(policy.bindings().containsKey(Role.viewer()));
    assertTrue(policy.bindings().get(Role.viewer()).contains(Identity.allAuthenticatedUsers()));
    List<Boolean> permissions = pubsubSnippets.testTopicPermissionsAsync(topicName);
    assertTrue(permissions.get(0));
    String subscriptionName = formatForTest("test-subscription-policy-async");
    Subscription subscription = pubsubSnippets.createSubscription(topicName, subscriptionName);
    policy = pubsubSnippets.getSubscriptionPolicyAsync(subscriptionName);
    assertNotNull(policy);
    policy = pubsubSnippets.replaceSubscriptionPolicyAsync(subscriptionName);
    assertTrue(policy.bindings().containsKey(Role.viewer()));
    assertTrue(policy.bindings().get(Role.viewer()).contains(Identity.allAuthenticatedUsers()));
    permissions = pubsubSnippets.testSubscriptionPermissionsAsync(subscriptionName);
    assertTrue(permissions.get(0));
    topic.delete();
    subscription.delete();
  }
}
