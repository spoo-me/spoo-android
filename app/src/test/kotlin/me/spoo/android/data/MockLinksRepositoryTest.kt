package me.spoo.android.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
            // Matches sit past the first page, so a search that filtered
            // only the loaded page would return fewer of them.
            repo.refresh(LinksQuery())
            val firstPage = repo.links.value
            val onPage = firstPage.count { it.originalUrl.contains("dev.to") }
            repo.refresh(LinksQuery(search = "dev.to"))
            val found = repo.links.value
            assertTrue(found.size > onPage, "search saw only the loaded page")
            assertTrue(found.all { it.originalUrl.contains("dev.to", ignoreCase = true) })
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
    fun `last-click sort is descending, and every stamp is plausible`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery(sort = LinkSort.LastClick))
            val links = repo.links.value
            val stamps = links.mapNotNull { it.lastClickMillis }
            assertEquals(stamps.sortedDescending(), stamps)
            // A last click before the link existed, or in the future, is
            // the kind of nonsense that only shows up in a demo.
            val now = System.currentTimeMillis()
            links.forEach { link ->
                val clicked = link.lastClickMillis ?: return@forEach
                assertTrue(clicked <= now, "${'$'}{link.shortCode} clicked in the future")
                link.createdAtMillis?.let {
                    assertTrue(clicked >= it, "${'$'}{link.shortCode} clicked before it existed")
                }
            }
        }

    @Test
    fun `never-clicked links sort after clicked ones`() =
        runTest {
            val repo = MockLinksRepository()
            val fresh = repo.create(CreateLinkRequest(url = "https://example.com", alias = "brand-new"))
            assertEquals(null, fresh.lastClickMillis)
            repo.refresh(LinksQuery(sort = LinkSort.LastClick))
            // On whatever page is visible, a clicked link never follows an
            // unclicked one.
            val seen = repo.links.value.map { it.lastClickMillis }
            val firstNull = seen.indexOfFirst { it == null }
            if (firstNull >= 0) assertTrue(seen.drop(firstNull).all { it == null })
        }

    @Test
    fun `alias status separates taken from reserved and invalid`() =
        runTest {
            val repo = MockLinksRepository()
            repo.refresh(LinksQuery())
            val taken =
                repo.links.value
                    .first()
                    .shortCode
            // A blocked alias is not automatically a taken one: the copy
            // under the field depends on telling these apart.
            assertEquals(AliasStatus.Taken, repo.aliasStatus(taken))
            assertEquals(AliasStatus.Reserved, repo.aliasStatus("api"))
            assertEquals(AliasStatus.Invalid, repo.aliasStatus("hi"))
            assertEquals(AliasStatus.Free, repo.aliasStatus("definitely-free-alias"))
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
