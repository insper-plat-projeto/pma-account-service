package store.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

public interface AccountRepository extends CrudRepository<AccountModel, String> {

    List<AccountModel> findAll();

    // Spring Data derives the JPQL from the method name
    // https://docs.spring.io/spring-data/jpa/reference/jpa/query-methods.html
    Optional<AccountModel> findByEmailAndHashPassword(
        String email,
        String hashPassword
    );

}