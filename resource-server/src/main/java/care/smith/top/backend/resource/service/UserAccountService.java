package care.smith.top.backend.resource.service;

import care.smith.top.backend.model.UserAccount;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserAccountService {
//  @Autowired DSLContext context;

  public UserAccount loadUserAccountByUsername(String username) throws UsernameNotFoundException {
//    UserAccountRecord record = context.fetchOne(USER_ACCOUNT, USER_ACCOUNT.USERNAME.eq(username));
//    if (record != null) return record.into(UserAccount.class);
//    else throw new UsernameNotFoundException(String.format("User '%s' not found", username));
    throw new NotImplementedException("");
  }
}
