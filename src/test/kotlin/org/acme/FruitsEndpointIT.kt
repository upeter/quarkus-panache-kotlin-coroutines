package org.acme

import io.quarkus.test.junit.QuarkusIntegrationTest

@QuarkusIntegrationTest
class FruitsReactiveEndpointIT : FruitsReactiveEndpointTest() { // Runs the same tests as the parent class
}

@QuarkusIntegrationTest
class FruitsCoroutinesEndointIT : FruitsCoroutinesEndpointTest() { // Runs the same tests as the parent class
}
