package io.github.rohits1402.gimmecomments.service;

import io.github.rohits1402.gimmecomments.TestDatabase;
import io.github.rohits1402.gimmecomments.model.OtpPurpose;
import io.github.rohits1402.gimmecomments.model.User;
import io.github.rohits1402.gimmecomments.repository.OtpTokenRepository;
import io.github.rohits1402.gimmecomments.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * A transaction can undo database work and nothing else. Emails and deleted files do
 * not come back, so both now happen after the commit rather than before it.
 * <p>
 * Each test runs the same call twice over: once in a transaction that is rolled back,
 * and once normally. The rollback case is the one that used to be wrong.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestDatabase.class)
class SideEffectsWaitForCommitTest {

    private static final String OLD_IMAGE = "https://files.example.test/old.png";
    private static final String NEW_IMAGE = "https://files.example.test/new.png";

    @Autowired
    private OtpService otpService;
    @Autowired
    private UserService userService;
    @Autowired
    private OtpTokenRepository otpTokens;
    @Autowired
    private UserRepository users;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private EmailService emailService;
    @MockitoBean
    private FileStorageService fileStorageService;

    private String email;
    private User user;

    @BeforeEach
    void setUp() {
        email = "commit-" + UUID.randomUUID() + "@example.test";

        User u = new User();
        u.setName("Committer");
        u.setEmail(email);
        u.setPassword("irrelevant");
        u.setProfileImage(OLD_IMAGE);
        user = users.save(u);
    }

    @AfterEach
    void tearDown() {
        otpTokens.findByEmailAndPurpose(email, OtpPurpose.ACCOUNT_VERIFICATION)
                .ifPresent(otpTokens::delete);
        users.deleteById(user.getId());
    }

    @Test
    void noCodeIsEmailedIfTheCodeIsNotKept() {
        new TransactionTemplate(transactionManager).execute(status -> {
            // generate joins this transaction instead of starting its own, so rolling
            // back here throws the code away exactly as a lost race would.
            otpService.generate(email, OtpPurpose.ACCOUNT_VERIFICATION);
            status.setRollbackOnly();
            return null;
        });

        verifyNoInteractions(emailService);
        assertThat(otpTokens.findByEmailAndPurpose(email, OtpPurpose.ACCOUNT_VERIFICATION))
                .as("nothing was kept, so nothing should have been sent")
                .isEmpty();
    }

    @Test
    void theCodeIsEmailedOnceItIsKept() {
        otpService.generate(email, OtpPurpose.ACCOUNT_VERIFICATION);

        verify(emailService).sendOtp(eq(email), anyString(), eq(OtpPurpose.ACCOUNT_VERIFICATION));
        assertThat(otpTokens.findByEmailAndPurpose(email, OtpPurpose.ACCOUNT_VERIFICATION))
                .isPresent();
    }

    @Test
    void theOldImageSurvivesIfTheNewOneIsNotKept() {
        when(fileStorageService.store(any())).thenReturn(NEW_IMAGE);

        new TransactionTemplate(transactionManager).execute(status -> {
            userService.updateProfileImage(user.getId().toString(), anImage());
            status.setRollbackOnly();
            return null;
        });

        verify(fileStorageService, never()).delete(anyString());
        assertThat(users.findById(user.getId()).orElseThrow().getProfileImage())
                .as("the row still points at the old file, so the old file must still exist")
                .isEqualTo(OLD_IMAGE);
    }

    @Test
    void theOldImageIsRemovedOnceTheNewOneIsKept() {
        when(fileStorageService.store(any())).thenReturn(NEW_IMAGE);

        userService.updateProfileImage(user.getId().toString(), anImage());

        verify(fileStorageService).delete(OLD_IMAGE);
        assertThat(users.findById(user.getId()).orElseThrow().getProfileImage())
                .isEqualTo(NEW_IMAGE);
    }

    private static MockMultipartFile anImage() {
        return new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1, 2, 3});
    }
}