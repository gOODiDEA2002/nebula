package io.nebula.example.modules.web.entity.dos;

import io.nebula.web.mask.MaskType;
import io.nebula.web.mask.SensitiveData;
import lombok.Data;
import java.time.LocalDateTime;
@Data
public class UserProfile {
  private String userId;
  private String username;
  
  @SensitiveData(type = MaskType.EMAIL)
  private String email;
  
  @SensitiveData(type = MaskType.PHONE)
  private String mobile;
  
  @SensitiveData(type = MaskType.ID_CARD)
  private String idCard;
  
  private LocalDateTime lastLoginTime;
}
