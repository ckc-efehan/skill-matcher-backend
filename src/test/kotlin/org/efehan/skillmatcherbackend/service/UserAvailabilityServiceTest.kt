package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.availability.UserAvailabilityService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.fixtures.builder.UserAvailabilityBuilder
import org.efehan.skillmatcherbackend.fixtures.builder.UserBuilder
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityRepository
import org.efehan.skillmatcherbackend.shared.exceptions.AccessDeniedException
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.LocalDate
import java.util.Optional

@ExtendWith(MockKExtension::class)
@DisplayName("UserAvailabilityService Unit Tests")
class UserAvailabilityServiceTest {
    @MockK
    private lateinit var availabilityRepo: UserAvailabilityRepository

    @InjectMockKs
    private lateinit var service: UserAvailabilityService

    @Test
    fun `create saves and returns availability entry`() {
        // given
        val user = UserBuilder().build()
        every { availabilityRepo.findByUser(user) } returns emptyList()
        every { availabilityRepo.save(any()) } returnsArgument 0

        // when
        val result =
            service.create(
                user = user,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )

        // then
        assertThat(result.availableFrom).isEqualTo(LocalDate.of(2026, 3, 1))
        assertThat(result.availableTo).isEqualTo(LocalDate.of(2026, 6, 1))
        assertThat(result.user).isEqualTo(user)
        verify(exactly = 1) { availabilityRepo.save(any()) }
    }

