package io.nebula.example.modules.web.entity.dos;

import io.nebula.web.mask.MaskType;
import io.nebula.web.mask.SensitiveData;
import lombok.Data;
import lombok.AllArgsConstructor;
@Data
@AllArgsConstructor
public class UserInfo {
  private String userId;
  private String username;
  
  @SensitiveData(type = MaskType.EMAIL)
  private String email;
  
  @SensitiveData(type = MaskType.PHONE)
  private String mobile;
}