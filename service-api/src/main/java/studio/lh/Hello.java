package studio.lh;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author :MayRain
 * @version :1.0
 * @date :2022/11/22 21:07
 * @description :
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Hello implements Serializable {
    private String message;
    private String description;
}
