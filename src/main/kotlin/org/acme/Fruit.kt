package org.acme

import io.quarkus.hibernate.reactive.panache.kotlin.PanacheEntity
import io.quarkus.hibernate.reactive.panache.kotlin.PanacheCompanion
import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity

//@Entity
//@Cacheable
//class Fruit : PanacheEntity {
//    @Column(length = 40, unique = true)
//    lateinit var name: String
//
//    constructor()
//    constructor(name: String) {
//        this.name = name
//    }
//
//    companion object: PanacheCompanion<Fruit>
//}


@Entity
@Cacheable
class Fruit(override var id: Long? = null,
            @field:Column(length = 40, unique = true)
            var name: String,
//            @field:Column(length = 40)
//            @field:NotBlank(message = "Color cannot be blank")
//            var color: String
): PanacheEntity() {
    companion object : PanacheCompanion<Fruit>
}
