package com.example.expense_tracker.service;

import com.example.expense_tracker.dto.ChangeExpenseStatusRequest;
import com.example.expense_tracker.dto.ExpenseCreateDto;
import com.example.expense_tracker.dto.ExpenseCreationResult;
import com.example.expense_tracker.dto.ExpenseDto;
import com.example.expense_tracker.entity.Expense;
import com.example.expense_tracker.entity.IdempotencyKey;
import com.example.expense_tracker.entity.Project;
import com.example.expense_tracker.entity.User;
import com.example.expense_tracker.entity.enums.ExpenseStatus;
import com.example.expense_tracker.exception.ConflictException;
import com.example.expense_tracker.exception.IdempotencyKeyReuseException;
import com.example.expense_tracker.exception.ResourceNotFoundException;
import com.example.expense_tracker.mapper.ExpenseMapper;
import com.example.expense_tracker.repository.ExpenseRepository;
import com.example.expense_tracker.repository.ExpenseStatusChangeRepository;
import com.example.expense_tracker.repository.IdempotencyKeyRepository;
import com.example.expense_tracker.repository.ProjectRepository;
import com.example.expense_tracker.repository.UserRepository;
import com.example.expense_tracker.security.ProjectAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    private static final Long PROJECT_ID = 10L;
    private static final Long USER_ID = 1L;
    private static final String USERNAME = "alice";
    private static final String KEY = "key-123";

    @Mock
    private ExpenseRepository expenseRepository;
    @Mock
    private ExpenseStatusChangeRepository historyRepository;
    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;
    @Mock
    private ProjectRepository projectRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ExpenseMapper expenseMapper;
    @Mock
    private ProjectAccessService accessService;

    @InjectMocks
    private ExpenseService expenseService;

    private User alice;
    private final ExpenseCreateDto dto = new ExpenseCreateDto(new BigDecimal("10.50"), "RUB", "taxi");

    @BeforeEach
    void setUp() {
        alice = new User();
        alice.setId(USER_ID);
        alice.setUsername(USERNAME);
        lenient().when(userRepository.findByUsername(USERNAME)).thenReturn(Optional.of(alice));
    }

    private ExpenseDto dtoOf(long id) {
        return new ExpenseDto(id, new BigDecimal("10.50"), "RUB", "taxi", ExpenseStatus.PENDING, PROJECT_ID, USER_ID, Instant.now());
    }

    private void stubNewExpensePipeline(long newId) {
        Project project = new Project();
        Expense entity = new Expense();
        lenient().when(projectRepository.findById(PROJECT_ID)).thenReturn(Optional.of(project));
        lenient().when(expenseMapper.toEntity(dto)).thenReturn(entity);
        lenient().when(expenseRepository.save(entity)).thenAnswer(inv -> {
            entity.setId(newId);
            return entity;
        });
        lenient().when(expenseMapper.toDto(entity)).thenReturn(dtoOf(newId));
    }

    // ---------- создание и идемпотентность ----------

    @Test
    void create_WithoutKey_DoesNotTouchIdempotencyTable() {
        stubNewExpensePipeline(100L);

        ExpenseCreationResult result = expenseService.createExpense(PROJECT_ID, dto, USERNAME, null);

        assertFalse(result.replayed());
        assertEquals(100L, result.expense().id());
        verify(idempotencyKeyRepository, never()).insertIfAbsent(any(), anyString(), anyString());
    }

    @Test
    void create_WithNewKey_ClaimsKeyAndLinksItToTheExpense() {
        stubNewExpensePipeline(100L);
        IdempotencyKey claimed = new IdempotencyKey();
        claimed.setRequestHash("irrelevant");
        when(idempotencyKeyRepository.insertIfAbsent(eqId(), anyString(), anyString())).thenReturn(1);
        when(idempotencyKeyRepository.findByUserIdAndKey(USER_ID, KEY)).thenReturn(Optional.of(claimed));

        ExpenseCreationResult result = expenseService.createExpense(PROJECT_ID, dto, USERNAME, KEY);

        assertFalse(result.replayed());
        // Ключ привязан к созданному расходу: именно по этой связи работает повтор
        assertEquals(100L, claimed.getExpenseId());
    }

    @Test
    void create_WithKnownKeyAndSameBody_ReturnsOriginalWithoutCreatingNew() {
        // Первый вызов запоминает отпечаток, который сервис посчитал для этого тела
        stubNewExpensePipeline(100L);
        IdempotencyKey claimed = new IdempotencyKey();
        when(idempotencyKeyRepository.insertIfAbsent(eqId(), anyString(), anyString())).thenReturn(1);
        when(idempotencyKeyRepository.findByUserIdAndKey(USER_ID, KEY)).thenReturn(Optional.of(claimed));
        expenseService.createExpense(PROJECT_ID, dto, USERNAME, KEY);

        org.mockito.ArgumentCaptor<String> hash = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(idempotencyKeyRepository).insertIfAbsent(eqId(), org.mockito.ArgumentMatchers.eq(KEY), hash.capture());

        // Повтор: ключ уже есть, отпечаток совпадает с тем, что сервис посчитал в первый раз
        when(idempotencyKeyRepository.insertIfAbsent(eqId(), anyString(), anyString())).thenReturn(0);
        claimed.setRequestHash(hash.getValue());
        Expense original = new Expense();
        when(expenseRepository.findById(100L)).thenReturn(Optional.of(original));
        when(expenseMapper.toDto(original)).thenReturn(dtoOf(100L));

        ExpenseCreationResult replay = expenseService.createExpense(PROJECT_ID, dto, USERNAME, KEY);

        assertTrue(replay.replayed());
        assertEquals(100L, replay.expense().id());
        verify(expenseRepository, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void create_WithKnownKeyAndDifferentBody_Throws() {
        IdempotencyKey existing = new IdempotencyKey();
        existing.setRequestHash("hash-of-another-request");
        existing.setExpenseId(100L);
        when(idempotencyKeyRepository.insertIfAbsent(eqId(), anyString(), anyString())).thenReturn(0);
        when(idempotencyKeyRepository.findByUserIdAndKey(USER_ID, KEY)).thenReturn(Optional.of(existing));

        assertThrows(IdempotencyKeyReuseException.class,
                () -> expenseService.createExpense(PROJECT_ID, dto, USERNAME, KEY));

        verify(expenseRepository, never()).save(any());
    }

    @Test
    void create_ChecksAccessBeforeTouchingTheKey() {
        doThrow(new AccessDeniedException("No access")).when(accessService).checkAccess(PROJECT_ID, USERNAME);

        assertThrows(AccessDeniedException.class,
                () -> expenseService.createExpense(PROJECT_ID, dto, USERNAME, KEY));

        verify(idempotencyKeyRepository, never()).insertIfAbsent(any(), anyString(), anyString());
    }

    // ---------- смена статуса ----------

    private Expense expenseOf(User author, ExpenseStatus status) {
        Expense expense = new Expense();
        expense.setId(5L);
        expense.setEmployee(author);
        expense.setStatus(status);
        lenient().when(expenseRepository.findByIdAndProjectId(5L, PROJECT_ID)).thenReturn(Optional.of(expense));
        lenient().when(expenseMapper.toDto(expense)).thenReturn(dtoOf(5L));
        return expense;
    }

    private User otherUser(long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        return user;
    }

    @Test
    void changeStatus_ByProjectAdmin_UpdatesStatusAndWritesHistory() {
        Expense expense = expenseOf(otherUser(2L), ExpenseStatus.PENDING);

        expenseService.changeStatus(PROJECT_ID, 5L, new ChangeExpenseStatusRequest(ExpenseStatus.APPROVED, "ok"), USERNAME);

        assertEquals(ExpenseStatus.APPROVED, expense.getStatus());
        verify(accessService).checkProjectAdmin(PROJECT_ID, USERNAME);
        verify(historyRepository).save(any());
    }

    @Test
    void changeStatus_ByNonAdmin_IsForbidden_AndStatusStaysTheSame() {
        Expense expense = expenseOf(alice, ExpenseStatus.PENDING);
        doThrow(new AccessDeniedException("Only the project admin can do this"))
                .when(accessService).checkProjectAdmin(PROJECT_ID, USERNAME);

        assertThrows(AccessDeniedException.class, () -> expenseService.changeStatus(
                PROJECT_ID, 5L, new ChangeExpenseStatusRequest(ExpenseStatus.APPROVED, null), USERNAME));

        assertEquals(ExpenseStatus.PENDING, expense.getStatus());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void changeStatus_Cancel_ByAuthor_IsAllowedWithoutAdminRights() {
        Expense expense = expenseOf(alice, ExpenseStatus.PENDING);

        expenseService.changeStatus(PROJECT_ID, 5L, new ChangeExpenseStatusRequest(ExpenseStatus.CANCELLED, null), USERNAME);

        assertEquals(ExpenseStatus.CANCELLED, expense.getStatus());
        verify(accessService, never()).checkProjectAdmin(any(), anyString());
    }

    @Test
    void changeStatus_Cancel_ByNotAuthor_IsForbidden() {
        expenseOf(otherUser(2L), ExpenseStatus.PENDING);

        assertThrows(AccessDeniedException.class, () -> expenseService.changeStatus(
                PROJECT_ID, 5L, new ChangeExpenseStatusRequest(ExpenseStatus.CANCELLED, null), USERNAME));
    }

    @Test
    void changeStatus_InvalidTransition_ThrowsConflict() {
        Expense expense = expenseOf(otherUser(2L), ExpenseStatus.PAID);

        assertThrows(ConflictException.class, () -> expenseService.changeStatus(
                PROJECT_ID, 5L, new ChangeExpenseStatusRequest(ExpenseStatus.APPROVED, null), USERNAME));

        assertEquals(ExpenseStatus.PAID, expense.getStatus());
        verify(historyRepository, never()).save(any());
    }

    @Test
    void changeStatus_ExpenseFromAnotherProject_IsNotFound() {
        when(expenseRepository.findByIdAndProjectId(5L, PROJECT_ID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> expenseService.changeStatus(
                PROJECT_ID, 5L, new ChangeExpenseStatusRequest(ExpenseStatus.APPROVED, null), USERNAME));
    }

    private static Long eqId() {
        return org.mockito.ArgumentMatchers.eq(USER_ID);
    }
}
