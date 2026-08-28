package me.spoo.android.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// The mock is the offline stand-in for the server list endpoint, so its
// query engine has to honor the same contract: filter, search, sort,
// then page.
class MockLinksRepositoryTest {
    @Test
    fun `refresh resets to the first page and more exists`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery())
            assertEquals(25, repo.links.value.size)
            assertTrue(repo.hasMore.value)
        }

    @Test
    fun `loadMore appends a page without duplicates`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery())
            repo.loadMore()
            val links = repo.links.value
            assertEquals(50, links.size)
            assertEquals(links.size, links.map { it.id }.distinct().size)
        }

    @Test
    fun `click sort is descending by total clicks`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery(sort = LinkSort.Clicks))
            val clicks = repo.links.value.map { it.totalClicks }
            assertEquals(clicks.sortedDescending(), clicks)
        }

    @Test
    fun `search narrows across the whole set, not the visible page`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery(search = "spotify"))
            assertTrue(repo.links.value.isNotEmpty())
            assertTrue(
                repo.links.value.all {
                    it.shortCode.contains("spotify", ignoreCase = true) ||
                        it.originalUrl.contains("spotify", ignoreCase = true)
                },
            )
        }

    @Test
    fun `password filter keeps only protected links`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery(filter = LinksFilter(passwordProtected = true)))
            assertTrue(repo.links.value.isNotEmpty())
            assertTrue(repo.links.value.all { it.hasPassword })
        }

    @Test
    fun `last-click sort is descending, never-clicked links last`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery(sort = LinkSort.LastClick))
            val stamps = repo.links.value.map { it.lastClickMillis ?: Long.MIN_VALUE }
            assertEquals(stamps.sortedDescending(), stamps)
        }

    @Test
    fun `alias availability knows the seeded aliases`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery())
            val taken =
                repo.links.value
                    .first()
                    .shortCode
            assertTrue(!repo.aliasAvailable(taken))
            assertTrue(repo.aliasAvailable("definitely-free-alias"))
        }

    @Test
    fun `refresh after a filtered query with null reruns the same query`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery(filter = LinksFilter(passwordProtected = true)))
            val filtered = repo.links.value.size
            repo.refresh(null)
            assertEquals(filtered, repo.links.value.size)
            assertTrue(repo.links.value.all { it.hasPassword })
        }
}
