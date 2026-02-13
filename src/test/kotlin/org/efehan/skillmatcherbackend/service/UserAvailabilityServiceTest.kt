package org.efehan.skillmatcherbackend.service

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.efehan.skillmatcherbackend.core.availability.CreateAvailabilityRequest
import org.efehan.skillmatcherbackend.core.availability.UpdateAvailabilityRequest
import org.efehan.skillmatcherbackend.core.availability.UserAvailabilityService
import org.efehan.skillmatcherbackend.exception.GlobalErrorCode
import org.efehan.skillmatcherbackend.persistence.RoleModel
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityModel
import org.efehan.skillmatcherbackend.persistence.UserAvailabilityRepository
import org.efehan.skillmatcherbackend.persistence.UserModel
import org.efehan.skillmatcherbackend.shared.exceptions.AccessDeniedException
import org.efehan.skillmatcherbackend.shared.exceptions.DuplicateEntryException
import org.efehan.skillmatcherbackend.shared.exceptions.EntryNotFoundException
import org.junit.jupiter.api.BeforeEach
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

    private lateinit var service: UserAvailabilityService

    private val role = RoleModel("EMPLOYER", null)

    private val user =
        UserModel(
            email = "user@firma.de",
            passwordHash = "hashed",
            firstName = "User",
            lastName = "One",
            role = role,
        ).apply { isEnabled = true }

    private val otherUser =
        UserModel(
            email = "other@firma.de",
            passwordHash = "hashed",
            firstName = "Other",
            lastName = "User",
            role = role,
        ).apply { isEnabled = true }

    @BeforeEach
    fun setUp() {
        service = UserAvailabilityService(availabilityRepo)
    }

    // ── create ───────────────────────────────────────────────────────────

    @Test
    fun `create saves and returns availability entry`() {
        // given
        val request =
            CreateAvailabilityRequest(
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )
        every { availabilityRepo.findByUser(user) } returns emptyList()
        every { availabilityRepo.save(any()) } returnsArgument 0

        // when
        val result = service.create(user, request)

        // then
        assertThat(result.availableFrom).isEqualTo(LocalDate.of(2026, 3, 1))
        assertThat(result.availableTo).isEqualTo(LocalDate.of(2026, 6, 1))
        verify(exactly = 1) { availabilityRepo.save(any()) }
    }

    @Test
    fun `create throws when availableTo is before availableFrom`() {
        // given
        val request =
            CreateAvailabilityRequest(
                availableFrom = LocalDate.of(2026, 6, 1),
                availableTo = LocalDate.of(2026, 3, 1),
            )

        // then
        assertThatThrownBy { service.create(user, request) }
            .isInstanceOf(IllegalArgumentException::class.java)
    }

    @Test
    fun `create throws DuplicateEntryException when period overlaps`() {
        // given
        val existing =
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 7, 1),
            )
        val request =
            CreateAvailabilityRequest(
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 5, 1),
            )
        every { availabilityRepo.findByUser(user) } returns listOf(existing)

        // then
        assertThatThrownBy { service.create(user, request) }
            .isInstanceOf(DuplicateEntryException::class.java)
            .satisfies({ ex ->
                val e = ex as DuplicateEntryException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_AVAILABILITY_OVERLAP)
            })
    }

    @Test
    fun `create succeeds when periods do not overlap`() {
        // given
        val existing =
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 1, 1),
                availableTo = LocalDate.of(2026, 3, 1),
            )
        val request =
            CreateAvailabilityRequest(
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )
        every { availabilityRepo.findByUser(user) } returns listOf(existing)
        every { availabilityRepo.save(any()) } returnsArgument 0

        // when
        val result = service.create(user, request)

        // then
        assertThat(result.availableFrom).isEqualTo(LocalDate.of(2026, 3, 1))
    }

    // ── getAll ───────────────────────────────────────────────────────────

    @Test
    fun `getAll returns entries sorted by availableFrom`() {
        // given
        val entry1 =
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 6, 1),
                availableTo = LocalDate.of(2026, 9, 1),
            )
        val entry2 =
            UserAvailabilityModel(
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
        every { availabilityRepo.findByUser(user) } returns emptyList()

        // when
        val result = service.getAll(user)

        // then
        assertThat(result).isEmpty()
    }

    // ── update ───────────────────────────────────────────────────────────

    @Test
    fun `update changes dates and returns updated entry`() {
        // given
        val entry =
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )
        val request =
            UpdateAvailabilityRequest(
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 7, 1),
            )
        every { availabilityRepo.findById(entry.id) } returns Optional.of(entry)
        every { availabilityRepo.findByUser(user) } returns listOf(entry)
        every { availabilityRepo.save(any()) } returnsArgument 0

        // when
        val result = service.update(user, entry.id, request)

        // then
        assertThat(result.availableFrom).isEqualTo(LocalDate.of(2026, 4, 1))
        assertThat(result.availableTo).isEqualTo(LocalDate.of(2026, 7, 1))
        verify(exactly = 1) { availabilityRepo.save(any()) }
    }

    @Test
    fun `update throws EntryNotFoundException when entry not found`() {
        // given
        val request =
            UpdateAvailabilityRequest(
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 7, 1),
            )
        every { availabilityRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy { service.update(user, "nonexistent", request) }
            .isInstanceOf(EntryNotFoundException::class.java)
            .satisfies({ ex ->
                val e = ex as EntryNotFoundException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_AVAILABILITY_NOT_FOUND)
            })
    }

    @Test
    fun `update throws AccessDeniedException when entry belongs to other user`() {
        // given
        val entry =
            UserAvailabilityModel(
                user = otherUser,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )
        val request =
            UpdateAvailabilityRequest(
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 7, 1),
            )
        every { availabilityRepo.findById(entry.id) } returns Optional.of(entry)

        // then
        assertThatThrownBy { service.update(user, entry.id, request) }
            .isInstanceOf(AccessDeniedException::class.java)
            .satisfies({ ex ->
                val e = ex as AccessDeniedException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_AVAILABILITY_ACCESS_DENIED)
            })
    }

    @Test
    fun `update throws DuplicateEntryException when new dates overlap with other entry`() {
        // given
        val entry =
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 1, 1),
                availableTo = LocalDate.of(2026, 3, 1),
            )
        val otherEntry =
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 5, 1),
                availableTo = LocalDate.of(2026, 8, 1),
            )
        val request =
            UpdateAvailabilityRequest(
                availableFrom = LocalDate.of(2026, 4, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )
        every { availabilityRepo.findById(entry.id) } returns Optional.of(entry)
        every { availabilityRepo.findByUser(user) } returns listOf(entry, otherEntry)

        // then
        assertThatThrownBy { service.update(user, entry.id, request) }
            .isInstanceOf(DuplicateEntryException::class.java)
            .satisfies({ ex ->
                val e = ex as DuplicateEntryException
                assertThat(e.errorCode).isEqualTo(GlobalErrorCode.USER_AVAILABILITY_OVERLAP)
            })
    }

    // ── delete ───────────────────────────────────────────────────────────

    @Test
    fun `delete removes entry`() {
        // given
        val entry =
            UserAvailabilityModel(
                user = user,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )
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
        every { availabilityRepo.findById("nonexistent") } returns Optional.empty()

        // then
        assertThatThrownBy { service.delete(user, "nonexistent") }
            .isInstanceOf(EntryNotFoundException::class.java)
    }

    @Test
    fun `delete throws AccessDeniedException when entry belongs to other user`() {
        // given
        val entry =
            UserAvailabilityModel(
                user = otherUser,
                availableFrom = LocalDate.of(2026, 3, 1),
                availableTo = LocalDate.of(2026, 6, 1),
            )
        every { availabilityRepo.findById(entry.id) } returns Optional.of(entry)

        // then
        assertThatThrownBy { service.delete(user, entry.id) }
            .isInstanceOf(AccessDeniedException::class.java)
    }
}
