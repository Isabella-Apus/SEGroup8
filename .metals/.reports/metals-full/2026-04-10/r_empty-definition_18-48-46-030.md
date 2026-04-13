error id: file:///C:/Users/29382/Desktop/VS%20code/software/SEGroup8/backend/src/main/java/com/segroup8/platform/entity/BrowseHistory.java:_empty_/TableId#
file:///C:/Users/29382/Desktop/VS%20code/software/SEGroup8/backend/src/main/java/com/segroup8/platform/entity/BrowseHistory.java
empty definition using pc, found symbol in pc: _empty_/TableId#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 441
uri: file:///C:/Users/29382/Desktop/VS%20code/software/SEGroup8/backend/src/main/java/com/segroup8/platform/entity/BrowseHistory.java
text:
```scala
package com.segroup8.platform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("browse_history")
public class BrowseHistory {
    @@@TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long productId;

    private LocalDateTime browseTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}

```


#### Short summary: 

empty definition using pc, found symbol in pc: _empty_/TableId#