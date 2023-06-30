package org.acme.tx

import io.quarkus.hibernate.reactive.panache.Panache
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async

@OptIn(ExperimentalCoroutinesApi::class)
suspend fun <T> withTransaction(block: suspend () -> T): T = Panache.withTransaction {
    CoroutineScope(Dispatchers.Unconfined).async { block() }.asUni()
}.awaitSuspending()
