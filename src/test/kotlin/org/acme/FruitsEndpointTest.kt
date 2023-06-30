package org.acme

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.quarkus.test.junit.QuarkusTest
import io.restassured.http.ContentType
import io.restassured.module.kotlin.extensions.Extract
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import io.restassured.response.ResponseBodyExtractionOptions
import jakarta.inject.Inject
import org.jboss.resteasy.reactive.RestResponse.StatusCode
import org.junit.jupiter.api.Test


@QuarkusTest
class FruitsReactiveEndpointTest:AbstractFruitsEndpointTest(FRUITS_REACTIVE){}
@QuarkusTest
class FruitsCoroutinesEndpointTest:AbstractFruitsEndpointTest(FRUITS_COROUTINES){}

abstract class AbstractFruitsEndpointTest(val resourcePath:String) {
    @Inject
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `should list all fruits`() {
        //List all, should have all 3 fruits the database has initially:
        assertGetFruits {
            it.map { it.name }.shouldContainExactlyInAnyOrder("Cherry", "Apple", "Banana")
        }

        // Update Cherry to Pineapple
        assertPutFruit(Fruit(id = 1, name = "Pineapple")){
            it.id.shouldNotBeNull()
            it.name shouldBe "Pineapple"
        }

        //List all, Pineapple should've replaced Cherry:
        assertGetFruits {
            it.map { it.name }.shouldContainExactlyInAnyOrder("Pineapple", "Apple", "Banana")
        }

        //Delete Pineapple:
        assertDeleteFruit(1)
        assertGetFruits {
            it.map { it.name }.shouldContainExactlyInAnyOrder( "Apple", "Banana")
        }

        //Create the Pear:
        assertPostFruit(Fruit(name = "Pear")){
            it.id.shouldNotBeNull()
            it.name shouldBe "Pear"
        }

        //List all, Pineapple should be still missing now:
        assertGetFruits {
            it.map { it.name }.shouldContainExactlyInAnyOrder("Pear", "Apple", "Banana")
        }
    }

    @Test
    fun `should not find entity for delete`() {
        assertDeleteFruit(9345, expectedStatusCode = 404)
    }

    @Test
    fun `should not find entity for update`() {
        assertPutFruit(Fruit(343434, "Watermelon"), expectedStatusCode = 404)
    }


    private fun assertGetFruits(reply:(List<Fruit>) -> Unit) {
        val response = Given{
            contentType(ContentType.JSON)
        } When {
            this[resourcePath]
        } Then {
            statusCode(200)
        } Extract {
            body()
        }
        reply(response.toEntities<List<Fruit>>())
    }

    private fun assertPutFruit(fruit:Fruit, expectedStatusCode:Int = 200, verify:(Fruit) -> Unit = {}): Fruit? {
        val response = Given {
            contentType(ContentType.JSON)
            body(fruit.asJson())
        } When {
            put("$resourcePath/${fruit.id}")
        } Then {
            statusCode(expectedStatusCode)
        } Extract {
            body()
        }

        return if(StatusCode.OK == expectedStatusCode)
            response.toEntities<Fruit>().also(verify) else null
    }

    private fun assertPostFruit(fruit:Fruit, verify:(Fruit) -> Unit): Fruit {
        val response = Given {
            contentType(ContentType.JSON)
            body(fruit.asJson())
        } When {
            post(resourcePath)
        } Then {
            statusCode(201)
        } Extract {
            body()
        }

        return response.toEntities<Fruit>().also(verify)
    }

    private fun assertDeleteFruit(id:Long, expectedStatusCode:Int = 204) {
        val response = Given {
            contentType(ContentType.JSON)
        } When {
            delete("$resourcePath/$id")
        } Then {
            statusCode(expectedStatusCode)
        }
    }


    fun <T : Any> T.asJson() = objectMapper.writeValueAsString(this)

    inline fun <reified T : Any> ResponseBodyExtractionOptions.toEntities():T = this.`as`(jacksonTypeRef<T>().type)

    companion object {
        const val FRUITS_REACTIVE = "/fruits"
        const val FRUITS_COROUTINES = "/fruits-co"
    }

}
