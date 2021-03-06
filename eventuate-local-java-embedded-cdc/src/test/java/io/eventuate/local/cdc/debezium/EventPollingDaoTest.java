package io.eventuate.local.cdc.debezium;


import com.google.common.collect.ImmutableList;
import io.eventuate.local.java.jdbckafkastore.EventuateLocalConfiguration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.*;
import java.util.stream.Collectors;

@ActiveProfiles("EventuatePolling")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = EventPollingDaoTest.EventPollingTestConfiguration.class)
@DirtiesContext
@IntegrationTest
public class EventPollingDaoTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private EventPollingDao eventPollingDao;

  @Autowired
  private EventTableChangesToAggregateTopicRelay eventTableChangesToAggregateTopicRelay;

  @org.springframework.context.annotation.Configuration
  @Import({EventuateLocalConfiguration.class, EventTableChangesToAggregateTopicRelayConfiguration.class})
  @EnableAutoConfiguration
  public static class EventPollingTestConfiguration {
  }
//
//  @Before
//  public void init() throws Exception {
//    eventTableChangesToAggregateTopicRelay.stopCapturingChanges();
//  }

  @Test
  public void testFindAndPublish() throws Exception {
    String idPrefix = createEvents();

    eventPollingDao.setMaxEventsPerPolling(1000);

    List<EventToPublish> eventsToTest = new ArrayList<>();

    List<EventToPublish> accumulator;
    while (!(accumulator = eventPollingDao.findEventsToPublish()).isEmpty()) {
      eventsToTest.addAll(accumulator.stream().filter(eventToPublish -> eventToPublish.getEventId().startsWith(idPrefix)).collect(Collectors.toList()));
      eventPollingDao.markEventsAsPublished(accumulator.stream().map(EventToPublish::getEventId).collect(Collectors.toList()));
    }

    Assert.assertEquals(2, eventsToTest.size());

    EventToPublish event1 = eventsToTest.get(0);

    Assert.assertEquals(idPrefix + "_1", event1.getEventId());
    Assert.assertEquals("type1", event1.getEventType());
    Assert.assertEquals("data1", event1.getEventData());
    Assert.assertEquals("entityType1", event1.getEntityType());
    Assert.assertEquals("entityId1", event1.getEntityId());
    Assert.assertEquals("triggeringEvent1", event1.getTriggeringEvent());
    Assert.assertEquals("meta1", event1.getMetadata());


    EventToPublish event2 = eventsToTest.get(1);

    Assert.assertEquals(idPrefix + "_2", event2.getEventId());
    Assert.assertEquals("type2", event2.getEventType());
    Assert.assertEquals("data2", event2.getEventData());
    Assert.assertEquals("entityType2", event2.getEntityType());
    Assert.assertEquals("entityId2", event2.getEntityId());
    Assert.assertEquals("triggeringEvent2", event2.getTriggeringEvent());
    Assert.assertNull(event2.getMetadata());
  }

  @Test
  public void testLimit() throws Exception {
    createEvents();

    eventPollingDao.setMaxEventsPerPolling(1);

    List<EventToPublish> eventsToPublish = eventPollingDao.findEventsToPublish();

    Assert.assertEquals(1, eventsToPublish.size());
  }

  private String createEvents() throws Exception {
    String idPrefix = UUID.randomUUID().toString();

    jdbcTemplate.update("INSERT INTO events VALUES (?, 'type1', 'data1', 'entityType1', 'entityId1', 'triggeringEvent1', 'meta1', 0)", idPrefix + "_1");
    jdbcTemplate.update("INSERT INTO events VALUES (?, 'type2', 'data2', 'entityType2', 'entityId2', 'triggeringEvent2', NULL, 0)", idPrefix + "_2");
    jdbcTemplate.update("INSERT INTO events VALUES (?, 'type3', 'data3', 'entityType3', 'entityId3', 'triggeringEvent3', 'meta3', 1)", idPrefix + "_3");

    return idPrefix;
  }

}
