package store.account;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public Account create(Account account) {

        if (null == account.password()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Password is empty"
            );            
        }

        final String pass = account.password().trim();
        if (pass.length() < 6) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Password must have at least 6 characters"
            );
        }

        if (account.email() == null || account.email().trim().isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Email is empty"
            );
        }

        // agora calcula o hash
        account.hashPassword(calcHash(pass));

        return accountRepository.save(
            new AccountModel(account)
        ).to();
    }

    // read-through cache: Redis key accounts::<id>, TTL from spring.cache.redis.time-to-live
    // a missing id throws 404 and never returns null, so nothing is cached for it
    @Cacheable(value = "accounts", key = "#id")
    public Account findById(String id) {
        return accountRepository.findById(id)
            .map(AccountModel::to)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Account not found"
            ));
    }

    public Account findByEmailAndPassword(String email, String password) {
        if (password == null || password.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "Password is empty"
            );
        }
        // create() hashes the trimmed password, so login must do the same
        final String hash = calcHash(password.trim());
        return accountRepository.findByEmailAndHashPassword(email, hash)
            .map(AccountModel::to)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED, "Invalid credentials"
            ));
    }

    // a deleted account must not keep answering from the cache
    @CacheEvict(value = "accounts", key = "#id")
    public void delete(String id) {
        accountRepository.deleteById(id);
    }

    public List<Account> findAll() {
        return accountRepository.findAll().stream()
            .map(AccountModel::to)
            .toList();
    }

    private String calcHash(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(text.getBytes(StandardCharsets.UTF_8));
            byte[] digest = md.digest();
            return Base64.getEncoder().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

}