package projectj.sm.gameserver.dto.chat;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SecretChatRoomVerificationDto {
    private Long chatRoomId;
    private String password;
}
