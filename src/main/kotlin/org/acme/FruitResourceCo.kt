package org.acme

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.CompositeException
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.coroutines.asUni
import io.smallrye.mutiny.coroutines.awaitSuspending
import io.smallrye.mutiny.uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import kotlinx.coroutines.*
import org.acme.tx.withTransaction
import org.jboss.logging.Logger

@Path("fruits-co")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
class FruitResourceCo {
    @GET
    suspend fun get(): List<Fruit> = withTransaction {
        Fruit.listAll(Sort.by("name")).awaitSuspending()
    }

    @GET
    @Path("{id}")
    suspend fun getSingle(id: Long): Fruit? = withTransaction {
        Fruit.findById(id).awaitSuspending()
    }

    @POST
    suspend fun create(fruit: Fruit): Response {
        if (fruit.id != null) {
            throw WebApplicationException("Id was invalidly set on request.", 422)
        }
        return withTransaction {
            fruit.persist<Fruit>().awaitSuspending().let { Response.ok(it).status(Response.Status.CREATED).build() }
        }
    }

    @PUT
    @Path("{id}")
    suspend fun update(id: Long, fruit: Fruit): Response = withTransaction {
        Fruit.findById(id).awaitSuspending()?.let { entity: Fruit ->
            entity.run {
                name = fruit.name
                entity.persist<Fruit>().awaitSuspending()
            }.let { Response.ok(it).build() }
        } ?: Response.ok().status(Response.Status.NOT_FOUND).build()
    }


    @DELETE
    @Path("{id}")
    suspend fun delete(id: Long): Response = withTransaction {
        val deleted = Fruit.deleteById(id).awaitSuspending()
        if (deleted) Response.ok().status(Response.Status.NO_CONTENT).build()
        else Response.ok().status(Response.Status.NOT_FOUND).build()
    }

    /**
     * Create a HTTP response from an exception.
     *
     * Response Example:
     *
     * <pre>
     * HTTP/1.1 422 Unprocessable Entity
     * Content-Length: 111
     * Content-Type: application/json
     *
     * {
     * "code": 422,
     * "error": "Fruit name was not set on request.",
     * "exceptionType": "jakarta.ws.rs.WebApplicationException"
     * }
    </pre> *
     */
    @Provider
    class ErrorMapper : ExceptionMapper<Exception> {
        @Inject
        lateinit var objectMapper: ObjectMapper
        override fun toResponse(exception: Exception): Response {
            LOGGER.error("Failed to handle request", exception)
            var throwable: Throwable = exception
            var code = 500
            if (throwable is WebApplicationException) {
                code = (exception as WebApplicationException).response.status
            }

            // This is a Mutiny exception and it happens, for example, when we try to insert a new
            // fruit but the name is already in the database
            if (throwable is CompositeException) {
                throwable = throwable.cause!!
            }
            val exceptionJson: ObjectNode = objectMapper.createObjectNode()
            exceptionJson.put("exceptionType", throwable.javaClass.name)
            exceptionJson.put("code", code)
            if (exception.message != null) {
                exceptionJson.put("error", throwable.message)
            }
            return Response.status(code)
                .entity(exceptionJson)
                .build()
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(FruitResourceCo::class.java.name)
    }
}
