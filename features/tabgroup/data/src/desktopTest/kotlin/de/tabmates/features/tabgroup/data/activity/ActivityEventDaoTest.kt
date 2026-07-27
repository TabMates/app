package de.tabmates.features.tabgroup.data.activity

import de.tabmates.features.tabgroup.data.sync.createInMemoryDatabase
import de.tabmates.features.tabgroup.data.sync.group
import de.tabmates.features.tabgroup.data.sync.insertGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ActivityEventDaoTest {
    @Test
    fun replayingAPageDoesNotDuplicateChildren() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            val events = listOf(activityEventEntity(id = "a1", seq = 1, groupId = "g1"))
            val changes = listOf(changeEntity(eventId = "a1"))

            database.activityEventDao.upsertPage(events, changes)
            database.activityEventDao.upsertPage(events, changes)

            val feed = database.activityEventDao.observeAccountFeed(limit = 10).first()
            assertEquals(1, feed.size)
            assertEquals(1, feed.single().changes.size)
        }

    @Test
    fun accountFeedIsOrderedBySeqDescendingAndRespectsLimit() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.activityEventDao.upsertPage(
                events =
                    listOf(
                        activityEventEntity(id = "a1", seq = 1, groupId = "g1"),
                        activityEventEntity(id = "a2", seq = 2, groupId = "g1"),
                        activityEventEntity(id = "a3", seq = 3, groupId = "g1"),
                    ),
                changes = emptyList(),
            )

            val feed = database.activityEventDao.observeAccountFeed(limit = 2).first()

            assertEquals(listOf("a3", "a2"), feed.map { it.event.id })
        }

    @Test
    fun groupFeedIsScopedToItsGroup() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.insertGroup(group(id = "g2"))
            database.activityEventDao.upsertPage(
                events =
                    listOf(
                        activityEventEntity(id = "a1", seq = 1, groupId = "g1"),
                        activityEventEntity(id = "a2", seq = 2, groupId = "g2"),
                    ),
                changes = emptyList(),
            )

            val feed = database.activityEventDao.observeGroupFeed("g1", limit = 10).first()

            assertEquals(listOf("a1"), feed.map { it.event.id })
        }

    /**
     * The group foreign key is what prunes a left group's history everywhere — no call site does it
     * explicitly — so the cascade reaching the change rows is load-bearing, not incidental.
     */
    @Test
    fun deletingTheGroupCascadesToEventsAndTheirChanges() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.activityEventDao.upsertPage(
                events = listOf(activityEventEntity(id = "a1", seq = 1, groupId = "g1")),
                changes = listOf(changeEntity(eventId = "a1")),
            )

            database.groupDao.deleteGroupById("g1")

            assertTrue(
                database.activityEventDao
                    .observeAccountFeed(limit = 10)
                    .first()
                    .isEmpty(),
            )
            assertEquals(null, database.activityEventDao.getMaxSeq())
            assertEquals(0, database.activityEventDao.countChanges())
        }

    @Test
    fun confirmedVersionsAggregatePerEntry() =
        runTest {
            val database = createInMemoryDatabase()
            database.insertGroup(group(id = "g1"))
            database.activityEventDao.upsertPage(
                events =
                    listOf(
                        activityEventEntity(id = "a1", seq = 1, groupId = "g1", entryVersion = 0),
                        activityEventEntity(id = "a2", seq = 2, groupId = "g1", entryVersion = 2),
                    ),
                changes = emptyList(),
            )

            val confirmed =
                database.activityEventDao
                    .observeConfirmedEntryVersions(listOf("e1"))
                    .first()
                    .single()

            assertEquals("e1", confirmed.tabEntryId)
            assertEquals(2, confirmed.maxVersion)
            assertEquals(false, confirmed.deleted)
        }
}
