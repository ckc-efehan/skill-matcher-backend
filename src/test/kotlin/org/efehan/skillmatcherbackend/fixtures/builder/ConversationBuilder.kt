package org.efehan.skillmatcherbackend.fixtures.builder

import org.efehan.skillmatcherbackend.persistence.ConversationModel
import org.efehan.skillmatcherbackend.persistence.UserModel

class ConversationBuilder {
    fun build(
        userOne: UserModel = UserBuilder().build(),
        userTwo: UserModel = UserBuilder().build(email = "bob@firma.de", firstName = "Bob", lastName = "Mueller"),
    ): ConversationModel =
        ConversationModel(
            userOne = userOne,
            userTwo = userTwo,
        )
}
