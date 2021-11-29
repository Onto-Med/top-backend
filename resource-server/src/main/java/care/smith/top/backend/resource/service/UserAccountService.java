package care.smith.top.backend.resource.service;

import care.smith.top.data.tables.pojos.UserAccount;
import care.smith.top.data.tables.records.UserAccountRecord;
import org.jooq.DSLContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import static care.smith.top.data.Tables.USER_ACCOUNT;

@Service
public class UserAccountService {
  @Autowired DSLContext context;

  public UserAccount loadUserAccountByUsername(String username) throws UsernameNotFoundException {
    UserAccountRecord record = context.fetchOne(USER_ACCOUNT, USER_ACCOUNT.USERNAME.eq(username));
    if (record != null) return record.into(UserAccount.class);
    else throw new UsernameNotFoundException(String.format("User '%s' not found", username));
  }
}
