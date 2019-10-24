package io.fotoapparat.coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.ConflatedBroadcastChannel

/**
 * A [ConflatedBroadcastChannel] which exposes a [getValue] which will [await] for at least one value.
 */
@ExperimentalCoroutinesApi
internal class AwaitBroadcastChannel<T>(
        private val channel: ConflatedBroadcastChannel<T> = ConflatedBroadcastChannel(),
        private val deferred: CompletableDeferred<Boolean> = CompletableDeferred()
) : BroadcastChannel<T> by channel, Deferred<Boolean> by deferred {

    /**
     * The most recently sent element to this channel.
     */
    suspend fun getValue(): T {
        deferred.await()
        return channel.value
    }

    override fun offer(element: T): Boolean {
        deferred.complete(true)
        return channel.offer(element)
    }

    override suspend fun send(element: T) {
        deferred.complete(true)
        channel.send(element)
    }

    override fun cancel(cause: CancellationException?) {
        channel.cancel(cause)
        deferred.cancel(cause)
    }

    override fun cancel(cause: Throwable?): Boolean {
        deferred.cancel(cause?.message ?:"", cause)
        return channel.close(cause)
    }
}
