package io.github.rohits1402.gimmecomments.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Removes a replaced profile image, but only once the row that replaced it is
 * committed.
 * <p>
 * Deleting inside the transaction meant a rollback left the user's row pointing at a
 * file that had already been destroyed - a broken avatar with nothing to restore it
 * from. After the commit, the worst case is an orphaned file nobody refers to.
 */
@Component
class ObsoleteImageRemover {

    private final FileStorageService fileStorageService;

    ObsoleteImageRemover(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void remove(ObsoleteImage event) {
        fileStorageService.delete(event.url());
    }
}