    @Test
    fun `create throws when availableTo is before availableFrom`() {
        // given
        val user = UserBuilder().build()

        // then
        assertThatThrownBy {
            service.create(
                user = user,
                availableFrom = LocalDate.of(2026, 6, 1),
                availableTo = LocalDate.of(2026, 3, 1),
            )
        }.isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `create throws DuplicateEntryException when period overlaps`() {
        // given
        val user = UserBuilder().build()
        val existing =
            UserAvailabilityBuilder().build(
                user = user,
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 7, 1),
            )
        every { availabilityRepo.findByUser(user) } returns listOf(existing)

        // then
        assertThatThrownBy {
            service.create(
                user = user,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 5, 1),
            )
        }.isInstanceOf(DuplicateEntryException::class.java)
            .satisfies({ ex ->
                val e = ex as DuplicateEntryException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_AVAILABILITY_OVERLAP)
            })
    }

    @Test
    fun `create succeeds when periods do not overlap`() {
        // given
        val user = UserBuilder().build()
        val existing =
            UserAvailabilityBuilder().build(
                user = user,
                availableFrom = LocalDate.of(2026, 1, 1),
                availableTo = LocalDate.of(2026, 3, 1),
            )
        every { availabilityRepo.findByUser(user) } returns listOf(existing)
        every { availabilityRepo.save(any()) } returnsArgument 0

        // when
        val result =
            service.create(
                user = user,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )

        // then
        assertThat(result.availableFrom).isEqualTo(LocalDate.of(2026, 3, 1))
    }

    @Test
    fun `getAll returns entries sorted by availableFrom`() {
        // given
        val user = UserBuilder().build()
        val entry1 =
            UserAvailabilityBuilder().build(
                user = user,
                availableFrom = LocalDate.of(2026, 6, 1),
                availableTo = LocalDate.of(2026, 9, 1),
            )
        val entry2 =
            UserAvailabilityBuilder().build(
                user = user,
                availableFrom = LocalDate.of(2026, 1, 1),
                availableTo = LocalDate.of(2026, 3, 1),
            )
        every { availabilityRepo.findByUser(user) } returns listOf(entry1, entry2)

        // when
        val result = service.getAll(user)

        // then
        assertThat(result).hasSize(2)
        assertThat(result[0].availableFrom).isEqualTo(LocalDate.of(2026, 1, 1))
        assertThat(result[1].availableFrom).isEqualTo(LocalDate.of(2026, 6, 1))
    }

    @Test
    fun `getAll returns empty list when no entries exist`() {
        // given
        val user = UserBuilder().build()
        every { availabilityRepo.findByUser(user) } returns emptyList()

        // when
        val result = service.getAll(user)

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `update changes dates and returns updated entry`() {
        // given
        val user = UserBuilder().build()
        val entry = UserAvailabilityBuilder().build(user = user)
        every { availabilityRepo.findById(entry.id) } returns Optional.of(entry)
        every { availabilityRepo.findByUser(user) } returns listOf(entry)
        every { availabilityRepo.save(any()) } returnsArgument 0

        // when
        val result =
            service.update(
                user = user,
                id = entry.id,
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 7, 1),
            )

        // then
        assertThat(result.availableFrom).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(result.availableTo).isEqualTo(LocalDate.of(2026, 7, 1))
        verify(exactly = 1) { availabilityRepo.save(any()) }
    }

    @Test
    fun `update throws EntryNotFoundException when entry not found`() {
        // given
        val user = UserBuilder().build()
        every { availabilityRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy {
            service.update(
                user = user,
                id = "nonexistent",
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 7, 1),
            )
        }.isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_AVAILABILITY_NOT_FOUND)
            })
    }

    @Test
    fun `update throws AccessDeniedException when entry belongs to other user`() {
        // given
        val user = UserBuilder().build()
        val otherUser = UserBuilder().build(email = "other@firma.de", firstName = "Other", lastName = "User")
        val entry = UserAvailabilityBuilder().build(user = otherUser)
        every { availabilityRepo.findById(entry.id) } returns Optional.of(entry)

        // then
        assertThatThrownBy {
            service.update(
                user = user,
                id = entry.id,
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 7, 1),
            )
        }.isInstanceOf(AccessDeniedException::class.java)
            .satisfies({ ex ->
                val e = ex as AccessDeniedException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_AVAILABILITY_ACCESS_DENIED)
            })
    }

    @Test
    fun `update throws DuplicateEntryException when new dates overlap with other entry`() {
        // given
        val user = UserBuilder().build()
        val entry =
            UserAvailabilityBuilder().build(
                user = user,
                availableFrom = LocalDate.of(2026, 1, 1),
                availableTo = LocalDate.of(2026, 3, 1),
            )
        val otherEntry =
            UserAvailabilityBuilder().build(
                user = user,
                availableFrom = LocalDate.of(2026, 5, 1),
                availableTo = LocalDate.of(2026, 8, 1),
            )
        every { availabilityRepo.findById(entry.id) } returns Optional.of(entry)
        every { availabilityRepo.findByUser(user) } returns listOf(entry, otherEntry)

        // then
        assertThatThrownBy {
            service.update(
                user = user,
                id = entry.id,
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )
        }.isInstanceOf(DuplicateEntryException::class.java)
            .satisfies({ ex ->
                val e = ex as DuplicateEntryException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_AVAILABILITY_OVERLAP)
            })
    }

    @Test
    fun `delete removes entry`() {
        // given
        val user = UserBuilder().build()
        val entry = UserAvailabilityBuilder().build(user = user)
        every { availabilityRepo.findById(entry.id) } returns Optional.of(entry)
        every { availabilityRepo.delete(entry) } returns Unit

        // when
        service.delete(user, entry.id)

        // then
        verify(exactly = 1) { availabilityRepo.delete(entry) }
    }

    @Test
    fun `delete throws EntryNotFoundException when entry not found`() {
        // given
        val user = UserBuilder().build()
        every { availabilityRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy { service.delete(user, "nonexistent") }
            .isInstanceOf(EntryNotFoundException::class.java)
    }

    @Test
    fun `delete throws AccessDeniedException when entry belongs to other user`() {
        // given
        val user = UserBuilder().build()
        val otherUser = UserBuilder().build(email = "other@firma.de", firstName = "Other", lastName = "User")
        val entry = UserAvailabilityBuilder().build(user = otherUser)
        every { availabilityRepo.findById(entry.id) } returns Optional.of(entry)

        // then
        assertThatThrownBy { service.delete(user, entry.id) }
            .isInstanceOf(AccessDeniedException::class.java)
    }
}
