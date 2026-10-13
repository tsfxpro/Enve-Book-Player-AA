package com.enve.audiobookshelf

import com.enve.audiobookshelf.dto.AbsMeResponse
import com.enve.core.data.model.Book
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AbsSharedRequests @Inject constructor() {
    private val me = InFlight<Pair<String, String?>, Response<AbsMeResponse>>()
    private val booksInProgress = InFlight<Pair<String, String?>, Result<List<Book>>>()

    suspend fun me(connection: Pair<String, String?>, fetch: suspend () -> Response<AbsMeResponse>) =
        me.run(connection, fetch)

    suspend fun booksInProgress(connection: Pair<String, String?>, fetch: suspend () -> Result<List<Book>>) =
        booksInProgress.run(connection, fetch)

    private class InFlight<K, V> {
        private val requests = HashMap<K, CompletableDeferred<V>>()

        suspend fun run(key: K, fetch: suspend () -> V): V {
            val shared = synchronized(requests) { requests[key] }
            if (shared != null) {
                return try {
                    shared.await()
                } catch (e: CancellationException) {
                    currentCoroutineContext().ensureActive()
                    run(key, fetch)
                }
            }
            val owned = CompletableDeferred<V>()
            val claimed = synchronized(requests) {
                requests.putIfAbsent(key, owned) == null
            }
            if (!claimed) return run(key, fetch)
            try {
                return fetch().also { owned.complete(it) }
            } catch (t: Throwable) {
                owned.completeExceptionally(t)
                throw t
            } finally {
                synchronized(requests) { requests.remove(key, owned) }
            }
        }
    }
}
