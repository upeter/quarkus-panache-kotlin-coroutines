package org.acme

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import io.quarkus.hibernate.reactive.panache.Panache
import io.quarkus.panache.common.Sort
import io.smallrye.mutiny.CompositeException
import io.smallrye.mutiny.Uni
import io.smallrye.mutiny.uni
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger

@Path("fruits")
@ApplicationScoped
@Produces("application/json")
@Consumes("application/json")
class FruitResource {
    @GET
    fun get(): Uni<List<Fruit>> {
        return Fruit.listAll(Sort.by("name"))
    }

    @GET
    @Path("{id}")
    fun getSingle(id: Long): Uni<Fruit?> {
        return Fruit.findById(id)
    }

    @POST
    fun create(fruit: Fruit?): Uni<Response> =
        if (fruit == null || fruit.id != null) {
            throw WebApplicationException("Id was invalidly set on request.", 422)
        } else Panache.withTransaction(fruit::persist).replaceWith {
            Response.ok(fruit).status(Response.Status.CREATED).build()
        }


    @PUT
    @Path("{id}")
    fun update(id: Long, fruit: Fruit): Uni<Response> = Panache.withTransaction {
        Fruit.findById(id).flatMap {
            it?.let { entity: Fruit ->
                entity.apply { name = fruit.name }
                    .persist<Fruit>()
                    .map { Response.ok(entity).build() }
            } ?: uni { Response.ok().status(Response.Status.NOT_FOUND).build() }
        }
    }

    @DELETE
    @Path("{id}")
    fun delete(id: Long): Uni<Response> = Panache.withTransaction {
        Fruit.deleteById(id)
            .map { deleted ->
                if (deleted) Response.ok().status(Response.Status.NO_CONTENT).build()
                else Response.ok().status(Response.Status.NOT_FOUND).build()
            }
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
            // This is a Mutiny exception and it happens, for example, when we try to insert a new
            // fruit but the name is already in the database
            val throwable: Throwable = (exception as? CompositeException)?.cause ?: exception
            val code =
                if (throwable is WebApplicationException) (exception as WebApplicationException).response.status else 500
            return with(objectMapper.createObjectNode()) {
                put("exceptionType", throwable.javaClass.name)
                put("code", code)
                put("error", throwable.message)
            }.let { Response.status(code).entity(it).build() }
        }
    }

    companion object {
        private val LOGGER = Logger.getLogger(FruitResource::class.java.name)
    }
}
